/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.feat;

import com.achteck.misc.log.Logger;
import de.planet.math.util.MatrixUtil;
import de.planet.sprnn.SLayer;
import de.planet.sprnn.SNet;
import de.planet.sprnn.TrainMode;
import de.planet.sprnn.act.SActIdent;
import de.planet.sprnn.cell.CellDft;
import de.planet.sprnn.conv.LayerConv;
import de.planet.sprnn.conv.filter.FilterHarmonicAndHann;
import de.planet.sprnn.util.WeightDistributionGauss;
import de.uros.citlab.module.interfaces.IFeatureGeneratorStreamable;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.FeatureIO;
import de.uros.citlab.module.util.FeatureUtil;
import de.uros.citlab.module.util.MetadataUtil;
import de.uros.citlab.module.util.PropertyUtil;
import eu.transkribus.interfaces.types.Image;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author gundram
 */
public class FeatHannFreq implements IFeatureGeneratorStreamable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(FeatHannFreq.class.getName());
    private final String name = "FeatHannFreq";
    private final int ss_x;// = 2;
    private final int ss_y;//= 4;

    private final int f;// = 2;
    private final double w;// = 3.75;
    private final int a;//= 4;
    private final double f0;// = 0.125;
    private final double n;//= 0.1;

    private transient SNet net;
    private transient boolean noisy = false;
//<editor-fold defaultstate="collapsed" desc="TrainModeHandler Stuff">

    public FeatHannFreq() {
        this(2, 4, 2, 3.75, 4, 0.125, 0.1);
    }

    public FeatHannFreq(int ss_x, int ss_y) {
        this(ss_x, ss_y, 2, 3.75, 4, 0.125, 0.1);
    }

    public FeatHannFreq(int ss_x, int ss_y, int f, double w, int a, double f0, double n) {
        this.ss_x = ss_x;
        this.ss_y = ss_y;
        this.f = f;
        this.w = w;
        this.a = a;
        this.f0 = f0;
        this.n = n;
    }

    private void init() {
        SLayer[] layers;
        net = new SNet();
        layers = net.addLayerStructD2(null, new CellDft((new SActIdent())), 1, 1, 1, 0, false);
        net.addLayers(LayerConv.getLayerConvHarmonicAndWindowAndNoise(layers, ss_y, ss_x, new FilterHarmonicAndHann(), f0, w, a, f, 2.0, n));
        net.initWeights(new WeightDistributionGauss(null, 1.0));
        net.init();

    }

    @Override
    public String usage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    public void process(String pathToFileIn, String pathToFileOut, String[] props) {
        FeatureIO.process(this, pathToFileIn, pathToFileOut, props);
    }

    @Override
    public void process(Image image, String pathToFileOut) {
        process(image, pathToFileOut, null);
    }

    @Override
    public void process(String pathToFileIn, String pathToFileOut) {
        process(pathToFileIn, pathToFileOut, null);
    }

    @Override
    public Savemode getSaveMode() {
        return Savemode.FLOAT;
    }

    @Override
    public int getLLHAccuracy() {
        return 0;
    }

    @Override
    public double[][] process(double[][] in, String[] props) {
        boolean noiseProperty = PropertyUtil.isPropertyTrue(props, Key.NOISE);
        if (this.noisy != noiseProperty) {
            this.noisy = noiseProperty;
            if (this.noisy) {
                net.activateModes(TrainMode.NET_NOISE);
            } else {
                net.deactivateModes(TrainMode.NET_NOISE);
            }
        }
        if (net == null) {
            init();
        }
        net.setInput(MatrixUtil.transpose(in));
        net.update();
        double[][][] output3D = net.getOutput3D();
        return MatrixUtil.transpose(FeatureUtil.makeFlat(output3D));
    }

}
