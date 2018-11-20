/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ConfMat;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.uros.citlab.module.train.TrainHtr;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PropertyUtil;
import de.uros.citlab.module.util.TrainDataUtil;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

/**
 *
 * @author gundram
 */
public class RunTraining extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(RunTraining.class.getName());
    @ParamAnnotation(name = "net_in", descr = "path to untrained/pretrained network (if not given charmap have to be set to create default network)")
    private String netIn = "";
    @ParamAnnotation(name = "net_out", descr = "path to trained network")
    private String netTrained = "";
    @ParamAnnotation(name = "train_xml", descr = "path to folder with PAGE-XML files and their images where traindata should be generated")
    private String folderPageXmlTrain = "";
    @ParamAnnotation(name = "train_data", descr = "path to folder with training data (generated from PAGE-XML files or other sources)")
    private String folderTraindata = "";
    @ParamAnnotation(name = "val_xml", descr = "path to folder with PAGE-XML files and their images where validationdata should be generated")
    private String folderPageXmlVal = "";
    @ParamAnnotation(name = "val_data", descr = "path to folder with validation data (generated from PAGE-XML files or other sources)")
    private String folderValdata = "";
//    private File folderInfos;
    @ParamAnnotation(name = "cm", descr = "character map if a network should be created")
    private String fileCharMap = "";
    @ParamAnnotation(descr = "if confidence in xml given, take only traindate with at least given confidence")
    private double minconf = 0.0;
    private String[] props;

    public RunTraining() {
        this(null);
    }

    public RunTraining(String[] props) {
        this("", "", "", "", "", props);
    }

    public RunTraining(String netIn, String netTrained, String folderPageXmlTrain, String folderTraindata, String fileCharMap, String[] props) {
        this(netIn, netTrained, folderPageXmlTrain, folderTraindata, "", "", fileCharMap, props);
    }

    public RunTraining(String netIn, String netTrained, String folderPageXmlTrain, String folderTraindata, String folderPageXmlVal, String folderValdata, String fileCharMap, String[] props) {
        this.netIn = netIn;
        this.netTrained = netTrained;
        this.folderPageXmlTrain = folderPageXmlTrain;
        this.folderTraindata = folderTraindata;
        this.folderPageXmlVal = folderPageXmlVal;
        this.folderValdata = folderValdata;
//        this.folderInfos = folderInfos;
        this.fileCharMap = fileCharMap;
        this.props = props;
        addReflection(this, RunTraining.class);
    }

    @Override
    public void init() {
        super.init();
        //plausi: createHtr
//        if (netIn == null && fileCharMap == null) {
//            throw new RuntimeException("netIn and fileCharMap are null, at least one of these have to be set (provide network or provide charmap to create network)");
//        }
        //plausi: createTraindata
        if (isValid(folderPageXmlTrain) && !isValid(folderTraindata)) {
            throw new RuntimeException("folder with PAGE-XML is given but no folder to save training samples");
        }
        props = PropertyUtil.setProperty(props, Key.MIN_CONF, String.valueOf(minconf));
    }

    private static boolean isValid(String str) {
        return str != null && !str.isEmpty();
    }

    public void run() {
        if (isValid(folderTraindata) && isValid(folderPageXmlTrain)) {
            String[] split = folderPageXmlTrain.split(File.pathSeparator);
            for (String path : split) {
                runCreateTraindata(new File(path), new File(folderTraindata), fileCharMap.isEmpty() ? null : new File(fileCharMap), props);
            }
        }
        if (isValid(folderValdata) && isValid(folderPageXmlVal)) {
            String propertyStatus = PropertyUtil.getProperty(props, Key.TRAIN_STATUS);
            PropertyUtil.setProperty(props, Key.TRAIN_STATUS, null);
            runCreateTraindata(new File(folderPageXmlVal), new File(folderValdata), null, props);
            PropertyUtil.setProperty(props, Key.TRAIN_STATUS, propertyStatus);
        }

        if (!isValid(netIn) && isValid(fileCharMap)) {
            runCreateHtr(props);
            netIn = netTrained;
        }
        if (isValid(folderTraindata) || isValid(folderValdata)) {
            if (!isValid(netTrained)) {
                LOG.log(Logger.WARN, "no net saved after training/validation, because net out path is not set");
            }
            runTrainHtr(props);
        }

    }

    public static void runCreateTraindata(File folderPageXml, File folderSnipets, File charMap, String[] props) {
        List<File> listFiles = FileUtil.listFiles(folderPageXml, "xml", true);
        FileUtil.deleteMetadataAndMetsFiles(listFiles);
        String[] names = new String[listFiles.size()];
        int i = 0;
        for (File listFile : listFiles) {
            names[i++] = listFile.getAbsolutePath();
        }
        TrainHtr instance = new TrainHtr();
        String[] propsCreate = PropertyUtil.setProperty(props, TrainDataUtil.KEY_SRC_FOLDER, folderPageXml.getAbsolutePath());
        instance.createTrainData(names, folderSnipets == null ? null : folderSnipets.getAbsolutePath(), charMap == null ? null : charMap.getAbsolutePath(), propsCreate);

    }

    public void runCreateHtr(String[] props) {
        String[] subTreeProperty = PropertyUtil.getSubTreeProperty(props, "create");
        TrainHtr instance = new TrainHtr();
        instance.createHtr(netTrained, fileCharMap, subTreeProperty);
    }

    public void runTrainHtr(String[] props) {
        String[] propsDefault = PropertyUtil.setProperty(null, Key.EPOCHS, "400");
        propsDefault = PropertyUtil.setProperty(propsDefault, Key.LEARNINGRATE, "1e-3");
//        propsDefault = PropertyUtil.setProperty(props, Key.TRAINSIZE, "10");
        propsDefault = PropertyUtil.setProperty(propsDefault, Key.NOISE, "both");
        propsDefault = PropertyUtil.setProperty(propsDefault, Key.THREADS, "4");
//        propsDefault = PropertyUtil.setProperty(props, "viewer", "true");
        String[] props4Method = PropertyUtil.merge(props, propsDefault);
        TrainHtr instance = new TrainHtr();
        instance.trainHtr(netIn,
                netTrained,
                folderTraindata,
                folderValdata,
                props4Method);
    }

    /**
     * @param args the command line arguments
     * @throws com.achteck.misc.exception.InvalidParameterException
     * @throws java.lang.ClassNotFoundException
     */
    public static void main(String[] args) throws InvalidParameterException, ClassNotFoundException, Exception {
//        System.out.println(Character.getName(ConfMat.Return));
//        String nameIn = "2017_01_20_newtraindata.sprnn";
//        String nameIn = "2017_07_21_Barlach.sprnn.best";
        String[] props = null;
        if (args.length == 0) {
            ArgumentLine al = new ArgumentLine();
//            al.addArgument("net_in", HomeDir.getFile("nets/train_29095_Janauschek/net.sprnn"));
            al.addArgument("net_out", HomeDir.getFile("nets/30032_parzival/net.sprnn"));
//            al.addArgument("train_xml", HomeDir.getFile("data/30032_parzival/train"));
//            al.addArgument("val_xml", HomeDir.getFile("data/30032_parzival/test"));
            al.addArgument("train_data", HomeDir.getFile("taindata/30032_parzival/train"));
            al.addArgument("val_data", HomeDir.getFile("taindata/30032_parzival/test"));
            al.addArgument("cm", HomeDir.getFile("nets/30032_parzival/cm.txt"));
            args = al.getArgs();
//        props = PropertyUtil.setProperty(null, "minconf", "0.0025");
//        props = PropertyUtil.setProperty(props, Key.STATISTIC, "true");
//        props = PropertyUtil.setProperty(props, Key.CREATEDICT, "true");
//        props = PropertyUtil.setProperty(props, Key.VIEWER, "true");
//        props = PropertyUtil.setProperty(props, Key.TRAIN_STATUS, "GT");
//            props = PropertyUtil.setProperty(props, Key.NOISE, "preproc");
            props = PropertyUtil.setProperty(props, Key.MINI_BATCH, "8");
            props = PropertyUtil.setProperty(props, Key.LEARNINGRATE, "1e-3");
            props = PropertyUtil.setProperty(props, Key.THREADS, "4");

//        props = PropertyUtil.setProperty(props, Key.MINI_BATCH, "96");
//        props = PropertyUtil.setProperty(props, "create/help", "true");
//            props = PropertyUtil.setProperty(props, "create/pp/pquantil_height", "24");
//            props = PropertyUtil.setProperty(props, "create/nn/net/ss", "12");
//        props = PropertyUtil.setProperty(props, "create/nn/show", "true");
//        props = PropertyUtil.setProperty(props, "create/nn/idx", "6");
        }
        RunTraining instance = new RunTraining(null);
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.ACCEPT); // be strict, don't accept generic parameter
        String[] remainingArgumentList = ps.getRemainingArgumentList();
//        System.out.println(Arrays.toString(remainingArgumentList));
        props = ArgumentLine.getPropertiesFromArgs(remainingArgumentList, props);
//        System.out.println("==>" + Arrays.toString(props));
        instance = new RunTraining(props);
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
