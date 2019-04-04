/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.PropertyUtil;
import de.uros.citlab.module.util.TrainDataUtil;
import java.io.File;

/**
 *
 * @author gundram
 */
public class CreateTraindata extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(CreateTraindata.class.getName());
    @ParamAnnotation(name = "xml", descr = "path to folder with PAGE-XML files and their images where traindata should be generated")
    private String folderPageXml = "";
    @ParamAnnotation(name = "out", descr = "path to folder with training data (generated from PAGE-XML files or other sources)")
    private String folderSnipets = "";
    @ParamAnnotation(name = "cm", descr = "character map of data")
    private String fileCharMap = "";
    @ParamAnnotation(descr = "if confidence in xml given, take only traindate with at least given confidence")
    private double minconf = 0.0;
    @ParamAnnotation(descr = "save dictionary to folder given in parameter 'out'")
    private boolean dict = false;
    @ParamAnnotation(descr = "save character statistic to folder given in parameter 'out'")
    private boolean stat = false;
    @ParamAnnotation(descr = "save training data to folder given in parameter 'out'")
    private boolean create = true;
    private String[] props;

    public CreateTraindata() {
        this(null);
    }

    public CreateTraindata(String[] props) {
        this("", "", "", props);
    }

    public CreateTraindata(String folderPageXmlTrain, String folderTraindata, String fileCharMap, String[] props) {
        this.folderPageXml = folderPageXmlTrain;
        this.folderSnipets = folderTraindata;
        this.fileCharMap = fileCharMap;
        this.props = props;
        addReflection(this, CreateTraindata.class);
    }

    @Override
    public void init() {
        super.init();
        if (isValid(folderPageXml) && !isValid(folderSnipets)) {
            throw new RuntimeException("folder with PAGE-XML is given but no folder to save training samples");
        }
        props = PropertyUtil.setProperty(props, Key.MIN_CONF, String.valueOf(minconf));
        props = PropertyUtil.setProperty(props, Key.CREATEDICT, String.valueOf(dict));
        props = PropertyUtil.setProperty(props, Key.STATISTIC, String.valueOf(stat));
        props = PropertyUtil.setProperty(props, Key.CREATETRAINDATA, String.valueOf(create));
    }

    private static boolean isValid(String str) {
        return str != null && !str.isEmpty();
    }

    public void run() {
        if (isValid(folderSnipets) && isValid(folderPageXml)) {
            TrainDataUtil.runCreateTraindata(new File(folderPageXml), new File(folderSnipets), fileCharMap == null || fileCharMap.isEmpty() ? null : new File(fileCharMap), props,null);
        }
    }

    /**
     * @param args the command line arguments
     * @throws com.achteck.misc.exception.InvalidParameterException
     * @throws java.lang.ClassNotFoundException
     */
    public static void main(String[] args) throws InvalidParameterException, ClassNotFoundException, Exception {
        if (args.length == 0) {
            ArgumentLine al = new ArgumentLine();
            al.addArgument("xml", HomeDir.getFile("data/TRAIN_CITlab_Test_Tuni_duplicated"));
            al.addArgument("cm", HomeDir.getFile("data/TRAIN_CITlab_Test_Tuni_duplicated/cm.txt"));
            al.addArgument("out", HomeDir.getFile("traindata/TRAIN_CITlab_Test_Tuni_duplicated"));
            al.addArgument("create", true);
            al.addArgument("stat", true);
            args = al.getArgs();
        }
        CreateTraindata instance = new CreateTraindata();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
