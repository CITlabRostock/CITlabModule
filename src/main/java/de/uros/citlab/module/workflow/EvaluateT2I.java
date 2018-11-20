/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.Pair;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.planet.math.util.VectorUtil;
import de.planet.util.Gnuplot;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.util.PolygonUtil;
import eu.transkribus.baselinemetrictool.Metric_BL_eval;
import eu.transkribus.baselinemetrictool.util.BaseLineMetricResult;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextEquivType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;

import javax.xml.bind.JAXBException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author gundram
 */
public class EvaluateT2I extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(EvaluateT2I.class.getName());

    @ParamAnnotation(descr = "path to folder containing images and pageXML files GT")
    private String gt;
    @ParamAnnotation(descr = "path to folder containing images and pageXML files HYP")
    private String hyp;
    @ParamAnnotation(descr = "threshold for f-measure (if two baselines fit together)")
    private double th;
//    @ParamAnnotation(descr = "create debug images")
//    private boolean debug;
    @ParamAnnotation(descr = "ratio of how much more critical are FP vs. FN (FP/FN)")
    private double ratio;
    @ParamAnnotation(descr = "beta of f-measure")
    private double beta;

    private File folderGT;
    private File folderHYP;
//    private boolean createDebug;

    public EvaluateT2I() {
        this("", "", 0.4, 10.0);
    }

    public EvaluateT2I(String gt, String hyp, double threshold, double ratio) {
        this.gt = gt;
        this.hyp = hyp;
//        debug = createDebugImg;
        th = threshold;
        this.ratio = ratio;
        beta = ratio;
        addReflection(this, EvaluateT2I.class);
    }

    @Override
    public void init() {
        super.init();
        if (gt.isEmpty()) {
            throw new RuntimeException("no input folder given");
        }
        if (hyp.isEmpty()) {
            throw new RuntimeException("no output folder given");
        }
        folderGT = new File(gt);
        folderHYP = new File(hyp);
    }

    public void run() throws MalformedURLException, IOException, JAXBException {
        List<File> listFilesGT = new LinkedList<>(FileUtil.listFiles(folderGT, "xml XML".split(" "), true));
        List<File> listFilesHYP = new LinkedList<>(FileUtil.listFiles(folderHYP, "xml XML".split(" "), true));
        FileUtil.deleteMetadataAndMetsFiles(listFilesGT);
        FileUtil.deleteMetadataAndMetsFiles(listFilesHYP);
        if (listFilesGT.size() != listFilesHYP.size()) {
            throw new RuntimeException("different number of files in folder GT (" + listFilesGT.size() + ") and HYP (" + listFilesHYP.size() + ").");
        }
        Collections.sort(listFilesGT);
        Collections.sort(listFilesHYP);
        List<MatchPoint> points = new LinkedList<>();
        for (int i = 0; i < listFilesHYP.size(); i++) {
            PcGtsType pageHyp = PageXmlUtil.unmarshal(listFilesHYP.get(i));
            PcGtsType pageGT = PageXmlUtil.unmarshal(listFilesGT.get(i));
            List<TextLineType> textLinesHyp = PageXmlUtil.getTextLines(pageHyp);
            List<TextLineType> textLinesGT = PageXmlUtil.getTextLines(pageGT);
            for (TextLineType tltGT : textLinesGT) {
                TextLineType tltHyp = getTextMatch(textLinesHyp, tltGT);
                if (tltHyp == null) {
                    points.add(new MatchPoint(tltGT));
                } else {
                    eu.transkribus.baselinemetrictool.Metric_BL_eval val = new Metric_BL_eval(30, 30);
                    val.calcMetricForPageBaseLinePolys(new Polygon[]{PolygonUtil.getBaseline(tltGT)}, new Polygon[]{PolygonUtil.getBaseline(tltHyp)});
                    BaseLineMetricResult res = val.getRes();
                    double precision = res.getPrecision();
                    double recall = res.getRecall();
                    double fmeasure = (1 + beta * beta) * precision * recall / (precision + beta * beta * recall);
                    points.add(new MatchPoint(tltGT, tltHyp, fmeasure > th));
                }
            }
        }
        Collections.sort(points);
        int size = points.size();
        double[] sum = new double[size];
        double[] conf = new double[size];
        double[] prec = new double[size];
        double[] recall = new double[size];
        double[] fMeasure = new double[size];
        double[] fitness = new double[size];
        int tp = 0;
        int fn = 0;
        int fp = 0;
        int cnt = 0;
        for (int i = 0; i < points.size(); i++) {
            MatchPoint point = points.get(i);
            switch (point.getType()) {
                case FN:
                    fn++;
                    break;
                case FP:
                    fp++;
                    break;
                case TP:
                    tp++;
                    break;
                default:
                    throw new RuntimeException("not expected type '" + point.getType() + "'");
            }
            conf[i] = point.conf;
            cnt++;
            sum[i] = ((double) cnt) / size;
            prec[i] = ((double) tp) / (tp + fp);
            recall[i] = ((double) tp) / (tp + size);
            fMeasure[i] = (1 + beta * beta) * (prec[i] * recall[i]) / (beta * beta * prec[i] + recall[i]);
            fitness[i] = tp - ratio * fp;
        }
        Pair<Double, Double> minMax = VectorUtil.getMinMax(fitness);
        for (int i = 0; i < fitness.length; i++) {
            fitness[i] = (fitness[i] - minMax.first) / (minMax.second - minMax.first);

        }
        List<double[]> data = new LinkedList<>();
        data.add(prec);
        data.add(recall);
        data.add(conf);
        data.add(fMeasure);
        data.add(fitness);
        Gnuplot.plot(sum, data, "measures over confidence", new String[]{"prec", "rec", "conf", "F_{" + beta + "}", "quality"}, 0, 1);
    }

    private TextLineType getTextMatch(List<TextLineType> hyps, TextLineType gt) {
        TextEquivType textEquivGT = gt.getTextEquiv();
        if (textEquivGT == null) {
            throw new RuntimeException("textequiv is emtpy for ground truth");
        }
        String ref = textEquivGT.getUnicode();
        List<TextLineType> candidates = new LinkedList<>();
        for (TextLineType hyp1 : hyps) {
            TextEquivType textEquiv = hyp1.getTextEquiv();
            if (textEquiv == null) {
                continue;
            }
            String unicode = textEquiv.getUnicode();
            if (unicode.equals(ref)) {
                candidates.add(hyp1);
            }
        }
        switch (candidates.size()) {
            case 0:
                return null;
            case 1:
                return candidates.get(0);
            default:
                throw new RuntimeException("more than one possible textline found - unexpected case");
        }
    }

    private static class MatchPoint implements Comparable<MatchPoint> {

        private final TextLineType gt;
        private final TextLineType hyp;
        private final double conf;
        private final Type type;

        public MatchPoint(TextLineType gt) {
            this(gt, null, false);
        }

        public MatchPoint(TextLineType gt, TextLineType hyp, boolean polygonsMatch) {
            this.gt = gt;
            this.hyp = hyp;
            if (hyp == null) {
                conf = 0;
                type = Type.FN;
            } else {
                Float conf1 = hyp.getTextEquiv().getConf();
                conf = conf1 == null ? 1.0 : conf1.doubleValue();
                type = polygonsMatch ? Type.TP : Type.FP;
            }
        }

        public Type getType() {
            return type;
        }

        @Override
        public int compareTo(MatchPoint o) {
            return Double.compare(o.conf, conf);
        }

    }

    private enum Type {
        TP, FP, FN;
    }

    public static void main(String[] args) throws InvalidParameterException, MalformedURLException, IOException, JAXBException, InterruptedException {
        if (args.length == 0) {
            ArgumentLine al = new ArgumentLine();
            al.addArgument("gt", HomeDir.getFile("data/val/"));//folder containing images and PAGE-XML with baselines
            al.addArgument("hyp", HomeDir.getFile("val_la_collection/")); //folder where recognition should be saved
            args = al.getArgs();
        }
        EvaluateT2I instance = new EvaluateT2I();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
