/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.feat;

import com.achteck.misc.log.Logger;
import de.planet.math.util.MatrixUtil;
import de.planet.decoding.util.ConfMatHelper;
import de.planet.sprnn.SLayer;
import de.planet.sprnn.SNet;
import de.planet.sprnn.util.SNetUtils;
import de.uros.citlab.module.interfaces.IFeatureGeneratorStreamable;
import de.uros.citlab.module.util.FeatureIO;
import de.uros.citlab.module.util.FeatureUtil;
import eu.transkribus.interfaces.types.Image;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 *
 * @author gundram
 */
public class FeatNetwork implements IFeatureGeneratorStreamable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(FeatNetwork.class.getName());
    private final String version = "1.0.0";
    private final String name = "FeatNetwork";

    private final SNet net;

    private final int unsample;

    private final boolean cm;

    private final boolean last;

    private final int anzFeatures;

    private final Savemode saveMode;

    private transient ArrayList<SLayer> layersLast;

    public FeatNetwork(SNet net, int unsample, boolean cm, boolean last) {
        this.net = net;
        this.unsample = unsample;
        this.cm = cm;
        this.last = last;
        anzFeatures = getNumFeatures(net);
        saveMode = last ? Savemode.FLOAT : Savemode.LLH;
        init();
    }

    private int getNumFeatures(SNet net) {
        return net.inLayer.PhysUnitArray.length;
    }

    public FeatNetwork(SNet net, boolean cm, boolean last) {
        this(net, net.xShrink, cm, last);
    }

    public FeatNetwork(SNet net) {
        this(net, true, false);
    }

    public final void init() {
        int ss_before = net.xShrink;
        LOG.log(Logger.DEBUG, "try to change subsampling from " + ss_before + " to " + ss_before / unsample + " ...");
        SNetUtils.changeSubsampling(net, ss_before / unsample);
        net.init();
        net.writeLogger(this, "change shrink of the network from " + ss_before + " to " + net.xShrink);
        LOG.log(Logger.DEBUG, "try to change subsampling from " + ss_before + " to " + ss_before / unsample + " ... [DONE]");
        layersLast = net.outLayer.getLayersSrc();
        layersLast.remove(net.outLayer);
    }

    @Override
    public String usage() {
        StringBuilder sb = new StringBuilder();
        sb.append("uses an SPRNN to generate features.\n");
        if (cm) {
            if (last) {
                sb.append("features are the ConfMat (as LLH-Mat) and the hidden values (in [-1,1]).\n");
            } else {
                sb.append("features is the ConfMat (as LLH-Mat).\n");
            }
        } else {
            sb.append("features are the hidden values (in [0,1]).\n");
        }
        sb.append("The subsampling of the input features is ").append(net.xShrink).append(".");
        sb.append("if property: noise=true, there will be dropout, if the neural networks has such a dropout layer.\n");
        return sb.toString();
    }

    @Override
    public String getToolName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getProvider() {
        return "University of Rostock\nInstitute of Mathematics\nCITlab\nGundram Leifert\ngundram.leifert@uni-rostock.de";
    }

    @Override
    public void process(InputStream is, OutputStream os) {
        FeatureIO.process(this, is, os, null);
    }

    @Override
    public void process(Image image, String pathToFileOut, String[] props) {
        FeatureIO.process(this, image, pathToFileOut, props);
    }

    @Override
    public void process(Image image, String pathToFileOut) {
        FeatureIO.process(this, image, pathToFileOut, null);
    }

    @Override
    public void process(String pathToFileIn, String pathToFileOut, String[] props) {
        FeatureIO.process(this, pathToFileIn, pathToFileOut, props);

    }

    @Override
    public void process(String pathToFileIn, String pathToFileOut) {
        FeatureIO.process(this, pathToFileIn, pathToFileOut, null);
    }

    @Override
    public Savemode getSaveMode() {
        return saveMode;
    }

    @Override
    public int getLLHAccuracy() {
        if (last) {
            throw new RuntimeException("no llh accuracy given if last hidden layer should be put out.");
        }
        return -50;
    }

    private double[][] conf2LnProb(double[][] confMat) {
        double[][] ret = new double[confMat.length][];
        for (int i = 0; i < ret.length; i++) {
            double lnSum = lnSumExp(confMat[i]);
            ret[i] = conf2LnProb(confMat[i], lnSum);
        }
        return ret;
    }

    private double[] conf2LnProb(double[] confVec, double sum) {
        double[] ret = new double[confVec.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = confVec[i] - sum;
        }
        return ret;
    }

    private double lnSumExp(double[] confVec) {
        double sum = 0;
        for (int i = 0; i < confVec.length; i++) {
            sum += Math.exp(confVec[i]);
        }
        sum = Math.log(sum);
        return sum;
    }

    @Override
    public double[][] process(double[][] in, String[] props) {
        in = MatrixUtil.transpose(in);
        if (net == null) {
            init();
        }
        if (anzFeatures == 1) {
            net.setInput(in);
        } else {
            net.setInput(FeatureUtil.makeDeep(in, anzFeatures));
        }
        net.update();
        if (!last && cm) {
            double[][][] output3D = net.getOutput3D();
            return conf2LnProb(MatrixUtil.transpose(FeatureUtil.makeFlat(output3D)));
        }
        LinkedList<double[]> outs = new LinkedList<>();
        if (cm) {
            double[][][] out = net.getOutput3D();
            double[][] copyTo = new double[out.length][];
            for (int i = 0; i < copyTo.length; i++) {
                copyTo[i] = out[i][0];
            }
            outs.addAll(Arrays.asList(MatrixUtil.transpose(ConfMatHelper.softmax(MatrixUtil.transpose(copyTo)))));
        }
        if (last) {
            for (SLayer sLayer : layersLast) {
                int[] idxVisable = sLayer.PhysUnitArrayVisible;
                for (int i : idxVisable) {
                    double[][] featureMap = net.dataY[i];
                    outs.addAll(Arrays.asList(featureMap));
                }
            }
        }
        double[][] res = new double[outs.size()][];
        for (int i = 0; i < outs.size(); i++) {
            res[i] = outs.get(i);
        }
        return res;

    }

}
