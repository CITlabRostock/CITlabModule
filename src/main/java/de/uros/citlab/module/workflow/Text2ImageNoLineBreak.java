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
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.module.text2image.Text2ImageParser;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author gundram
 */
public class Text2ImageNoLineBreak extends ParamTreeOrganizer implements Runnable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(Text2ImageNoLineBreak.class.getName());

    @ParamAnnotation(name = "htr", descr = "path to htr (network)")
    private String htrName = "";
    @ParamAnnotation(name = "lr", descr = "language resource (dict...)")
    private String lrName = "";
    private Text2ImageParser text2image;
    //    private ILayoutAnalysis la;
    @ParamAnnotation(name = "cm", descr = "character map (if empty, character map of net is taken")
    private String charMapName = "";
    @ParamAnnotation(name = "in", descr = "folder which contains the images and pageXMLs")
    private String folderIn = "";
    @ParamAnnotation(name = "storage", descr = "folder which can be used for confmat storage")
    private String folderStorage = "";
    //    @ParamAnnotation(name = "ref", descr = "folder which contains the text files")
//    private String folderRef = "";
    @ParamAnnotation(name = "out", descr = "folder where the results should be saved")
    private String folderOut = "";
    private String[] props = null;
    private ObjectCounter<String> oc = new ObjectCounter<>();

    public Text2ImageNoLineBreak() {
        this(null);
    }

    public Text2ImageNoLineBreak(String[] props) {
        this("", "", "", "", "", props);
    }

    public Text2ImageNoLineBreak(String htr, String lr, String charMap, String folderIn, String folderOut, String[] props) {
        setTask(folderIn, folderOut, props);
        htrName = htr;
        lrName = lr;
        charMapName = charMap;
        addReflection(this, Text2ImageNoLineBreak.class);
    }

    @Override
    public void init() {
        super.init(); //To change body of generated methods, choose Tools | Templates.
        this.text2image = new Text2ImageParser();
//        if (laName == null) {
        if (folderOut.isEmpty()) {
            throw new RuntimeException("no output folder given (");
        }
//            this.la = null;
//        } else {
//            this.la = new LayoutAnalysisParser(laName, null);
//            b2p = null;
//        }
    }

    private void setTask(String folderIn, String folderOut, String[] props) {
        this.folderIn = folderIn;
        this.folderOut = folderOut;
        this.props = props;
    }

    public ObjectCounter<Text2ImageParser.Stat> getStat() {
        return text2image.getStat();
    }

    public void resetStat() {
        text2image.resetStat();
    }

    @Override
    public void run() {
        File fIn = new File(this.folderIn);
        File fOut = new File(this.folderOut);
        File fStorage = folderStorage != null && !folderStorage.isEmpty() ? new File(this.folderStorage) : null;
        //find folders to execute
        List<File> foldersExec = FileUtil.getFoldersLeave(fIn);
        Collections.sort(foldersExec);
        LOG.info("found {} folders...", foldersExec.size());
        //for each folder assume multipy images and one txt-file
        for (int i = 0; i < foldersExec.size(); i++) {
            File folderSrc = foldersExec.get(i);
            try {
                LOG.debug("process {}/{} {} ...", i + 1, foldersExec.size(), folderSrc);
                //find txt-file
                List<File> textsSrc = (List<File>) FileUtil.listFiles(folderSrc, "txt".split(" "), false);
                switch (textsSrc.size()) {
                    case 0:
                        LOG.info("in folder {} no txt-file found", folderSrc);
                        continue;
                    case 1:
                        break;
                    default:
                        throw new RuntimeException("found " + textsSrc.size() + " txt-files in folder " + folderSrc + " - expect only 1.");
                }
                //find image files
                List<File> imagesSrc = FileUtil.listFiles(folderSrc, FileUtil.IMAGE_SUFFIXES, false);
                FileUtil.deleteFilesInPageFolder(imagesSrc);
                Collections.sort(imagesSrc, Comparator.comparing(File::toString));
                if (imagesSrc.isEmpty()) {
                    LOG.info("no image found in folder {}.", folderSrc);
                    continue;
                }
                //create tgt structure
                File folderTgt = FileUtil.getTgtFile(fIn, fOut, folderSrc);
                folderTgt.mkdirs();
                List<File> imagesTgt = new LinkedList<>();
                List<File> xmlsTgt = new LinkedList<>();
                List<File> storages = fStorage != null ? new LinkedList<>() : null;
                //copy images, create default xml and apply LA
                for (File fileImgSrc : imagesSrc) {
                    File fileImgTgt = FileUtil.getTgtFile(fIn, fOut, fileImgSrc);
                    if (!fileImgSrc.equals(fileImgTgt)) {
                        FileUtil.copyFile(fileImgSrc, fileImgTgt);
                    }
                    File fileXmlTgt = null;
//                Image imgTgt;
//                try {
//                    imgTgt = new Image(fileImgTgt.toURI().toURL());
//                } catch (MalformedURLException ex) {
//                    throw new RuntimeException(ex);
//                }
                    File fileXmlSrc = PageXmlUtil.getXmlPath(fileImgSrc, true);
                    fileXmlTgt = FileUtil.getTgtFile(fIn, fOut, fileXmlSrc);
                    if (!fileXmlSrc.equals(fileXmlTgt)) {
                        FileUtil.copyFile(fileXmlSrc, fileXmlTgt);
                    }
                    xmlsTgt.add(fileXmlTgt);
                    imagesTgt.add(fileImgTgt);
                    if (storages != null) {
                        storages.add(FileUtil.getTgtFile(fIn, fStorage, fileImgSrc));
                    }
//                if (PropertyUtil.isPropertyTrue(props, Key.DEBUG)) {
//                    BufferedImage imageBufferedImage = imgTgt.getImageBufferedImage(true);
//                    ImageUtil.printPolygons(imageBufferedImage, PageXmlUtil.unmarshal(fileXmlTgt), false, true, true);
//                    ImageUtil.write(imageBufferedImage, "jpg", new File(fileXmlTgt.getAbsoluteFile() + ".jpg"));
//                }
                }
                File textFile = textsSrc.get(0);
                if (htrName != null) {
                    text2image.matchCollection(
                            htrName,
                            lrName,
                            charMapName,
                            textFile.getAbsolutePath(),
                            FileUtil.asStringList(imagesTgt),
                            FileUtil.asStringList(xmlsTgt),
                            folderStorage != null ? FileUtil.asStringList(storages) : null,
                            props);
                }
            } catch (RuntimeException ex) {
                LOG.error("for folder {} not text alignment done ", folderSrc, ex);
            }
        }
    }

    public static void main(String[] args) throws InvalidParameterException, MalformedURLException, IOException, JAXBException {
        File folder = HomeDir.getFile("t2i");//folder containing images and PAGE-XML with baselines
        if (args.length == 0) {
            ArgumentLine al = new ArgumentLine();
            al.addArgument("in", HomeDir.getFile("data/la/t4"));//004/004_070_015
            al.addArgument("out", HomeDir.getFile("/data/t2i"));
            al.addArgument("htr", HomeDir.getFile("nets/8023"));
//            al.addArgument("storage", HomeDir.getFile("data/storage"));
            args = al.getArgs();
        }
        String[] props = null;
        props = PropertyUtil.setProperty(props, Key.T2I_HYPHEN,
                "{\"skipSuffix\":true," +
                        "\"suffixes\":[\"¬\",\"-\",\"–\",\"—\"]," +
                        "\"hypCosts\":6.0}");
//        props = PropertyUtil.setProperty(props, Key.T2I_HYPHEN_LANG, "EN_UK");
        props = PropertyUtil.setProperty(props, Key.T2I_JUMP_BASELINE, "50.0");
        props = PropertyUtil.setProperty(props, Key.T2I_SKIP_WORD, "6.0");
        props = PropertyUtil.setProperty(props, Key.T2I_SKIP_BASELINE, "0.2");
//        props = PropertyUtil.setProperty(props, Key.T2I_MAX_COUNT, "10000000");
//        props = PropertyUtil.setProperty(props, Key.T2I_BEST_PATHES, "200.0");
        props = PropertyUtil.setProperty(props, Key.T2I_THRESH, "-0.02");
//        props = PropertyUtil.setProperty(props, Key.DEBUG, "true");
//        props = PropertyUtil.setProperty(props, Key.DEBUG_DIR, HomeDir.getFile("data/debug").getPath());
//        props = PropertyUtil.setProperty(props, "b2p", "true");
        props = PropertyUtil.setProperty(props, Key.STATISTIC, "true");
        Text2ImageNoLineBreak instance = new Text2ImageNoLineBreak(props);
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
        System.out.println(instance.getStat());
    }

}
