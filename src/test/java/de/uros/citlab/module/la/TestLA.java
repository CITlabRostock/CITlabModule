/*
 * Copyright 2015 Sharmarke Aden.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uros.citlab.module.la;

import com.achteck.misc.log.Logger;
import de.planet.imaging.types.HybridImage;
import de.uros.citlab.module.TestFiles;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.ILayoutAnalysis;
import eu.transkribus.interfaces.types.Image;
import eu.transkribus.interfaces.types.util.ImageUtils;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class TestLA {

    public static Logger LOG = Logger.getLogger(TestLA.class.getName());

    Random r = new Random(1234);
    private static File testFolder = new File(TestFiles.getPrefix(), "test_la_tmp/");

    @BeforeClass
    public static void setUpClass() {
        testFolder.mkdirs();
    }

    @AfterClass
    public static void tearDownClass() {
        FileUtils.deleteQuietly(testFolder);
    }

    @Test
    public void testWorkflow() {
        System.out.println("testWorkflow");
//        ILayoutAnalysis laParser = new LayoutAnalysisParser("src/main/resources/planet/la/20160125_historical_fulltext.bin", null);
        List<File> layoutAnalysis = TestFiles.getLayoutAnalysis();
        Assert.assertEquals("number of la-modules should be one", 1, layoutAnalysis.size());
        ILayoutAnalysis laParser = new LayoutAnalysisParser(layoutAnalysis.get(0).getAbsolutePath(), null);
//        List<File> asList = Arrays.asList(new File(TestFiles.getPrefix(), "/test_la/No-MM_N1116-00-01v.jpg"), new File(TestFiles.getPrefix(), "/test_la/No-MM_N2933-00-02v.jpg"));
        List<File> asList = Arrays.asList(new File(TestFiles.getPrefix(), "/test_la/No-MM_N2933-00-02v.jpg"));
        for (File testImgFile : asList) {
            //SETUP
            String testFileImg = testImgFile.getName();
            String testFileImgBase = testFileImg.substring(0, testFileImg.lastIndexOf("."));
            Image image = null;
            try {
                image = new Image(ImageUtils.convertToBufferedImage(testImgFile.toURI().toURL()));
            } catch (IOException ex) {
                LOG.log(Logger.ERROR, ex);
                Assert.fail("creating test szenario did not work: (load image) " + ex.getMessage());
            }
            File testXmlFile = null;
            File testXmlFile_LA = null;
            PcGtsType createEmptyPcGtsType = null;
//            try {
            //                System.out.println("testname = '" + testFileImg + "'");
//                System.out.println("width = '" + image.getImageBufferedImage(true).getWidth() + "'");
//                System.out.println("width = '" + image.getImageBufferedImage(true).getHeight() + "'");
            createEmptyPcGtsType = PageXmlUtils.createEmptyPcGtsType(testFileImg, image.getImageBufferedImage(true).getWidth(), image.getImageBufferedImage(true).getHeight());
//            } catch (IOException ex) {
//                Assert.fail("creating test szenario did not work: (create empty xml-file) " + ex.toString());
//            }
            testXmlFile = new File(testFolder, testFileImgBase + "_empty.xml");
            try {
//                System.out.println("try to save to file '" + testXmlFile + "'");
                PageXmlUtils.marshalToFile(createEmptyPcGtsType, testXmlFile);
            } catch (IOException | JAXBException ex) {
                ex.printStackTrace();
                LOG.log(com.achteck.misc.log.Logger.ERROR, ex);
                Assert.fail("creating test szenario did not work: (save empty xml-file) " + ex.toString());
            }
            //TEST LA
            try {
                testXmlFile_LA = new File(testFolder, testFileImgBase + "_la.xml");
                FileUtils.copyFile(testXmlFile, testXmlFile_LA);
                try {
                    laParser.process(image, testXmlFile_LA.getAbsolutePath(), null, null);
                } catch (Throwable e) {
                    LOG.log(Logger.ERROR, "error:", e);
                    Assert.fail("applying Layout Analysis to image results an error: " + e.getMessage());
                }
                HybridImage.newInstance(ImageUtil.getDebugImage(
                        image.getImageBufferedImage(true),
                        PageXmlUtil.unmarshal(testXmlFile_LA),
                        1.0, false, true, true, true, true)).save(new File(testFolder, "debug.jpg").getPath());
            } catch (IOException ex) {
                Assert.fail("creating test szenario did not work: (copy file for LA) " + ex.getMessage());
            }
        }
//        }
    }

}
