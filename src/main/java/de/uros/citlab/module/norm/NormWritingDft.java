/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.norm;

import com.achteck.misc.log.Logger;
import de.planet.imaging.types.HybridImage;
import de.planet.itrtech.reco.IImagePreProcess;
import de.planet.citech.trainer.loader.IImageLoader;
import de.planet.trainer.factory.ImagePreprocessDft;
import de.planet.util.LoaderIO;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.MetadataUtil;
import de.uros.citlab.module.util.PropertyUtil;
import eu.transkribus.interfaces.IFeatureGenerator;
import eu.transkribus.interfaces.types.Image;

/**
 *
 * @author gundram
 */
public class NormWritingDft implements IFeatureGenerator {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(NormWritingDft.class.getName());
    private final String name = NormWritingDft.class.getSimpleName();

    private final int h;
    private final double q_p;
    private final int q_h;
    private transient boolean isNoisy = false;
    private transient IImagePreProcess pp;

    public NormWritingDft() {
        this(32, 0.5, 11);
    }

    public NormWritingDft(int h, double q_p, int q_h) {
        this.h = h;
        this.q_p = q_p;
        this.q_h = q_h;
    }

    private void init() {
        pp = ImagePreprocessDft.getPreProcess(h, q_p, q_h, true);
        if (isNoisy) {
            pp.activateModes(de.planet.reco.types.RecoTrainMode.NOISE);
        } else {
            pp.deactivateModes(de.planet.reco.types.RecoTrainMode.NOISE);
        }
    }

    @Override
    public String usage() {
        return "normalizes an image. Assumes that there is dark wrinting on light background.\n"
                + "There should be between 2% and 30% writing pixels. The image will have a height of " + h + " pixels. \n"
                + "The main quantil of " + String.format("%.2f", q_p * 100) + "% will have a height of " + q_h + " pixels.\n"
                + "if property: noise=true, there will be noise on the normalization parameters.\n"
                + "if property: mask=<POLYGON>, all pixels which are outside of this polygon will be set to white. <POLGON>='x_1,y_1 x_2,y_2 ... x_n,y_n'."
                + "";
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
    public void process(Image image, String pathToFileOut) {
        process(image, pathToFileOut, null);
    }

    @Override
    public void process(String pathToFileIn, String pathToFileOut) {
        process(pathToFileIn, pathToFileOut, null);
    }

    @Override
    public void process(Image image, String pathToFileOut, String[] props) {
        process(ImageUtil.getHybridImage(image, true, props), pathToFileOut, props);
    }
    int length = 0;

    public void process(HybridImage image, String pathToFileOut, String[] props) {
        if (length < image.getWidth()) {
            length = image.getWidth();
            LOG.log(Logger.DEBUG, "maximal width of images = " + length);
        }
        if (pp == null) {
            init();
        }
        boolean noisy = PropertyUtil.isPropertyTrue(props, Key.NOISE);
        if (noisy != this.isNoisy) {
            this.isNoisy = noisy;
            if (noisy) {
                pp.activateModes(de.planet.reco.types.RecoTrainMode.NOISE);
            } else {
                pp.deactivateModes(de.planet.reco.types.RecoTrainMode.NOISE);
            }
        }
        image = pp.preProcess(image);
        image.save(pathToFileOut);
    }

    @Override
    public void process(String pathToFileIn, String pathToFileOut, String[] props) {
        IImageLoader.IImageHolder loadImageHolder = LoaderIO.loadImageHolder(pathToFileIn, true, false);
        process(loadImageHolder.getImage(), pathToFileOut, props);
    }

}
