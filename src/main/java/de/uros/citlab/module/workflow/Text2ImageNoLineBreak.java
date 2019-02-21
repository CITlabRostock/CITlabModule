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
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author gundram
 */
public class Text2ImageNoLineBreak extends ParamTreeOrganizer implements Runnable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(Text2ImageNoLineBreak.class.getName());

    @ParamAnnotation(descr = "link image instead of copy")
    private boolean link = true;
    @ParamAnnotation(name = "htr", descr = "path to htr (network)")
    private String htrName = "";
    @ParamAnnotation(name = "lr", descr = "language resource (dict...)")
    private String lrName = "";
    //    private ILayoutAnalysis la;
    @ParamAnnotation(name = "cm", descr = "character map (if empty, character map of net is taken")
    private String charMapName = "";
    @ParamAnnotation(name = "in", descr = "folder which contains the images and pageXMLs")
    private String folderIn = "";
    @ParamAnnotation(name = "storage", descr = "folder which can be used for confmat storage")
    private String folderStorage = "";
    @ParamAnnotation(descr = "number of threads")
    private int t = 3;
    //    @ParamAnnotation(name = "ref", descr = "folder which contains the text files")
//    private String folderRef = "";
    @ParamAnnotation(name = "out", descr = "folder where the results should be saved")
    private String folderOut = "";
    private String[] props = null;
    private ObjectCounter<String> oc = new ObjectCounter<>();
    private List<Runner> runners = new LinkedList<>();
    private File fIn;
    private File fOut;
    private File fStorage;

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
        ObjectCounter<Text2ImageParser.Stat> res = new ObjectCounter<>();
        for (Runner runner : runners) {
            res.addAll(runner.getStat());
        }
        return res;
    }

    public void resetStat() {
        for (Runner runner : runners) {
            runner.resetStat();
        }
    }

    @Override
    public void run() {
        fIn = new File(this.folderIn);
        fOut = new File(this.folderOut);
        fStorage = folderStorage != null && !folderStorage.isEmpty() ? new File(this.folderStorage) : null;
        //find folders to execute
        List<File> foldersExec = FileUtil.getFoldersLeave(fIn);
        List<Task> tasks = new LinkedList<>();
        Collections.sort(foldersExec);
        LOG.info("found {} folders...", foldersExec.size());
        //for each folder assume multipy images and one txt-file
        for (int i = 0; i < foldersExec.size(); i++) {
            tasks.add(new Task(foldersExec.get(i), i, foldersExec.size()));
        }
        ExecutorService threadPool = Executors.newFixedThreadPool(t);
        Iterator<Task> iterator = tasks.iterator();
        List<Future> results = new LinkedList<>();
        for (int j = 0; j < t; j++) {
            Runner runner = new Runner(iterator);
            runners.add(runner);
            results.add(threadPool.submit(runner));
        }
        threadPool.shutdown();

    }

    private static class Task {
        File folder;
        int idx;
        int size;

        public Task(File folder, int idx, int size) {
            this.folder = folder;
            this.idx = idx;
            this.size = size;
        }
    }

    private class Runner implements Runnable {
        private Iterator<Task> iterator;
        private Text2ImageParser text2ImageParser = new Text2ImageParser();

        public Runner(Iterator<Task> iterator) {
            this.iterator = iterator;
        }

        @Override
        public void run() {
            while (true) {
                Task task = null;
                synchronized (iterator) {
                    if (iterator.hasNext()) {
                        task = iterator.next();
                    }
                }
                try {
                    LOG.debug("process {}/{} {} ...", task.idx + 1, task.size, task.folder);
                    //find txt-file
                    List<File> textsSrc = (List<File>) FileUtil.listFiles(task.folder, "txt".split(" "), false);
                    switch (textsSrc.size()) {
                        case 0:
                            LOG.info("in folder {} no txt-file found", task.folder);
                            continue;
                        case 1:
                            break;
                        default:
                            throw new RuntimeException("found " + textsSrc.size() + " txt-files in folder " + task.folder + " - expect only 1.");
                    }
                    //find image files
                    List<File> imagesSrc = FileUtil.listFiles(task.folder, FileUtil.IMAGE_SUFFIXES, false);
                    FileUtil.deleteFilesInPageFolder(imagesSrc);
                    Collections.sort(imagesSrc, Comparator.comparing(File::toString));
                    if (imagesSrc.isEmpty()) {
                        LOG.info("no image found in folder {}.", task.folder);
                        continue;
                    }
                    //create tgt structure
                    File folderTgt = FileUtil.getTgtFile(fIn, fOut, task.folder);
                    if (folderTgt.exists()) {
                        LOG.warn("Folder {} already exists, task already solved.", folderTgt);
                        continue;
                    }
                    folderTgt.mkdirs();
                    List<File> imagesTgt = new LinkedList<>();
                    List<File> xmlsTgt = new LinkedList<>();
                    List<File> storages = fStorage != null ? new LinkedList<>() : null;
                    //copy images, create default xml and apply LA
                    for (File fileImgSrc : imagesSrc) {
                        File fileImgTgt = FileUtil.getTgtFile(fIn, fOut, fileImgSrc);
                        fileImgTgt.getParentFile().mkdirs();
                        if (!fileImgSrc.equals(fileImgTgt)) {
                            if (link) {
                                FileUtils.deleteQuietly(fileImgTgt);
                                Path toPath = fileImgSrc.getAbsoluteFile().toPath();
                                try {
                                    while (true) {
                                        toPath = Files.readSymbolicLink(toPath);
                                    }
                                } catch (Throwable ex) {
                                }
                                Files.createSymbolicLink(fileImgTgt.toPath(), fileImgSrc.getAbsoluteFile().toPath());
                            } else {
                                FileUtils.copyFile(fileImgSrc, fileImgTgt);
                            }
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
                        text2ImageParser.matchCollection(
                                htrName,
                                lrName,
                                charMapName,
                                textFile.getAbsolutePath(),
                                FileUtil.asStringList(imagesTgt),
                                FileUtil.asStringList(xmlsTgt),
                                folderStorage != null ? FileUtil.asStringList(storages) : null,
                                props);
                    }
                } catch (RuntimeException | IOException ex) {
                    LOG.error("for folder {} not text alignment done ", task.folder, ex);
                }
            }
        }

        public ObjectCounter<Text2ImageParser.Stat> getStat() {
            return text2ImageParser.getStat();
        }

        public void resetStat() {
            text2ImageParser.resetStat();
        }
    }

    public static void main(String[] args) throws InvalidParameterException, MalformedURLException, IOException, JAXBException {
        File out = null;
        String[] props = null;
        if (args.length == 0) {
            out = HomeDir.getFile("debug/RO/geo");
            ArgumentLine al = new ArgumentLine();
            al.addArgument("in", HomeDir.getFile("data/LA"));//004/004_070_015
            al.addArgument("out", HomeDir.getFile("data/T2I/RO/geo"));
            al.addArgument("htr", HomeDir.getFile("models/geo"));
//            al.addArgument("storage", HomeDir.getFile("data/storage"));
            props = PropertyUtil.setProperty(props, Key.T2I_BEST_PATHES, "400.0");
            args = al.getArgs();
        } else if (args.length == 2 || args.length == 3) {
            switch (args[0]) {
                case "RO_HYP":
                    props = PropertyUtil.setProperty(props, Key.T2I_HYPHEN,
                            "{\"skipSuffix\":true," +
                                    "\"suffixes\":[\"¬\",\"-\",\"–\",\"—\"]," +
                                    "\"hypCosts\":6.0}");
                case "RO":
                    props = PropertyUtil.setProperty(props, Key.T2I_BEST_PATHES, "400.0");
                    break;
                case "NO_RO_HYP":
                    props = PropertyUtil.setProperty(props, Key.T2I_HYPHEN,
                            "{\"skipSuffix\":true," +
                                    "\"suffixes\":[\"¬\",\"-\",\"–\",\"—\"]," +
                                    "\"hypCosts\":6.0}");
                case "NO_RO":
                    props = PropertyUtil.setProperty(props, Key.T2I_JUMP_BASELINE, "50.0");
                    break;
                default:
                    throw new RuntimeException("cannot interprete " + args[0] + ".");
            }
            Integer idx = Integer.valueOf(args[1]);
            ArgumentLine al = new ArgumentLine();
            al.addArgument("in", HomeDir.getFile("data/LA"));//004/004_070_015
            al.addArgument("out", HomeDir.getFile("data/T2I/" + args[0] + "_net" + idx));
            al.addArgument("htr", HomeDir.getFile("models/" + args[0] + "_net" + idx));
            if (args.length == 3) {
                al.addArgument("t", args[2]);
            }
            args = al.getArgs();
        } else {
            throw new RuntimeException("needs 2 arguments for (branch of T2I was merged )");
        }
//        props = PropertyUtil.setProperty(props, Key.T2I_HYPHEN,
//                "{\"skipSuffix\":true," +
//                        "\"suffixes\":[\"¬\",\"-\",\"–\",\"—\"]," +
//                        "\"hypCosts\":6.0}");
//        props = PropertyUtil.setProperty(props, Key.T2I_HYPHEN_LANG, "EN_UK");
//        props = PropertyUtil.setProperty(props, Key.T2I_JUMP_BASELINE, "50.0");
        props = PropertyUtil.setProperty(props, Key.T2I_SKIP_WORD, "6.0");
        props = PropertyUtil.setProperty(props, Key.T2I_SKIP_BASELINE, "0.2");
//        props = PropertyUtil.setProperty(props, Key.T2I_MAX_COUNT, "10000000");
        props = PropertyUtil.setProperty(props, Key.T2I_THRESH, "-0.05");
        if (out != null) {
            props = PropertyUtil.setProperty(props, Key.DEBUG, "true");
            props = PropertyUtil.setProperty(props, Key.DEBUG_DIR, out);
        }
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
