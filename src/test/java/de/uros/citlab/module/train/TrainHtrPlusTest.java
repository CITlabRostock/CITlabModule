/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.train;

import de.uros.citlab.module.TestFiles;
import de.uros.citlab.module.htr.HTRParserPlus;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.util.PropertyUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.interfaces.types.Image;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * @author gundram
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TrainHtrPlusTest {

    private static final File dirRoot;
    private static final File dirTraindata;
    private static final File dirData;
    private static final File dirTmp;
    private static final File dirHtr;
    private static final File dirHtrTrained;

    static {
        dirRoot = new File(TestFiles.getPrefix(), "test_trainHtrPlus");
        dirTraindata = new File(dirRoot, "traindata");
        dirTmp = new File(dirRoot, "tmp");
        dirHtr = new File(dirRoot, "network_v1");
        dirHtrTrained = new File(dirRoot, "network_v2");
        dirData = new File(dirRoot, "data");
    }

    @BeforeClass
    public static void setUpClass() {
//        if (dirRoot.exists()) {
//            try {
//                FileUtils.deleteDirectory(dirRoot);
//            } catch (IOException ex) {
//                throw new RuntimeException(ex);
//            }
//        }
        dirRoot.mkdirs();
    }

    @AfterClass
    public static void tearDownClass() {
        try {
            FileUtils.deleteDirectory(dirRoot);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Test of usage method, of class TrainHtrSGD.
     */
    @Test
    public void testUsage() {
        System.out.println("usage");
        TrainHtr instance = new TrainHtr();
        String result = instance.usage();
        if (result == null || result.isEmpty()) {
            fail("usage string is '" + result + "'");
        }
    }

    /**
     * Test of getToolName method, of class TrainHtrSGD.
     */
    @Test
    public void testGetToolName() {
        System.out.println("getToolName");
        TrainHtr instance = new TrainHtr();
        String result = instance.getToolName();
        if (result == null || result.isEmpty()) {
            fail("tool name is '" + result + "'");
        }
    }

    /**
     * Test of getVersion method, of class TrainHtrSGD.
     */
    @Test
    public void testGetVersion() {
        System.out.println("getVersion");
        TrainHtr instance = new TrainHtr();
        String result = instance.getVersion();
        if (!result.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) {
            fail("Version does not fit to regex \"[0-9]+\\\\.[0-9]+\\\\.[0-9]+\".");

        }
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * Test of getProvider method, of class TrainHtrSGD.
     */
    @Test
    public void testGetProvider() {
        System.out.println("getProvider");
        TrainHtr instance = new TrainHtr();
        String result = instance.getProvider();
        if (result == null || result.isEmpty()) {
            fail("provider is '" + result + "'");
        }
        if (!result.contains("CITlab")) {
            fail("provider does not contain CITlab");
        }
    }

    /**
     * Test of createTrainData method, of class TrainHtrSGD.
     */
    @Test
    public void test001CreateTrainData() {
        System.out.println("createTrainData");
        List<File> testGTXmlFiles = TestFiles.getTestFiles();
        File[] pageXmls = new File[testGTXmlFiles.size()];
        String[] pageXmlsStr = new String[testGTXmlFiles.size()];
        for (int i = 0; i < testGTXmlFiles.size(); i++) {
            pageXmls[i] = PageXmlUtil.getXmlPath(testGTXmlFiles.get(i));
            pageXmlsStr[i] = pageXmls[i].getAbsolutePath();
        }
        for (File pageXml : pageXmls) {
            if (!pageXml.exists()) {
                fail("cannot find file '" + pageXml + "'.");
            }
        }
        String[] props = PropertyUtil.setProperty(null, Key.CREATEDICT, "true");
        props = PropertyUtil.setProperty(props, Key.STATISTIC, "true");
        TrainHtrPlus instance = new TrainHtrPlus();
        instance.createTrainData(pageXmlsStr, dirTraindata.getAbsolutePath(), dirTraindata.getAbsolutePath() + File.separator + "chars.txt", props);
//        File f = new File(dir.getAbsolutePath() + File.separator + "traindata" + File.separator + TrainHtrSGD.dictArpa);
//        ContextEncodedProbBackoffLm<String> lm = LmReaders.readContextEncodedLmFromArpa(dir.getAbsolutePath() + File.separator + "traindata" + File.separator + TrainHtrSGD.dictArpa);
//        System.out.println("file exists:" + lm);
    }

    /**
     * Test of createHtr method, of class TrainHtrSGD.
     */
    @Test
    public void test02CreateHtr() {
        System.out.println("createHtr");
//        List<File> testGTXmlFiles = TestFiles.getTestFiles();
//        String[] pageXmls = new String[testGTXmlFiles.size()];
//        for (int i = 0; i < testGTXmlFiles.size(); i++) {
//            pageXmls[i] = PageXmlUtil.getXmlPath(testGTXmlFiles.get(i)).getAbsolutePath();
//        }
//        for (String pageXml : pageXmls) {
//            if (!new File(pageXml).exists()) {
//                fail("cannot find file '" + pageXml + "'.");
//            }
//        }
        String[] props = PropertyUtil.setProperty(null, Key.TMP_FOLDER, dirTmp.getAbsolutePath());
        TrainHtrPlus instance = new TrainHtrPlus();
        instance.createHtr(dirHtr.getPath(), new File(dirTraindata, Key.GLOBAL_CHARMAP).getAbsolutePath(), props);
        File f = new File(dirHtr.getPath());
        if (!f.exists()) {
            fail("htr could not be created.");
        }
    }

    /**
     * Test of trainHtr method, of class TrainHtrSGD.
     */
    @Test
    public void test03TrainHtr() {
//        if (TestFiles.skipLargeTests()) {
//            return;
//        }
        System.out.println("trainHtr");
        String[] props = PropertyUtil.setProperty(null, Key.EPOCHS, "2");
//        props = PropertyUtil.setProperty(props, Key.LEARNINGRATE, "1e-3");
//        props = PropertyUtil.setProperty(props, Key.MINI_BATCH, "16");
//        props = PropertyUtil.setProperty(props, Key.NOISE, "preproc");
        props = PropertyUtil.setProperty(props, Key.TRAINSIZE, "32");
        props = PropertyUtil.setProperty(props, Key.TMP_FOLDER, dirTmp.getPath());
        TrainHtrPlus instance = new TrainHtrPlus();
        instance.trainHtr(dirHtr.getAbsolutePath(),
                dirHtrTrained.getAbsolutePath(),
                dirTraindata.getAbsolutePath(),
                dirTraindata.getAbsolutePath(),
                props);
//        recoTrain = TrainHtrPlus.getReco();
//        Assert.assertNotNull("recognition result while traning must not be null", recoTrain);
//        Assert.assertNotEquals("recognition result while traning must not be 0", 0, recoTrain.size());

    }
//    private static List<String> recoTrain;

    @Test
    public void test04LoadNetwork() throws MalformedURLException {
//        if (TestFiles.skipLargeTests()) {
//            return;
//        }

        System.out.println("trainHtr");
        HTRParserPlus parser = new HTRParserPlus();
        Image img = new Image(TestFiles.getTestFiles().get(0).toURI().toURL());
        PcGtsType page = PageXmlUtil.unmarshal(PageXmlUtil.getXmlPath(TestFiles.getTestFiles().get(0)));
        System.out.println("before:###############################");
        List<String> text = PageXmlUtil.getText(page);
        for (String string : text) {
            System.out.println(string);
        }
        parser.process(dirHtrTrained.getAbsolutePath(), null, null, img, page, null, null, null);
        List<String> recoApply = PageXmlUtil.getText(page);
        System.out.println("output of network:");
        for (String line : recoApply) {
            System.out.println(line);
        }
//        Collections.sort(recoApply);
//        Collections.sort(recoTrain);
//        Assert.assertEquals("number of detected lines have to be the same", recoApply.size(), recoTrain.size());
//        for (int i = 0; i < recoApply.size(); i++) {
//            System.out.println("apply: " + recoApply.get(i));
//            System.out.println("train: " + recoTrain.get(i));
//            System.out.println("----------------------------------------");
//        }
//        for (int i = 0; i < recoApply.size(); i++) {
//            Assert.assertEquals("output have to be the same", recoApply.get(i), recoTrain.get(i));
//        }

    }

    public static void main(String[] args) throws MalformedURLException {
        TrainHtrPlusTest p = new TrainHtrPlusTest();
//        TrainHtrPlusTest.setUpClass();
        p.test04LoadNetwork();
    }
}
