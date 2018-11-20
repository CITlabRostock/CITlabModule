/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.CharMap;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import com.achteck.misc.util.IO;
import de.planet.reco.types.SNetwork;
import de.uros.citlab.module.text2image.Text2ImageParser;
import de.uros.citlab.module.train.TrainHtr;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.CharMapUtil;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PropertyUtil;
import de.uros.citlab.module.util.TrainDataUtil;
import de.uros.citlab.errorrate.util.ObjectCounter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author gundram
 */
public class RunTrainingSemiSupervised extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(RunTrainingSemiSupervised.class.getName());
    @ParamAnnotation(name = "net_in", descr = "path to untrained/pretrained network (if not given charmap have to be set to create default network)")
    private String netIn = "";

    @ParamAnnotation(name = "net_out", descr = "folder to trained networks")
    private String netTrained = "";

    @ParamAnnotation(name = "xml_sup", descr = "path to folder with PAGE-XML files and their images where GT is available")
    private String folderSupervised = "";

    @ParamAnnotation(name = "xml_semi", descr = "path to folder with PAGE-XML files, txt-files and their images where Traindata should be generated automaticly")
    private String folderSemisupervised = "";

    @ParamAnnotation(name = "xml_val", descr = "path to validation folder with PAGE-XML files and their images where GT is available")
    private String folderValidation = "";

    @ParamAnnotation(name = "f_tmp", descr = "path to folder to save traindata")
    private String folderTmp = "";

    @ParamAnnotation(descr = "divide semi supervised traindata in ss sets")
    private int ss = 2;

    @ParamAnnotation(descr = "number of epochs")
    private int epochs = 100;

    @ParamAnnotation(descr = "number of training epochs (one number or ;-sep. with length of parameter 'epochs'")
    private String trainepochs = "5";
    int[] epochs_inner;

    @ParamAnnotation(descr = "early stopping (-1 ==> no early stopping)")
    private int est = -1;

    @ParamAnnotation(descr = "epoch size in training (-1 ==> take all traindata 1 time")
    private int epoch_size = -1;

    @ParamAnnotation(descr = "number of thread to apply Text2Images and create traindata")
    private int threads = 2;

    private String[] props;
//    private Text2ImageParser t2i;

    public RunTrainingSemiSupervised(String[] props) {
        this();
        this.props = props;
    }

    public RunTrainingSemiSupervised() {
        addReflection(this, RunTrainingSemiSupervised.class);
    }

    private File getFolderAndCheckExistance(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        File res = new File(path);
        if (!res.exists() || !res.isDirectory()) {
            throw new RuntimeException("folder '" + path + "' does not exist or is no directory");
        }
        return res;
    }

    @Override
    public void init() {
        super.init();
        String[] split = trainepochs.split(";");
        if (split.length == 1) {
            epochs_inner = new int[epochs];
            for (int i = 0; i < epochs_inner.length; i++) {
                epochs_inner[i] = Integer.parseInt(trainepochs);
            }
        } else {
            if (split.length != epochs) {
                throw new RuntimeException("epochs = " + epochs + " but training epochs '" + trainepochs + "' can only be split into " + split.length + " parts.");
            }
            epochs_inner = new int[epochs];
            for (int i = 0; i < split.length; i++) {
                epochs_inner[i] = Integer.parseInt(split[i]);
            }
        }
    }

    public void run() {
        File fXmlGT = getFolderAndCheckExistance(folderSupervised);
        File fXmlSemi = getFolderAndCheckExistance(folderSemisupervised);
        File fXmlVal = getFolderAndCheckExistance(folderValidation);
        File fSnipets = new File(folderTmp);
        fSnipets.mkdirs();
        File fSnGT = null;
        if (fXmlGT != null) {
            fSnGT = new File(fSnipets, "gt");
            fSnGT.mkdirs();
        }
        File fSnVal = null;
        if (fXmlVal != null) {
            fSnVal = new File(fSnipets, "val");
            fSnVal.mkdirs();
        }
        File folderNets = new File(netTrained);
        folderNets.mkdirs();

        //update CharMap
        //create all nets
        List<File> nets = new LinkedList<>();
        CharMap<Integer> charMap = null;
        {
            File fileNetIn = new File(this.netIn);
            if (fileNetIn.isDirectory()) {
                List<File> listFiles = FileUtil.listFiles(fileNetIn, ".sprnn".split(" "), false);
                if (listFiles.size() != ss) {
                    throw new RuntimeException("found " + listFiles.size() + " files, but expect " + ss + ".");
                }
                nets = listFiles;
                Collections.sort(nets);
            } else {
                List<File> listFilesSemi = FileUtil.listFiles(fXmlSemi, "txt".split(" "), true);
                TrainDataUtil.Statistic stat = TrainDataUtil.getStatistic(listFilesSemi, props);
                if (fXmlGT != null) {
                    List<File> listFilesGT = FileUtil.listFiles(fXmlGT, "xml".split(" "), true);
                    stat.addStatistic(TrainDataUtil.getStatistic(listFilesGT, props));
                }
                charMap = CharMapUtil.getCharMap(stat.getStatChar().getResult(), false);
                for (int i = 0; i < ss; i++) {
                    File net = new File(folderNets, fileNetIn.getName().replace(".sprnn", "_" + i + ".sprnn"));
                    FileUtil.copyFile(fileNetIn, net);
//                    try {
//                        SNetwork load = (SNetwork) IO.load(fileNetIn);
//                        load.setParamSet(load.getDefaultParamSet(null));
//                        load.init();
//                        charMap = load.getCharMap();
//                    } catch (IOException ex) {
//                        java.util.logging.Logger.getLogger(RunTrainingSemiSupervised.class.getName()).log(Level.SEVERE, null, ex);
//                    } catch (ClassNotFoundException ex) {
//                        java.util.logging.Logger.getLogger(RunTrainingSemiSupervised.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                    CharMapUtil.setCharMap(fileNetIn.getPath(), net.getPath(), charMap, -3.0);
                    nets.add(net);
                }
            }
        }

        //find all possible matches (assume collection-layout: each subfolder contains xml, txt and img file (xml can be in subdirectory page/)
        List<File> foldersSemi = new ArrayList<>(ss);
        List<File> fSnSemis = new ArrayList<>(ss);
        for (int i = 0; i < ss; i++) {
            File fSemi = new File(folderTmp, "xml_semi_" + i);
            FileUtils.deleteQuietly(fSemi);
            fSemi.mkdirs();
            foldersSemi.add(fSemi);
            File fSnSemi = new File(folderTmp, "semi_" + i);
//            fSnSemi.mkdirs();
            fSnSemis.add(fSnSemi);
        }
        if (fXmlSemi != null && ss > 0) {
            //divide them into ss sets
            int idx = 0;
            List<File> foldersLeave = FileUtil.getFoldersLeave(fXmlSemi);
            for (File file : foldersLeave) {
                try {
                    File tgtFile = FileUtil.getTgtFile(fXmlSemi, foldersSemi.get(idx++ % ss), file);
                    FileUtils.copyDirectory(file, tgtFile);
                } catch (IOException ex) {
                    throw new RuntimeException("cannot copy folder", ex);
                }
            }
        }
        ExecutorService threadPool = Executors.newFixedThreadPool(threads);

        //create traindata of supervised images and validation images
        List<Future> futuresCreateTraindata = new LinkedList<>();
        LOG.log(Logger.INFO, "Create Traindata Supervised...");
        if (fXmlGT != null) {
            futuresCreateTraindata.add(threadPool.submit(getThreadCreateTraindata(fXmlGT, fSnGT, null, props)));
        }
        if (fXmlVal != null) {
            futuresCreateTraindata.add(threadPool.submit(getThreadCreateTraindata(fXmlVal, fSnVal, null, props)));
        }
        //each epoch do:
        //1. Text2Image for seimsupervised training
        //2. Create Traindata for seimisupervised training
        //3. Train (and validate) Networks
        for (int i = 0; i < epochs; i++) {
            LOG.log(Logger.INFO, "Text2Image...");
            {
                //text to image of semi supervised pages
                List<Future> futuresText2Image = new LinkedList<>();
                List<Text2ImageNoLineBreak> worker = new LinkedList<>();
                String[] propsT2I = PropertyUtil.getSubTreeProperty(props, "t2i");
                for (int j = 0; j < ss; j++) {
                    File file = foldersSemi.get(j);
                    File net = nets.get(j);
                    Text2ImageNoLineBreak t2i = new Text2ImageNoLineBreak(net.getAbsolutePath(), null, null, file.getAbsolutePath(), file.getAbsolutePath(), propsT2I);
                    t2i.setParamSet(t2i.getDefaultParamSet(new ParamSet()));
                    t2i.init();
                    worker.add(t2i);
                    futuresText2Image.add(threadPool.submit(t2i));
                }
                //ensure processes finished
                for (Future f : futuresText2Image) {
                    try {
                        f.get();
                    } catch (InterruptedException | ExecutionException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                futuresText2Image.clear();
                ObjectCounter<Text2ImageParser.Stat> stat = new ObjectCounter<>();
                for (Text2ImageNoLineBreak text2ImageCollection : worker) {
                    LOG.log(Logger.DEBUG, "statistic of submodule: " + text2ImageCollection.getStat().toString());
                    stat.addAll(text2ImageCollection.getStat());
                }
                LOG.log(Logger.INFO, "statistic of Text2Image: " + stat.toString());
            }
            LOG.log(Logger.INFO, "Text2Image... [DONE]");
            //create traindata from semi-supervised xmls
            LOG.log(Logger.INFO, "Create Traindata Semisupervised...");
            for (int j = 0; j < foldersSemi.size(); j++) {
                File folderXml = foldersSemi.get(j);
                File folderSnippets = fSnSemis.get(j);
                FileUtils.deleteQuietly(folderSnippets);
                folderSnippets.mkdirs();
                futuresCreateTraindata.add(threadPool.submit(getThreadCreateTraindata(folderXml, folderSnippets, null, props)));
            }
            //ensure processes finished (all traindata created)
            for (Future f : futuresCreateTraindata) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException ex) {
                    throw new RuntimeException(ex);
                }
            }
            futuresCreateTraindata.clear();
            if (LOG.isInfoEnabled()) {
                LOG.log(Logger.INFO, "(Create Traindata Supervised... [DONE])");
                LOG.log(Logger.INFO, "Create Traindata Semisupervised... [DONE]");
                LOG.log(Logger.INFO, "found " + FileUtil.getFilesListsOrFolders(getPathesExceptOne(null, fSnSemis, -1), FileUtil.IMAGE_SUFFIXES, true).size() + " automatical assigned training sampls");
                LOG.log(Logger.INFO, "found " + FileUtil.getFilesListsOrFolders(getPathesExceptOne(fSnGT, fSnSemis, -1), FileUtil.IMAGE_SUFFIXES, true).size() + " training sampls in total ");
            }

            //train networks
            if (epochs_inner[i] > 0) {
                //set new charmap in first training epoch
                {
                    for (File net : nets) {
                        CharMapUtil.setCharMap(net.getPath(), net.getPath(), charMap, -5.0);
                    }
                }
                LOG.log(Logger.INFO, "Train Nets...");
                List<Future> fu_train = new LinkedList<>();
                String[] propsTrain = PropertyUtil.setProperty(null, Key.NOISE, "both");
                propsTrain = PropertyUtil.setProperty(propsTrain, Key.EPOCHS, String.valueOf(epochs_inner[i]));
                propsTrain = PropertyUtil.setProperty(propsTrain, Key.EST, String.valueOf(est));
                int threadsTmp = this.threads;
                propsTrain = PropertyUtil.setProperty(propsTrain, Key.TRAINSIZE, String.valueOf(epoch_size));
                for (int j = 0; j < ss; j++) {
                    int threadsSS = Math.max(1, threadsTmp / (ss - j));
                    propsTrain = PropertyUtil.setProperty(propsTrain, Key.THREADS, String.valueOf(threadsSS));
                    threadsTmp -= threadsSS;
                    String net = nets.get(j).getAbsolutePath();
                    fu_train.add(
                            threadPool.submit(
                                    getThreadTrainHTR(
                                            net,
                                            net,
                                            getPathesExceptOne(fSnGT, fSnSemis, j),
                                            fSnVal == null ? null : fSnVal.getAbsolutePath(),
                                            propsTrain
                                    )
                            )
                    );
                }
                for (Future f : fu_train) {
                    try {
                        f.get();
                    } catch (InterruptedException | ExecutionException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                LOG.log(Logger.INFO, "Train Nets... [DONE]");
            } else {
                LOG.log(Logger.WARN, "no training done for semi-supervised learning");
            }
        }
        threadPool.shutdown();
    }

    private String getPathesExceptOne(File pathGT, List<File> pathes, int idxExcept) {
        StringBuilder sb = new StringBuilder();
        if (pathGT != null) {
            sb.append(pathGT.getAbsolutePath()).append(File.pathSeparator);
        }
        for (int i = 0; i < pathes.size(); i++) {
            if (i != idxExcept) {
                sb.append(pathes.get(i).getAbsolutePath()).append(File.pathSeparator);
            } else if (pathes.size() == 1) {
                LOG.log(Logger.WARN, "only one subpath given - take this anyway (expect #subsets = 1)");
                sb.append(pathes.get(i).getAbsolutePath()).append(File.pathSeparator);
            }
        }
        String all = sb.toString();
        if (all.isEmpty()) {
            throw new RuntimeException("generated pathes are empty - input was pathGT = '" + pathGT + "' pathes Semi = '" + pathes + "' idxExcept = " + idxExcept + ".");
        }
        return all.substring(0, all.length() - 1);
    }

    private static Thread getThreadTrainHTR(final String pathToModelsIn, final String pathToModelsOut, final String inputTrainDir, final String inputValDir, final String[] props) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TrainHtr htr = new TrainHtr();
//                    String[] props1 = PropertyUtil.setProperty(props, "viewer", "true");
                    htr.trainHtr(pathToModelsIn, pathToModelsOut, inputTrainDir, inputValDir, props);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    System.exit(-1);
                }
            }
        });
    }

    private static Thread getThreadCreateTraindata(final File folderPageXml, final File folderSnipets, final File charMap, final String[] props) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileUtils.deleteQuietly(folderSnipets);
                    folderSnipets.mkdirs();
                    int res = TrainDataUtil.runCreateTraindata(folderPageXml, folderSnipets, charMap, props);
                    if (res == 0) {
                        LOG.log(Logger.WARN, "copy folder - assume that folder " + folderPageXml.getAbsolutePath() + " contains snipets.");
                        FileUtils.copyDirectory(folderPageXml, folderSnipets);
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    System.exit(-1);
                }
            }
        }, folderPageXml.getPath()
        );
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidParameterException {
        String english = false ? "english" : "french";
        File folder = HomeDir.getFile("data/transkribus_beckett_training");//folder containing images and PAGE-XML with baselines
        String[] props = null;
        if (args.length == 0) {
            ArgumentLine al = new ArgumentLine();
//            al.addArgument("xml_sup", prefix + "data/train");
            al.addArgument("xml_semi", new File(folder, english + "_la"));
            al.addArgument("xml_val", new File(folder, english + "_val"));
            al.addArgument("f_tmp", new File(folder, english + "_tmp2"));
            al.addArgument("net_in", HomeDir.getFile("nets/meganet_37.sprnn"));
            al.addArgument("net_out", new File(folder, "nets/" + english));
            al.addArgument("ss", "4");//0.0025 ~ exp(-6)
            al.addArgument("trainepochs", "1;1;2;3;5;8;13;21;34;55");
            al.addArgument("epochs", "10");//0.0025 ~ exp(-6)
            al.addArgument("est", "10");//0.0025 ~ exp(-6)
            al.addArgument("threads", "4");//0.0025 ~ exp(-6)
            al.addArgument("epoch_size", "2000");//0.0025 ~ exp(-6)
//        al.setHelp();
            args = al.getArgs();
        }
        {
            String[] propsT2I = null;
//        props = PropertyUtil.setProperty(props, Key.T2I_HYPHEN, "6.0");
//        props = PropertyUtil.setProperty(props, Key.T2I_HYPHEN_LANG, "DE");
//        props = PropertyUtil.setProperty(props, Key.T2I_JUMP_BASELINE, "5.0");
            propsT2I = PropertyUtil.setProperty(propsT2I, Key.T2I_SKIP_WORD, "3.0");
            propsT2I = PropertyUtil.setProperty(propsT2I, Key.T2I_SKIP_BASELINE, "0.3");
            propsT2I = PropertyUtil.setProperty(propsT2I, Key.T2I_BEST_PATHES, "200.0");
            propsT2I = PropertyUtil.setProperty(propsT2I, Key.T2I_THRESH, "0.01");
            propsT2I = PropertyUtil.setProperty(propsT2I, Key.DEBUG, "true");
            propsT2I = PropertyUtil.setProperty(propsT2I, Key.DEBUG_DIR, new File(folder, english + "_debug").getPath());
//        props = PropertyUtil.setProperty(props, "b2p", "true");
            propsT2I = PropertyUtil.setProperty(propsT2I, Key.STATISTIC, "true");
            props = PropertyUtil.setSubTreeProperty(props, propsT2I, "t2i");
        }
        RunTrainingSemiSupervised instance = new RunTrainingSemiSupervised(props);
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
