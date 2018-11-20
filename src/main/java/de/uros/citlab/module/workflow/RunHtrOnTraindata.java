/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.io.File;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ConfMat;
import com.achteck.misc.types.IParamSetHandler;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import com.achteck.misc.util.IO;
import com.achteck.misc.util.StopWatch;
import com.achteck.misc.util.StringIO;
import de.planet.citech.trainer.loader.IImageLoader;
import de.planet.citech.types.IExecutorService;
import de.planet.imaging.types.HybridImage;
import de.planet.itrtech.reco.IImagePreProcess;
import de.planet.itrtech.types.IDictOccurrence;
import de.planet.langmod.types.ILangMod;
import de.planet.reco.types.SNetwork;
import de.planet.sprnn.SNet;
import de.planet.sprnn.util.SNetUtils;
import de.planet.util.LoaderIO;
import de.planet.util.types.DictOccurrence;
import de.uros.citlab.module.htr.HTRParser;
import de.uros.citlab.module.util.IOUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author gundram
 */
public class RunHtrOnTraindata extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(RunHtrOnTraindata.class.getName());
    @ParamAnnotation(descr = "path to network (preproces will be ignored)", member = "network")
    private String net = "";
    SNetwork network;

    @ParamAnnotation(descr = "path to dictionary (can be empty)", member = "lmObject")
    private String lm = "";
    IParamSetHandler lmObject;
    IDictOccurrence dictImpl = null;
    ILangMod langmod = null;

    @ParamAnnotation(descr = "path to image list, normalized to a specific height")
    private String l = "";
//    private IImageLoader imageLoader;

    @ParamAnnotation(descr = "path to output file - containing transcripts of each image")
    private String o = "";

    @ParamAnnotation(descr = "use internal preprocess for given images")
    private boolean pp = false;

    //    @ParamAnnotation(descr = "path to time-file")
//    private String t = "";
    @ParamAnnotation(descr = "folder to cache confmats")
    private String cm = "";

    private IExecutorService es;

    public RunHtrOnTraindata() {
        addReflection(this, RunHtrOnTraindata.class);
    }

    @Override
    public void init() {
        super.init();
        if (o.isEmpty()) {
            throw new RuntimeException("no output file is given");
        }
        if (!o.endsWith("/")) {
            o += "/";
        }
        if (l.isEmpty()) {
            throw new RuntimeException("no input file is given");
        }
        if (!l.endsWith("/")) {
            l += "/";
        }
        if (net.isEmpty()) {
            throw new RuntimeException("no network is given");
        }
        if (lmObject instanceof ILangMod) {
            langmod = (ILangMod) lmObject;
        } else if (lmObject instanceof DictOccurrence) {
            dictImpl = (DictOccurrence) lmObject;
            langmod = HTRParser.getLangMod2(dictImpl, network.getCharMap(), null);
        } else if (lmObject != null) {
            throw new RuntimeException("cannot interprete langmod of class '" + lmObject.getClass().getName() + "'.");
        }
    }

    private static class ConfMatCache {

        private String folder;
        private Map<String, ConfMat> map = new HashMap<>();

        public ConfMatCache(File folder) {
            folder.mkdirs();
            this.folder = folder.getPath();
            if (!this.folder.endsWith(File.separator)) {
                this.folder += File.separator;
            }
            java.io.File[] listFiles = folder.listFiles();
            for (java.io.File listFile : listFiles) {
                map.put(listFile.getName(), (ConfMat) IOUtil.load(listFile));
            }
        }

        public ConfMat get(File file) {
            return map.get(file.getName());
        }

        public void save(File file, ConfMat confMat) {
            if (!map.containsKey(file.getName())) {
                try {
                    IO.save(confMat, new File(folder + file.getName()));
                } catch (IOException ex) {
                    java.util.logging.Logger.getLogger(RunHtrOnTraindata.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public List<String> process(SNet sNet, IImagePreProcess preProc, List<String> loadLineList, StopWatch sw) {
        List<String> res = new LinkedList<>();
        ConfMatCache cache = cm.isEmpty() ? null : new ConfMatCache(new File(cm));
        for (String pathToImage : loadLineList) {
            ConfMat confMat = null;
            if (cache != null) {
                confMat = cache.get(new File(pathToImage));
            }
            if (confMat == null) {
                IImageLoader.IImageHolder loadImageHolder = LoaderIO.loadImageHolder(pathToImage, true, true);
                HybridImage image = loadImageHolder.getImage();
                double[][] netInput = image.getAsInverseGrayMatrix();
                image.getAsByteImage();
                sw.start();
                if (pp) {
                    HybridImage preProcess = preProc.preProcess(image);
                    netInput = preProcess.getAsInverseGrayMatrix();
                }
                LOG.log(Logger.INFO, "pp = " + pp + " and image is " + netInput.length + ":" + netInput[0].length);
                sNet.setInput(netInput);
                sNet.update();
                double[][][] output3D = sNet.getOutput3D();
                double[][] dim3toDim2 = SNetUtils.dim3toDim2(output3D);
                confMat = new ConfMat(sNet.getCharmap());
                confMat.copyFrom(dim3toDim2);
                if (cache != null) {
                    cache.save(new File(pathToImage), confMat);
                }
            } else {
                sw.start();
            }
            if (langmod == null) {
                res.add(confMat.getString(confMat.getBestPath()));
            } else {
                langmod.setConfMat(confMat);
//                langmod.g
//                langmod.update();
                res.add(langmod.getResult().getText());
                System.out.println("Confmat: " + confMat.getString(confMat.getBestPath()));
                System.out.println("Langmod: " + langmod.getResult().getText());
            }
            sw.stop();
//            System.out.println("hyp: '" + res.get(res.size() - 1) + "'");
//            if (loadImageHolder.getTarget() != null) {
//                System.out.println("gt : '" + loadImageHolder.getTarget() + "'");
//            }
        }
        return res;
    }

    public void run() throws IOException {
        SNet sNet = (SNet) network.getNet();
        IImagePreProcess preProc = network.getPreProc();
        List<String> loadLineList = StringIO.loadLineList((File) new File(l));
        LOG.log(Logger.INFO, "found " + loadLineList.size() + " files");
//        StringBuilder sbTime = new StringBuilder();
        {
            StopWatch sw = new StopWatch();
            List<String> process = process(sNet, preProc, loadLineList, sw);
            System.out.println("TIME_1=" + sw.getCumulatedMillis());
//            System.out.println("average time = " + sw.getAverageTime());
//            System.out.println("sum time = " + sw.getCumulatedMillis());
//            System.out.println("count = " + sw.getCounter());
            StringIO.saveLineList(new File(o), process, true);
//            sbTime.append(sw.getCumulatedMillis());
        }
//        {
//            StopWatch sw = new StopWatch();
////            List<String> doubleList = new LinkedList<>(loadLineList);
////            doubleList.addAll(loadLineList);
//            List<String> process = process(sNet, preProc, loadLineList, sw);
//            System.out.println("TIME_2=" + sw.getCumulatedMillis());
////            System.out.println("average time = " + sw.getAverageTime());
////            System.out.println("sum time = " + sw.getCumulatedMillis());
////            System.out.println("count = " + sw.getCounter());
////            sbTime.append(";").append(sw.getCumulatedMillis());
////            StringIO.saveLineList(new File(o), process);
//        }
//        StringIO.saveLineArray(new File(t), new String[]{"TIME_1;TIME_2", sbTime.toString()});
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InvalidParameterException {
//        try {
//            if (args.length == 0) {
//                args = (""
//                        //                                + "-il train_gw.lst "
//                        //                + "-il " + de.planet.trainer.loader.ImageLoaderList.class.getName() + " "
//                        + "-net nets/meganet_37.sprnn "
//                        //                + "-il/size 100 "
//                        //                + "-il/###prefix### ../read/ "
//                        + "-o out.txt "
//                        + "-l list.lst "
//                        + "-pp true "
//                        //                                                + "--help"
//                        + "").split(" ");
//            }
        RunHtrOnTraindata instance = new RunHtrOnTraindata();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
//        } catch (Throwable ex) {
//            System.out.println(ex);
//            LOG.log(Logger.ERROR, "error in process: ", ex);
//            System.exit(-1);
//        }
    }

}
