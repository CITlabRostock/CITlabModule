/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.train;

import de.uros.citlab.module.TestFiles;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.*;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import static de.uros.citlab.module.util.TrainDataUtil.cmLong;
import static org.junit.Assert.fail;

/**
 *
 * @author gundram
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TrainHtrTest {

    private static final File dir;
    private static final File dirTraindata;
    private static final File dirHtr;
    private static final File fileHtr;
    private static final File fileHtrTrained;

    static {
        dir = new File(TestFiles.getPrefix(), "test_trainHtr");
        dirTraindata = new File(dir, "traindata");
        dirHtr = new File(dir, "htr");
        fileHtr = new File(dirHtr, "htr_in");
        fileHtrTrained = new File(dirHtr, "htr_out");
    }

    public TrainHtrTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        dir.mkdirs();
    }

    @AfterClass
    public static void tearDownClass() {
        try {
            FileUtils.deleteDirectory(dir);
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
        TrainHtr instance = new TrainHtr();
        instance.createTrainData(pageXmlsStr, dirTraindata.getAbsolutePath(), dirTraindata.getAbsolutePath() + File.separator + "chars.txt", props);
//        File f = new File(dir.getAbsolutePath() + File.separator + "traindata" + File.separator + TrainHtrSGD.dictArpa);
//        ContextEncodedProbBackoffLm<String> lm = LmReaders.readContextEncodedLmFromArpa(dir.getAbsolutePath() + File.separator + "traindata" + File.separator + TrainHtrSGD.dictArpa);
//        System.out.println("file exists:" + lm);
    }

    @Test
    public void test000CreateTrainDataWithStatus() {
        System.out.println("CreateTrainDataOnlyGT");
        List<File> testGTXmlFiles = TestFiles.getTestFiles();
        File[] pageXmls = new File[testGTXmlFiles.size()];
        String[] pageXmlsStr = new String[testGTXmlFiles.size()];
        for (int i = 0; i < testGTXmlFiles.size(); i++) {
            pageXmls[i] = PageXmlUtil.getXmlPath(testGTXmlFiles.get(i));
            pageXmlsStr[i] = pageXmls[i].getAbsolutePath();
        }
        String[] props = PropertyUtil.setProperty(null, Key.CREATEDICT, "true");
        props = PropertyUtil.setProperty(props, Key.STATISTIC, "true");

        {
            props = PropertyUtil.setProperty(props, Key.TRAIN_STATUS, "GT;DONE");
            TrainHtr instance = new TrainHtr();
            instance.createTrainData(pageXmlsStr, dirTraindata.getAbsolutePath(), dirTraindata.getAbsolutePath() + File.separator + "chars.txt", props);
            List<String> readLinesAfter = FileUtil.readLines(new File(dirTraindata + File.separator + cmLong));
            Assert.assertEquals("traindata should not be taken - resulting in emtpy statistic.", 40, readLinesAfter.size());
        }
        {
            props = PropertyUtil.setProperty(props, Key.TRAIN_STATUS, "DONE");
            TrainHtr instance = new TrainHtr();
            instance.createTrainData(pageXmlsStr, dirTraindata.getAbsolutePath(), dirTraindata.getAbsolutePath() + File.separator + "chars.txt", props);
            List<String> readLinesAfter = FileUtil.readLines(new File(dirTraindata + File.separator + cmLong));
            Assert.assertEquals("traindata should not be taken - resulting in emtpy statistic.", 0, readLinesAfter.size());
        }
    }

    /**
     * Test of createHtr method, of class TrainHtrSGD.
     */
    @Test
    public void test02CreateHtr() {
        System.out.println("createHtr");
        List<File> testGTXmlFiles = TestFiles.getTestFiles();
        String[] pageXmls = new String[testGTXmlFiles.size()];
        for (int i = 0; i < testGTXmlFiles.size(); i++) {
            pageXmls[i] = PageXmlUtil.getXmlPath(testGTXmlFiles.get(i)).getAbsolutePath();
        }
        for (String pageXml : pageXmls) {
            if (!new File(pageXml).exists()) {
                fail("cannot find file '" + pageXml + "'.");
            }
        }

        TrainHtr instance = new TrainHtr();
        File f = TrainHtr.getNet(fileHtr);
        instance.createHtr(fileHtr.getPath(), dirTraindata.getAbsolutePath() + File.separator + "chars.txt", null);
        if (!f.exists()) {
            fail("htr could not be created.");
        }
    }

    /**
     * Test of trainHtr method, of class TrainHtrSGD.
     */
    @Test
    public void test03TrainHtr() {
        if (TestFiles.skipLargeTests()) {
            return;
        }
        System.out.println("trainHtr");
        String[] props = PropertyUtil.setProperty(null, "NumEpochs", "7;2");
        props = PropertyUtil.setProperty(props, "LearningRate", "5e-3;1e-3");
        props = PropertyUtil.setProperty(props, "Noise", "both");
        props = PropertyUtil.setProperty(props, "Threads", "4");
        props = PropertyUtil.setProperty(props, "TrainSizePerEpoch", "12");
        TrainHtr instance = new TrainHtr();
        ObserverImpl observerImpl = new ObserverImpl();
        instance.addObserver(observerImpl);
        instance.trainHtr(fileHtr.getPath(),
                fileHtrTrained.getPath(),
                dirTraindata.getAbsolutePath(),
                dirTraindata.getAbsolutePath(),
                props);
        Assert.assertEquals("counter should match NumEpochs", 9, observerImpl.counter);
    }

    private class ObserverImpl implements Observer {

        int counter = -1;

        @Override
        public void update(Observable o, Object arg) {
            counter++;
            Assert.assertEquals("unexpected class", TrainHtr.Status.class.getName(), arg.getClass().getName());
            TrainHtr.Status status = (TrainHtr.Status) arg;
            Assert.assertEquals("unexpected epoch", counter, status.epoch);

        }

    }

    @Test
    public void test01Dict() throws LangModConfigurator.EmptyDictException {
        DictionaryTest dictTest = new DictionaryTest();
        dictTest.testDict(new File(dirTraindata, "dict.csv"), new File(dirTraindata, "chars.txt"), DictionaryTest.TestType.TOKENIZER, DictionaryTest.TestType.CATEGORY, DictionaryTest.TestType.CHARMAP);
    }

}
