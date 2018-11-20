/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.Param;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.uros.citlab.module.train.TrainHtr;
import de.uros.citlab.module.util.PropertyUtil;

/**
 *
 * @author gundram
 */
public class CreateHtr extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(CreateHtr.class.getName());
    @ParamAnnotation(name = "net_out", descr = "path to trained network")
    private String netTrained = "";
    @ParamAnnotation(name = "cm", descr = "character map if a network should be created")
    private String fileCharMap = "";
    private String[] props;

    public CreateHtr() {
        this(null);
    }

    public CreateHtr(String[] props) {
        this("", "", props);
    }

    public CreateHtr(String net_out, String fileCharMap, String[] props) {
        this.netTrained = net_out;
        this.fileCharMap = fileCharMap;
        this.props = props;
        addReflection(this, CreateHtr.class);
    }

    public void run() {
        TrainHtr instance = new TrainHtr();
        instance.createHtr(netTrained, fileCharMap, props);
    }

    /**
     * @param args the command line arguments
     * @throws com.achteck.misc.exception.InvalidParameterException
     * @throws java.lang.ClassNotFoundException
     */
    public static void main(String[] args) throws InvalidParameterException, ClassNotFoundException, Exception {
        if (args.length == 0) {
            args = (""
                    + ""
                    + ""
                    + ""
                    //                    + "-train_xml /home/gundram/devel/projects/read/data/alfred_escher/mapXml "//folder,if traindata should be created from PAGE-XML data
                    //                    + "-train_data /home/gundram/devel/projects/read/traindata/data/Greifswald_Alvermann/ "//folder where trainingdata are located or should be saved
                    //                    +"-val_xml /home/gundram/devel/projects/read/data/GT_TEST_CO_5_400_clean/ "//folder,if traindata should be created from PAGE-XML data
                    //                    + "-val_data /home/gundram/devel/projects/read/traindata/GT_TEST_CO_5_400_clean/ "//folder where trainingdata are located or should be saved
                    //                    + "-net_in /home/gundram/devel/projects/read/nets/escher_v4.sprnn "//path to trained network
                    + "-net_out " + HomeDir.getFile("nets/barlach_20161118.sprnn") + " "//null;// new File("/home/gundram/devel/projects/barlach/nets/barlach_v1.sprnn");//path to trained network
                    + "-cm " + HomeDir.getFile("../barlach/configs/cm.txt") + " "//path where charmap is saved (when traindata are constructed) or taken, if new network has to be created
                    //                                        + "--help"
                    + "").split(" ");
        }
        String[] props = null;
//        props=PropertyUtil.setProperty(props,"help", "true");
//        props=PropertyUtil.setProperty(props,"dict", "true");
//        props=PropertyUtil.setProperty(props,"stat", "true");
//        props=PropertyUtil.setProperty(props,"nn/show", "true");
//        props = PropertyUtil.setProperty(props, "peephole/help", "true");
//        props = PropertyUtil.setProperty(props, "peephole/pp/pquantil_p", "0.4");
//        props = PropertyUtil.setProperty(props, "peephole/pp/pquantil_height", "26");
//        props = PropertyUtil.setProperty(props, "peephole/pp/height_total", "96");
//        props = PropertyUtil.setProperty(props, "peephole/pp/use_new_cn", "true");
//        props = PropertyUtil.setProperty(props, "peephole/nn/show", "true");
        CreateHtr instance = new CreateHtr(props);
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.STRICT); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
