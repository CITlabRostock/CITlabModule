/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.text2image;

import com.achteck.misc.types.ConfMat;
import de.planet.imaging.types.HybridImage;
import de.uros.citlab.confmat.CharMap;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.module.htr.HTRParser;
import de.uros.citlab.module.types.HTR;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.types.LineImage;
import de.uros.citlab.module.types.PageStruct;
import de.uros.citlab.module.util.*;
import de.uros.citlab.textalignment.HyphenationProperty;
import de.uros.citlab.textalignment.TextAligner;
import de.uros.citlab.textalignment.types.LineMatch;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.IText2Image;
import eu.transkribus.interfaces.types.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

/**
 * @author gundram
 */
public class Text2ImageParser extends HTRParser implements IText2Image {

    public static Logger LOG = LoggerFactory.getLogger(Text2ImageParser.class.getName());
    private ObjectCounter<Stat> oc = new ObjectCounter<>();

    @Override
    public String getToolName() {
        return "Matcher(" + super.getToolName() + ")";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    public enum Stat {
        REF, RECO, MATCH
    }

    public ObjectCounter<Stat> getStat() {
        return oc;
    }

    public void resetStat() {
        oc.reset();
    }

    private List<String> loadReferencePrepareBidi(File inFile) {
        List<String> in = FileUtil.readLines(inFile);
        deleteEmptyEntries(in);
        for (int i = 0; i < in.size(); i++) {
            in.set(i, BidiUtil.logical2visual(in.get(i)));
        }
        return in;
    }

    @Override
    public void matchImage(String pathToOpticalModel, String pathToLangugageResource, String pathCharMap, String txtIn, Image image, String xmlInOut, String[] region_ids, String[] props) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static void deleteEmptyEntries(List<String> lst) {
        for (int i = 0; i < lst.size(); i++) {
            String str = lst.get(i).replace("\n", "").replace("\r", "");
            if (str.isEmpty()) {
                lst.remove(i--);
            } else {
                lst.set(i, str);
            }
        }
    }

    @Override
    public void matchCollection(String pathToOpticalModel, String pathToLanguageModel, String pathToCharacterMap, String pathToText, String[] images, String[] xmlInOut, String[] props) {
        matchCollection(pathToOpticalModel, pathToLanguageModel, pathToCharacterMap, pathToText, images, xmlInOut, null, props);
    }

    private static de.uros.citlab.confmat.ConfMat convert(ConfMat cm) {
        CharMap cmNew = new CharMap();
        com.achteck.misc.types.CharMap<Integer> charMap = cm.getCharMap();
        int size = charMap.keySet().size();
        for (int i = 1; i < size; i++) {
            cmNew.add(charMap.get(i));
        }
        return new de.uros.citlab.confmat.ConfMat(cmNew, cm.getDoubleMat());
    }

    private static List<de.uros.citlab.confmat.ConfMat> convert(List<ConfMat> cms) {
        List<de.uros.citlab.confmat.ConfMat> res = new ArrayList<>(cms.size());
        for (int i = 0; i < cms.size(); i++) {
            res.add(convert(cms.get(i)));
        }
        return res;
    }

    public void matchCollection(String pathToOpticalModel, String pathToLanguageModel, String pathToCharacterMap, String pathToText, String[] images, String[] xmlInOut, String[] storages, String[] props) {
        LOG.info("process xmls {}...", Arrays.asList(images));
        List<PageStruct> pages = PageXmlUtil.getPages(images, xmlInOut);
        List<String> in = loadReferencePrepareBidi(new File(pathToText));
        final List<LineImage> linesExecution = new LinkedList<>();
        final List<PageStruct> pageExecution = new LinkedList<>();
        final List<ConfMat> confMats = new LinkedList<>();
        final Map<LineImage, HybridImage> lineMap = new LinkedHashMap<>();
        Set<HybridImage> freeImages = new LinkedHashSet<>();
        for (int i = 0; i < pages.size(); i++) {
            PageStruct page = pages.get(i);
            String storage = storages != null ? storages[i] : null;
            HybridImage hi = ImageUtil.getHybridImage(page.getImg(), true);
            List<TextRegionType> textRegions = PageXmlUtils.getTextRegions(page.getXml());
            HTR htr = getHTR(pathToOpticalModel, pathToLanguageModel, pathToCharacterMap, storage, props);
            for (TextRegionType textRegion : textRegions) {
                List<TextLineType> linesInRegion1 = textRegion.getTextLine();
                for (TextLineType textLineType : linesInRegion1) {
                    LineImage lineImage = new LineImage(hi, textLineType, textRegion);
                    lineImage.deleteTextEquiv();
//                Display.addPanel(new DisplayPlanet(subImage),lineImage.getTextLine().getId());
                    ConfMat confMat = htr.getConfMat(lineImage, props);
                    linesExecution.add(lineImage);
                    pageExecution.add(page);
                    lineMap.put(lineImage, hi);
                    confMats.add(confMat);
                }
            }
            freeImages.add(hi);
            htr.finalizePage();
        }
        if (confMats.isEmpty()) {
            LOG.warn("nothing to match for xmls {} because no textlines found.", Arrays.asList(images));
            for (PageStruct page : pages) {
                MetadataUtil.addMetadata(page.getXml(), this);
                page.saveXml();
            }
            return;
        }
        Double costSkipWords = PropertyUtil.hasProperty(props, Key.T2I_SKIP_WORD) ? Double.parseDouble(PropertyUtil.getProperty(props, Key.T2I_SKIP_WORD)) : null;
        Double costSkipBaseline = PropertyUtil.hasProperty(props, Key.T2I_SKIP_BASELINE) ? Double.parseDouble(PropertyUtil.getProperty(props, Key.T2I_SKIP_BASELINE)) : null;
        Double costJumpBaseline = PropertyUtil.hasProperty(props, Key.T2I_JUMP_BASELINE) ? Double.parseDouble(PropertyUtil.getProperty(props, Key.T2I_JUMP_BASELINE)) : null;
        int maxCount = PropertyUtil.hasProperty(props, Key.T2I_MAX_COUNT) ? Integer.parseInt(PropertyUtil.getProperty(props, Key.T2I_MAX_COUNT)) : -1;
        double threshold = Double.parseDouble(PropertyUtil.getProperty(props, Key.T2I_THRESH, "0.0"));
        double nacOffset = -2.0;
        String lbChars = " ";
        TextAligner textAligner = new TextAligner(
                lbChars,
                costSkipWords, //4.0
                costSkipBaseline, //0.2
                costJumpBaseline
        );
        textAligner.setNacOffset(-2.0);
        if (maxCount > 0) {
            textAligner.setMaxVertexCount(maxCount);
        }
        if (PropertyUtil.hasProperty(props, Key.T2I_BEST_PATHES)) {
            throw new RuntimeException("path filter not implemented so far");
        }
        String property = PropertyUtil.getProperty(props, Key.T2I_HYPHEN);
        String lang = PropertyUtil.getProperty(props, Key.T2I_HYPHEN_LANG);
        textAligner.setHp(HyphenationProperty.newInstance(property, lang));
//        String ref = sb.toString();
//        ref = ref.trim();
        List<de.uros.citlab.confmat.ConfMat> confMatsConverted = convert(confMats);
        List<LineMatch> alignmentResult = textAligner.getAlignmentResult(in, confMatsConverted);
        if (alignmentResult != null) {
            for (LineMatch match : alignmentResult) {
                LineImage lineImage = linesExecution.get(confMatsConverted.indexOf(match.getCm()));
                PageXmlUtil.setTextEquiv(lineImage.getTextLine(), match.getReference(), match.getConfidence());
            }
        }
        for (PageStruct page : pages) {
            MetadataUtil.addMetadata(page.getXml(), this);
            page.saveXml();
        }
        if (PropertyUtil.isPropertyTrue(props, Key.DEBUG)) {
            try {
                double threshBaseline = Double.parseDouble(PropertyUtil.getProperty(props, Key.T2I_THRESH, "0.0"));
                for (PageStruct page : pages) {
                    File folder = PropertyUtil.hasProperty(props, Key.DEBUG_DIR)
                            ? new File(PropertyUtil.getProperty(props, Key.DEBUG_DIR))
                            : page.getPathXml().getParentFile();
                    folder.mkdirs();
                    {
                        BufferedImage debugImage = ImageUtil.getDebugImage(page.getImg().getImageBufferedImage(true), page.getXml(), 1.0, false, true, threshBaseline, true, false);
                        debugImage = ImageUtil.resize(debugImage, 6000, debugImage.getHeight() * 6000 / debugImage.getWidth());
                        String imageFilename = page.getXml().getPage().getImageFilename();
                        imageFilename += ".jpg";
                        ImageIO.write(debugImage, "jpg", new File(folder, imageFilename));
                    }
                }
            } catch (Throwable ex) {
                LOG.warn("writing debug images fails", ex);
            }
        }
    }

    @Override
    public void matchRegions(String pathToOpticalModel, String pathToLanguageModel, String pathToCharacterMap, String[] pathToText, String images, String xmlInOut, String[] region_ids, String[] props) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
