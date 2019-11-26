package de.uros.citlab.module.text2image;

import de.planet.imaging.types.HybridImage;
import de.uros.citlab.module.TestFiles;
import de.uros.citlab.module.la.LayoutAnalysisURO_ML;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.util.PropertyUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextEquivType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.types.Image;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Text2ImageParserTest {

    static File imgFile = TestFiles.getTestFiles().get(0);
    static File xmlFile = null;
    static File xmlFileCopy = null;
    static File outFolder = null;
    static File txtFile = null;

    static File imgFileXML = null;
    static File xmlFileCopyXML = null;

    @BeforeClass
    public static void setUp() throws Exception {
        TemporaryFolder folder = new TemporaryFolder();
        folder.create();
        outFolder = folder.newFolder("outTxt");
        xmlFile = PageXmlUtil.getXmlPath(imgFile, true);
        PcGtsType unmarshal = PageXmlUtil.unmarshal(xmlFile);
        List<String> textLines = getText(unmarshal);
        txtFile = new File(outFolder, imgFile.getName().replace(".xml", ".txt"));
        xmlFileCopy = new File(outFolder, xmlFile.getName());
        if (txtFile.exists()) {
            throw new RuntimeException("file " + txtFile.getAbsolutePath() + " is written two times.");
        }
        FileUtil.copyFile(xmlFile, xmlFileCopy);
        FileUtil.writeLines(txtFile, textLines, false);

        File folder_t2i = new File(TestFiles.getPrefix(), "test_t2i");
        imgFileXML = new File(folder_t2i, "image1.jpg");
        File xml = PageXmlUtil.getXmlPath(imgFileXML, true);
        File outFolderXML = folder.newFolder("outXML");
        xmlFileCopyXML = new File(outFolderXML, xml.getName());
        FileUtil.copyFile(xml, xmlFileCopyXML);
    }

    public static List<String> getText(PcGtsType page) {
        List<String> res = new LinkedList<>();
        List<TextRegionType> trt = PageXmlUtils.getTextRegions(page);
        for (TextRegionType textRegionType : trt) {
            TextEquivType textEquiv1 = textRegionType.getTextEquiv();
            if (textEquiv1 == null) {
                continue;
            }
            String[] split = textEquiv1.getUnicode().split("\n");
            for (String s : split) {
                s = s.trim();
                if (!s.isEmpty()) {
                    res.add(s.trim());
                }
            }
        }
        return res;
    }

    //    @Test
    public void testBidiHtrPlus() throws IOException, JAXBException {
        Text2ImageParser parser = new Text2ImageParser();
        String[] props = PropertyUtil.setProperty(null, Key.T2I_SKIP_BASELINE, 0.4);
        props = PropertyUtil.setProperty(props, Key.T2I_JUMP_BASELINE, 0.0);
        props = PropertyUtil.setProperty(props, Key.T2I_SKIP_WORD, 4.0);
        props = PropertyUtil.setProperty(props, Key.T2I_THRESH, 0.05);
        File folderTest = new File(TestFiles.getPrefix(), "test_bidi" + File.separator + "realtest");
        File folderImages = new File(folderTest, "t2itest-beforeUpload");
        File fileImage = new File(folderImages, "700106909.jpg");
//        props = PropertyUtil.setProperty(props, Key.DEBUG_DIR, new File(fileImage, "debug"));
        props = PropertyUtil.setProperty(props, Key.DEBUG, true);
//        {
//            Apply2Folder_ML la = new Apply2Folder_ML("", "", "",
//                    folderImages.getPath(),
//                    folderImages.getPath(),
//                    B2PSeamMultiOriented.class.getName(),
//                    true, false, null);
//            la.setParamSet(la.getDefaultParamSet(new ParamSet()));
//            la.init();
//            la.run();
//        }
        File file = new File(folderImages, "page" + File.separator + "700106909.xml");
        File fileOut = new File(file.getParentFile(), file.getName().replace(".xml", "_t2i.xml"));
        FileUtil.copyFile(file, fileOut);
        parser.matchCollection(
                new File(folderTest, "HTR_13304_Ephi").getAbsolutePath(),
                null,
                null,
                new File(folderImages, "txt" + File.separator + "700106909.txt").getAbsolutePath(),
                new String[]{
                        new File(folderImages, "700106909.jpg").getAbsolutePath(),
                },
                new String[]{
                        fileOut.getAbsolutePath()
                },
                props);
    }


    @Test
    public void matchCollection() {
        Text2ImageParser parser = new Text2ImageParser();
        String[] props = PropertyUtil.setProperty(null, Key.T2I_SKIP_BASELINE, 1.0);
        props = PropertyUtil.setProperty(props, Key.T2I_SKIP_WORD, 10.0);
        props = PropertyUtil.setProperty(props, Key.T2I_THRESH, 0.0);
        parser.matchCollection(
                TestFiles.getHtrDft().getAbsolutePath(),
                null,
                null,
                txtFile.getAbsolutePath(),
                new String[]{imgFile.getAbsolutePath()},
                new String[]{xmlFileCopy.getAbsolutePath()},
                props);

    }

    @Test
    public void matchCollectionFromPageXML() {
        String[] propLA = PropertyUtil.setProperty(null, Key.LA_DELETESCHEME, LayoutAnalysisURO_ML.DEL_REGIONS);
        LayoutAnalysisURO_ML parserLA = new LayoutAnalysisURO_ML(null);
        Image image = new Image(HybridImage.newInstance(imgFileXML).getAsBufferedImage());
        parserLA.process(image, xmlFileCopyXML.getAbsolutePath(), null, propLA);
        parserLA.process(image, xmlFileCopyXML.getAbsolutePath(), null, null);
        Text2ImageParser parser = new Text2ImageParser();
        String[] props = PropertyUtil.setProperty(null, Key.T2I_SKIP_BASELINE, 1.0);
        props = PropertyUtil.setProperty(props, Key.T2I_SKIP_WORD, 10.0);
        props = PropertyUtil.setProperty(props, Key.T2I_THRESH, 0.0);
        props = PropertyUtil.setProperty(props, Key.T2I_IGNORE_LB, true);
        parser.matchCollection(
                TestFiles.getHtrDft().getAbsolutePath(),
                null,
                null,
                null,
                new String[]{imgFileXML.getAbsolutePath()},
                new String[]{xmlFileCopyXML.getAbsolutePath()},
                props);
        propLA=null;

    }

}
