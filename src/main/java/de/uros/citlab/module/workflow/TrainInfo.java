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
import de.planet.trainer.util.SNetworkUtil;
import de.uros.citlab.module.util.CharMapUtil;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author gundram
 */
public class TrainInfo extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(TrainInfo.class.getName());

    @ParamAnnotation(descr = "path to network")
//    private String i = "/home/gundram/devel/src/git/CITlabModule/src/main/resources/planet/htr/20160408_htrts_midfinal_11.sprnn";
//    private String i = "/home/gundram/devel/projects/barlach/nets/barlach_20161118_trptrain.sprnn";
//    private String i = "/home/gundram/devel/projects/read/nets/net_heb/";
    private String i = HomeDir.PATH + "data/transkribus_beckett_training/nets/french/meganet_37_all2.sprnn";

    public TrainInfo() {
        addReflection(this, TrainInfo.class);
    }

    public void run() throws IOException, ClassNotFoundException, InvalidParameterException, Exception {
        File f = new File(i);
        if (f.isDirectory()) {
            {
                String[] args = ("-option 5 -i " + i + " -o out.csv -key CER -val true").trim().split(" ");
                SNetworkUtil instance = new SNetworkUtil();
                ParamSet ps = new ParamSet();
                ps.setCommandLineArgs(args);    // allow early parsing
                ps = instance.getDefaultParamSet(ps);
                ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
                instance.setParamSet(ps);
                instance.init();
                instance.run();

            }
            {
                String[] args = ("-option 5 -i " + i + " -o out_train.csv -key CER -val false").trim().split(" ");
                SNetworkUtil instance = new SNetworkUtil();
                ParamSet ps = new ParamSet();
                ps.setCommandLineArgs(args);    // allow early parsing
                ps = instance.getDefaultParamSet(ps);
                ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
                instance.setParamSet(ps);
                instance.init();
                instance.run();

            }
        } else {
            {
                String[] args = ("-option 2 -i " + i + " ").trim().split(" ");
                SNetworkUtil instance = new SNetworkUtil();
                ParamSet ps = new ParamSet();
                ps.setCommandLineArgs(args);    // allow early parsing
                ps = instance.getDefaultParamSet(ps);
                ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
                instance.setParamSet(ps);
                instance.init();
                instance.run();
            }
            {
                String[] args = ("-option 1 -i " + i + " ").trim().split(" ");
                SNetworkUtil instance = new SNetworkUtil();
                ParamSet ps = new ParamSet();
                ps.setCommandLineArgs(args);    // allow early parsing
                ps = instance.getDefaultParamSet(ps);
                ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
                instance.setParamSet(ps);
                instance.init();
                instance.run();
            }
        }
    }

    /**
     * @param args the command line arguments
     * @throws com.achteck.misc.exception.InvalidParameterException
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    public static void main(String[] args) throws InvalidParameterException, IOException, ClassNotFoundException, Exception {
        TrainInfo instance = new TrainInfo();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
