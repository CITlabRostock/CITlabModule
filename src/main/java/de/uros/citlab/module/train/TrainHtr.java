/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.train;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import de.planet.trainer.Trainer;
import de.planet.trainer.factory.SNetworkFactory;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.*;
import eu.transkribus.interfaces.ITrainHtr;

import java.io.File;
import java.util.Arrays;
import java.util.Observable;

/**
 * @author gundram
 */
public class TrainHtr extends Observable implements ITrainHtr {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(TrainHtr.class.getName());
    private static final String INTERN_SPRNN = "net.sprnn";
    private static final String INTERN_SPRNN_BEST = "best_net.sprnn";
    public static final String EVENT_PRESENT_OBSERVABLE = "present_observable";
    private static final String provider = "University of Rostock\nInstitute of Mathematics\nCITlab\nGundram Leifert\ngundram.leifert@uni-rostock.de";
    private static final String name = TrainHtr.class.getName();

    public static class Status {

        public int epoch;
        public double metricTrain;
        public double metricTest;
        public String metric;
        public boolean bestNet;
        public Long processId;

        public Status(int epoch, double metricTrain, double metricTest, String metric, boolean bestNet, Long processId) {
            this.epoch = epoch;
            this.metricTrain = metricTrain;
            this.metricTest = metricTest;
            this.metric = metric;
            this.bestNet = bestNet;
            this.processId = processId;
        }

        @Override
        public String toString() {
            if (epoch == 0) {
                return String.format("epoch %d: %s train / test: ?%% / ?%%", epoch, metric);
            }
            return String.format("epoch %d: %s train / test: %.2f%% / %.2f%%", epoch, metric, metricTrain * 100, metricTest * 100);
        }

    }

    public TrainHtr() {
//        addReflection(this, TrainHtrSGD.class);
    }

    @Override
    public void notifyObservers(Object arg) {
        super.setChanged();
        super.notifyObservers(arg);
    }

    @Override
    public void createTrainData(String[] pageXmls, String outputDir, String pathToCharMap, String[] props) {
        TrainDataUtil.createTrainData(pageXmls, outputDir, pathToCharMap, props);
    }

    @Override
    public String usage() {
        return "This class has three methods: Creating trainingsdata, creating an HTR and training an HTR.\n"
                + "CREATING TRAINING DATA:\n"
                + "For a list of paths to PAGE-XML files the method process these files and saves the result into the given folder. "
                + "It is assumed that the corresponding image file has the suffixes " + Arrays.asList(FileUtil.IMAGE_SUFFIXES) + " and are located either in the same or in the parent folder. "
                + "\n"
                + "If pathToCharMap is set, a cannonical CharMap-file will be constructed and saved to the given path.\n"
                + "The following properties can be set:\n"
                + Key.TRAIN_STATUS + "=STATUS_1;STATUS_2;STATUS_3 (default:[empty])\n"
                + "If a value is given, only traindata are generated from pages, where the status has the given status values. "
                + "If one wants to accept multiply statuses, they can be given ;-separated (e.g. GT;DONE). "
                + "The values are case-insensitive\n"
                + Key.NORMALIZE_FORM + "=NFD|NFC|NFKD|NFKC (default:[empty])\n"
                + "As default no normalisation will be done.\n"
                + "the references are normalized using a Stringnormalizer. Specific characters, which are unable to put into the recognition process are deleted.\n"
                + Key.STATISTIC + "=true|false (default:false)\n"
                + "As default no character statistic will be done.\n"
                + "When charstat is set to true, statistic (unigram) of the characters containing in this documents will be done and saved in the folder to the files " + TrainDataUtil.cmLong + "'\n"
                + Key.CREATEDICT + "=true|false (default: false)\n"
                + "As default no dictionary will be saved.\n"
                + "When dict is set to true, a dictionary with all pure letter-sequences is generated (unigram) and saved to " + TrainDataUtil.dict + " of the given folder. "
                + "In addition a 1-gram in arpa-format is saved to " + TrainDataUtil.dictArpa + ". Both dictionaries can be used for the language model - but different loader have to be used.\n"
                + Key.CREATETRAINDATA + "=true|false (default:true)\n"
                + "As default traindata will be constructed.\n"
                + "When traindata is NOT set to false, from each image all textlines having groundtruth (Unicode) will be saved in the given folder. "
                + "For each sample there will be an image <IMAGE>=<ImageFilename>_<LineID>.png, the corresponding groundtruth <IMAGE>.txt and additional information <IMAGE>.info.\n"
                + "\n"
                + "CREATING HTR-MODEL:\n"
                + "creates an HTR-model ready for training and saves it to the path given by 'pathToModelsOut'.\n"
                + "To define the characters, the method requires a path to a character-map-file: This file have to have lines '<CODE>=<CHANNEL-NO>' where <CODE> defines one single character.\n"
                + "If <CODE> is an \"=\" or a \"\\\", it has to be escaped by \"\\\".\n"
                + "Using Properties the following things can be configured:\n"
                + Key.PATH_NET + "=<PATH-String>: If an existing htr should be used instead of initialize a new htr, a path to a folder,  must be given.\n"
                //                + "NumFeatMaps=<POS_NUMBER>: If the input is not an image but a more dimensional feature map, one can specify the number of feature maps here. It is not supported so far.\n"
                //                + "RelScaleHiddenUnits=<Double>: Depending on the task one can increase the size of the SPRNN (default=1.0).\n"
                + "\n"
                + "TRAINING HTR-MODEL:\n"
                + "Trains an HTR-Model with a given training set and validates it with a given validation set.\n"
                + Key.THREADS + "=<POS_NUMBER>: Number of threads for training\n"
                + Key.TRAINSIZE + "=<NUMBER>: Size of one training Epoch. If value is <0, in one epoch each training sample is used one time.\n"
                + Key.EST + "=<POS_NUMBER>: If there are <POS_NUMBER> numbers of epochs without improvements, trainings is stopped. (default=-1[==>off])\n"
                + Key.EPOCHS + "=<POS_NUMBER>: Number of epochs (default=1)\n"
                + Key.LEARNINGRATE + "=<DOUBLE>: Effective learning rate (default=5e-3). To change the learning rate over epochs, separate the rates by ';'(e.g. 5e-3;1e-3). "
                + "Then, for each learning rate the given numer of epochs is trained. It is also possible to specify the number of epochs for each learning rate by separate epochs by ';'. "
                + "There have to be one or as many as learning rates epochs (e.g. NumEpochs=30;20;10 LearningRate=5e-3;2e-3;1e-3)."
                + Key.NOISE + "=<NO|PREPROC|NET|BOTH>: It is possible to add noise while training to simulate a larger training set. It is possible to add noise to the preprocess, the network or both.\n"
                + Key.PATH_BEST_NET + "=<Path-String>: If the path is given, the network with the minimal error rate over an epoch (typically Character Error Rate) is saved to this path. "
                + "";
    }

    public static File getNet(File folder) {
        return new File(folder, INTERN_SPRNN);
    }

    public static File getNetBestOrLast(File folder) {
        File best = new File(folder, INTERN_SPRNN_BEST);
        if (best.exists()) {
            return best;
        }
        File last = new File(folder, INTERN_SPRNN);
        if (last.exists()) {
            return last;
        }
        throw new RuntimeException("cannot find file " + best + " and " + last);
    }

    //    private static int getFactoredInteger(int init, double factor) {
//        return (int) Math.round(init * factor);
//    }
    @Override
    public void createHtr(String htrOut, String pathToCharMapFile, String[] props) {
        new File(htrOut).mkdirs();
        //        double factor = Double.parseDouble(PropertyUtil.getProperty(props, "RelScaleHiddenUnits", "1.0"));
        //        int featMaps = Integer.parseInt(PropertyUtil.getProperty(props, "NumFeatMaps", "1"));
        //        if (featMaps != 1) {
        //            throw new RuntimeException("NumFeatMaps is " + featMaps + " but HTRs with only one input feature are implemented so far.");
        //        }
        if (PropertyUtil.hasProperty(props, Key.PATH_NET)) {
            File folderHtr = new File(PropertyUtil.getProperty(props, Key.PATH_NET));
            LOG.log(Logger.INFO, "use network '" + folderHtr + "' instead of create new network");
            File folderHtrOut = new File(htrOut);
            CharMapUtil.setCharMap(getNetBestOrLast(folderHtr).getPath(), getNet(folderHtrOut).getPath(), pathToCharMapFile, -4.0);
            return;
        }
        try {
            ArgumentLine al = new ArgumentLine();
            al.addArgument("cm", de.uros.citlab.module.train.CharMapProducerREAD.class.getName());
            al.addArgument("cm/i", pathToCharMapFile);
            al.addArgument("o", getNet(new File(htrOut)));
            al.addArgument("pp", de.planet.trainer.factory.ImagePreprocessDft.class.getName());
            al.addArgument("nn/idx", "5");
            al.addArgument("nn/net/ss", "12");
            String[] subTreeProperty = PropertyUtil.copy(props);
            if (PropertyUtil.isPropertyTrue(subTreeProperty, Key.HELP)) {
                al.setHelp();
                subTreeProperty = PropertyUtil.setProperty(subTreeProperty, Key.HELP, null);
            }
            for (int i = 0; subTreeProperty != null && i < subTreeProperty.length; i += 2) {
                al.addArgument(subTreeProperty[i], subTreeProperty[i + 1]);
            }
            String[] args = al.getArgs();
            SNetworkFactory instance = new SNetworkFactory();
            ParamSet ps = new ParamSet();
            ps.setCommandLineArgs(args);    // allow early parsing
            ps = instance.getDefaultParamSet(ps);
            ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
            instance.setParamSet(ps);
            instance.init();
            instance.run();
        } catch (InvalidParameterException ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public String getToolName() {
        return name;
    }

    @Override
    public String getVersion() {
        return MetadataUtil.getSoftwareVersion();
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public void trainHtr(String htrIn, String htrOut, String inputTrainDir, String inputValDir, String[] props) {

        File folderHtrOut = null;
        if (htrOut != null && !htrOut.isEmpty()) {
            folderHtrOut = new File(htrOut);
            folderHtrOut.mkdirs();
        }
        String threads = PropertyUtil.getProperty(props, Key.THREADS, "1");
        String minibatch = PropertyUtil.getProperty(props, Key.MINI_BATCH, String.valueOf(Math.min(50, Math.max(Integer.parseInt(threads) * 4, 10))));
        String trainSizePerEpoch = PropertyUtil.getProperty(props, Key.TRAINSIZE, "-1");
        String numEpochs = PropertyUtil.getProperty(props, Key.EPOCHS, "1");
        String learningRate = PropertyUtil.getProperty(props, Key.LEARNINGRATE, "2e-3");
        String noise = PropertyUtil.getProperty(props, Key.NOISE, "no").toLowerCase();
        String pathBestNet = PropertyUtil.getProperty(props, Key.PATH_BEST_NET, folderHtrOut == null ? "" : new File(folderHtrOut, INTERN_SPRNN_BEST));
        String earlyStopping = PropertyUtil.getProperty(props, Key.EST, "-1");
        boolean viewer = PropertyUtil.isPropertyTrue(props, Key.VIEWER);
        try {
            ArgumentLine al = new ArgumentLine();
            al.addArgument("trainer", de.planet.trainer.trainer.STrainerOfflineNAG.class.getName());
            al.addArgument("i", getNetBestOrLast(new File(htrIn)));
            if (folderHtrOut != null) {
                al.addArgument("o", getNet(new File(htrOut)));
            }
            if (inputTrainDir != null && !inputTrainDir.isEmpty()) {
                al.addArgument("tl", de.uros.citlab.module.train.ImageLoaderFolder.class.getName());
                al.addArgument("tl/d", inputTrainDir);
                al.addArgument("tl/s", trainSizePerEpoch);
            }
            if (inputValDir != null && !inputValDir.isEmpty()) {
                al.addArgument("vl", de.uros.citlab.module.train.ImageLoaderFolder.class.getName());
                al.addArgument("vl/d", inputValDir);
            }
            if (!noise.equals("no")) {
                al.addArgument("trainopt", de.planet.trainer.util.NoiseTrainOptions.class.getName());
                switch (noise) {
                    case "both":
                        al.addArgument("trainopt/noise_net", "true");
                    case "preproc":
                        al.addArgument("trainopt/noise_preproc", "true");
                        break;
                    case "net:":
                        al.addArgument("trainopt/noise_net", "true");
                        break;
                    default:
                        throw new RuntimeException("cannot interprete noise value '" + noise + "'.");
                }
            }
            al.addArgument("trainer/epochs", numEpochs);
            al.addArgument("trainer/delta", learningRate);
            al.addArgument("trainer/batch", minibatch);
            al.addArgument("threads", threads);
            if (viewer) {
                al.addArgument("listener", de.uros.citlab.module.train.TrainingListener.class.getName() + ";" + de.planet.trainer.listener.ListenerViewNetStates.class.getName());
            } else {
                al.addArgument("listener", de.uros.citlab.module.train.TrainingListener.class.getName());
            }
            if (!pathBestNet.isEmpty()) {
                al.addArgument("listener_0/save_best", pathBestNet);
            }
            if (folderHtrOut != null) {
                al.addArgument("listener_0/save_train", new File(folderHtrOut, Key.GLOBAL_CER_TRAIN));
                al.addArgument("listener_0/save_test", new File(folderHtrOut, Key.GLOBAL_CER_VAL));
            }
            al.addArgument("listener_0/est", earlyStopping);
//            al.setHelp();
            String[] args = al.getArgs();
            Trainer instance = new Trainer();
            ParamSet ps = new ParamSet();
            ps.setCommandLineArgs(args);    // allow early parsing
            ps = instance.getDefaultParamSet(ps);
            ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
            instance.setParamSet(ps);
            instance.init();
            instance.fireEvent(EVENT_PRESENT_OBSERVABLE, this, this);
            instance.run();
        } catch (InvalidParameterException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) throws Exception {
//        ArgumentLine al = new ArgumentLine();
//        al.addArgument("net_in", "/home/gundram/devel/projects/barlach/nets/barlach_20161118.sprnn");
//        args = al.getArgs();
        TrainHtr te = new TrainHtr();
        te.createHtr("/home/gundram/devel/projects/barlach/out.sprnn", "/home/gundram/devel/projects/barlach/configs/cm.txt", null);
    }
}
