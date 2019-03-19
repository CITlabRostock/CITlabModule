package de.uros.citlab.module.workflow;

import com.achteck.misc.param.ParamSet;
import com.achteck.misc.util.IO;
import de.planet.imaging.panels.DisplayPlanet;
import de.planet.imaging.types.HybridImage;
import de.planet.itrtech.reco.IImagePreProcess;
import de.planet.reco.ImagePreprocModules;
import de.planet.reco.RecoGlobals;
import de.planet.reco.preproc.BasicMainBodyNormalizer;
import de.planet.reco.preproc.COGLShapeNormalizer;
import de.planet.reco.preproc.ContrastNormalizer6;
import de.planet.reco.preproc.Cropper;
import de.planet.util.gui.Display;
import de.uros.citlab.module.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SetUpPreproc {

    public static final IImagePreProcess getPreProcess(int tgtHeight, double pquantil_p, int pquantil_height) {
        ImagePreprocModules imagePreprocModules = new ImagePreprocModules("pp_deploy_As");
        imagePreprocModules.addModule("contrast", new ContrastNormalizer6());
        imagePreprocModules.addModule("coglshape", new COGLShapeNormalizer());
        imagePreprocModules.addModule("size", new de.planet.util.preproc.SizeNormalizer(pquantil_height, 0.25, 0.75));
        imagePreprocModules.addModule("crop", new Cropper());
//        imagePreprocModules.addModule("main2", new MainBodyNormalizer2());
        imagePreprocModules.addModule("squash", new BasicMainBodyNormalizer());
//        imagePreprocModules.addModule("si", new SizeNormalizerSimple());
        imagePreprocModules.setParamPrefix("");
        ParamSet ps = imagePreprocModules.getDefaultParamSet((ParamSet) null);
        ps.getParam("coglshape/prescale_height").set(tgtHeight);
        ps.getParam("coglshape/subsample_x").set(100);
        ps.getParam("coglshape/subsample_y").set(10);
        ps.getParam("coglshape/degree_divisor").set(5);
        ps.getParam("coglshape/use_abs_x_contrast_matrix").set(true);
//        ps.getParam("main2/mbr_target_height").set(pquantil_height);
        int remain = (tgtHeight - pquantil_height) / 2;
        int remainRemain = tgtHeight - pquantil_height - remain;
//        ps.getParam("main2/bs_top_border_target_size").set(remain / 2);
//        ps.getParam("main2/bs_bottom_border_target_size").set(remainRemain);
//        ps.getParam("main2/mbr_hor_resize_non_prop_factor").set(1.0D);
//        ps.getParam("main2/mbr_scaler_type").copyFrom(ResizeUtil.Algorithm.IP.toString());
        int nonlinearity = (int) (tgtHeight * pquantil_p);
        int linearity = tgtHeight - nonlinearity;
        int mb_lower = linearity / 2;
        int mb_upper = linearity - mb_lower;
        int descender = nonlinearity / 2;
        int ascender = nonlinearity - descender - 1;
        ps.getParam("contrast/slope").copyFrom("1.0");
        ps.getParam("contrast/fg_q").copyFrom("0.05");
        ps.getParam("squash/basic_main_body_normalizer_quantil").copyFrom("0.5");
        ps.getParam("squash/basic_main_body_normalizer_squash_top").copyFrom(String.valueOf(ascender));
        ps.getParam("squash/basic_main_body_normalizer_squash_bottom").copyFrom(String.valueOf(descender));
        ps.getParam("squash/basic_main_body_normalizer_lower_border_size").copyFrom(String.valueOf(mb_lower));
        ps.getParam("squash/basic_main_body_normalizer_upper_border_size").copyFrom(mb_upper);

        imagePreprocModules.setParamSet(ps);
        imagePreprocModules.init();
        imagePreprocModules.setMinimumPrecision(RecoGlobals.Precision.FLOAT);
        return imagePreprocModules;
    }

    public static void main(String[] args) throws IOException {
        IImagePreProcess preProcess1 = getPreProcess(64, 0.75, 30);
        IImagePreProcess preProcess2 = getPreProcess(64, 0.75, 26);
        IO.save(preProcess2, HomeDir.getFile("preproc_01.bin"));
        List<File> jpg = FileUtil.listFiles(HomeDir.getFile("data/base"), "jpg", true);
        int max = 0;
        for (File file : jpg) {
            HybridImage hybridImage = HybridImage.newInstance(file);
            HybridImage hybridImage1 = preProcess1.preProcess(hybridImage);
            HybridImage hybridImage2 = preProcess2.preProcess(hybridImage);
            System.out.println(hybridImage1.getHeight() + " x " + hybridImage1.getWidth());
            max = Math.max(hybridImage1.getWidth(), max);
            System.out.println(max);
            Display.addPanel(new DisplayPlanet(hybridImage1));
            Display.addPanel(new DisplayPlanet(hybridImage2));
            Display.addPanel(new DisplayPlanet(hybridImage));
            Display.show(true, true);
        }


    }
}
