/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.la;

import de.planet.imaging.types.HybridImage;
import de.uros.citlab.module.TestFiles;
import de.uros.citlab.module.interfaces.IP2B;
import de.uros.citlab.module.util.PageXmlUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.types.Image;
import java.io.File;
import java.util.List;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author gundram
 */
public class BaselineGenerationTest {

    static PcGtsType xmlFile;
    static HybridImage imgFile;
    static Image imageFile;

    public BaselineGenerationTest() {
    }

    @BeforeClass
    public static void setUp() {
        List<File> testFiles = TestFiles.getTestFiles();
        Assert.assertEquals("expect only one test file", 1, testFiles.size());
        File img = testFiles.get(0);
        imgFile = HybridImage.newInstance(img, true);
        xmlFile = PageXmlUtil.unmarshal(PageXmlUtil.getXmlPath(img, true));
        imageFile = new Image(imgFile.getAsBufferedImage());
    }

    @AfterClass
    public static void tearDown() {
    }

    /**
     * Test of processNew method, of class BaselineGeneration.
     */
    @Test
    public void testProcessNew() {
        System.out.println("processNew");
        IP2B instance = new BaselineGenerationHist();
        instance.processImage(imageFile, xmlFile);
        List<TextRegionType> textRegions = PageXmlUtils.getTextRegions(xmlFile);
        for (TextRegionType textRegion : textRegions) {
            instance.processRegion(imgFile, textRegion);
            for (TextLineType textLineType : textRegion.getTextLine()) {
                instance.processLine(imgFile, textLineType, textRegion);
            }
        }
//        BufferedImage debugImage = ImageUtil.getDebugImage(imgFile.getAsBufferedImage(), xmlFile, 1.0, false, true, true, true, false);
//        HybridImage.newInstance(debugImage).save(TestFiles.getPrefix() + "out.jpg");
    }

    /**
     * Test of testProcessOld method, of class BaselineGeneration.
     */
    @Test
    public void testProcessOld() {
        System.out.println("testProcessOld");
        IP2B instance = new BaselineGeneration();
        instance.processImage(imageFile, xmlFile);
        List<TextRegionType> textRegions = PageXmlUtils.getTextRegions(xmlFile);
        for (TextRegionType textRegion : textRegions) {
            instance.processRegion(imgFile, textRegion);
            for (TextLineType textLineType : textRegion.getTextLine()) {
                instance.processLine(imgFile, textLineType, textRegion);
            }
        }
    }
}
