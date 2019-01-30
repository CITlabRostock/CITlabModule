/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.train;

import com.achteck.misc.param.ParamSet;
import com.achteck.misc.util.IO;
import de.planet.itrtech.reco.IImagePreProcess;
import de.planet.reco.ImagePreprocModules;
import de.planet.reco.RecoGlobals;
import de.planet.reco.preproc.*;
import de.planet.reco.preproc.util.ResizeUtil.Algorithm;
import de.planet.trainer.factory.ImagePreprocessDft;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;

/**
 * @author gundram
 */
public class TrainHtrPlus extends TrainHtr {

    private static final String NAME_PREPROC = "preproc.bin";
    //    public static final String NAME_FROZEN = "frozen_model.pb";
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(TrainHtrPlus.class.getName());
    private static final String provider = "University of Rostock\nInstitute of Mathematics\nCITlab\nGundram Leifert\ngundram.leifert@uni-rostock.de";
    private static final String name = TrainHtrPlus.class.getName();

    public TrainHtrPlus() {
        super();
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
                + Key.PATH_NET + "=<PATH-String>: If an existing htr should be used instead of initialize a new htr, a path to the htr must be given.\n"
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

    @Override
    public void createHtr(String htrOut, String pathToCharMapFile, String[] props) {
        File folderHtrOut = new File(htrOut);
        folderHtrOut.mkdirs();
//        double factor = Double.parseDouble(PropertyUtil.getProperty(props, "RelScaleHiddenUnits", "1.0"));
//        int featMaps = Integer.parseInt(PropertyUtil.getProperty(props, "NumFeatMaps", "1"));
//        if (featMaps != 1) {
//            throw new RuntimeException("NumFeatMaps is " + featMaps + " but HTRs with only one input feature are implemented so far.");
//        }
//Properties p = new Properties();
        if (PropertyUtil.hasProperty(props, Key.PATH_NET)) {
            String pathNet = PropertyUtil.getProperty(props, Key.PATH_NET);
            LOG.info("use network '{}' instead of create new network", pathNet);
            // If we load a TensorFlow Network a CharMap is already linked
            if (pathToCharMapFile != null && !pathToCharMapFile.isEmpty()) {
                throw new RuntimeException("cannot change charmap in trained model using HTR+");
            }
            // Overwrite with new CharMap?! Or just ignore it?!
//            CharMapUtil.setCharMap(pathNet, pathToModelsOut, pathToCharMapFile, -4.0);
            return;
        }
//        File folderTmp = new File(PropertyUtil.getProperty(props, Key.TMP_FOLDER));
        IImagePreProcess preProc = getPreProcDft(props);
        savePreProc(folderHtrOut, preProc);
//        saveDataTypeConfig(folderHtrOut, preProc);

        File charMap = new File(pathToCharMapFile);
        FileUtil.copyFile(charMap, new File(folderHtrOut, Key.GLOBAL_CHARMAP));
        // Execute python script to build the TensorFlow model and save it
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

    private static File getTmpDir(String[] props) {
        if (!PropertyUtil.hasProperty(props, Key.TMP_FOLDER)) {
            throw new RuntimeException("no value for key '" + Key.TMP_FOLDER + "' given.");
        }
        File tmpDir = new File(PropertyUtil.getProperty(props, Key.TMP_FOLDER));
        try {
            if (!tmpDir.mkdirs()) {
                if (tmpDir.listFiles().length > 0) {
                    LOG.warn("temporal directory {} already exists and contains files", tmpDir);
                }
            }
        } catch (RuntimeException ex) {
            LOG.error("cannot create path '{}'", tmpDir, ex);
            throw new RuntimeException("cannot create directory " + tmpDir.getAbsolutePath(), ex);
        }
        return tmpDir;
    }

    private String[] getTrainingProperties(
            int gpu,
            String learningRate,
            double dropout,
            int trainSizePerEpoch,
            int numEpochs,
            String pathToModelsOut,
            int minibatch,
            File charMap,
            File fileListTrain,
            File fileListVal) {
        String[] propsTraining = null;
        propsTraining = PropertyUtil.setProperty(propsTraining, "inference", gpu >= 0 ? "InferenceLayers3_3_cu" : "InferenceLayers3_3");
        propsTraining = PropertyUtil.setProperty(propsTraining, "learning_rate", learningRate);
        propsTraining = PropertyUtil.setProperty(propsTraining, "dropout_out", dropout);
        propsTraining = PropertyUtil.setProperty(propsTraining, "train_steps_per_epoch", trainSizePerEpoch);
        propsTraining = PropertyUtil.setProperty(propsTraining, "gpu_device", gpu);
        propsTraining = PropertyUtil.setProperty(propsTraining, "epochs", numEpochs);
        propsTraining = PropertyUtil.setProperty(propsTraining, "override_export", "true");
        propsTraining = PropertyUtil.setProperty(propsTraining, "export_dir", pathToModelsOut);
        propsTraining = PropertyUtil.setProperty(propsTraining, "checkpoint_dir", pathToModelsOut);
        propsTraining = PropertyUtil.setProperty(propsTraining, "batch_size", minibatch);
        propsTraining = PropertyUtil.setProperty(propsTraining, "charmap", charMap.getAbsolutePath());
//        propsTraining = PropertyUtil.setProperty(propsTraining, "dtype_cfg", new File(fileHtrOut, NAME_DTYPE_CFG).getAbsolutePath());
        if (fileListTrain != null) {
            propsTraining = PropertyUtil.setProperty(propsTraining, "train_list", fileListTrain.getAbsolutePath());
        }
        if (fileListVal != null) {
            propsTraining = PropertyUtil.setProperty(propsTraining, "val_list", fileListVal.getAbsolutePath());
        }
        return propsTraining;

    }

    private String[] getFurtherTrainingProperties(
            String[] propsTraining,
            int numEpochs,
            boolean finetuneOnlyLogits,
            boolean continueTraining) {
        propsTraining = PropertyUtil.copy(propsTraining);
        propsTraining = PropertyUtil.setProperty(propsTraining, "epochs", numEpochs);
        if (continueTraining) {
            propsTraining = PropertyUtil.setProperty(propsTraining, "continue_point", PropertyUtil.getProperty(propsTraining, "checkpoint_dir"));
        }
        if (finetuneOnlyLogits) {
            //if charMap changed we have to reinitialize the last layer
            propsTraining = PropertyUtil.setProperty(propsTraining, "train_scopes", "logits");
            propsTraining = PropertyUtil.setProperty(propsTraining, "not_to_load", "logits");
        }
//        propsTraining = PropertyUtil.setProperty(propsTraining, "dtype_cfg", new File(fileHtrOut, NAME_DTYPE_CFG).getAbsolutePath());
        return propsTraining;
    }


    @Override
    public void trainHtr(String pathToModelsIn, String pathToModelsOut, String inputTrainDir, String inputValDir, String[] props) {
        if (System.getenv("PYTHONPATH") == null || System.getenv("PYTHONPATH").isEmpty()) {
            LOG.warn("environment variable $PYTHONPATH is not set");
        }
        File fileHtrOut = new File(pathToModelsOut);
        File fileHtrIn = new File(pathToModelsIn);
        int minibatch = Integer.valueOf(PropertyUtil.getProperty(props, Key.MINI_BATCH, 16));
        int trainSizePerEpoch = Integer.valueOf(PropertyUtil.getProperty(props, Key.TRAINSIZE, 1024 * 8));
        int numEpochs = Integer.valueOf(PropertyUtil.getProperty(props, Key.EPOCHS, 1));
        double dropout = PropertyUtil.isProperty(props, Key.NOISE, "both", "net") ? 0.5 : 0.0;
        String learningRate = PropertyUtil.getProperty(props, Key.LEARNINGRATE, "1e-3");
        File tmpDir = getTmpDir(props);
        File charMap = new File(fileHtrOut, Key.GLOBAL_CHARMAP);
        if (!fileHtrIn.equals(fileHtrOut)) {
            if (fileHtrOut.exists()) {
                if (charMap.exists()) {
                    LOG.warn("delete all files in folder {}", fileHtrOut);
                    FileUtils.deleteQuietly(fileHtrOut);
                    fileHtrOut.mkdir();
                }
            }
            try {
                FileUtils.copyDirectory(fileHtrIn, fileHtrOut);
            } catch (IOException ex) {
                throw new RuntimeException("cannot copy folder", ex);
            }
        }
//        IImagePreProcess pp = loadPreProc(fileHtrOut);
        File folderLists = new File(tmpDir, "lists");
        File fileListVal = null;
        File fileListTrain = null;
        if (inputValDir != null && !inputValDir.isEmpty()) {
            fileListVal = new File(folderLists, "val.lst");
            FileUtil.writeLines(fileListVal, FileUtil.getStringList(FileUtil.listFiles(new File(inputValDir), FileUtil.IMAGE_SUFFIXES, true)));
        }
        if (inputTrainDir != null && !inputTrainDir.isEmpty()) {
            fileListTrain = new File(folderLists, "train.lst");
            FileUtil.writeLines(fileListTrain, FileUtil.getStringList(FileUtil.listFiles(new File(inputTrainDir), FileUtil.IMAGE_SUFFIXES, true)));
        }
        int gpu = getGPU(props);

        String[] trainingProperties = getTrainingProperties(gpu,
                learningRate, dropout,
                trainSizePerEpoch, numEpochs, pathToModelsOut, minibatch, charMap, fileListTrain, fileListVal);

        //check, if training is based on a further training or is a new training - different training strategies have to be done.
        boolean hasBaseModel = new File(fileHtrOut, "export").exists();
        LOG.info("found base model = {}", hasBaseModel);
        if (hasBaseModel) {
            boolean reinitLogits = reinitLogits(charMap, new File(new File(fileHtrOut, "export"), Key.GLOBAL_CHARMAP));
            //train further! -> 2 option:
            // 1. CharMap changed => reinit logits and train with continue point
            // 2. CharMap unchanged => train with continue point
            LOG.info("re-initialize upper layer because CharMap changed = {}", reinitLogits);
            if (reinitLogits) {
                LOG.info("apply training one epoch only on the logit layer.");
                if (PythonUtil.runPythonFromFile("tf_htsr/models/trainer/trainer.py",
                        getProcessListener(inputTrainDir, inputValDir, fileHtrOut, props),
                        getFurtherTrainingProperties(trainingProperties, 1, true, true)
                ) != 0) {
                    LOG.error("training of HTR+ ends with status !=0");
                    throw new RuntimeException("training of HTR+ ends with status !=0");
                }
                numEpochs--;
            }
            LOG.info("apply training {} epoch(s) on all layer.", numEpochs);
            if (PythonUtil.runPythonFromFile("tf_htsr/models/trainer/trainer.py",
                    getProcessListener(inputTrainDir, inputValDir, fileHtrOut, props),
                    getFurtherTrainingProperties(trainingProperties, numEpochs, false, true)
            ) != 0) {
                LOG.error("training of HTR+ ends with status !=0");
                throw new RuntimeException("training of HTR+ ends with status !=0");
            }
        } else {
            LOG.info("apply new training {} epoch(s).", numEpochs);
            //train without continue point
            if (PythonUtil.runPythonFromFile("tf_htsr/models/trainer/trainer.py",
                    getProcessListener(inputTrainDir, inputValDir, fileHtrOut, props),
                    trainingProperties
            ) != 0) {
                LOG.error("training of HTR+ ends with status !=0");
                throw new RuntimeException("training of HTR+ ends with status !=0");
            }
        }
    }

    private boolean reinitLogits(File charMapNew, File charMapExport) {
        if (!charMapExport.exists()) {
            return false;
        }
        if (!charMapNew.exists()) {
            throw new RuntimeException("charmap " + charMapNew.getAbsolutePath() + " not found");
        }
        //reinitialize logits, if charmap changed.
        return !CharMapUtil.loadCharMap(charMapNew).equals(CharMapUtil.loadCharMap(charMapExport));

    }

    private int getGPU(String[] properties) {
        if (PropertyUtil.hasProperty(properties, Key.TRAIN_GPU)) {
            return Integer.parseInt(PropertyUtil.getProperty(properties, Key.TRAIN_GPU));
        }
        String gpuDevice = System.getenv("GPU_DEVICE");
        if (gpuDevice == null) {
            return -1;
        }
        gpuDevice = gpuDevice.trim();
        if (!gpuDevice.isEmpty()) {
            try {
                Integer.parseInt(gpuDevice);
            } catch (RuntimeException ex) {
                throw new RuntimeException("cannot interprete $GPU_DEVICE = '" + gpuDevice + "' as integer");
            }
            return Integer.parseInt(gpuDevice);
        }
        return -1;
    }

    private PythonUtil.ProcessListener getProcessListener(String inputTrainDir, String inputValDir, File fileHtrOut, String[] props) {
        if (isValid(inputTrainDir) || isValid(inputValDir)) {
            return new ProcessListener(isValid(inputTrainDir) ? new File(fileHtrOut, Key.GLOBAL_CER_TRAIN) : null,
                    isValid(inputValDir) ? new File(fileHtrOut, Key.GLOBAL_CER_VAL) : null, this);
        }
        throw new RuntimeException("not trainDir and ValDir is set");
    }

    private boolean isValid(String str) {
        return str != null && !str.isEmpty();
    }

    public static IImagePreProcess loadPreProc(File folderHtr) {
        IImagePreProcess pp = (IImagePreProcess) IOUtil.load(new File(folderHtr, NAME_PREPROC));
        pp.setParamSet(pp.getDefaultParamSet(null));
        pp.init();
        return pp;
    }

    private void savePreProc(File folderHtr, IImagePreProcess pp) {
        try {
            IO.save(pp, new File(folderHtr, NAME_PREPROC));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private IImagePreProcess getPreProcDft(String[] props) {
        try {
            return getPreProcess(64, 0.5, 24);
        } catch (RuntimeException ex) {
            LOG.warn("use old and slow preprocess");
            return ImagePreprocessDft.getPreProcess(64, 0.5, 20, true);
        }
    }

    public static final IImagePreProcess getPreProcess(int tgtHeight, double pquantil_p, int pquantil_height) {
        ImagePreprocModules imagePreprocModules = new ImagePreprocModules("pp_deploy_As");
        imagePreprocModules.addModule("contrast", new ContrastNormalizer6());
        imagePreprocModules.addModule("maskR", new MaskRemover());
        imagePreprocModules.addModule("coglshape", new COGLShapeNormalizer());
        imagePreprocModules.addModule("slant", new SlantNormalizer());
        imagePreprocModules.addModule("crop", new Cropper());
        imagePreprocModules.addModule("main2", new MainBodyNormalizer2());
        imagePreprocModules.addModule("si", new SizeNormalizerSimple());
        imagePreprocModules.setParamPrefix("");
        ParamSet ps = imagePreprocModules.getDefaultParamSet((ParamSet) null);
        ps.getParam("coglshape/prescale_height").set(64);
        ps.getParam("coglshape/subsample_x").set(100);
        ps.getParam("coglshape/subsample_y").set(10);
        ps.getParam("coglshape/degree_divisor").set(5);
        ps.getParam("coglshape/use_abs_x_contrast_matrix").set(true);
        ps.getParam("slant/slant_detector/use_gaussian_blur").copyFrom(false);
        ps.getParam("main2/mbr_target_height").set(26);
        ps.getParam("main2/bs_top_border_target_size").set(19);
        ps.getParam("main2/bs_bottom_border_target_size").set(19);
        ps.getParam("main2/mbr_hor_resize_non_prop_factor").set(1.0D);
        ps.getParam("main2/mbr_scaler_type").copyFrom(Algorithm.IP.toString());
        ps.getParam("si/size_normalizer_simple_target_size").set(64);
        imagePreprocModules.setParamSet(ps);
        imagePreprocModules.init();
        imagePreprocModules.setMinimumPrecision(RecoGlobals.Precision.FLOAT);
        return imagePreprocModules;
    }

    public static int getPreProcHeight(IImagePreProcess pp) {
        int height = 1;
        try {

//        if (pp instanceof ImagePreprocModules) {
            ImagePreprocModules ppM = (ImagePreprocModules) pp;
            IImagePreProcess lastModule = ppM.getModuleList().get(ppM.getModuleList().size() - 1);
//            if (lastModule instanceof BasicMainBodyNormalizer) {
            BasicMainBodyNormalizer module = (BasicMainBodyNormalizer) lastModule;
            height += module.getParamSet().getParam(module.getParamPrefix() + BasicMainBodyNormalizer.P_BASIC_MAIN_BODY_NORMALIZER_SQUASH_BOTTOM).getInt();
            height += module.getParamSet().getParam(module.getParamPrefix() + BasicMainBodyNormalizer.P_BASIC_MAIN_BODY_NORMALIZER_SQUASH_TOP).getInt();
            height += module.getParamSet().getParam(module.getParamPrefix() + BasicMainBodyNormalizer.P_BASIC_MAIN_BODY_NORMALIZER_LOWER_BORDER_SIZE).getInt();
            height += module.getParamSet().getParam(module.getParamPrefix() + BasicMainBodyNormalizer.P_BASIC_MAIN_BODY_NORMALIZER_UPPER_BORDER_SIZE).getInt();
//            }
//        }
        } catch (RuntimeException ex) {
            throw new RuntimeException("cannot evaluate the fixed height of the preprocess which is needed for the HTR+", ex);
        }
        return height;
    }

    private static class ProcessListenerModules implements PythonUtil.ProcessListener {

        public ProcessListenerModules() {
        }

        public ProcessListenerModules(PythonUtil.ProcessListener... modules) {
            for (PythonUtil.ProcessListener module : modules) {
                listeners.add(module);
            }
        }

        private List<PythonUtil.ProcessListener> listeners = new LinkedList<>();

        public boolean add(PythonUtil.ProcessListener e) {
            return listeners.add(e);
        }

        @Override
        public void handleOutput(String line) {
            for (PythonUtil.ProcessListener listener : listeners) {
                listener.handleOutput(line);
            }
        }

        @Override
        public void handleError(String line) {
            for (PythonUtil.ProcessListener listener : listeners) {
                listener.handleError(line);
            }
        }

        @Override
        public void setProcessID(Long processID) {
            for (PythonUtil.ProcessListener listener : listeners) {
                listener.setProcessID(processID);
            }
        }

    }

//    private static List<String> recos = new LinkedList<>();

//    protected static List<String> getReco() {
//        return recos;
//    }

//    static class ProcessListenerReco implements PythonUtil.ProcessListener {
//
//        boolean delete = false;
//        private Pattern p = Pattern.compile("[^']*'(?<ref>.*)' *<> '(?<reco>.*)'[^']*");
//
//        @Override
//        public void handleOutput(String line) {
//            if (line.matches(".*Train: CER = .*") || line.matches(".*Val: CER = .*")) {
//                delete = true;
//                return;
//            }
//            if (line.matches(".*(error|correct).*")) {
//                if (delete) {
//                    recos.clear();
//                    LOG.debug("clear recognition");
//                    delete = false;
//                }
////                boolean matches = line.matches(p.pattern());
//                Matcher matcher = p.matcher(line);
//                matcher.find();
//                String group = matcher.group("reco");
////                String group2 = matcher.group("ref");
//                LOG.debug("model recognized '{}'", group);
//                recos.add(group);
//            }
//        }
//
//        @Override
//        public void handleError(String line) {
//        }
//
//    }

//    public static void main(String[] args) {
//        String s = " correct: 'Greifsw. d. 4.' Septbr. Gehors. Diener' <> 'Greifsw. d. 4. 'Septbr. Gehors. Diener'";
//        ProcessListenerReco r = new ProcessListenerReco();
//        r.handleOutput(s);
//        List<String> reco = getReco();
//        System.out.println(reco);
//    }

    private static class ProcessListener implements PythonUtil.ProcessListener {

        public ProcessListener(File trainFile, File valFile, Observable trainHTR) {
            this.valFile = valFile;
            this.trainFile = trainFile;
            if (valFile != null && valFile.exists()) {
                valFile.delete();
            }
            if (trainFile != null && trainFile.exists()) {
                trainFile.delete();
            }
            this.trainerHTR = trainHTR;
            handleOutput("");
        }

        private File valFile;
        private File trainFile;
        private Observable trainerHTR;

        private LinkedList<String> val = new LinkedList<>();
        private LinkedList<String> train = new LinkedList<>();
        boolean changed = true;
        double bestval = Double.NaN;
        boolean isBestVal = false;
        private Long processID = null;

        @Override
        public void handleOutput(String line) {
            if (valFile != null && line.matches(".*Val: CER = .*")) {
                String trim = line.split("Val: CER = ")[1].trim();
                double val = Double.parseDouble(trim);
                if (Double.isNaN(bestval) || bestval > val) {
                    bestval = val;
                    isBestVal = true;
                } else {
                    isBestVal = false;
                }
                this.val.add(String.format("%.4f", Double.parseDouble(trim)));
                FileUtil.writeLines(valFile, this.val);
                changed = true;
            }
            if (trainFile != null && line.matches(".*Train: CER = .*")) {
                String trim = line.split("Train: CER = ")[1].trim();
                train.add(String.format("%.4f", Double.parseDouble(trim)));
                FileUtil.writeLines(trainFile, train);
                changed = true;
            }
            if (trainerHTR != null && changed) {
                int epochs = Math.max(val.size(), train.size());
                double lastTrain = train.isEmpty() ? 1.0 : Double.parseDouble(train.getLast());
                double lastVal = val.isEmpty() ? 1.0 : Double.parseDouble(val.getLast());
                trainerHTR.notifyObservers(new TrainHtr.Status(epochs, lastTrain, lastVal, "CER", isBestVal, processID));
                changed = false;
            }
        }

        @Override
        public void handleError(String line) {
        }

        @Override
        public void setProcessID(Long processID) {
            this.processID = processID;
        }

    }
}
