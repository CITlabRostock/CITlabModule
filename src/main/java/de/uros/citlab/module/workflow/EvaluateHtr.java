/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.uros.citlab.errorrate.HtrError;
import de.uros.citlab.errorrate.types.Metric;
import de.uros.citlab.errorrate.types.Result;
import de.uros.citlab.languagemodel.TrainLM;
import de.uros.citlab.module.baseline2polygon.B2PSeamMultiOriented;
import de.uros.citlab.module.baseline2polygon.Baseline2PolygonParser;
import de.uros.citlab.module.htr.HTRParser;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.*;
import eu.transkribus.interfaces.IBaseline2Polygon;
import eu.transkribus.interfaces.IHtr;
import eu.transkribus.interfaces.types.Image;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author gundram
 */
public class EvaluateHtr extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(EvaluateHtr.class.getName());

    @ParamAnnotation(descr = "path to htr (if htr is empty, assume that folder already contains transcribed images)")
    private String htr;
    @ParamAnnotation(descr = "path to dictionary")
    private String lr;
    @ParamAnnotation(descr = "path to folder containing images and pageXML files")
    private String gt;
    @ParamAnnotation(descr = "path to folder where changed pageXML files should be saved")
    private String out;
    @ParamAnnotation(descr = "calculate wer")
    private boolean wer = false;

    @ParamAnnotation(descr = "create debug images")
    private boolean debug;

    private IHtr htrInstance;
    private IBaseline2Polygon b2p;
    private File folderIn;
    private File folderOut;
//    private boolean createDebug;

    public EvaluateHtr() {
        this("", "", "", "", false);
    }

    public EvaluateHtr(String htr, String lr, String gt, String out, boolean createDebugImg) {
        this.htrInstance = new HTRParser();
        b2p = new Baseline2PolygonParser(B2PSeamMultiOriented.class.getName());
        this.gt = gt;
        this.out = out;
        this.lr = lr;
        debug = createDebugImg;
        addReflection(this, EvaluateHtr.class);
    }

    @Override
    public void init() {
        super.init();
//        if (htr.isEmpty()) {
//            throw new RuntimeException("no htr given");
//        }
        if (gt.isEmpty()) {
            throw new RuntimeException("no input folder given");
        }
        if (out.isEmpty()) {
            throw new RuntimeException("no output folder given");
        }
        folderIn = new File(gt);
        folderOut = new File(out);
    }

    public double run(String[] props) throws MalformedURLException, JAXBException, IOException {
        boolean runHTR = true;
        List<String> refs = new LinkedList<>();
        List<String> recos = new LinkedList<>();
        List<File> listFiles = FileUtil.listFiles(folderIn, "xml", true);
        FileUtil.deleteMetadataAndMetsFiles(listFiles);
        Collections.sort(listFiles);
        if (runHTR) {
            folderOut.mkdirs();
            LOG.info("found {} images...", listFiles.size());
            if (htr != null && !htr.isEmpty()) {
                for (File fileInXml : listFiles) {
                    File fileInImg = PageXmlUtil.getImagePath(fileInXml, true);
                    File fileOutImg = FileUtil.getTgtFile(folderIn, folderOut, fileInImg);
                    File fileOutXml = FileUtil.getTgtFile(folderIn, folderOut, fileInXml);
                    refs.add(fileInXml.getPath());
                    recos.add(fileOutXml.getPath());
                    FileUtil.copyFile(fileInImg, fileOutImg);
                    FileUtil.copyFile(fileInXml, fileOutXml);
                    Image img = new Image(fileOutImg.toURI().toURL());
                    b2p.process(img, fileOutXml.getAbsolutePath(), null, null);
                    htrInstance.process(htr, lr, null, img, fileOutXml.getAbsolutePath(), null, null, props);
                    if (debug) {
                        BufferedImage imageBufferedImage = img.getImageBufferedImage(true);
                        imageBufferedImage = ImageUtil.getDebugImage(imageBufferedImage, PageXmlUtil.unmarshal(fileOutXml), 1.0, false, false, false, true, true);
                        ImageIO.write(imageBufferedImage, "png", new File(fileOutXml.getAbsoluteFile() + ".png"));
                    }
                }
            } else {
                refs = FileUtil.getStringList(listFiles);
                List<File> listFiles2 = FileUtil.listFiles(folderOut, "xml", true);
                FileUtil.deleteMetadataAndMetsFiles(listFiles2);
                Collections.sort(listFiles2);
                recos = FileUtil.getStringList(listFiles2);
                if (recos.size() != refs.size()) {
                    throw new RuntimeException("gt folder (" + refs.size() + ") and hyp folder (" + recos.size() + ") do not have the same number of files");
                }
            }
        } else {
            for (File fileInXml : listFiles) {
                File fileOutXml = FileUtil.getTgtFile(folderIn, folderOut, fileInXml);
                refs.add(fileInXml.getPath());
                recos.add(fileOutXml.getPath());
            }
        }
        File fileRef = new File("ref.lst");
        File fileReco = new File("reco.lst");
        FileUtil.writeLines(fileRef, refs);
        FileUtil.writeLines(fileReco, recos);
        for (int i = 0; i < recos.size(); i++) {
            List<String> recoList = PageXmlUtil.getText(PageXmlUtil.unmarshal(new File(recos.get(i))));
            List<String> refList = PageXmlUtil.getText(PageXmlUtil.unmarshal(new File(refs.get(i))));
            FileUtil.writeLines(new File("ref.txt"), refList);
            FileUtil.writeLines(new File("raw.txt"), recoList);

        }
        HtrError erp = new HtrError();
        Result err = erp.run(("ref.lst reco.lst" + (wer ? " -w" : "")).split(" "));
        FileUtils.deleteQuietly(fileRef);
        FileUtils.deleteQuietly(fileReco);
        return err.getMetric(Metric.ERR) * 100;
    }

    public static void createLM_NGram(List<File> filesXML, File out) {
        List<String> text = new LinkedList<>();
        for (File file : filesXML) {
            text.addAll(PageXmlUtil.getText(PageXmlUtil.unmarshal(file)));
        }
        TrainLM.train(text, 6, out);
    }

    public static void main(String[] args) throws InvalidParameterException, MalformedURLException, IOException, JAXBException, InterruptedException {
        HomeDir.setPath("/home/gundram/devel/projects/read");
        boolean createLangMods = false;
        if (createLangMods) {
            List<File> xml = FileUtil.listFiles(HomeDir.getFile("data/TRAIN_CITlab_Konzilsprotokolle_M3/"), "xml", true);
            FileUtil.deleteMetadataAndMetsFiles(xml);
            TrainDataUtil.createTrainData(
                    FileUtil.asStringList(xml),
                    HomeDir.getFile("data/LM_Konzilsprotokolle/").getAbsolutePath(),
                    null,
                    PropertyUtil.setProperty(
                            PropertyUtil.setProperty(
                                    null,
                                    Key.CREATEDICT,
                                    true),
                            Key.CREATETRAINDATA,
                            false)
            );
            createLM_NGram(xml, new File(HomeDir.getFile("data/LM_Konzilsprotokolle/"), "dict.arp"));
            List<File> xml2 = FileUtil.listFiles(HomeDir.getFile("data/TRAIN_CITlab_Bentham_himself_M4/"), "xml", true);
            FileUtil.deleteMetadataAndMetsFiles(xml2);
            TrainDataUtil.createTrainData(FileUtil.asStringList(xml2), HomeDir.getFile("data/LM_Bentham/").getAbsolutePath(), null, PropertyUtil.setProperty(PropertyUtil.setProperty(null, Key.CREATEDICT, true), Key.CREATETRAINDATA, false));
            createLM_NGram(xml2, new File(HomeDir.getFile("data/LM_Bentham/"), "dict.arp"));
            return;

        }
        for (String szenario : new String[]{
/*                "Bentham_ARPA",*/
                "Konzilsprotokolle_ARPA"/*,
                "Bentham_CSV",
                "Bentham_RAW",
                "Konzilsprotokolle_CSV",
                "Konzilsprotokolle_RAW"*/}) {
            File folderGT = null, folderOut = null, fileLR = null, fileHtr = null;
            switch (szenario) {
                case "Bentham_CSV":
                    folderGT = HomeDir.getFile("data/TEST_CITlab_Bentham_himself_M4/");
                    folderOut = HomeDir.getFile("data/TEST_CITlab_Bentham_himself_M4_CSV/");
                    fileHtr = new File("src/test/resources/test_htr/Bentham");
                    fileLR = HomeDir.getFile("data/LM_Bentham/dict.csv");
                    break;
                case "Bentham_ARPA":
                    folderGT = HomeDir.getFile("data/TEST_CITlab_Bentham_himself_M4/");
                    folderOut = HomeDir.getFile("data/TEST_CITlab_Bentham_himself_M4_ARPA/");
                    fileHtr = new File("src/test/resources/test_htr/Bentham");
                    fileLR = HomeDir.getFile("data/LM_Bentham/dict.arp");
                    break;
                case "Bentham_RAW":
                    folderGT = HomeDir.getFile("data/TEST_CITlab_Bentham_himself_M4/");
                    folderOut = HomeDir.getFile("data/TEST_CITlab_Bentham_himself_M4_RAW/");
                    fileHtr = new File("src/test/resources/test_htr/Bentham");
                    fileLR = null;
                    break;
                case "Konzilsprotokolle_CSV":
                    folderGT = HomeDir.getFile("data/TEST_CITlab_Konzilsprotokolle_M3/");
                    folderOut = HomeDir.getFile("data/TEST_CITlab_Konzilsprotokolle_M3_CSV/");
                    fileHtr = new File("src/test/resources/test_htr/Konzilsprotokolle_M4");
                    fileLR = HomeDir.getFile("data/LM_Konzilsprotokolle/dict.csv");
                    break;

                case "Konzilsprotokolle_ARPA":
                    folderGT = HomeDir.getFile("data/TEST_CITlab_Konzilsprotokolle_M3/");
                    folderOut = HomeDir.getFile("data/TEST_CITlab_Konzilsprotokolle_M3_ARPA/");
                    fileHtr = new File("src/test/resources/test_htr/Konzilsprotokolle_M4");
                    fileLR = HomeDir.getFile("data/LM_Konzilsprotokolle/dict.arp");
                    break;
                case "Konzilsprotokolle_RAW":
                    folderGT = HomeDir.getFile("data/TEST_CITlab_Konzilsprotokolle_M3/");
                    folderOut = HomeDir.getFile("data/TEST_CITlab_Konzilsprotokolle_M3_RAW/");
                    fileHtr = new File("src/test/resources/test_htr/Konzilsprotokolle_M4");
                    fileLR = null;
                    break;
                default:
                    throw new RuntimeException("no szenario '" + szenario + "'");

            }

            ArgumentLine al = new ArgumentLine();
            al.addArgument("gt", folderGT);
            al.addArgument("out", folderOut);
            al.addArgument("htr", fileHtr);
            if (fileLR != null) {
                al.addArgument("lr", fileLR);
            }
            args = al.getArgs();
            EvaluateHtr instance = new EvaluateHtr();
            ParamSet ps = new ParamSet();
            ps.setCommandLineArgs(args);    // allow early parsing
            ps = instance.getDefaultParamSet(ps);
            ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
            instance.setParamSet(ps);
            instance.init();
            double run = instance.run(null);
            String res = szenario + "," + run + "," + Arrays.toString(al.getArgs());
            System.out.println(res);
            FileUtil.writeLines(HomeDir.getFile("cmpLM.csv"), Arrays.asList(res), true);
        }
    }

}
