/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import de.planet.citech.trainer.loader.IImageLoader;
import de.planet.imaging.types.HybridImage;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.trafo.ITransform;
import de.planet.math.util.PolygonHelper;
import de.planet.util.LoaderIO;
import de.uros.citlab.errorrate.normalizer.StringNormalizerDft;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.types.LineImage;
import de.uros.citlab.module.types.PageStruct;
import de.uros.citlab.tokenizer.TokenizerCategorizer;
import de.uros.citlab.tokenizer.categorizer.CategorizerWordMergeGroups;
import de.uros.citlab.tokenizer.interfaces.ICategorizer;
import eu.transkribus.core.model.beans.pagecontent.*;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.IStringNormalizer;
import eu.transkribus.interfaces.ITokenizer;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.*;

/**
 * @author gundram
 */
public class TrainDataUtil {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(TrainDataUtil.class);
    public static final String KEY_SRC_FOLDER = "src_folder";
    public static final String cmLong = "charStat.txt";
    public static final String dict = "dict.csv";
    public static final String dictArpa = "dict.arpa";
    public static final String lr = "lr.txt";

    public static class Statistic {

        final private ObjectCounter<Character> statChar = new ObjectCounter<>();
        final private ObjectCounter<String> statWord;
        final private IStringNormalizer normalizer;
        final private ITokenizer tokenizer;
        final private ICategorizer categorizer;
        final private boolean onlyLetter;
        final private LinkedList<String> languageResource;

        public Statistic(IStringNormalizer normalizer, ICategorizer categorizer, boolean onlyLetter, boolean languageResource) {
            this.onlyLetter = onlyLetter;
            this.normalizer = normalizer;
            this.categorizer = categorizer;
            this.statWord = categorizer != null ? new ObjectCounter<String>() : null;
            this.tokenizer = categorizer != null ? new TokenizerCategorizer(categorizer) : null;
            this.languageResource = languageResource ? new LinkedList<>() : null;
        }

        public ObjectCounter<String> getStatWord() {
            return statWord;
        }

        public ObjectCounter<Character> getStatChar() {
            return statChar;
        }

        public void addStatistic(Statistic statistic) {
            statChar.addAll(statistic.getStatChar());
            if (statWord != null) {
                ObjectCounter<String> statWord1 = statistic.getStatWord();
                if (statWord1 != null) {
                    statWord.addAll(statWord1);
                }
            }
        }

        public List<String> getLanguageResource() {
            return languageResource;
        }

        private void addLine(String line) {
            if (languageResource != null) {
                languageResource.add(line == null ? "" : line);
            }
            if (line == null || line.isEmpty()) {
                return;
            }
            if (normalizer != null) {
                line = normalizer.normalize(line);
            }
            for (char c : line.toCharArray()) {
                statChar.add(c);
            }
            if (tokenizer != null) {
                for (String string : tokenizer.tokenize(line)) {
                    if (onlyLetter) {
                        if (categorizer.getCategory(string.charAt(0)).equals("L")) {
                            statWord.add(string);
                        }
                    } else {
                        statWord.add(string);
                    }
                }
            }
        }

    }

    public static void createTrainData(String[] pageXmls, String outputDir, String pathToCharMap, String[] props) {
        boolean saveCharMap = pathToCharMap != null && !pathToCharMap.isEmpty();
        String[] propsTraindata = props;
        if (saveCharMap) {
            propsTraindata = PropertyUtil.setProperty(props, Key.STATISTIC, "true");
        }
        TrainDataUtil.Statistic statistic = createTrainData(pageXmls, outputDir, propsTraindata);
        if (PropertyUtil.isPropertyTrue(props, Key.STATISTIC)) {
            TrainDataUtil.saveCharacterStatistic(statistic.getStatChar(), new File(outputDir, cmLong));
        }
        if (statistic != null) {
            if (statistic.getStatWord() != null) {
                TrainDataUtil.savePDict(statistic.getStatWord(), new File(outputDir, dict));
                TrainDataUtil.saveArpa(statistic.getStatWord(), new File(outputDir, dictArpa));
            }
            if (saveCharMap) {
                CharMapUtil.saveCharMap(statistic.getStatChar(), new File(pathToCharMap));
            }
        }
    }

    public static int runCreateTraindata(File folderPageXml, File folderSnipets, File charMap, String[] props) {
        Collection<File> listFiles = FileUtil.listFiles(folderPageXml, "xml", true);
        FileUtil.deleteMetadataAndMetsFiles(listFiles);
        String[] names = FileUtil.asStringList(listFiles);
        props = PropertyUtil.setProperty(props, KEY_SRC_FOLDER, folderPageXml.getAbsolutePath());
        createTrainData(names, folderSnipets == null ? null : folderSnipets.getAbsolutePath(), charMap == null ? null : charMap.getAbsolutePath(), props);
        return names.length;
    }

    private static String getUniqueIdOld(PcGtsType page) {
        String res = page.getPage().getImageFilename();
        int lastIndexOf = res.lastIndexOf(".");
        return lastIndexOf < 0 ? res : res.substring(0, lastIndexOf);

    }

    private static String getUniqueId(PcGtsType page) {
        MetadataType metadata = page.getMetadata();
        if (metadata == null) {
            return getUniqueIdOld(page);
        }
        TranskribusMetadataType dat = metadata.getTranskribusMetadata();
        if (dat == null) {
            return getUniqueIdOld(page);
        }
        return String.format("%05d_%04d_%06d", dat.getDocId(), dat.getPageNr(), dat.getPageId());

    }

    private static Boolean isValidStatus(String[] statuses, PcGtsType page) {
        MetadataType metadata = page.getMetadata();
        if (metadata == null) {
            return null;
        }
        TranskribusMetadataType transkribusMetadata = metadata.getTranskribusMetadata();
        if (transkribusMetadata == null) {
            return null;
        }
        String status = transkribusMetadata.getStatus().toUpperCase();
        if (status == null || status.isEmpty()) {
            return null;
        }
        for (String statuse : statuses) {
            if (statuse.equals(status)) {
                return true;
            }
        }
        return false;
    }

    public static Statistic getStatistic(String[] filesPageXMLorTXT, String[] props) {
        LinkedList<File> list = new LinkedList<>();
        for (String string : filesPageXMLorTXT) {
            list.add(new File(string));
        }
        return getStatistic(list, props);
    }

    public static Statistic getStatistic(List<File> filesPageXMLorTXT, String[] props) {
        IStringNormalizer sn = null;
        String formValue = PropertyUtil.getProperty(props, Key.NORMALIZE_FORM);
        if (formValue != null) {
            sn = new StringNormalizerDft(Normalizer.Form.valueOf(formValue.toUpperCase()), false);
            LOG.debug("uses Stringnormalizer with form " + formValue.toUpperCase() + ".");
        }
        ICategorizer cat = PropertyUtil.isPropertyTrue(props, Key.CREATEDICT) ? new CategorizerWordMergeGroups() : null;
        Statistic stat = new Statistic(sn, cat, true, PropertyUtil.hasProperty(props, Key.CREATE_LR));
        for (int i = 0; i < filesPageXMLorTXT.size(); i++) {
            File txtFile = filesPageXMLorTXT.get(i);
            List<String> text = txtFile.getName().endsWith(".xml") ? PageXmlUtil.getText(PageXmlUtil.unmarshal(txtFile)) : FileUtil.readLines(txtFile);
            for (String readLine : text) {
                stat.addLine(readLine);
            }
        }
        return stat;
    }

    public static Statistic createTrainData(String[] pageXmls, String outputDir, String[] props) {
//        ObjectCounter<Character> statChar = (PropertyUtil.isPropertyTrue(props, Key.STATISTIC) || createCharacterStatistic) ? new ObjectCounter<Character>() : null;
//        ObjectCounter<String> statWord = PropertyUtil.isPropertyTrue(props, Key.CREATEDICT) ? new ObjectCounter<String>() : null;
        IStringNormalizer sn = null;
        String formValue = PropertyUtil.getProperty(props, Key.NORMALIZE_FORM);
        if (formValue != null) {
            sn = new StringNormalizerDft(Normalizer.Form.valueOf(formValue.toUpperCase()), false);
            LOG.debug("uses Stringnormalizer with form " + formValue.toUpperCase() + ".");
        }
        boolean calcLR = PropertyUtil.hasProperty(props, Key.CREATE_LR);
        Statistic stat = PropertyUtil.isPropertyTrue(props, Key.CREATEDICT) || PropertyUtil.isPropertyTrue(props, Key.STATISTIC) || calcLR ?
                new Statistic(sn, new CategorizerWordMergeGroups(), true, calcLR) : null;
        final boolean saveTrainData = !PropertyUtil.isPropertyFalse(props, Key.CREATETRAINDATA);
        double minConf = Double.parseDouble(PropertyUtil.getProperty(props, Key.MIN_CONF, "0.0"));
        String[] statuses = PropertyUtil.hasProperty(props, Key.TRAIN_STATUS) ? PropertyUtil.getProperty(props, Key.TRAIN_STATUS).toUpperCase().split(";") : null;
//        String[] imgList = StringIO.loadLineArray(img);
//        if (imgList.length != xmlList.length) {
//            throw new RuntimeException("size of image-list and xml-list different");
//        }
        if (outputDir.isEmpty()) {
            throw new RuntimeException("outputDir is empty");
        }
        File fileFolderOut = new File(outputDir);
        File folderOut = new File(outputDir);
        if (folderOut.exists() && folderOut.listFiles().length > 0) {
            LOG.warn("folder " + folderOut.getAbsolutePath() + " already exists with " + folderOut.listFiles().length + " files - add data in this folder.");
        }
        folderOut.mkdirs();
        for (int i = 0; i < pageXmls.length; i++) {
            String xmlFilePath = pageXmls[i];
            File fileXml = new File(xmlFilePath);
            PageStruct page = new PageStruct(fileXml);
            try {
                PcGtsType pageType = page.getXml();
                if (statuses != null) {
                    Boolean gt = isValidStatus(statuses, pageType);
                    if (gt == null || !gt) {
                        LOG.warn("skip page '" + xmlFilePath + "' because status is not Transkribus status is not  'GT'.");
                        continue;
                    }
                }
                String imageFilename = pageType.getPage().getImageFilename();
                String imageFilenameID = getUniqueId(pageType);
                List<TextRegionType> regions = PageXmlUtils.getTextRegions(pageType);
                List<TextLineType> tlts = new LinkedList<>();
                Map<TextLineType, TextRegionType> regionMap = new HashMap<>();
                for (TextRegionType r : regions) {
                    List<TextLineType> lines = r.getTextLine();
                    if (lines == null || lines.isEmpty()) {
                        continue;
                    }
                    for (TextLineType l : lines) {
                        String unicode = PageXmlUtil.getTextEquiv(l);
                        if (unicode == null || unicode.isEmpty()) {
                            continue;
                        }
                        if (saveTrainData) {
                            if (l.getTextEquiv().getConf() == null || l.getTextEquiv().getConf() >= minConf) {
                                tlts.add(l);
                                regionMap.put(l, r);
                            }
                        }
                        if (stat != null) {
                            stat.addLine(unicode);
                        }
                    }
                    //if LR should be saved, add empty line in LR-file to indicate Region-End
                    if (stat != null) {
                        stat.addLine(null);
                    }
                }
                if (calcLR) {
                    String toFile = PropertyUtil.getProperty(props, Key.CREATE_LR);
                    try {
                        File file = toFile.equals("true") ? new File(fileFolderOut, lr) : new File(toFile);
                        List<String> languageResource = stat.getLanguageResource();
                        FileUtil.writeLines(file, languageResource);
                    } catch (RuntimeException ex) {
                        LOG.error("cannot save language resource to file '{}'", toFile);
                    }
                }
                if (saveTrainData) {
                    File folderImage = fileFolderOut;
                    if (PropertyUtil.hasProperty(props, KEY_SRC_FOLDER)) {
                        folderImage = FileUtil.getTgtFile(new File(PropertyUtil.getProperty(props, KEY_SRC_FOLDER)), fileFolderOut, page.getPathImg()).getParentFile();
                    }
                    HybridImage hi = ImageUtil.getHybridImage(page.getImg());
                    for (TextLineType line : tlts) {
                        TextRegionType region = regionMap.get(line);
                        HybridImage subImage = null;
                        try {
                            LineImage li = new LineImage(hi, line, region);
                            subImage = li.getSubImage();
                            HashMap<String, Object> properties = subImage.getProperties();
                            properties.put("imageName", imageFilename);
                            properties.put("regionID", region.getId());
                            properties.put("lineID", line.getId());
                            if (line.getCustom() != null && !line.getCustom().isEmpty()) {
                                properties.put("custom", line.getCustom());
                            }
                            if (line.getTextEquiv().getConf() != null) {
                                properties.put("conf", line.getTextEquiv().getConf().toString());
                            }
                            BaselineType blt = line.getBaseline();
                            if (blt != null) {
                                ITransform trafo = subImage.getTrafo();
                                Polygon2DInt baseline = PolygonUtil.convert(PolygonUtil.getBaseline(line));
                                PolygonHelper.transform(baseline, trafo);
                                properties.put("baseline", PolygonHelper.asString(baseline));
                            }
                            String reference = PageXmlUtil.getTextEquiv(line);
                            if (sn != null) {
                                String ref2 = sn.normalize(reference);
                                if (!ref2.equals(reference)) {
                                    LOG.debug("change reference '" + reference + "' to '" + ref2 + "' using stringnormalizer");
                                    reference = ref2;
                                }
                            }
                            File f = new File(folderImage, imageFilenameID + "_" + region.getId() + "_" + line.getId() + ".png");
                            if (f.exists()) {
                                LOG.warn("file " + f.getAbsolutePath() + " already exists - new trainitem overwrites it.");
                            }
                            LoaderIO.saveImageHolder(f.getAbsolutePath(), new IImageLoader.ImageHolderDft(subImage, reference, new HashMap<>()));
                        } catch (Throwable t) {
                            LOG.error("could not save ImageHolder", t);
                        } finally {
                            if (subImage != null) {
                                subImage.clear(false);
                            }
                        }
                    }
                    LOG.debug("saved " + tlts.size() + " training samples from page " + xmlFilePath);
                    hi.clear(false);
                }
            } catch (Throwable t) {
                LOG.error("could not save ImageHolders", t);
            }
        }

        return stat;
    }

    public static void savePDict(ObjectCounter<String> oc, File path) {
        LinkedList<String> out = new LinkedList<>();
        out.add("value;occurence");
        for (Pair<String, Long> pair : oc.getResultOccurrence()) {
            out.add(pair.getFirst() + ";" + pair.getSecond());
        }
        FileUtil.writeLines(path, out);
    }

    public static void saveArpa(ObjectCounter<String> oc, File path) {
        LinkedList<String> out = new LinkedList<>();
        out.add("\\data\\");
        final int size = oc.getResult().size();
        out.add("ngram 1=" + (size + 1));
        out.add("");
        out.add("\\1-grams:");
        List<Pair<String, Long>> resultOccurrence = oc.getResultOccurrence();
        long counter = 0;
        for (Pair<String, Long> pair : resultOccurrence) {
            counter += pair.getSecond();
        }
//            double d = 0;
        double oovLog = -5;
        double probOOV = Math.exp(oovLog);
        double sumLog10 = Math.log10(1 - probOOV);
        double counterLog10 = Math.log10(counter);
        double oovLog10 = oovLog / Math.log(10);
        for (Pair<String, Long> pair : resultOccurrence) {
            final double p = Math.log10(pair.getSecond()) - counterLog10 + sumLog10;
            out.add(String.format("%f\t%s", p, pair.getKey()));
//                d += Math.pow(10, p);
        }
//            System.out.println(d);
        out.add(String.format("%.3e\t%s", oovLog10, "<unk>"));
//            d += Math.pow(10, oovLog10);
//            System.out.println(d);
        out.add("");
        out.add("\\end\\");
        FileUtil.writeLines(path, out);
    }

    public static void saveCharacterStatistic(ObjectCounter<Character> oc, File outCharLong) {
        if (outCharLong == null) {
            throw new NullPointerException("no output file given");
        }
        List<Pair<Character, Long>> resultOccurrence = oc.getResultOccurrence();
        Collections.sort(resultOccurrence, new Comparator<Pair<Character, Long>>() {
            @Override
            public int compare(Pair<Character, Long> o1, Pair<Character, Long> o2) {
                return Long.compare(o2.getSecond(), o1.getSecond());
            }
        });
        try (FileWriter fw = new FileWriter(outCharLong)) {
            for (int i = 0; i < resultOccurrence.size(); i++) {
                Pair<Character, Long> pair = resultOccurrence.get(i);
//                    System.out.println(String.format("%c | \\u%04x | %d", pair.getFirst(), (int) pair.getFirst(), pair.getSecond()));
                fw.write(String.format("%c | \\u%04x | %d%s", pair.getFirst(), (int) pair.getFirst(), pair.getSecond(), i < resultOccurrence.size() - 1 ? "\n" : "\n"));
            }
            fw.flush();
        } catch (IOException ex) {
            LOG.warn("cannot save file '" + outCharLong.getAbsolutePath() + "'.", ex);
        }
    }

}
