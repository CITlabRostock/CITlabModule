/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.train;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.util.IO;
import de.planet.imaging.panels.DisplayPlanet;
import de.planet.imaging.types.HybridImage;
import de.planet.itrtech.reco.IImagePreProcess;
import de.planet.math.util.MatrixUtil;
import de.planet.trainer.factory.ImagePreprocessDft;
import de.planet.util.gui.Display;
import de.uros.citlab.module.TestFiles;
import de.uros.citlab.module.baseline2polygon.B2PSeamMultiOriented;
import de.uros.citlab.module.baseline2polygon.Baseline2PolygonParser;
import de.uros.citlab.module.htr.HTRParserPlus;
import de.uros.citlab.module.la.LayoutAnalysisURO_ML;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.util.PropertyUtil;
import de.uros.citlab.module.workflow.ApplyPreProc;
import de.uros.citlab.module.workflow.CreateTraindata;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.types.Image;
import org.apache.commons.io.FileUtils;
import org.junit.*;
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

    private static final File dir32223;
    private static final File dirRoot;
    private static final File dirTraindata;
    private static final File dirData;
    private static final File dirTmp;
    private static final File dirHtr;
    private static final File dirHtrTrained;
    private static final File dirDiagonalLine;
    private static final File HtrPlusTrainError_2019_04_03;


    static {
        dirRoot = new File(TestFiles.getPrefix(), "test_trainHtrPlus");
        dirTraindata = new File(dirRoot, "traindata");
        dirTmp = new File(dirRoot, "tmp");
        dirHtr = new File(dirRoot, "network_v1");
        dirHtrTrained = new File(dirRoot, "network_v2");
        dirData = new File(dirRoot, "data");
        dir32223 = new File(dirRoot, "32223");
        dirDiagonalLine = new File(dirRoot, "diagonalLine");
        HtrPlusTrainError_2019_04_03 = new File(dirRoot, "HtrPlusTrainError_2019-04-03");
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

    private static boolean test02CreateHtrOK = false;
    private static boolean test03TrainHtrOK = false;

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
        test02CreateHtrOK = true;
    }


    /**
     * Test of trainHtr method, of class TrainHtrSGD.
     */
    @Test
    public void test03TrainHtr() {
//        if (TestFiles.skipLargeTests()) {
//            return;
//        }
        Assume.assumeTrue(test02CreateHtrOK);
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
        test03TrainHtrOK = true;
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
        Assume.assumeTrue(test03TrainHtrOK);
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

    @Test
    public void test05RunNetwork() throws MalformedURLException {
//        if (TestFiles.skipLargeTests()) {
//            return;
//        }

        System.out.println("runHTR");
        HTRParserPlus parser = new HTRParserPlus();
        Image img = new Image(TestFiles.getTestFiles().get(0).toURI().toURL());
        PcGtsType page = PageXmlUtil.unmarshal(PageXmlUtil.getXmlPath(TestFiles.getTestFiles().get(0)));
        parser.process(new File(TestFiles.getPrefix(), "test_htr/HTR").getAbsolutePath(),
                null,
                null,
                img,
                page,
                null,
                null,
                null);
//        System.out.println("output of network:");
//        for (String line : recoApply) {
//            System.out.println(line);
//        }
        String tgt = "d\n" +
                "e Hagacfeee Foodeieen seiter\n" +
                "ce s mes Gaeheh. hen.Gais mtganste\n" +
                "wwoden, dasd sagele Rag deuhlsahen uh en\n" +
                "Abunafaahut chr Voteon ehgeben Sdeiaee\n" +
                "o gige iich hn. Fagaheennn Hadicg zal.\n" +
                "diah mns dasd fat dn Mucht genr saßhte,\n" +
                "ttioe dat aiesernsgen densshanstaas ee\n" +
                "Aitgen Seno wedlennnn allte, ich\n" +
                "den f wtseor sage mnieesllotennn gale.\n" +
                "Giich de Ghen Gabe Brlestange aaee\n" +
                "ge Gihan\n" +
                "Henn. Sugeng.\n" +
                "steh. dwisette Aahasst wdennn\n" +
                "Vi. elllle\n" +
                "CVVMCekebere";
        String out = String.join("\n", PageXmlUtil.getText(page));
        Assert.assertEquals("output between CITlab and this machine should be tze same", tgt, out);
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

    @Test
    public void testHtrPlusTrainError_2019_04_03() {
        System.out.println("HtrPlusTrainError_2019-04-03");
        File folder = new File(new File(TestFiles.getPrefix(), "test_htr_bug"), "HtrPlusTrainError_2019-04-03");
        Assume.assumeTrue(folder.exists());
        String[] props = PropertyUtil.setProperty(null, Key.TMP_FOLDER, dirTmp.getAbsolutePath());
        TrainHtrPlus instance = new TrainHtrPlus();
        List<File> files = FileUtil.listFiles(new File(folder, "valInput"), "xml", false);
        FileUtil.deleteMetadataAndMetsFiles(files);
        String[] filesString = FileUtil.getStringList(files).toArray(new String[0]);
        File folderSnippets = new File(HtrPlusTrainError_2019_04_03, "snippets");
        instance.createTrainData(filesString, folderSnippets.getAbsolutePath(), new File(HtrPlusTrainError_2019_04_03, "cm.txt").getAbsolutePath(), null);
        int size = FileUtil.listFiles(folderSnippets, FileUtil.IMAGE_SUFFIXES, false).size();
        Assert.assertEquals("wrong number of snippets found for validation", 95, size);
    }

    @Test
    public void testDiagonalLine() throws IOException, InvalidParameterException, ClassNotFoundException {
        System.out.println("testDiagonalLine");
        LayoutAnalysisURO_ML parser = new LayoutAnalysisURO_ML(null);
        Baseline2PolygonParser polygonParser = new Baseline2PolygonParser(B2PSeamMultiOriented.class.getName());
        File folder = new File(new File(TestFiles.getPrefix(), "test_htr_bug"), "diagonalLine");
        File folderHtr = new File(new File(TestFiles.getPrefix(), "test_htr_bug"), "job_err_id_32223_data");
        Assume.assumeTrue(folder.exists());
        File htr = new File(folderHtr, "HTR");
        File lr = new File(htr, "lr.txt");
        List<File> imgFiles = FileUtil.listFiles(folder, FileUtil.IMAGE_SUFFIXES, true);
        File folderXml = new File(dirDiagonalLine, "pagexml");
        folderXml.mkdirs();
        for (File imgFile : imgFiles) {
            Image img = new Image(imgFile.toURL());
            PcGtsType page = PageXmlUtils.createEmptyPcGtsType(imgFile.getName(), 3, 4);
            parser.process(img, page, null, null);
            File tgtFile = FileUtil.getTgtFile(folder, folderXml, imgFile);
            FileUtil.copyFile(imgFile, tgtFile);
            File xmlPath = PageXmlUtil.getXmlPath(tgtFile, false);
            xmlPath.getParentFile().mkdirs();
            List<TextLineType> textLines = PageXmlUtil.getTextLines(page);
            for (TextLineType textLine : textLines) {
                PageXmlUtil.setTextEquiv(textLine, "test", null);
            }
//            PageXmlUtil.marshal(page, xmlPath);
//        PageViewer.main(("-i " + tgtFile.getAbsolutePath()).split(" "));
            polygonParser.process(img, page, null, null);
            PageXmlUtil.marshal(page, xmlPath);
        }
//        PageViewer.main(("-i " + tgtFile.getAbsolutePath()).split(" "));
        File snippets = new File(dirDiagonalLine, "snippets");
        snippets.mkdirs();
        {
            CreateTraindata createTraindata = new CreateTraindata(folderXml.getAbsolutePath(), snippets.getAbsolutePath(), null, null);
            createTraindata.setParamSet(createTraindata.getDefaultParamSet(new ParamSet()));
            createTraindata.init();
            createTraindata.run();
        }
        File preprocNew = new File(dirDiagonalLine, "preprocNew");
        File preprocTag2 = new File(dirDiagonalLine, "preprocTag2");
        File preprocOld = new File(dirDiagonalLine, "preprocOld");
        File compare = new File(dirDiagonalLine, "compare");
        {
            IImagePreProcess ppOld = ImagePreprocessDft.getPreProcess(64, 0.5, 20, true);
            ApplyPreProc applyPreProc2 = new ApplyPreProc(snippets.getPath(), preprocOld.getPath(), false, 1, ppOld, false);
            applyPreProc2.setParamSet(applyPreProc2.getDefaultParamSet(null));
            applyPreProc2.init();
            applyPreProc2.run();
        }
        {
            IImagePreProcess ppTag2 = (IImagePreProcess) IO.load(new File(folder, "preproc_tag2.bin"), "de.planet.tech.util.preproc.ContrastNormalizer6", "de.planet.reco.preproc.ContrastNormalizer6");
            ppTag2.setParamSet(ppTag2.getDefaultParamSet(new ParamSet()));
            ppTag2.init();
            ApplyPreProc applyPreProcTag = new ApplyPreProc(snippets.getPath(), preprocTag2.getPath(), false, 1, ppTag2, false);
            applyPreProcTag.setParamSet(applyPreProcTag.getDefaultParamSet(null));
            applyPreProcTag.init();
            applyPreProcTag.run();
        }
        {
            IImagePreProcess ppNew = TrainHtrPlus.getPreProcess(64, 0.5, 24);
            ApplyPreProc applyPreProc = new ApplyPreProc(snippets.getPath(), preprocNew.getPath(), false, 1, ppNew, false);
            applyPreProc.setParamSet(applyPreProc.getDefaultParamSet(null));
            applyPreProc.init();
            applyPreProc.run();
        }
        List<File> filesOld = FileUtil.listFiles(preprocOld, FileUtil.IMAGE_SUFFIXES, false);
        List<File> filesTag2 = FileUtil.listFiles(preprocTag2, FileUtil.IMAGE_SUFFIXES, false);
        List<File> filesNew = FileUtil.listFiles(preprocNew, FileUtil.IMAGE_SUFFIXES, false);
        for (int i = 0; i < filesNew.size(); i++) {
            double[][][] stack = new double[3][][];
            String name = filesOld.get(i).getName();
            stack[0] = HybridImage.newInstance(filesOld.get(i)).getAsInverseGrayMatrix();
            stack[1] = HybridImage.newInstance(filesTag2.get(i)).getAsInverseGrayMatrix();
            stack[2] = HybridImage.newInstance(filesNew.get(i)).getAsInverseGrayMatrix();
            double[][] doubles = MatrixUtil.concatExtV(stack, 1, 1, 0.5);
            HybridImage imgCmp = HybridImage.newInstance(doubles);
            Display.addPanel(new DisplayPlanet(imgCmp), name);
            imgCmp.save(new File(compare, name).getAbsolutePath());
        }
        Display.show();

        System.out.println("done");

    }

    //    @Test
//    public void testID322223() throws MalformedURLException {
//        System.out.println("testID32223");
//        HTRParserPlus parser = new HTRParserPlus();
//        File folder = new File(new File(TestFiles.getPrefix(), "test_htr_bug"), "job_err_id_32223_data");
//        Assume.assumeTrue(folder.exists());
//        File imgPath = FileUtil.listFiles(folder, FileUtil.IMAGE_SUFFIXES, false).get(0);
//        File htr = new File(folder, "HTR");
//        File lr = new File(htr, "lr.txt");
//        List<String> strings = FileUtil.readLines(lr);
//        ObjectCounter<Character> oc = new ObjectCounter<>();
////        for (String string : strings) {
////            for (char c : string.toCharArray()) {
////                boolean surrogate = Character.isSurrogate(c);
////                System.out.println(surrogate + " " + Character.isDefined(c));
////                if (((int) c) == 55357) {
////                    System.out.println("stop");
////                }
////                oc.add(c);
////            }
////        }
//        CharMapUtil.saveCharMap(oc, new File("cm2.txt"));
//        Image img = new Image(imgPath.toURL());
//        PcGtsType page = PageXmlUtil.unmarshal(PageXmlUtil.getXmlPath(imgPath));
//        System.out.println("before:###############################");
//        List<String> text = PageXmlUtil.getText(page);
//        for (String string : text) {
//            System.out.println(string);
//        }
//        parser.process(htr.getAbsolutePath(), null, null, img, page, null, null, null);
//        List<String> recoApply = PageXmlUtil.getText(page);
//        System.out.println("output of network:");
//        for (String line : recoApply) {
//            System.out.println(line);
//        }
//    }
    @Test
    public void testID32312_Innsbruck() throws MalformedURLException {
        System.out.println("testID32312");
        HTRParserPlus parser = new HTRParserPlus();
        File folder = new File(new File(TestFiles.getPrefix(), "test_htr_bug"), "job_err_id_32312_data");
        Assume.assumeTrue(folder.exists());
        PcGtsType page = getPage(folder);
        System.out.println("before:###############################");

        Baseline2PolygonParser b2pParser = new Baseline2PolygonParser(B2PSeamMultiOriented.class.getName());
        b2pParser.process(getImage(folder), page, null, null);

        List<String> text = PageXmlUtil.getText(page);
        for (String string : text) {
            System.out.println(string);
        }
        parser.process(getHTR(folder), getDict(folder), null, getImage(folder), page, null, null, null);
        List<String> recoApply = PageXmlUtil.getText(page);
        System.out.println("output of network:");
        for (String line : recoApply) {
            System.out.println(line);
        }
    }

    @Test
    public void testID32312() throws MalformedURLException {
        System.out.println("testID32312");
        HTRParserPlus parser = new HTRParserPlus();
        File folder = new File(new File(TestFiles.getPrefix(), "test_htr_bug"), "job_err_id_32312_data");
        Assume.assumeTrue(folder.exists());
        PcGtsType page = getPage(folder);
        System.out.println("before:###############################");
        List<String> text = PageXmlUtil.getText(page);
        for (String string : text) {
            System.out.println(string);
        }
        parser.process(getHTR(folder), getDict(folder), null, getImage(folder), page, null, null, null);
        List<String> recoApply = PageXmlUtil.getText(page);
        System.out.println("output of network:");
        for (String line : recoApply) {
            System.out.println(line);
        }
    }

    private String getDict(File folder) {
        File dictFolder = new File(folder, "dict");
        if (dictFolder.exists()) {
            return FileUtil.listFiles(dictFolder, "dict", false).get(0).getAbsolutePath();
        }
        return null;
    }

    private String getHTR(File folder) {
        return new File(folder, "HTR").exists() ? new File(folder, "HTR").getAbsolutePath() : null;
    }

    private Image getImage(File folder) throws MalformedURLException {
        return new Image(getImagePath(folder).toURL());
    }

    private File getImagePath(File folder) {
        List<File> files = FileUtil.listFiles(folder, FileUtil.IMAGE_SUFFIXES, false);
        if (files.size() != 1) {
            throw new RuntimeException("found " + files.size() + "images");
        }
        return files.get(0);
    }

    private PcGtsType getPage(File folder) {
        return PageXmlUtil.unmarshal(PageXmlUtil.getXmlPath(PageXmlUtil.getXmlPath(getImagePath(folder))));
    }

    public static void main(String[] args) throws MalformedURLException {
        TrainHtrPlusTest p = new TrainHtrPlusTest();
//        TrainHtrPlusTest.setUpClass();
        p.test04LoadNetwork();
    }
}
