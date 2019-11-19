/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.htr;

import com.achteck.misc.types.CharMap;
import com.achteck.misc.util.ArrayUtil;
import de.planet.imaging.types.HybridImage;
import de.planet.itrtech.reco.IImagePreProcess;
import de.planet.itrtech.reco.ISNetwork;
import de.planet.langmod.types.ILangMod;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.tensorflow.types.SNetworkTF;
import de.uros.citlab.module.interfaces.IHtrCITlab;
import de.uros.citlab.module.kws.ConfMatContainer;
import de.uros.citlab.module.la.BaselineGenerationHist;
import de.uros.citlab.module.train.TrainHtrPlus;
import de.uros.citlab.module.types.ErrorNotification;
import de.uros.citlab.module.types.HTR;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.types.LineImage;
import de.uros.citlab.module.util.*;
import eu.transkribus.core.model.beans.customtags.ReadingOrderTag;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.core.util.SysResourcesUtil;
import eu.transkribus.core.util.LogUtil.Level;
import eu.transkribus.interfaces.types.Image;
import eu.transkribus.interfaces.types.Image.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * @author gundram
 */
public class HTRParserPlus extends Observable implements IHtrCITlab {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(HTRParserPlus.class.getName());
    private transient final HashMap<String, ISNetwork> networks = new HashMap<>();
    private transient final HashMap<Integer, HTR> htrs = new HashMap<>();
    String nameCurrent = "?";
    private BaselineGenerationHist baselineGeneration = new BaselineGenerationHist();

    @Override
    public void notifyObservers(Object arg) {
        super.setChanged();
        super.notifyObservers(arg);
    }

    private ISNetwork getNetwork(String htrIn, String pathCharMap) {
        String key = htrIn + (pathCharMap == null || pathCharMap.isEmpty() ? "" : File.pathSeparator + pathCharMap);
        if (!networks.containsKey(key)) {
            File htrFolder = new File(htrIn);
            if (!htrFolder.isDirectory()) {
                throw new RuntimeException("cannot find folder '" + htrIn + "'");
            }
            //verify that Charmap is the same
            File fileOrigCM = TrainHtrPlus.getCharMap(htrFolder);
            if (pathCharMap != null && !pathCharMap.isEmpty()) {
                CharMap<Integer> origCM = CharMapUtil.loadCharMap(fileOrigCM);
                CharMap<Integer> newCM = CharMapUtil.loadCharMap(new File(pathCharMap));
                if (CharMapUtil.equals(newCM, origCM)) {
                    throw new RuntimeException("Charmap in '" + pathCharMap + "' differs from Charmap in '" + htrIn + "'.");
                }
            }
            IImagePreProcess preproc = TrainHtrPlus.loadPreProc(htrFolder);
            File frozenModel = getFrozenModel(htrFolder);
            SNetworkTF network = new SNetworkTF(
                    preproc,
                    frozenModel.getAbsolutePath(),
                    fileOrigCM.getAbsolutePath(),
                    getImgHeight(htrFolder));
            network.setParamSet(network.getDefaultParamSet(null));
            network.setGpuMode(false);
            network.init(true);

            networks.put(key, network);
        }
        return networks.get(key);
    }

    private static int getImgHeight(File htrFolder) {
        File export = new File(htrFolder, "export");
        if (!export.exists()) {
            throw new RuntimeException("cannot find folder " + export);
        }
        List<String> strings = FileUtil.readLines(new File(export, "netconfig.info"));
        if (strings.size() != 1) {
            throw new RuntimeException("expect file " + new File(export, "netconfig.info") + " to have only one line");
        }
        String line = strings.get(0);
        int idx = line.indexOf("inHeight");
        idx = line.indexOf("inImg", idx);
        int from = line.indexOf(":", idx);
        int to = line.indexOf("}", idx);
        return Integer.parseInt(line.substring(from + 1, to).trim());
    }

    private static File getFrozenModel(File htrFolder) {
        File export = new File(htrFolder, "export");
        if (!export.exists()) {
            throw new RuntimeException("cannot find folder " + export);
        }
        List<File> pb = FileUtil.listFiles(export, "pb", false);
        if (pb.size() != 1) {
            throw new RuntimeException("found " + pb.size() + "pb-files in folder " + export + " but expect 1");
        }
        return pb.get(0);
    }

    @Override
    public void process(String pathToOptivalModel, String pathToLanguageModels, String pathCharMap, Image image, String xmlInOut, String storageDir, String[] lineIds, String[] props) {
        PcGtsType loadXml = PageXmlUtil.unmarshal(new File(xmlInOut));
        process(pathToOptivalModel, pathToLanguageModels, pathCharMap, image, loadXml, storageDir, lineIds, props);
        PageXmlUtil.marshal(loadXml, new File(xmlInOut));
    }

    public HTR getHTR(String om, String lm, String cm, String storageDir, String[] props) {
        HTR htrDummy = new HTR(om, lm, cm);
        int hash = htrDummy.hashCode();
        if (!htrs.containsKey(hash)) {
            ISNetwork network = getNetwork(om, cm);
            ILangMod langMod = HTRParser.getLangMod(lm, network.getCharMap(), props);
            htrs.put(hash, new HTR(network, langMod, om, lm, cm, props));
        }
        HTR res = htrs.get(hash);
        if (storageDir == null || storageDir.isEmpty()) {
            res.setStorageFile(null);
        } else {
            String containerFileName = PropertyUtil.getProperty(props, Key.HTR_CONFMAT_CONTAINER_FILENAME, "");
            if (containerFileName == null || containerFileName.isEmpty()) {
                containerFileName = ConfMatContainer.CONFMAT_CONTAINER_FILENAME;
            }
            res.setStorageFile(new File(storageDir, containerFileName));
        }
        nameCurrent = res.getName();
        return res;
    }


    private static boolean useDict(String lm) {
        return lm != null && !lm.isEmpty();
    }

    private void addMissigBaselines(HybridImage hi, List<LineImage> linesExecution) {
        for (LineImage lineImage : linesExecution) {
            Polygon2DInt baseLine = lineImage.getBaseLine();
            if (baseLine == null) {
                baselineGeneration.processLine(hi, lineImage.getTextLine(), lineImage.getTextRegion());
                LOG.warn("for region {} and line {} no baseline found. {} is used to create default baseline from polygon.", lineImage.getTextRegion().getId(), lineImage.getTextLine().getId(), baselineGeneration.getClass().getName());
            }
        }
    }
    
    private String getPathFromImageOrPcGtsType(Image image, PcGtsType xmlFile) {
        String imgPath=xmlFile.getPage().getImageFilename();
        if (image.hasType(Type.URL)) {
        	URL url = image.getImageUrl();
        	if (url != null) {
        		imgPath = url.toString();
        	}
        }
    	return imgPath;
    }

    @Override
    public void process(String pathToOpticalModel, String pathToLanguageModel, String pathCharMap, Image image, PcGtsType xmlFile, String storageDir, String[] lineIds, String[] props) {
        HybridImage hi = ImageUtil.getHybridImage(image, true);
        List<TextRegionType> textRegions = PageXmlUtils.getTextRegions(xmlFile);
        List<LineImage> linesExecution = new LinkedList<>();
        for (TextRegionType textRegion : textRegions) {
            List<TextLineType> linesInRegion1 = textRegion.getTextLine();
            for (TextLineType textLineType : linesInRegion1) {
                if (lineIds == null || ArrayUtil.linearSearch(lineIds, textLineType.getId()) >= 0) {
                    try {
                        linesExecution.add(new LineImage(hi, textLineType, textRegion));
                    } catch (RuntimeException ex) {
                        LOG.info("in " + xmlFile.getPage().getImageFilename() + ", region " + textRegion.getId() + ", line " + textLineType.getId() + " generating LineImage fails.", ex);
                    }
                }
            }
        }
        if (linesExecution.isEmpty() && lineIds != null) {
            throw new RuntimeException("regions with ids " + Arrays.deepToString(lineIds) + " not found.");
        }
        addMissigBaselines(hi, linesExecution);
        HTR htr = getHTR(pathToOpticalModel, pathToLanguageModel, pathCharMap, storageDir, props);
        for (LineImage lineImage : linesExecution) {
            TextLineType textLine = lineImage.getTextLine();
            try {
                HTR.Result text = htr.getText(lineImage, props);
                String result = text.getText();
                PageXmlUtil.deleteCustomTags(textLine, ReadingOrderTag.TAG_NAME);
                PageXmlUtil.setTextEquiv(textLine, result);
                if (text.getTags() != null && !text.getTags().isEmpty()) {
                    notifyObservers(new ErrorNotification(xmlFile, lineImage.getTextLine().getId(), new RuntimeException("ignore tags " + text.getTags()), TrainHtrPlus.class));
                }
//            textEquivType.setConf(new Float(result.getSecond()));
                LOG.info("decoded '" + result + "' for textline " + textLine.getId() + " with" + ((PropertyUtil.isPropertyTrue(props, Key.RAW) || !useDict(pathToLanguageModel)) ? "out" : "") + " language model .");
            } catch (RuntimeException ex) {
                notifyObservers(new ErrorNotification(xmlFile, lineImage.getTextLine().getId(), ex, TrainHtrPlus.class));
//                PageXmlUtil.deleteCustomTags(textLine, ReadingOrderTag.TAG_NAME);
//                PageXmlUtil.setTextEquiv(textLine, "");
            } catch (OutOfMemoryError ex) {
            	String imgPath = getPathFromImageOrPcGtsType(image, xmlFile);
            	LOG.error("OutOfMemoryError image="+imgPath+" lineid="+textLine.getId());
            	SysResourcesUtil.logMemUsage(LOG, Level.ERROR, true);
            	System.gc();
                notifyObservers(new ErrorNotification(xmlFile, lineImage.getTextLine().getId(), ex, TrainHtrPlus.class));
//                PageXmlUtil.deleteCustomTags(textLine, ReadingOrderTag.TAG_NAME);
//                PageXmlUtil.setTextEquiv(textLine, "");
            }
        }
        htr.finalizePage();
        PageXmlUtil.copyTextEquivLine2Region(xmlFile);
        MetadataUtil.addMetadata(xmlFile, this);
    }

    @Override
    public String usage() {
        return "loads the model which is specified in the path. If one set the property 'raw'=>'true', the result is the raw net output.\n"
                + "To create a model the following parameters are avaiable:\n"
                + "'net': required. Path to the network\n"
                + "'dict': optional. Path to a dictinary (unigram)\n"
                + "'maxanz': optional, integer format,default = '10000'. Is only used if dictionary is set. Take only the 'maxanz' most frequency words for the dictionary\n"
                + "'free': optional. If free = 'true' the model does not stay into memory/cache.\n"
                + "When a model is created it stays in the memory";
    }

    @Override
    public String getToolName() {
        return nameCurrent;
    }

    @Override
    public String getProvider() {
        return MetadataUtil.getProvider("Gundram Leifert", "gundram.leifert@uni-rostock.de");
    }

    @Override
    public String getVersion() {
        return MetadataUtil.getSoftwareVersion();
    }

    //    @Override
    public void process(String pathToOpticalModel, String pathToLanguageModel, String pathToCharacterMap, Image image, String xmlInOut, String storageDir, String pathToAbbrDict, String[] lineIds, String[] props) {
        process(pathToOpticalModel, pathToLanguageModel, pathToCharacterMap, image, xmlInOut, storageDir, lineIds, props);
    }

}
