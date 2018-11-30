/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.uros.citlab.module.baseline2polygon.B2PSeamMultiOriented;
import de.uros.citlab.module.baseline2polygon.Baseline2PolygonParser;
import de.uros.citlab.module.htr.HTRParser;
import de.uros.citlab.module.la.LayoutAnalysisParser;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.util.PropertyUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.IBaseline2Polygon;
import eu.transkribus.interfaces.IHtr;
import eu.transkribus.interfaces.ILayoutAnalysis;
import eu.transkribus.interfaces.types.Image;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

/**
 *
 * @author gundram
 */
public class Apply2Folder extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(Apply2Folder.class.getName());

    private File htrName;
    private File lrName;
    private IHtr htr;
    private ILayoutAnalysis la;
    private IBaseline2Polygon b2p;
    private File folderIn;
    private File folderOut;
    private boolean createDebug;
    private boolean saveCM;
    private int threads = 1;

    public Apply2Folder(File htr, File lr, File la, File folderIn, File folderOut, boolean createDebugImg, boolean saveCM) {
        this(htr, lr, la, folderIn, folderOut, B2PSeamMultiOriented.class.getName(), createDebugImg, saveCM);
    }

    public Apply2Folder(File htr, File lr, File la, File folderIn, File folderOut, String b2pClassName, boolean createDebugImg, boolean saveCM) {
        this.htr = new HTRParser();
        htrName = htr;
        lrName = lr;
        b2p = b2pClassName != null && !b2pClassName.isEmpty() ? new Baseline2PolygonParser(b2pClassName) : null;
        if (la != null) {
            this.la = new LayoutAnalysisParser(la.getAbsolutePath(), null);
        }
        this.folderIn = folderIn;
        this.folderOut = folderOut;
        this.createDebug = createDebugImg;
        this.saveCM = saveCM;
    }

    public void run() throws MalformedURLException, IOException, JAXBException {
        Collection<File> listFiles = FileUtil.listFiles(folderIn, FileUtil.IMAGE_SUFFIXES, true);
        FileUtil.deleteFilesInPageFolder(listFiles);
        folderOut.mkdirs();
        int i = 1;
        for (File srcImg : listFiles) {
            LOG.log(Logger.INFO, "process image " + i++ + "/" + listFiles.size());
            Path pathFull = Paths.get(srcImg.getAbsolutePath());
            Path pathPart = Paths.get(folderIn.getAbsolutePath());
            Path p = pathPart.relativize(pathFull);
            File tgtImg = new File(folderOut, p.toString());
            tgtImg.getParentFile().mkdirs();
            if (!srcImg.equals(tgtImg)) {
                FileUtils.copyFile(srcImg, tgtImg);
            }
            Image img = new Image(tgtImg.toURI().toURL());

//            File parent = outImg.getParentFile();
            File srcXml = PageXmlUtil.getXmlPath(srcImg, false);
            File tgtXml = PageXmlUtil.getXmlPath(tgtImg, false);
//            srcXml.getParentFile().mkdirs();
            if (srcXml.exists()) {
                if (!tgtXml.equals(srcXml)) {
                    FileUtils.copyFile(srcXml, tgtXml);
                }
            } else {
                PcGtsType createEmptyPcGtsType = PageXmlUtils.createEmptyPcGtsType(tgtImg.getName(), img.getImageBufferedImage(true).getWidth(), img.getImageBufferedImage(true).getHeight());
                tgtXml.getParentFile().mkdirs();
                PageXmlUtils.marshalToFile(createEmptyPcGtsType, tgtXml);
            }
            if (la != null) {
                la.process(img, tgtXml.getAbsolutePath(), null, PropertyUtil.setProperty(null, Key.DELETE, "true"));
            }
            if (b2p != null) {
                b2p.process(img, tgtXml.getAbsolutePath(), null, null);
            }
            if (htrName != null) {
                String storageDir = null;
                if (saveCM) {
                    File parent = tgtXml.getParentFile().getParentFile();
                    File storage = new File(parent, "storage" + File.separator + tgtImg.getName());
                    storage.mkdirs();
                    storageDir = storage.getPath();
                }
                htr.process(htrName.getAbsolutePath(), lrName == null ? null : lrName.getAbsolutePath(), null, img, tgtXml.getAbsolutePath(), storageDir, null, null);
            }
            if (createDebug) {
                BufferedImage imageBufferedImage = img.getImageBufferedImage(true);
//                ImageUtil.printPolygons(imageBufferedImage, PageXmlUtils.unmarshal(tgtXml), false, true, true);
                BufferedImage debugImage1 = ImageUtil.getDebugImage(imageBufferedImage, PageXmlUtils.unmarshal(tgtXml), 0, false, false, true, true, true);
                ImageIO.write(debugImage1, "jpg", new File(tgtXml.getAbsoluteFile() + ".jpg"));
                if (htrName != null) {
                    BufferedImage debugImage = ImageUtil.getDebugImage(imageBufferedImage, PageXmlUtils.unmarshal(tgtXml), 1.0, false, false, true, true, true);
                    ImageIO.write(debugImage, "jpg", new File(tgtXml.getAbsoluteFile() + "_txt.jpg"));
                }
            }
            if (img.hasType(Image.Type.OPEN_CV)) {
                img.getImageOpenCVImage().release();
            }
        }
    }

    public static void main(String[] args) throws InvalidParameterException, MalformedURLException, IOException, JAXBException {
        File folder = null, folderOut = null, htr = null, lr = null, la = null;
//        String dir = /*"23737";//"25126"*/ "TEST_CITlab_Konzilsprotokolle_M4";
        folder = HomeDir.getFile("data/sets_orig");//folder containing images and PAGE-XML with baselines
        folderOut = HomeDir.getFile("data/sets_b2p"); //folder where recognition should be saved
//        htr = HomeDir.getFile("netsUIBK/Konzielsprotokolle_M4.sprnn");
//        lr = HomeDir.getFile("dicts/transkribus/Konzilsprotokolle_v1.dict");//path to htr model
//        la = HomeDir.getFile("configs/historical_90_dft_20161011.bin"); //path to la - if no la is given, the PAGE-XML should have baselines.
        Apply2Folder instance = new Apply2Folder(htr, lr, la, folder, folderOut, true, true);
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
