package de.uros.citlab.module.workflow;

import com.achteck.misc.types.CharMap;
import de.planet.imaging.types.HybridImage;
import de.planet.imaging.util.ImageHelper;
import de.uro.citlab.cjk.Decomposer;
import de.uro.citlab.cjk.util.DecomposerUtil;
import de.uro.citlab.cjk.util.Gnuplot;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.module.util.CharMapUtil;
import de.uros.citlab.module.util.FileUtil;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SetUpData {

    public static boolean saveImages = false;
    public static double factorReduce = 0.9;
    public static double factorStart = 0.02;
    public static double sizeOffset = 1;
    public static double minOccurance = 5;
    public static Logger LOG = LoggerFactory.getLogger(SetUpData.class);

    //    public static int[] = false;
    public static Decomposer getBestDecomposer(int maxLength, Map<Character, Long> trainMap, Decomposer.Coding decoding) {
        List<String> run = new LinkedList<>();
        double relCurrent = factorStart;
        int minSizeGlobal = Integer.MAX_VALUE;
        double minRelGlobal = relCurrent;
        while (true) {
            Decomposer dec = new Decomposer(Decomposer.Coding.ANY);
            for (Character character : trainMap.keySet()) {
                dec.count(String.valueOf(character), trainMap.get(character).intValue());
            }
            Map<String, LinkedList<Double>> stringLinkedListMap = DecomposerUtil.reduceDecomposer(dec, relCurrent, maxLength);
            System.out.println("-----------------------------------------------------");
            for (String s : stringLinkedListMap.keySet()) {
                System.out.println(s + "=>" + stringLinkedListMap.get(s));
            }
            //show some stuff
            List<Double> xs = stringLinkedListMap.get("size");
            stringLinkedListMap.remove("size");
            List<double[]> ys = new LinkedList<>();
            String[] names = new String[stringLinkedListMap.size()];
            int idx = 0;
            for (String name : stringLinkedListMap.keySet()) {
                names[idx++] = name;
                ys.add(DecomposerUtil.toArray(stringLinkedListMap.get(name)));
            }
            Gnuplot.withGrid = true;
//                        Gnuplot.plot(DecomposerUtil.toArray(xs), ys, "Average decomposition length compared to CharSet minSizeGlobal", names, "example_decomposition.png", null);
            int sizeCurrent = (int) Math.round(xs.get(xs.size() - 1));
            double avgLength = stringLinkedListMap.get("same occurace").get(xs.size() - 1);
            run.add(String.format("%4d %5.3f %8.5f", sizeCurrent, avgLength, relCurrent));
            if (minSizeGlobal < sizeCurrent - sizeOffset) {
                Gnuplot.plot(DecomposerUtil.toArray(xs), ys, "Average decomposition length compared to CharSet minSizeGlobal", names, null);
                System.out.println("DONE! with " + minSizeGlobal + " (before: " + sizeCurrent + " )and rel implovement " + relCurrent);
                break;
            } else {
                relCurrent *= factorReduce;
            }
            if (sizeCurrent < minSizeGlobal) {
                minSizeGlobal = sizeCurrent;
                minRelGlobal = relCurrent;
            }
        }
        Decomposer dec = new Decomposer(decoding);
        for (Character character : trainMap.keySet()) {
            dec.count(String.valueOf(character), trainMap.get(character).intValue());
        }
        System.out.println("minimal minSizeGlobal and rel:" + minSizeGlobal + " " + minRelGlobal);
        DecomposerUtil.reduceDecomposer(dec, minRelGlobal, minSizeGlobal);
        for (String s : run) {
            System.out.println(s);
        }
        return dec;
    }

    private static double getOOV(Set<Character> trainChars, ObjectCounter<Character> ocVal) {
        long inCm = 0;
        long outCm = 0;
        long in = 0;
        long out = 0;
        Map<Character, Long> valMap = ocVal.getMap();
        for (Character character : valMap.keySet()) {
            if (trainChars.contains(character)) {
                in += valMap.get(character);
                inCm++;
            } else {
                out += valMap.get(character);
                outCm++;
            }
        }
        System.out.println("in: " + in);
        System.out.println("out: " + out);
        System.out.println("in CharMap: " + inCm);
        System.out.println("out CharMap: " + outCm);
        System.out.println(String.format("OOV = %.2f%s", ((double) out) / (in + out) * 100.0, "%"));
        return ((double) out) / (in + out);
    }

    public static void main(String[] args) throws IOException {

        File folderData = HomeDir.getFile("data");
        File folderCharmaps = HomeDir.getFile("charmaps");
        folderCharmaps.mkdirs();
        File folderLists = HomeDir.getFile("lists");
        folderLists.mkdirs();
        Path folderPath = folderData.toPath();
        File folderRawTask1 = new File(folderData, "raw");
        File folderBase = new File(folderData, "base");
        File folderVal = new File(folderBase, "val");
        File folderTrain = new File(folderBase, "train");
        File folderDecomposed = new File(folderData, "decomposed");
//        File link = HomeDir.getFile("deep1/deeper/out.jpg");
//        link.getParentFile().mkdirs();
//        File target = HomeDir.getFile("deep2/text.jpg");
//        Files.createSymbolicLink(link.toPath(), link.getParentFile().toPath().relativize(target.toPath()));
//        System.exit(-1);
        List<File> jpg = FileUtil.listFiles(folderRawTask1, "jpg", true);
        System.out.println("number of lines: " + jpg.size());
        HashSet<String> imageNames = new LinkedHashSet<>();
        for (File image : jpg) {
            String name = image.getName();
            name = name.substring(0, name.lastIndexOf("_"));
            imageNames.add(name);
        }
        System.out.println("number of pages: " + imageNames.size());
        List<String> setSorted = new LinkedList<>(imageNames);
        setSorted.sort(String::compareTo);
        Collections.shuffle(setSorted, new Random(1234));
        Set<String> pagesVal = new HashSet<>(setSorted.subList(0, 200));
        Set<String> pagesTrain = new HashSet<>(setSorted.subList(200, setSorted.size()));
        ObjectCounter<Character> ocTrain = new ObjectCounter<>();
        ObjectCounter<Character> ocVal = new ObjectCounter<>();
        for (File image : jpg) {
            String name = image.getName();
            name = name.substring(0, name.lastIndexOf("_"));
            File outFolder = pagesVal.contains(name) ? folderVal : pagesTrain.contains(name) ? folderTrain : null;
            boolean isTrain = folderTrain == outFolder;
            String ref = FileUtil.readLines(new File(image.getPath().replace(".jpg", ".txt"))).get(0);
            if (saveImages) {
                File pageFolder = new File(outFolder, name);
                pageFolder.mkdirs();
                File outFile = new File(pageFolder, image.getName());
                HybridImage hi = HybridImage.newInstance(image);
                hi = ImageHelper.rotate270(hi);
                hi.save(outFile.getAbsolutePath());
                FileUtil.writeLine(new File(outFile.getPath().replace(".jpg", ".txt")), ref);
            }
            if (isTrain) {
                for (char c1 : ref.toCharArray()) {
                    ocTrain.add(c1);
                }
            } else {
                for (char c : ref.toCharArray()) {
                    ocVal.add(c);
                }

            }
        }
        int[] maxLen = new int[]{16, 8, 10, 12, 14, 18};
        Decomposer.Coding[] codings = new Decomposer.Coding[]{Decomposer.Coding.UTF8, Decomposer.Coding.UTF8_IDC};
//        int[] maxLen = new int[]{16};
        for (Decomposer.Coding coding : codings) {
            for (int len : maxLen) {
                String prefix = String.format("chinese_%02d%s", len, coding.equals(Decomposer.Coding.UTF8_IDC) ? "_IDC" : "");
                File folderDecopmoseOut = new File(folderDecomposed, prefix);
                Decomposer decomposer = getBestDecomposer(len, ocTrain.getMap(), coding);
                ObjectCounter<Character> oc = new ObjectCounter<>();
                List<File> txts = FileUtil.listFiles(folderBase, "txt", true);
                for (File txt : txts) {
                    StringBuilder sb = new StringBuilder();
                    String ref = FileUtil.readLine(txt);
                    for (char aChar : ref.toCharArray()) {
                        if (aChar == ' ') {
                            throw new RuntimeException("space in reference " + txt.getPath());
                        }
                        String decompose = decomposer.decompose("" + aChar);
                        if (decompose == null) {
                            LOG.error("map is null - maybe cannot interprete sign '{}' - skip sign in reference.", aChar);
                            continue;
                        }
                        if (decompose.length() > len) {
                            LOG.info("should not be larger than 16 but is len= {} seq = '{}' file = {}", decompose.length(), decompose, txt);
//                        throw new RuntimeException("should not be larger than 16");
                        }
                        sb.append(decompose).append(' ');
                    }
                    String refNew = sb.toString().trim();
                    for (char c : refNew.toCharArray()) {
                        oc.add(c);
                    }

                    File out = FileUtil.getTgtFile(folderBase, folderDecopmoseOut, txt);
//                System.out.println(ref + " ==> " + sb.toString().trim());
                    FileUtil.writeLine(out, refNew);
//                    File imgTgt = new File(HomeDir.getFile("text.jpg").getPath());
                    File imgTgt = new File(txt.getPath().replace(".txt", ".jpg"));
                    File imgSrc = new File(out.getPath().replace(".txt", ".jpg"));
                    if (!imgSrc.exists()) {
                        Files.createSymbolicLink(imgSrc.toPath(), imgSrc.getParentFile().toPath().relativize(imgTgt.toPath()));
                    }
//                    Files.createSymbolicLink(imgSrc.toPath(),folderPath.relativize(imgTgt.toPath()));
                }
                File folderDecomposeVal = new File(folderDecopmoseOut, "val");
                File folderDecomposeTrain = new File(folderDecopmoseOut, "train");
                for (File file : new File[]{folderDecomposeTrain, folderDecomposeVal}) {
                    List<File> images = FileUtil.listFiles(file, "jpg", true);
                    List<String> out = new LinkedList<>();
                    for (File image : images) {
                        out.add(HomeDir.PATH_FILE.toPath().relativize(image.toPath()).toString());
                    }
                    Collections.shuffle(out, new Random(1234));
                    FileUtil.writeLines(new File(folderLists, prefix + "_" + file.getName() + ".lst"), out);
                }

                List<Pair<Character, Long>> resultOccurrence = oc.getResultOccurrence();
                HashSet<Character> set = new HashSet<>();
                for (Pair<Character, Long> characterLongPair : resultOccurrence) {
                    if (characterLongPair.getSecond() < minOccurance) {
                        break;
                    }
                    if (set.contains(characterLongPair.getFirst())) {
                        throw new RuntimeException("double occurance of" + characterLongPair.getFirst());
                    }
                    set.add(characterLongPair.getFirst());
                    System.out.println(characterLongPair);
                }
                CharMap<Integer> charMap = CharMapUtil.getCharMap(set, false);
                CharMapUtil.saveCharMap(charMap, new File(folderCharmaps, "cm_" + prefix + ".txt"));
                LOG.debug("charMap = {}", charMap);
                System.out.println(charMap);

            }
        }

    }
}
