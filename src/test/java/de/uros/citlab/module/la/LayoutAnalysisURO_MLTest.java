package de.uros.citlab.module.la;

import de.planet.imaging.types.HybridImage;
import de.uros.citlab.module.TestFiles;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.*;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextRegionType;
import eu.transkribus.interfaces.types.Image;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.util.List;

public class LayoutAnalysisURO_MLTest {
    static File folder = new File(TestFiles.getPrefix(), "test_la_ml");
    static File fileImage = new File(folder, "00000052.jpg");
    static File fileXML = new File(folder, "page/00000052.xml");
    static boolean createDebugImages = false;

    @BeforeClass
    public static void setUp() {
//        FileUtil.copyFile(fileXML, fileXMLTmp);
    }

    @AfterClass
    public static void tearDown() {
//        fileXMLTmp.delete();
        if (!createDebugImages) {
            for (File file : FileUtil.listFiles(folder, "png xml".split(" "), false)) {
                file.delete();
            }
        }
    }

    @Test
    public void process() throws MalformedURLException {
        LayoutAnalysisURO_ML la = new LayoutAnalysisURO_ML(null);
        Image img = new Image(fileImage.toURL());
        String[] schemes = new String[]{LayoutAnalysisURO_ML.DEL_REGIONS, null, LayoutAnalysisURO_ML.DEL_LINES};
        for (String scheme : schemes) {
            PcGtsType unmarshal = PageXmlUtil.unmarshal(fileXML);
            if (createDebugImages) {
                BufferedImage debugImage = ImageUtil.getDebugImage(
                        img.getImageBufferedImage(true),
                        unmarshal,
                        -1,
                        false, true,
                        true, true, true);
                HybridImage.newInstance(debugImage).save(new File(folder, "debug_orig.png").getAbsolutePath());
            }
            String[] strings = PropertyUtil.setProperty(null, Key.LA_ROTSCHEME, LayoutAnalysisURO_ML.ROT_HETRO);
            strings = PropertyUtil.setProperty(strings, Key.LA_DELETESCHEME, scheme);
            int sizeBefore = unmarshal.getPage().getTextRegionOrImageRegionOrLineDrawingRegion().size();
            boolean process = la.process(
                    img,
                    unmarshal,
                    null,
                    strings
            );
            if (createDebugImages) {
                BufferedImage debugImage = ImageUtil.getDebugImage(
                        img.getImageBufferedImage(true),
                        unmarshal,
                        -1,
                        false, true,
                        true, true, true);
                HybridImage.newInstance(debugImage).save(new File(folder, "debug_" + scheme + ".png").getAbsolutePath());
            }
            List<TrpRegionType> regions = unmarshal.getPage().getTextRegionOrImageRegionOrLineDrawingRegion();
            if (!LayoutAnalysisURO_ML.DEL_REGIONS.equals(scheme)) {
                Assert.assertEquals("number of regions should not change with scheme " + scheme, sizeBefore, regions.size());
            }
            int height = img.getImageBufferedImage(true).getHeight();
            int width = img.getImageBufferedImage(true).getWidth();
            for (TrpRegionType region : regions) {
                TrpTextRegionType textRegion = (TrpTextRegionType) region;
                Float orientation = textRegion.getOrientation();
//                System.out.println("id = " + textRegion.getId() + " orientation = " + orientation);
                if (Math.abs(orientation.doubleValue()) > 0.01) {
                    List<TrpTextLineType> trpTextLine = textRegion.getTrpTextLine();
                    for (TrpTextLineType trpTextLineType : trpTextLine) {
//                        System.out.println("id = " + trpTextLineType.getId() + " baseline = " + trpTextLineType.getBaseline().getPoints());
                        Polygon baseline = PolygonUtil.getBaseline(trpTextLineType);
                        for (int i = 0; i < baseline.npoints; i++) {
                            int y = baseline.ypoints[i];
                            int x = baseline.xpoints[i];
                            Assert.assertTrue("x value out of [0, " + (width - 1) + "] (" + x + ") with deletion scheme = " + scheme, x >= 0 && x < width);
                            Assert.assertTrue("y value out of [0, " + (height - 1) + "] (" + y + ") with deletion scheme = " + scheme, y >= 0 && y < height);
                        }
                    }

                }
            }
        }


    }
}
