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
import de.planet.itrtech.reco.ISNetwork;
import de.planet.reco.types.SNetwork;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.util.CharMapUtil;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.IOUtil;
import org.apache.commons.math3.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author gundram
 */
public class HtrInfo extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(HtrInfo.class.getName());

    @ParamAnnotation(descr = "path to network")
    String i = "";
    @ParamAnnotation(descr = "path where charmap should be saved (empty for no saving)")
    String cm = "";
    @ParamAnnotation(descr = "path where train-log should be saved (empty for no saving)")
    String cer_train = "";
    @ParamAnnotation(descr = "path where test-log should be saved (empty for no saving)")
    String cer_test = "";

    public HtrInfo() {
        addReflection(this, HtrInfo.class);
    }

    @Override
    public void init() {
        super.init();
        if (i.isEmpty()) {
            throw new RuntimeException("no parameter 'i' given. Please set '-i <path_to_network_>.");
        }
        if (cm.isEmpty() && cer_train.isEmpty() && cer_test.isEmpty()) {
            throw new RuntimeException("no parameter 'cm', 'cer_test' and 'cer_train' given. Please set one of these parameters.");
        }
    }

    private static Pair<List<String>, List<String>> getTrainingInfos(SNetwork net) {
        List<com.achteck.misc.types.Pair<String, String>> traininfo = net.getNet().getTraininfo();
        List<String> train = new ArrayList<>();
        List<String> test = new ArrayList<>();
        for (com.achteck.misc.types.Pair<String, String> pair : traininfo) {
            boolean isTrain = false;
            String[] infos = pair.second.split(";");
            if (!infos[0].equals("Run")) {
                continue;
            }
            for (String info : infos) {
                if (info.equals("Train")) {
                    isTrain = true;
                }
                if (info.equals("Val")) {
                    isTrain = false;
                }
                if (info.startsWith("CER=")) {
                    String cer = info.substring(4);
                    if (isTrain) {
                        train.add(cer);
                    } else {
                        test.add(cer);
                    }
                }
            }
        }
        return new Pair<>(train, test);
    }

    public void run() {
        ISNetwork load = (ISNetwork) IOUtil.load(i);
        load.setParamSet(load.getDefaultParamSet(new ParamSet()));
        load.init();
        if (!cm.isEmpty()) {
            CharMapUtil.saveCharMap(load.getCharMap(), new File(cm));
        }
        if (!cer_test.isEmpty() || !cer_train.isEmpty()) {
            SNetwork network = (SNetwork) IOUtil.load(i);
            Pair<List<String>, List<String>> trainingInfos = getTrainingInfos(network);
            if (!cer_train.isEmpty()) {
                FileUtil.writeLines(new File(cer_train), trainingInfos.getFirst());
            }
            if (!cer_test.isEmpty()) {
                FileUtil.writeLines(new File(cer_test), trainingInfos.getSecond());
            }
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidParameterException, IOException, ClassNotFoundException {
        if (args.length == 0) {
            ArgumentLine al = new ArgumentLine();
            al.addArgument("i", "/home/gundram/devel/projects/racetrack_stazh/nets/t2i_nolb_hyp/net_0.sprnn");
//            al.addArgument("cm", "cm.txt");
            al.addArgument("cer_train", "cer_train.txt");
            al.addArgument("cer_test", "cer_test.txt");
            args = al.getArgs();
        }
        System.out.println(Arrays.asList(args));
        HtrInfo instance = new HtrInfo();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
