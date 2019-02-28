/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.norm;

import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import de.planet.itrtech.reco.IImagePreProcess;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.MetadataUtil;
import de.uros.citlab.module.util.PropertyUtil;
import eu.transkribus.interfaces.IImageManipulator;
import eu.transkribus.interfaces.types.Image;

/**
 *
 * @author gundram
 */
public class NormWritingInstance implements IImageManipulator {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(NormWritingInstance.class.getName());
    private final String name = NormWritingInstance.class.getSimpleName();

    private transient boolean isNoisy = false;
    private final IImagePreProcess pp;
    private transient boolean isInitialized = false;

    public NormWritingInstance(IImagePreProcess pp) {
        this.pp = pp;
    }

    @Override
    public String usage() {
        return " uses class " + pp.getClass().getName() + " to normalize image. Parameters are:\n"
                + pp.getParamSet().toString() + "\n"
                + "if property: noise=true, there will be noise on the normalization parameters.\n"
                + "if property: mask=<POLYGON>, all pixels which are outside of this polygon will be set to white. <POLGON>='x_1,y_1 x_2,y_2 ... x_n,y_n'.";
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
    public Image process(Image image) {
        return process(image, null);
    }

    @Override
    public Image process(Image image, String[] props) {
        if (!isInitialized) {
            pp.setParamSet(pp.getDefaultParamSet(new ParamSet()));
            pp.init();
            isInitialized = true;
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
        return ImageUtil.getImage(pp.preProcess(ImageUtil.getHybridImage(image, true, props)));
    }

}
