/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.htr;

import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.CharMap;
import com.achteck.misc.util.ArrayUtil;
import de.planet.citech.types.ISortingFunction;
import de.planet.imaging.types.HybridImage;
import de.planet.itrtech.reco.ISNetwork;
import de.planet.itrtech.types.IDictOccurrence;
import de.planet.langmod.LangMod;
import de.planet.langmod.LangModFullText;
import de.planet.langmod.types.ILangMod;
import de.planet.languagemodel.langmod.LanguageDecoderLangModAdapter;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.reco.types.SNetwork;
import de.planet.util.types.DictOccurrence;
import de.uros.citlab.module.interfaces.IHtrCITlab;
import de.uros.citlab.module.kws.ConfMatContainer;
import de.uros.citlab.module.la.BaselineGenerationHist;
import de.uros.citlab.module.train.TrainHtr;
import de.uros.citlab.module.types.HTR;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.types.LineImage;
import de.uros.citlab.module.util.*;
import eu.transkribus.core.model.beans.customtags.ReadingOrderTag;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.types.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author gundram
 */
public class HTRParser implements IHtrCITlab {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(HTRParser.class.getName());
    private transient final HashMap<String, ISNetwork> networks = new HashMap<>();
    private transient final HashMap<Integer, HTR> htrs = new HashMap<>();
    String nameCurrent = "?";
    private BaselineGenerationHist baselineGeneration = new BaselineGenerationHist();

    public static ILangMod getLangMod2(IDictOccurrence dict, CharMap<Integer> cm, String[] props) {
//so far a dictionary is a langmod - this changes later
        if (dict != null) {
            String[] subTreeProperty = PropertyUtil.getSubTreeProperty(props, "sf");
            ISortingFunction sf = new SortingFunctionDA();
            ParamSet psSF = sf.getDefaultParamSet(new ParamSet());
            for (int i = 0; subTreeProperty != null && i < subTreeProperty.length; i += 2) {
                String key = subTreeProperty[i];
                String value = subTreeProperty[i + 1];
                psSF.setParamFromString(key, value);
            }
            sf.setParamSet(psSF);
            sf.init();
            LangMod lm = new LangModFullText();
            lm.setParamSet(lm.getDefaultParamSet(new ParamSet()));
            lm.init();
            lm.setSortingFunction(sf);
            LangModConfigurator lmConfig = new LangModConfigurator(dict, cm, "dict");
            try {
                lmConfig.initLangMod(lm);
            } catch (LangModConfigurator.EmptyDictException ex) {
                LOG.warn("Error in initializing dictionary " + dict.getClass().getName() + ", use bestpath instead.", ex);
                return null;
            }
            return lm;
        }
        return null;
    }

    public static ILangMod getLangMod(String pathLanguageModel, CharMap<Integer> cm, String[] props) {
//so far a dictionary is a langmod - this changes later
        if (useDict(pathLanguageModel)) {
            if (pathLanguageModel.endsWith(".bin")) {
                //use better version of LM
//                ILM langModImpl = new LMBerkleyChar(pathLanguageModel);
//                langModImpl.setParamSet(langModImpl.getDefaultParamSet(null));
//                langModImpl.init();
                ILangMod res = new LanguageDecoderLangModAdapter(pathLanguageModel,
                        "@",
                        1,
                        -1.5,
                        0.0);
                return res;
            }
            String[] subTreeProperty = PropertyUtil.getSubTreeProperty(props, "sf");
            ISortingFunction sf = new SortingFunctionDA();
            ParamSet psSF = sf.getDefaultParamSet(new ParamSet());
            for (int i = 0; subTreeProperty != null && i < subTreeProperty.length; i += 2) {
                String key = subTreeProperty[i];
                String value = subTreeProperty[i + 1];
                psSF.setParamFromString(key, value);
            }
            sf.setParamSet(psSF);
            sf.init();
            LangMod lm = new LangModFullText();
            lm.setParamSet(lm.getDefaultParamSet(new ParamSet()));
            lm.init();
            IDictOccurrence ido;
            {
                String propMaxLength = PropertyUtil.getProperty(props, Key.MAX_ANZ, "-1");
                if (pathLanguageModel.endsWith(".csv") || pathLanguageModel.endsWith(".txt") || pathLanguageModel.endsWith(".dict")) {
                    try {
                        ido = new DictOccurrence(pathLanguageModel, ",", 1, 0, false);
                        ParamSet defaultParamSet = ido.getDefaultParamSet(new ParamSet());
                        defaultParamSet.getParam(DictOccurrence.P_MAXANZ).set(Integer.parseInt(propMaxLength));
                        ido.setParamSet(defaultParamSet);
                        ido.init();
                    } catch (RuntimeException ex) {
                        LOG.info("cannot load dict without header and with , as seperator - assume header and ; as selerator", ex);
                        ido = new DictOccurrence(pathLanguageModel, ";", 1, 0, true);
                        ParamSet defaultParamSet = ido.getDefaultParamSet(new ParamSet());
                        defaultParamSet.getParam(DictOccurrence.P_MAXANZ).set(Integer.parseInt(propMaxLength));
                        ido.setParamSet(defaultParamSet);
                        ido.init();
                    }
                } else if (pathLanguageModel.endsWith(".arpa")) {
                    ido = new DictOccurenceArpa(pathLanguageModel, Integer.parseInt(propMaxLength));
                    ido.setParamSet(ido.getDefaultParamSet(new ParamSet()));
                    ido.init();
                } else {
                    throw new RuntimeException("cannot load " + pathLanguageModel + ": unknown suffix (.csv, .txt, .dict and .arpa allowed).");
                }
            }
            lm.setSortingFunction(sf);
            LangModConfigurator lmConfig = new LangModConfigurator(ido, cm, "dict");
            try {
                lmConfig.initLangMod(lm);
            } catch (LangModConfigurator.EmptyDictException ex) {
                LOG.warn("Error in initializing dictionary " + pathLanguageModel + ", use bestpath instead.", ex);
                return null;
            }
            return lm;
        }
        return null;
    }

    private ISNetwork getNetwork(String pathOpticalModel, String pathCharMap) {
        String key = pathOpticalModel + (pathCharMap == null || pathCharMap.isEmpty() ? "" : File.pathSeparator + pathCharMap);
        if (!networks.containsKey(key)) {
            File folder = new File(pathOpticalModel);
            //create model
            ISNetwork load = (ISNetwork) IOUtil.load(TrainHtr.getNetBestOrLast(folder));
            load.setParamSet(load.getDefaultParamSet(new ParamSet()));
            load.init();
            if (pathCharMap != null && !pathCharMap.isEmpty()) {
                CharMap<Integer> loadCharMap = CharMapUtil.loadCharMap(new File(pathCharMap));
                CharMap<Integer> charMap = load.getCharMap();
                if (CharMapUtil.equals(charMap, loadCharMap)) {
                    LOG.debug("charmaps are equal - do not change charmap");
                } else {
                    if (!(load instanceof SNetwork)) {
                        throw new RuntimeException("loaded network '" + pathOpticalModel + "' have to be of instance " + SNetwork.class.getName() + ".");
                    }
                    CharMapUtil.setCharMap((SNetwork) load, loadCharMap, -4);
                }
            }
            networks.put(key, load);
        }
        return networks.get(key);
    }

    @Override
    public void process(String pathToOptivalModel, String pathToLanguageModels, String pathCharMap, Image image, String xmlInOut, String storageDir, String[] lineIds, String[] props) {
        PcGtsType loadXml = PageXmlUtil.unmarshal(new File(xmlInOut));
        process(pathToOptivalModel, pathToLanguageModels, pathCharMap, image, loadXml, storageDir, lineIds, props);
        PageXmlUtil.marshal(loadXml, new File(xmlInOut));
    }

    protected HTR getHTR(String om, String lm, String cm, String storageDir, String[] props) {
        HTR htrDummy = new HTR(om, lm, cm);
        int hash = htrDummy.hashCode();
        if (!htrs.containsKey(hash)) {
            ISNetwork network = getNetwork(om, cm);
            ILangMod langMod = getLangMod(lm, network.getCharMap(), props);
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
            HTR.Result text = htr.getText(lineImage, props);
            String result = text.getText();
            PageXmlUtil.deleteCustomTags(textLine, ReadingOrderTag.TAG_NAME);
            PageXmlUtil.setTextEquiv(textLine, result);
            if (text.getTags() != null && !text.getTags().isEmpty()) {
                LOG.error("in line {} ignore tags {}", textLine.getId(), text.getTags());
            }
//            textEquivType.setConf(new Float(result.getSecond()));
            LOG.info("decoded '" + result + "' for textline " + textLine.getId() + " with" + ((PropertyUtil.isPropertyTrue(props, Key.RAW) || !useDict(pathToLanguageModel)) ? "out" : "") + " language model .");
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
