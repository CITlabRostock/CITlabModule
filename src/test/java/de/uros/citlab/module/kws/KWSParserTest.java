/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.kws;

import com.achteck.misc.types.CharMap;
import com.achteck.misc.types.ConfMat;
import com.achteck.misc.util.IO;
import de.planet.math.util.MatrixUtil;
import de.uros.citlab.confmat.util.ConfMatUtil;
import de.uros.citlab.module.TestFiles;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PropertyUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author gundram
 */
public class KWSParserTest {

    static File folderKws = new File(TestFiles.getPrefix(), "test_kws");
    static String[] storageIn;
    static String[] imagesIn;
    private static Logger LOG = LoggerFactory.getLogger(KWSParserTest.class);
    //    private static File folderKwsTmp = new File(TestFiles.getPrefix(), "test_kws_tmp");
    @Rule
    public final TemporaryFolder folderKwsTmp = new TemporaryFolder();

    public KWSParserTest() {
    }

    @BeforeClass
    public static void initPathes() {
        File[] lst = new File(folderKws, "doc25919").listFiles();
        storageIn = FileUtil.asStringList(Arrays.asList(lst));
        Arrays.sort(storageIn);
        imagesIn = new String[storageIn.length];
        for (int i = 0; i < storageIn.length; i++) {
            imagesIn[i] = storageIn[i].substring(storageIn[i].lastIndexOf("/") + 1);

        }
    }

//    @AfterClass
//    public static void tearDownClass() {
//        FileUtils.deleteQuietly(folderKwsTmp);
//    }

    private String getResult(File file) {
        List<String> readLines = FileUtil.readLines(file);
        StringBuilder sb = new StringBuilder();
        for (String readLine : readLines) {
            sb.append(readLine).append("\n");
        }
        String res = sb.toString();
        return res.isEmpty() ? res : res.substring(0, res.length() - 1);
    }

    @Test
    public void ExportConfMatTest() throws IOException, ClassNotFoundException {
        System.out.println("ExportConfMatTest");
        for (String name : storageIn) {
            File folder = folderKwsTmp.newFolder(new File(name).getName().replace(".bin","" ));
            ConfMatContainer container = (ConfMatContainer) IO.load(new File(name), "de.uro.citlab", "de.uros.citlab");
            List<ConfMat> confmats = container.getConfmats();
            for (ConfMat cm : confmats) {
                String id = container.getLineID(cm);
                CharMap<Integer> charMapPlanet = cm.getCharMap();
                int size = charMapPlanet.keySet().size();
                de.uros.citlab.confmat.CharMap charMapUro = new de.uros.citlab.confmat.CharMap();
                for (int i = 0; i < size; i++) {
                    char[] chars = charMapPlanet.get(i).toCharArray();
                    if (chars[0] == ConfMat.NaC) {
//                        charMapUro.add("");
                        continue;
                    }
                    for (int charIdx = 0; charIdx < chars.length; charIdx++) {
                        charMapUro.add(chars[charIdx], i);
                    }
                }
                double[][] mat = cm.getProbMat().copyTo(null);
                de.uros.citlab.confmat.ConfMat confMatUro = new de.uros.citlab.confmat.ConfMat(charMapUro, mat);
                ConfMatUtil.toCSV(confMatUro, new File(folder, id + ".csv"));
                de.uros.citlab.confmat.ConfMat confMatReload = ConfMatUtil.fromCSV(new File(folder, id + ".csv"));
                de.uros.citlab.confmat.CharMap charMapReload = confMatReload.getCharMap();
                double[][] matReload = MatrixUtil.transpose(confMatReload.getMatrix());
                double[][] matOrig = MatrixUtil.transpose(mat);
                for (int idx : charMapPlanet.keySet()) {
                    String s = charMapPlanet.get(idx);
                    Character c = s.charAt(0);
                    Integer integer = c.equals(ConfMat.NaC) ? 0 : charMapReload.get(c);
//                    System.out.println(Arrays.toString(matOrig[idx]));
//                    System.out.println(Arrays.toString(matReload[integer]));
                    Assert.assertArrayEquals(matOrig[idx], matReload[integer], 1e-5);
                }
            }
        }
        System.out.println("done");
    }

//    @Test
//    public void sameScores() {
//        ConfMatContainer cmc1 = ConfMatContainer.newInstance(new File(storageIn[0]));
//        ConfMatContainer cmc2 = ConfMatContainer.newInstance(new File(storageIn[1]));
//        ConfMat cm1 = cmc1.getConfmats().get(352);
//        ConfMat cm2 = cmc2.getConfmats().get(352);
//        System.out.println(cm1);
//        System.out.println(cm2);
//    }
//
//    @Test
//    public void showConfmat() throws IOException {
//        File folderConfmats = new File(folderKwsTmp, "confmats");
//        for (String path : storageIn) {
//            File f = new File(path);
//            ConfMatContainer cmc = ConfMatContainer.newInstance(f);
//            List<ConfMat> confmats = cmc.getConfmats();
//            for (int i = 0; i < confmats.size(); i++) {
//                ConfMat cm = confmats.get(i);
//                Polygon2DInt baseline = cmc.getBaseline(cm);
//                HybridImage probMat = ConfMatUtil.getProbMat(confmats.get(i));
////                File file = new File(folderConfmats, f.getName() + "_" + i + ".jpg");
//                File file = new File(folderConfmats, Arrays.deepHashCode(probMat.getAsByteImage().pixels) + f.getName() + "_" + i + ".jpg");
//                probMat.save(file.getPath());
//                FileUtils.writeLines(new File(file.getPath() + ".txt"), Arrays.asList(cm.toString(), PolygonUtil.polygon2String(baseline)));
//            }
//
//        }
//
//    }
//

    /**
     * Test of process method, of class KWSParser.
     */
    @Test
    public void testProcess() {
        System.out.println("testProcess");
        String[] queriesIn = FileUtil.readLines(new File(folderKws, "keywords.txt")).toArray(new String[0]);
        String dictIn = null;
        boolean[] tf = new boolean[]{true, false};
        for (boolean upper : tf) {
            for (boolean expert : tf) {
                for (boolean part : tf) {
                    if (part && expert) {
                        continue;
                    }
                    String[] props = null;
                    String name = "res";
                    if (upper) {
                        props = PropertyUtil.setProperty(props, Key.KWS_UPPER, "true");
                        name += "_upper";
                    }
                    if (expert) {
                        props = PropertyUtil.setProperty(props, Key.KWS_EXPERT, "true");
                        name += "_expert";
                    }
                    if (!part) {
                        props = PropertyUtil.setProperty(props, Key.KWS_PART, "true");
                    } else {
                        name += "_part";
                    }
                    props = PropertyUtil.setProperty(props, Key.KWS_MIN_CONF, "0.05");
                    //                    String res = "res_" + upper + "_" + expert + "_" + sep + ".json";
                    name += ".json";
                    KWSParser instance = new KWSParser();
                    String[] queriesModified = queriesIn;
                    if (expert) {
                        queriesModified = new String[queriesIn.length];
                        for (int i = 0; i < queriesIn.length; i++) {
                            queriesModified[i] = ".*(?<KW>" + queriesIn[i] + ").*";
                        }
                    }
//                    String expResult = getResult(new File(folderKws, "result.txt"));
                    String result = instance.process(imagesIn, storageIn, queriesModified, dictIn, props);
                    result = result.replaceAll("\"time\": [0-9]+\n", "\"time\": 123456 \n");
//                    if (!result.equals(expResult)) {
//                    LOG.error("result file differs: expect vs result");
//                        LOG.error(expResult.replace("\n", " "));
//                    LOG.error(result.replace("\n", " "));
//                    try {
//                        FileUtils.write(new File(folderKws, name), result);
//                    } catch (IOException ex) {
//                        LOG.error("io-exception", ex);
//                    }
//                    }
//        assertEquals(expResult, result);
                }
            }
        }
    }

}
