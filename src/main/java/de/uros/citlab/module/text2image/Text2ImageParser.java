/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.text2image;

import com.achteck.misc.types.ConfMat;
import de.planet.imaging.types.HybridImage;
import de.planet.math.geom2d.types.Rectangle2DInt;
import de.uros.citlab.confmat.CharMap;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.module.htr.HTRParser;
import de.uros.citlab.module.types.*;
import de.uros.citlab.module.util.*;
import de.uros.citlab.textalignment.BestPathPart;
import de.uros.citlab.textalignment.DynProg;
import de.uros.citlab.textalignment.HyphenationProperty;
import de.uros.citlab.textalignment.TextAligner;
import de.uros.citlab.textalignment.types.ConfMatCollection;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.IText2Image;
import eu.transkribus.interfaces.types.Image;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

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

    private static de.uros.citlab.confmat.ConfMat convert(ConfMat cm){
        CharMap cmNew = new CharMap();
        com.achteck.misc.types.CharMap<Integer> charMap = cm.getCharMap();
        int size = charMap.keySet().size();
        for (int i =1; i < size; i++) {
            cmNew.add(charMap.get(i));
        }
        return new de.uros.citlab.confmat.ConfMat(cmNew,cm.getDoubleMat());
    }
    private static List<de.uros.citlab.confmat.ConfMat> convert(List<ConfMat> cms){
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
        boolean appendFinalReturn = true;
        double nacOffset = -2.0;
        String lbChars = " ";
//                new ConfMatCollection(1.0, confMats, appendFinalReturn, nacOffset);
//        CharMap charMapCollection = cmc.getCharMap();
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < in.size(); i++) {
//            sb.append(in.get(i).trim());
//            if (i < in.size() - 1) {
//                sb.append('\n');
//            } else {
////                sb.append('\n');
//                sb.append(lbChars.charAt(0));
//            }
//        }
        TextAligner dp = new TextAligner(lbChars, costSkipWords/*4.0*/, /*0.2*/ costSkipBaseline, costJumpBaseline, nacOffset, maxCount);
        if (PropertyUtil.hasProperty(props, Key.T2I_BEST_PATHES)) {
            throw new RuntimeException("path filter not implemented so far");
//            try {
//                Integer numPathes = Integer.parseInt(PropertyUtil.getProperty(props, Key.T2I_BEST_PATHES));
//                dp.setMaxPathes(numPathes, Mode.VERTICAL);
//            } catch (NumberFormatException ex) {
//            try {
//                Double offset = Double.parseDouble(PropertyUtil.getProperty(props, Key.T2I_BEST_PATHES));
//                dp.setMaxPathes(offset, Mode.VERTICAL);
//            } catch (NumberFormatException ex2) {
//                throw new NumberFormatException("Input string \"" + PropertyUtil.getProperty(props, Key.T2I_BEST_PATHES) + "\" cannot be parsed to integer or double.");
//            }
//            }
        }
        String property = PropertyUtil.getProperty(props, Key.T2I_HYPHEN);
        String lang = PropertyUtil.getProperty(props, Key.T2I_HYPHEN_LANG);
        dp.setHp(HyphenationProperty.newInstance(property, lang));
//        String ref = sb.toString();
//        ref = ref.trim();
        dp.setRecognition(confMats);
        dp.setReference(in);
        List<PathCalculatorGraph.IDistance<DynProg.ConfMatVector, DynProg.NormalizedCharacter>> bestPath = dp.getBestPath();
        oc.add(Stat.REF, in.size());
        oc.add(Stat.RECO, confMats.size());
        boolean partsArePossible = false;

        if (bestPath != null) {
            if (LOG.isDebugEnabled()) {
                for (PathCalculatorGraph.IDistance<DynProg.ConfMatVector, DynProg.NormalizedCharacter> distance : bestPath) {
                    LOG.debug(distance.toString());
                }
            }
            List<BestPathPart> grouping = GroupUtil.getGrouping(bestPath, new GroupUtil.Joiner<PathCalculatorGraph.IDistance<DynProg.ConfMatVector, DynProg.NormalizedCharacter>>() {
                @Override
                public boolean isGroup(List<PathCalculatorGraph.IDistance<DynProg.ConfMatVector, DynProg.NormalizedCharacter>> group, PathCalculatorGraph.IDistance<DynProg.ConfMatVector, DynProg.NormalizedCharacter> element) {
                    return keepElement(element);
                }

                @Override
                public boolean keepElement(PathCalculatorGraph.IDistance<DynProg.ConfMatVector, DynProg.NormalizedCharacter> element) {
                    if (element.getManipulation().startsWith("SKIP_")) {
                        return false;
                    }
                    DynProg.ConfMatVector[] recos = element.getRecos();
                    if (recos == null || recos.length == 0) {
                        return false;
                    }
                    int returnIndex = cmc.getCharMap().get(CharMap.Return);
                    for (DynProg.ConfMatVector cmVec : recos) {
                        if (cmVec.maxIndex == returnIndex) {
                            if (recos.length != 1) {
                                throw new RuntimeException("unexpected length");
                            }
                            return false;
                        }
                    }
                    return true;
                }
            }, new GroupUtil.Mapper<PathCalculatorGraph.IDistance<DynProg.ConfMatVector, DynProg.NormalizedCharacter>, BestPathPart>() {
                @Override
                public BestPathPart map(List<PathCalculatorGraph.IDistance<DynProg.ConfMatVector, DynProg.NormalizedCharacter>> elements) {
                    return BestPathPart.newInstance(elements);
                }
            });
            List<Pair<BestPathPart, BestPathPart>> parts = new LinkedList<>();
            BestPathPart start = null;
            BestPathPart last = null;
            PageStruct current = null;
            for (BestPathPart cmp : grouping) {
                int index = cmc.getOrigIdx(cmp.startReco).first;
                ConfMat cmOld = confMats.get(index);
                LineImage lineImage = linesExecution.get(index);
                if (start == null) {
                    start = cmp;
                    current = pageExecution.get(index);
                }
                ConfMat cmNew = new ConfMat(charMapCollection);
                cmNew.copyFrom(cmp.orig);
                if (cmOld.getLength() != cmNew.getLength()) {
                    throw new RuntimeException("confmats differ in length");
                }
                if (nacOffset == 0.0 && !cmOld.getBestPath().equals(cmNew.getBestPath())) {
                    throw new RuntimeException("error in reconstructing confmat\n" + cmOld.getBestPath().replace(ConfMat.NaC, '*') + "\n" + cmNew.getBestPath().replace(ConfMat.NaC, '*'));
                }
                double conf = cmp.getConf();
                if (conf > threshold) {
                    oc.add(Stat.MATCH);
                    PageXmlUtil.setTextEquiv(lineImage.getTextLine(), cmp.reference, conf);
                }
                if (current != pageExecution.get(index)) {
                    parts.add(new Pair<>(start, last));
                    start = cmp;
                    current = pageExecution.get(index);
                }
                last = cmp;
            }
            if (start != null) {
                parts.add(new Pair<>(start, last));
            }
            if (PropertyUtil.isPropertyTrue(props, Key.DEBUG)) {
                for (Pair<BestPathPart, BestPathPart> pair : parts) {
                    BestPathPart cmp1 = pair.getFirst();
                    BestPathPart cmp2 = pair.getSecond();
                    PageStruct page1 = pageExecution.get(cmc.getOrigIdx(cmp1.startReco).first);
                    PageStruct page2 = pageExecution.get(cmc.getOrigIdx(cmp2.startReco).first);
                    if (page1 != page2) {
                        LOG.error("saving debug images produces an error - different pages.");
                    }
                    File folder = PropertyUtil.hasProperty(props, Key.DEBUG_DIR)
                            ? new File(PropertyUtil.getProperty(props, Key.DEBUG_DIR))
                            : page1.getPathXml().getParentFile();
                    folder.mkdirs();
                    partsArePossible = true;
                    if (images.length == 1) {
                        dp.getDistMatImage(true).save(new File(folder, page1.getXml().getPage().getImageFilename().replaceAll("\\.[a-zA-Z]{3}", "_dynProg.png")).getAbsolutePath());
                    } else {
                        dp.getDistMatImage(true, new Rectangle2DInt(cmp1.startRef, cmp1.startReco, cmp2.endRef - cmp1.startRef, cmp2.endReco - cmp1.startReco)).save(new File(folder, page1.getXml().getPage().getImageFilename().replaceAll("\\.[a-zA-Z]{3}", "_dynProg.png")).getAbsolutePath());
                    }
                }
            }

            for (PageStruct page : pages) {
                PageXmlUtil.copyTextEquivLine2Region(page.getXml());
            }
        } else if (PropertyUtil.isPropertyTrue(props, Key.DEBUG)) {
            File folder = PropertyUtil.hasProperty(props, Key.DEBUG_DIR)
                    ? new File(PropertyUtil.getProperty(props, Key.DEBUG_DIR))
                    : pageExecution.get(0).getPathXml().getParentFile();
            folder.mkdirs();
            dp.getDistMatImage(true).save(new File(folder, pageExecution.get(0).getXml().getPage().getImageFilename().replaceAll("\\.[a-zA-Z]{3}", "_dynProg.png")).getAbsolutePath());
        }
        if (!partsArePossible) {
            File folder = PropertyUtil.hasProperty(props, Key.DEBUG_DIR)
                    ? new File(PropertyUtil.getProperty(props, Key.DEBUG_DIR))
                    : pageExecution.get(0).getPathXml().getParentFile();
            dp.getDistMatImage(false).save(new File(folder, "all_dynProg.jpg").getAbsolutePath());
        }
        LOG.info("Statistic: {}", oc);
        for (PageStruct page : pages) {
            MetadataUtil.addMetadata(page.getXml(), this);
            page.saveXml();
        }
        if (PropertyUtil.isPropertyTrue(props, Key.DEBUG)) {
            double threshBaseline = Double.parseDouble(PropertyUtil.getProperty(props, Key.T2I_THRESH, "0.0"));
            for (PageStruct page : pages) {
                File folder = PropertyUtil.hasProperty(props, Key.DEBUG_DIR)
                        ? new File(PropertyUtil.getProperty(props, Key.DEBUG_DIR))
                        : page.getPathXml().getParentFile();
                folder.mkdirs();
                {
                    BufferedImage debugImage = ImageUtil.getDebugImage(page.getImg().getImageBufferedImage(true), page.getXml(), 1.0, false, true, threshBaseline, true, false);
                    debugImage = ImageUtil.resize(debugImage, 6000, debugImage.getHeight() * 6000 / debugImage.getWidth());
                    try {
                        String imageFilename = page.getXml().getPage().getImageFilename();
                        imageFilename += ".jpg";
                        ImageIO.write(debugImage, "jpg", new File(folder, imageFilename));
                    } catch (IOException ex) {
                        java.util.logging.Logger.getLogger(Text2ImageParser.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    @Override
    public void matchRegions(String pathToOpticalModel, String pathToLanguageModel, String pathToCharacterMap, String[] pathToText, String images, String xmlInOut, String[] region_ids, String[] props) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
