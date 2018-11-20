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
package de.uros.citlab.module;

import com.achteck.misc.log.Logger;
import de.uros.citlab.errorrate.costcalculator.CostCalculatorDft;
import de.uros.citlab.errorrate.htr.ErrorModuleDynProg;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.module.baseline2polygon.B2PSeamMultiOriented;
import de.uros.citlab.module.baseline2polygon.Baseline2PolygonParser;
import de.uros.citlab.module.htr.HTRParser;
import de.uros.citlab.module.la.LayoutAnalysisParser;
import de.uros.citlab.module.la.LayoutAnalysisURO_ML;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.util.PropertyUtil;
import de.uros.citlab.tokenizer.categorizer.CategorizerCharacterDft;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.IBaseline2Polygon;
import eu.transkribus.interfaces.IHtr;
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
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TestWorkflow {

    public static Logger LOG = Logger.getLogger(TestWorkflow.class.getName());

    Random r = new Random(1234);
    private static File testFolder = new File(TestFiles.getPrefix(), "test_workflow_tmp/");

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
        ILayoutAnalysis laParser2 = new LayoutAnalysisURO_ML(null);
        IHtr htrParser = new HTRParser();
        IBaseline2Polygon b2pParser = new Baseline2PolygonParser(B2PSeamMultiOriented.class.getName());
        for (File testImgFile : TestFiles.getTestFiles()) {
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
            File testXmlFile_LA_B2P = null;
            File testXmlFile_LA_HTR = null;
            File testXmlFile_LA_B2P_HTR = null;
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
                    Assert.fail("applying Layout Analysis to image results an error: " + e.getMessage());
                }
            } catch (IOException ex) {
                Assert.fail("creating test szenario did not work: (copy file for LA) " + ex.getMessage());
            }
            //TEST LA ADVANCED
            try {
                testXmlFile_LA = new File(testFolder, testFileImgBase + "_la2.xml");
                FileUtils.copyFile(testXmlFile, testXmlFile_LA);
                try {
                    laParser2.process(image, testXmlFile_LA.getAbsolutePath(), null, null);
                } catch (Throwable e) {
                    Assert.fail("applying Layout Analysis to image results an error: " + e.getMessage());
                }
            } catch (IOException ex) {
                Assert.fail("creating test szenario did not work: (copy file for LA) " + ex.getMessage());
            }
            //TEST B2P
            try {
                testXmlFile_LA_B2P = new File(testFolder, testFileImgBase + "_la_b2p.xml");
                FileUtils.copyFile(testXmlFile_LA, testXmlFile_LA_B2P);
                try {
                    b2pParser.process(image, testXmlFile_LA_B2P.getAbsolutePath(), null, null);
                } catch (Throwable e) {
                    Assert.fail("applying Layout Analysis to image results an error: " + e.getMessage());
                }
            } catch (IOException ex) {
                Assert.fail("creating test szenario did not work: (copy file for LA) " + ex.getMessage());
            }
            if (TestFiles.skipLargeTests()) {
                return;
            }
            File htr = TestFiles.getHtrDft();
            try {
                testXmlFile_LA_HTR = new File(testFolder, testFileImgBase + "_la_htr.xml");
                testXmlFile_LA_B2P_HTR = new File(testFolder, testFileImgBase + "_la_b2p_htr.xml");
                FileUtils.copyFile(testXmlFile_LA, testXmlFile_LA_HTR);
                FileUtils.copyFile(testXmlFile_LA_B2P, testXmlFile_LA_B2P_HTR);
                try {
                    htrParser.process(htr.getAbsolutePath(),
                            null,
                            null,
                            image,
                            testXmlFile_LA_HTR.getAbsolutePath(),
                            new File(testFolder, "confmats").getAbsolutePath(),
                            null,
                            PropertyUtil.setProperty(null, "raw", "true"));
                    htrParser.process(htr.getAbsolutePath(),
                            null,
                            null,
                            image,
                            testXmlFile_LA_B2P_HTR.getAbsolutePath(),
                            new File(testFolder, "confmats_b2p").getAbsolutePath(),
                            null,
                            PropertyUtil.setProperty(null, "raw", "true"));
                } catch (Throwable e) {
                    Assert.fail("applying HTR to image results an error: " + e.getMessage());
                }
            } catch (IOException ex) {
                Assert.fail("creating test szenario did not work: (copy file for LA) " + ex.getMessage());
            }
            //checking quality between direct Polygons and Baseline->Poly-Alg ==> quality should not be too bad
            String recoRef = PageXmlUtils.getFulltextFromLines(PageXmlUtil.unmarshal(PageXmlUtil.getXmlPath(testImgFile)));
            double corRate, corRateB2P;
            {
                String recoLaHtr = PageXmlUtils.getFulltextFromLines(PageXmlUtil.unmarshal(testXmlFile_LA_HTR));
                ErrorModuleDynProg errorModule = new ErrorModuleDynProg(new CostCalculatorDft(), new CategorizerCharacterDft(), null, Boolean.FALSE);
                errorModule.calculate(recoLaHtr, recoRef);
                Map<Count, Long> map = errorModule.getCounter().getMap();
                long gt = map.get(Count.GT);
                long cor = map.get(Count.COR);
                corRate = ((double) cor) / gt * 100;
//                    System.out.println(map);
                System.out.println(String.format("correct without B2P = %.2f%s", corRate, "%"));
                Assert.assertEquals("correct rate is too bad without b2p", 100.0, corRate, 56.0);
            }
            {
                String recoLaB2PHtr = PageXmlUtils.getFulltextFromLines(PageXmlUtil.unmarshal(testXmlFile_LA_B2P_HTR));
                ErrorModuleDynProg errorModule = new ErrorModuleDynProg(new CostCalculatorDft(), new CategorizerCharacterDft(), null, Boolean.FALSE);
                errorModule.calculate(recoLaB2PHtr, recoRef);
                Map<Count, Long> map = errorModule.getCounter().getMap();
                long gt = map.get(Count.GT);
                long cor = map.get(Count.COR);
                corRateB2P = ((double) cor) / gt * 100;
//                    System.out.println(map);
                System.out.println(String.format("correct with B2P = %.2f%s", corRateB2P, "%"));
                Assert.assertEquals("correct rate is too bad with b2p", 100.0, corRate, 60.0);
            }
//            Assert.assertEquals("htr should benefit from B2P", corRate + Math.abs(corRate - corRateB2P), corRateB2P, Math.abs(corRate - corRateB2P));
        }
//        }
    }

}
