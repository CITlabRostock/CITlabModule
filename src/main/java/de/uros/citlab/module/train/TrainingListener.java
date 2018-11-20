/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.train;

import com.achteck.misc.IFEventListener;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.Param;
import com.achteck.misc.types.CharMap;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamSetOrganizer;
import com.achteck.misc.util.IO;
import com.achteck.misc.util.StopWatch;
import de.planet.itrtech.reco.ISNetwork;
import de.planet.itrtech.reco.ISNetworkBase;
import de.planet.citech.trainer.IListener;
import de.planet.trainer.Trainer;
import de.planet.trainer.TrainerThread;
import de.planet.trainer.util.ErrorCTC;
import de.planet.trainer.util.TrainerException;
import de.planet.util.ObjectCounter;
import de.planet.util.PathCalculatorDft;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.errorrate.costcalculator.CostCalculatorDft;
import de.uros.citlab.errorrate.htr.ErrorModuleDynProg;
import de.uros.citlab.errorrate.interfaces.IErrorModule;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.tokenizer.categorizer.CategorizerWordMergeGroups;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import org.apache.commons.io.FileUtils;

/**
 *
 * Calculates the CTC error and logs it.
 *
 * 2014.05.26 implements the external SubstitutionCounter and ErrorCTC instead
 * of internal classes. (Tobias)
 *
 * @author gundram
 */
public class TrainingListener extends ParamSetOrganizer implements Comparable<TrainingListener>, IListener {

    public static String EVENT_RECORESULT = "recoresult";
    protected TrainingListener val;
    protected TrainingListener train;
    protected double time;
    protected boolean normed = false;
    protected final HashMap<Object, StopWatch> stopWatches = new HashMap<>();
    protected ErrorCTC errorCalculator;
    IErrorModule errModule = null;
    public static Logger LOG = Logger.getLogger(TrainingListener.class.getName());
    private int cntWorseEpochs = 0;
    private int cntEpochs = 0;
    private boolean earlyStoppingStarted = false;
    private double best_cer = Double.MAX_VALUE;
    private double best_wer = Double.MAX_VALUE;
    private double best_ctc = Double.MAX_VALUE;
    private double best_method = Double.MAX_VALUE;
    Observable observable = null;
    @ParamAnnotation(descr = "path to save the best network")
    private String save_best = "";
    @ParamAnnotation(descr = "method for saving possible: CER WER CTC")
    private String method = "CER";
    @ParamAnnotation(descr = "saves the process during training to given path")
    private String save_train = "";
    @ParamAnnotation(descr = "saves the process during training to given path")
    private String save_test = "";
    @ParamAnnotation(descr = "early stopping. If a positive number of epochs no better network is saved, break actual epoch (but contiue other epoch blocks)")
    private int est = -1;

    public TrainingListener() {
        addReflection(this, TrainingListener.class);
    }

    protected void reset() {
        errorCalculator.reset();
        time = 0;
        stopWatches.clear();
        normed = false;
        errModule = new ErrorModuleDynProg(new CostCalculatorDft(), new CategorizerWordMergeGroups(), null, false);
    }

    protected synchronized void addPoint(TrainerThread trainerThread) {
        String target = (String) trainerThread.target;
        if (target != null) {
            errorCalculator.calcStat(target, ((String) trainerThread.output), trainerThread.error);
            errModule.calculate(((String) trainerThread.output), target);
        }
        trainerThread.fireEvent(EVENT_RECORESULT, this, trainerThread.output);
    }

    public String getInfo() {
        TrainingListener normedValues = normed ? this : getNormedValues();
        String werString = String.format("WER=%6.4f;", getErrorRate(normedValues.errModule));
        return String.format(errorCalculator.getInfo() + werString + "TIME=%06.1f;", normedValues.time);
    }

    @Override
    public int compareTo(TrainingListener src) {
        double own = normed ? errorCalculator.getCtcMlError() : (errorCalculator.getCtcMlError() / (double) errorCalculator.getCntSeq());
        double other = src.normed ? src.errorCalculator.getCtcMlError() : (src.errorCalculator.getCtcMlError() / (double) src.errorCalculator.getCntSeq());
        return (int) Math.signum(own - other);
    }

    public TrainingListener getNormedValues() {
        TrainingListener normedFitness = new TrainingListener();
        normedFitness.errorCalculator = errorCalculator.getNormedValues();
        normedFitness.errModule = errModule;
//        normedFitness.errorCalculator = normedFitness.errorCalculator.getNormedValues();
        long timeacc = 0;
        long counter = 0;
        for (StopWatch stopWatch : stopWatches.values()) {
            timeacc += stopWatch.getCumulatedMillis();
            counter += stopWatch.getCounter();
        }

        normedFitness.time = ((double) timeacc) / ((double) counter);
        normedFitness.normed = true;
        return normedFitness;
    }

    @Override
    public boolean onError(Exception x, Object source, Object reason, boolean handled) {
        return false;
    }

    @Override
    public void init() {
        super.init();
        LOG.log(Logger.INFO, "method = " + method + " path4network = '" + save_best + "' trainError = '" + save_train + "' testError = '" + save_test + "'");
        if (!save_train.isEmpty()) {
            File f = new File(save_train);
            try {
                if (f.isDirectory()) {
                    throw new RuntimeException("file '" + save_train + "' is a directory");
                }
                FileUtils.deleteQuietly(f);
            } catch (RuntimeException ex) {
                LOG.log(Logger.ERROR, "cannot save statistic to '" + save_train + "'.", ex);
            }
        }
        if (!save_test.isEmpty()) {
            File f = new File(save_test);
            try {
                if (f.isDirectory()) {
                    throw new RuntimeException("file '" + save_test + "' is a directory");
                }
                FileUtils.deleteQuietly(f);
            } catch (RuntimeException ex) {
                LOG.log(Logger.ERROR, "cannot save statistic to '" + save_test + "'.", ex);
            }
        }
    }

    private static double getThreshold2StartEST(String method) {
        switch (method) {
            case "CER":
            case "WER":
                return 0.8;
            case "CTC":
                return Double.MAX_VALUE;
            default:
                throw new RuntimeException("unknown method '" + method + "'.");
        }
    }

    @Override
    public void onEvent(String message, Object source, Object reason) {
        try {
            if (message.equals(TrainHtr.EVENT_PRESENT_OBSERVABLE)) {
                observable = (Observable) reason;
                observable.notifyObservers(new TrainHtr.Status(0, 1.0, 1.0, method, false));
            }
            if (message.equals(Trainer.EVENT_NET_LOAD)) {
                ISNetwork network = (ISNetwork) ((Trainer) source).getNet();
                CharMap<Integer> charMap = network.getCharMap();
                if (charMap == null) {
                    Param param = network.getPAA().getParam("type");
                    if (param == null) {
                        Logger.getLogger(TrainingListener.class.getName()).log(Logger.ERROR, "neighter charmap nore type set into network.");
                        throw new TrainerException("neighter charmap nore type set into network.");
                    }
                }
                errorCalculator = new ErrorCTC(network.getCharMap());
                errorCalculator.init();
            }
            if (message.equals(TrainerThread.EVENT_NET_SETINPUT)) {
                synchronized (stopWatches) {
                    StopWatch sw = stopWatches.get(reason);
                    if (sw == null) {
                        sw = new StopWatch();
                        stopWatches.put(reason, sw);
                    }
                    sw.start();
                }
            }
            if (message.equals(TrainerThread.EVENT_NET_UPDATE)) {
                stopWatches.get(reason).stop();
            }
            if (message.equals(TrainerThread.EVENT_STEP_END)) {
                addPoint((TrainerThread) source);
            }
            if (message.equals(Trainer.EVENT_TRAIN_START) || message.equals(Trainer.EVENT_VAL_START)) {
                reset();
            }
            if (message.equals(Trainer.EVENT_TRAIN_END)) {
                train = getNormedValues();
                LOG.log(Logger.INFO, "Training-Fitness: " + train.getInfo());
                LOG.log(Logger.INFO, String.format("Training WER; %6.2f%s; %s", getErrorRate(errModule) * 100.0, "%", errModule.getCounter().getResultOccurrence().toString()));
                errorCalculator.getSubstituteCounter().complete();
                errorCalculator.newSubstitutionCounter();
//                StringIO.saveLineList("out.txt", out_tmp_tobias);
            }
            if (message.equals(Trainer.EVENT_VAL_END)) {
                val = getNormedValues();
                LOG.log(Logger.INFO, "Validation-Fitness: " + val.getInfo());
                LOG.log(Logger.INFO, String.format("Validation WER; %6.2f%s; %s", getErrorRate(errModule) * 100.0, "%", errModule.getCounter().toString()));
                errorCalculator.getSubstituteCounter().complete();
                errorCalculator.newSubstitutionCounter();
//                StringIO.saveLineList("out.txt", out_tmp_tobias);
            }
            if (message.equals(Trainer.EVENT_NET_SAVE)) {
                if (train != null || val != null) {
                    cntEpochs++;
                    Trainer trainer = (Trainer) source;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Run;").append(trainer.getEpochInfo().replace(";", ",")).append(";");
                    if (train != null) {
                        sb.append("Train;").append(train.getInfo());
                    }
                    if (val != null) {
                        sb.append("Val;").append(val.getInfo());
                    }
                    sb.append((new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss")).format(new Date())).append(";");
                    reset();
                    TrainingListener thisFitness = (val != null) ? val : train;
                    double errCTC = thisFitness.errorCalculator.getCtcMlError();
                    double errCER = thisFitness.errorCalculator.getLabelErrorRate();
                    double errWER = getErrorRate(thisFitness.errModule);
                    LOG.log(Logger.DEBUG, "best ctc = " + best_ctc);
                    LOG.log(Logger.DEBUG, "actu ctc = " + thisFitness.errorCalculator.getCtcMlError());
                    if (errCTC < best_ctc) {
                        best_ctc = errCTC;
                        sb.append("bestCTC;");
                    }
                    LOG.log(Logger.DEBUG, "best wer = " + best_wer);
                    LOG.log(Logger.DEBUG, "actu wer = " + errWER);
                    if (errWER < best_wer) {
                        best_wer = errWER;
                        sb.append("bestWER;");
                    }
                    LOG.log(Logger.DEBUG, "best cer = " + best_cer);
                    LOG.log(Logger.DEBUG, "actu cer = " + errCER);
                    if (errCER < best_cer) {
                        best_cer = errCER;
                        sb.append("bestCER;");
                    }
                    trainer.writeLogger(this, sb.toString());

                    double errMethod = -1;
                    double errTrain = -1;
                    double errTest = -1;
                    switch (method) {
                        case "CER":
                            errMethod = errCER;
                            errTrain = train == null ? -1 : train.errorCalculator.getLabelErrorRate();
                            errTest = val == null ? -1 : val.errorCalculator.getLabelErrorRate();
                            break;
                        case "WER":
                            errMethod = errWER;
                            errTrain = train == null ? -1 : getErrorRate(train.errModule);
                            errTest = val == null ? -1 : getErrorRate(val.errModule);
                            break;
                        case "CTC":
                            errMethod = errCTC;
                            errTrain = train == null ? -1 : train.errorCalculator.getCtcMlError();
                            errTest = val == null ? -1 : val.errorCalculator.getCtcMlError();
                            break;
                        default:
                            LOG.log(Logger.ERROR, "unknown method to calculate error: '" + method + "'.");
                    }
                    boolean bestMatch = errMethod < best_method;
                    earlyStoppingStarted = earlyStoppingStarted || (errMethod < getThreshold2StartEST(method));
                    if (bestMatch) {
                        cntWorseEpochs = 0;
                        best_method = errMethod;
                    } else if (earlyStoppingStarted) {
                        cntWorseEpochs++;
                        if (est > 0 && cntWorseEpochs >= est) {
                            LOG.log(Logger.INFO, "set early stopping of trainer, because number of epochs is " + cntWorseEpochs);
                            trainer.setEarlyStopping(true);
                        }
                    }
                    if (bestMatch && !save_best.isEmpty()) {
                        File f = new File(save_best);
                        File parentFile = f.getParentFile();
                        if (parentFile != null) {
                            parentFile.mkdirs();
                        }
                        try {
                            saveNet(trainer.getNet(), save_best);
                        } catch (RuntimeException ex) {
                            LOG.log(Logger.ERROR, "cannot save network to '" + save_best + "'.", ex);
                        }
                    }
                    if (!save_test.isEmpty()) {
                        File f = new File(save_test);
                        File parentFile = f.getParentFile();
                        if (parentFile != null) {
                            parentFile.mkdirs();
                        }
                        try {
                            String out = String.format("%.6f", errTest);
                            FileUtil.writeLines(f, Arrays.asList(out), true);
                        } catch (RuntimeException ex) {
                            LOG.log(Logger.ERROR, "cannot save statistic to '" + save_test + "'.", ex);
                        }
                    }
                    if (!save_train.isEmpty()) {
                        File f = new File(save_train);
                        File parentFile = f.getParentFile();
                        if (parentFile != null) {
                            parentFile.mkdirs();
                        }
                        try {
                            String out = String.format("%.6f", errTrain);
                            FileUtil.writeLines(f, Arrays.asList(out), true);
                        } catch (RuntimeException ex) {
                            LOG.log(Logger.ERROR, "cannot save statistic to '" + save_train + "'.", ex);
                        }
                    }
                    train = null;
                    val = null;
                    observable.notifyObservers(new TrainHtr.Status(cntEpochs, errTrain, errTest, method, bestMatch));
                }
            }
        } catch (Throwable t) {
            //exceptions caused in this listener call should stop the trainer, because without this listener training result do not make sense
            throw new TrainerException(t);
        }
    }
//    ArrayList<String> out_tmp_tobias = new ArrayList<>();

    private ObjectCounter<PathCalculatorDft.Manipulation> calculateWER(String s1, String s2) {
//        out_tmp_tobias.add(s2 + "^" + s1);
        ObjectCounter<PathCalculatorDft.Manipulation> counterWER = new ObjectCounter<>();
        List<String> s1_split = new ArrayList<>();
        List<String> list = Arrays.asList(s1.split("[ ]+"));
        for (String s : list) {
            s = s.trim();
            if (!s.isEmpty() && !s.equals(" ")) {
                s1_split.add(s);
            }
        }
        List<String> s2_split = new ArrayList<>();
        List<String> list2 = Arrays.asList(s2.split("[ ]+"));
        for (String s : list2) {
            s = s.trim();
            if (!s.isEmpty() && !s.equals(" ")) {
                s2_split.add(s);
            }
        }
        errModule.calculate(s1, s2);
        return counterWER;
    }

    private static double getErrorRate(IErrorModule module) {
        return ((double) module.getCounter().get(Count.ERR)) / ((double) module.getCounter().get(Count.GT));
    }

    private void saveNet(ISNetworkBase net, String name) {
        try {
            IO.save(net, name);
        } catch (IOException ex) {
            Logger.getLogger(TrainingListener.class.getName()).log(Logger.ERROR, "network " + name + " cannot be saved", ex);
        }
    }

    @Override
    public IFEventListener getInstanceForThread() {
        return this;
    }

    @Override
    public void combineInstancesForThread() {
    }

}
