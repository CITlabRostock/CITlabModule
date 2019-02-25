/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.feat;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamSetOrganizer;
import de.planet.imaging.types.HybridImage;
import de.planet.itrtech.reco.IImagePreProcess;
import de.planet.trainer.factory.ImagePreprocessDft;
import de.uros.citlab.module.interfaces.IFeatureGeneratorStreamable;
import de.uros.citlab.module.util.FeatureIO;
import de.uros.citlab.module.util.MetadataUtil;
import eu.transkribus.interfaces.types.Image;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author gundram
 */
public class PreProcConvert extends ParamSetOrganizer implements IFeatureGeneratorStreamable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(PreProcConvert.class.getName());
    private final String name = "PreProcDft";
    private IImagePreProcess impl = null;

    public PreProcConvert() {
    }

    public void init() {
        try {
            ImagePreprocessDft instance = new ImagePreprocessDft();
            String[] args = (""
                    + "" // +"--help"
                    ).split(" ");
            ParamSet ps = new ParamSet();
            ps.setCommandLineArgs(args);    // allow early parsing
            ps = instance.getDefaultParamSet(ps);
            ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
            instance.setParamSet(ps);
            instance.init();
            impl = instance.getImagePreProcess();
        } catch (InvalidParameterException ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public String usage() {
        return "no parameters available - just use it. applies the default preprocessing steps to snipets";
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
        return Savemode.BYTE;
    }

    @Override
    public int getLLHAccuracy() {
        return 0;
    }

    @Override
    public double[][] process(double[][] in, String[] props) {
        return impl.preProcess(HybridImage.newInstance(in)).getAsInverseGrayMatrix();
    }

}
