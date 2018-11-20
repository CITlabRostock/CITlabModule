package de.uros.citlab.module;

import java.io.File;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

import de.uros.citlab.module.baseline2polygon.B2PSeamMultiOriented;
import de.uros.citlab.module.baseline2polygon.Baseline2PolygonParser;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import eu.transkribus.interfaces.IBaseline2Polygon;
import eu.transkribus.interfaces.types.Image;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class TestLoadOpenCV {

    final static File imgFile = new File(TestFiles.getPrefix(), "test_load_opencv/example.png");
    final static File xmlFolderTmp = new File(TestFiles.getPrefix(), "test_load_opencv_tmp");
    static File xmlFileTmp;

    @BeforeClass
    public static void createXML() {
        File xmlFile = PageXmlUtil.getXmlPath(imgFile);
        xmlFileTmp = FileUtil.getTgtFile(imgFile.getParentFile(), xmlFolderTmp, xmlFile);
        xmlFolderTmp.mkdirs();
        xmlFileTmp.getParentFile().mkdirs();
        FileUtil.copyFile(xmlFile, xmlFileTmp);
    }

    @AfterClass
    public static void deleteXML() {
        if (xmlFolderTmp != null) {
            FileUtils.deleteQuietly(xmlFolderTmp);
        }
    }

    @Test
    public void testB2P_CV_64FC2() throws MalformedURLException {
        IBaseline2Polygon laParser = new Baseline2PolygonParser(B2PSeamMultiOriented.class.getName());

        Image img = new Image(imgFile.toURI().toURL());

        try {

            laParser.process(img, xmlFileTmp.getAbsolutePath(), null, null);
        } catch (Throwable t) {
            Logger.getLogger(TestLoadOpenCV.class.getName()).log(Level.SEVERE, null, t);
            Assert.fail(t.getMessage());
        }
    }
}
