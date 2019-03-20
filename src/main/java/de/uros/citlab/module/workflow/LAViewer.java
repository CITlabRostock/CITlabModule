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
import de.planet.imaging.types.HybridImage;
import de.planet.imaging.util.GraphicalMonPanelFrame;
import de.planet.imaging.util.StdGraphicalMonPanelFrame;
import de.planet.math.functor.DoubleFunctor;
import de.uros.citlab.module.la.LayoutAnalysisURO_ML;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.segmentation.CITlab_LA_ML;
import de.uros.citlab.segmentation.core.Bla;
import de.uros.citlab.segmentation.core.SuperPixelCalcML_BL;
import de.uros.citlab.segmentation.interfaces.ILineClusterer;
import de.uros.citlab.segmentation.interfaces.ISuperPixel;
import de.uros.citlab.segmentation.types.ImageContainer;
import de.uros.citlab.segmentation.types.WrapperTF_BL_SEP;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.types.Image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author gundram
 */
public class LAViewer extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(LAViewer.class.getName());

    @ParamAnnotation(descr = "path or file of image(s)")
    private String i = "";
    @ParamAnnotation(descr = "path to net")
    private String p = "";
    @ParamAnnotation(descr = "path to net")
    private int t = 200;

    public LAViewer() {
        addReflection(this, LAViewer.class);
    }

    public void run() throws MalformedURLException {
        StdGraphicalMonPanelFrame fr = new StdGraphicalMonPanelFrame(GraphicalMonPanelFrame.PANEL_MODE_RESULT);
        fr.enable(true);
        File f = new File(i);
        List<File> files;
        if (f.isDirectory()) {
            files = FileUtil.listFiles(f, FileUtil.IMAGE_SUFFIXES, true);
            FileUtil.deleteFilesInPageFolder(files);
            Collections.sort(files);
        } else {
            files = new LinkedList<>();
            files.add(f);
        }
        String netPath = p.isEmpty() ? "/net_tf/LA73_249_0mod360.pb" : p;
        LayoutAnalysisURO_ML implOrig = new LayoutAnalysisURO_ML(netPath, null);
        CITlab_LA_ML impl = new CITlab_LA_ML(netPath, "");
        SuperPixelCalcML_BL net = new SuperPixelCalcML_BL(netPath, "");
        WrapperTF_BL_SEP wrapper = new WrapperTF_BL_SEP(netPath, "");
        for (File file : files) {
            HybridImage hi = HybridImage.newInstance(file);
            PcGtsType emptyPcGtsType = PageXmlUtils.createEmptyPcGtsType(file.getName(), hi.getWidth(), hi.getHeight());
            boolean process = implOrig.process(new Image(hi.getAsBufferedImage()), emptyPcGtsType, null, null);
            ILineClusterer.IRes process3 = impl.process(hi, null, true);
            ImageContainer container = new ImageContainer(hi.getAsOpenCVMatImage());
            ISuperPixel[] process1 = net.process(null, container);
            short[][][] update = wrapper.update(hi.getAsOpenCVMatImage());

            byte[][] b = new byte[hi.getHeight()][hi.getWidth()];
            for (ISuperPixel iSuperPixel : process1) {
                b[iSuperPixel.getY()][iSuperPixel.getX()] = -1;
//                System.out.println(iSuperPixel.getX() + "," + iSuperPixel.getY() + " " + iSuperPixel.getAngle());
            }
//            HybridImage hybridImage = HybridImage.newInstance();
            byte[][][] colored = new byte[Math.max(update.length, 2) + 1][][];
            colored[0] = hi.getAsByteImage().pixels[0];
//            Image img = new Image(file.toURI().toURL());
//            BufferedImage imageBufferedImage = img.getImageBufferedImage(true);
//            PcGtsType unmarshal = PageXmlUtil.unmarshal(PageXmlUtil.getXmlPath(file, true));
//            BufferedImage debugImage = ImageUtil.getDebugImage(imageBufferedImage, unmarshal, t ? 1.0 : -1.0, false, r, b, p, !c);
            fr.addImage(hi, file.getPath(), null, file.getPath());
            fr.addImage(HybridImage.newInstance(b), process1.length + " pixels", null, process1.length + " pixels");
            fr.addImage(Bla.debug, "skelet", null,"skelet" );
            for (int jj = 0; jj < update.length; jj++) {
                short[][] baseProb = update[jj];
                byte[][] floatImg = new byte[baseProb.length][baseProb[0].length];
                for (int i = 0; i < baseProb.length; i++) {
                    short[] fs = baseProb[i];
                    for (int j = 0; j < fs.length; j++) {
                        float ff = fs[j];
                        if (ff > t) {
                            floatImg[i][j] = (byte) Math.min(255, ff);
                        }
                    }
                }
                if (jj < 3) {
                    colored[jj + 1] = floatImg;
                }
                fr.addImage(HybridImage.newInstance(floatImg), "image " + jj, null, "image " + jj);
            }
            BufferedImage debugImage = ImageUtil.getDebugImage(hi.getAsBufferedImage(), emptyPcGtsType, -1, false, true, true, true, true);
            fr.addImage(HybridImage.newInstance(debugImage),"res" , null, "res");
            fr.addImage(HybridImage.newInstance(colored), "image together", null, "image together");
            fr.next();
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidParameterException, MalformedURLException {
        args = ("-i " + "/home/gundram/devel/projects/ICDAR_chinese/racetrack_ONB/data/val/104587_0018_3954045.tif -p LA_news_onb_190314_2019-03-15.pb").split(" ");
//        args=("-i "+HomeDir.getFile("tmp_20170308/xml_semi_0/")).split(" ");
        LAViewer instance = new LAViewer();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
