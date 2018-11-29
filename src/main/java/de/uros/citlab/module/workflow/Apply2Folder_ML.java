/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.uros.citlab.module.baseline2polygon.B2PSeamMultiOriented;
import de.uros.citlab.module.baseline2polygon.Baseline2PolygonParser;
import de.uros.citlab.module.htr.HTRParser;
import de.uros.citlab.module.la.LayoutAnalysisURO_ML;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.IBaseline2Polygon;
import eu.transkribus.interfaces.IHtr;
import eu.transkribus.interfaces.ILayoutAnalysis;
import eu.transkribus.interfaces.types.Image;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.NotLinkException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

/**
 *
 * @author gundram
 */
public class Apply2Folder_ML extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(Apply2Folder_ML.class.getName());

    @ParamAnnotation(descr = "path to htr (empty if no htr should be proceeded")
    private String htr_path = "";
    private IHtr htr;

    @ParamAnnotation(descr = "path to language resource (empty if no lr should be used")
    private String lr_path = "";

    @ParamAnnotation(descr = "path to la (empty if default la should be used")
    private String la_path = "";
    private ILayoutAnalysis la;

    @ParamAnnotation(descr = "classname of baseline2polygon")
    private String b2p = "";
    private IBaseline2Polygon b2pInstance;

    @ParamAnnotation(descr = "folder of input")
    private String xml_in = "";

    @ParamAnnotation(descr = "folder of ouput")
    private String xml_out = "";

    @ParamAnnotation(descr = "save debug images to output page folder")
    private boolean debug;

    @ParamAnnotation(descr = "if htr is set, save cm-storage")
    private boolean saveCM;

    @ParamAnnotation(descr = "link image file instead of copy")
    private boolean link = true;
//    private int threads = 1;
    private String[] props;

    public Apply2Folder_ML(String htr, String lr, String la, String folderIn, String folderOut, boolean createDebugImg, boolean saveCM, String[] props) {
        this(htr, lr, la, folderIn, folderOut, null, createDebugImg, saveCM, props);
    }

    public Apply2Folder_ML(String htr, String lr, String la, String folderIn, String folderOut, String b2pClassName, boolean createDebugImg, boolean saveCM, String[] props) {
        this.htr = new HTRParser();
        htr_path = htr;
        lr_path = lr;
        b2p = b2pClassName;
//            String[] props = PropertyUtil.setProperty(null, Key.LA_SINGLECORE, "false");
//            props = PropertyUtil.setProperty(props, Key.LA_ROTSCHEME, "default");
        if (la != null) {
            if (la.isEmpty()) {
                LOG.info("use default layout analysis");
            }
            this.la = new LayoutAnalysisURO_ML(la.isEmpty() ? null : la, props);
        }
        xml_in = folderIn;
        xml_out = folderOut;
        this.debug = createDebugImg;
        this.saveCM = saveCM;
        this.props = props;
        addReflection(this, Apply2Folder_ML.class);
    }

    @Override
    public void init() {
        super.init(); //To change body of generated methods, choose Tools | Templates.
        b2pInstance = b2p != null && !b2p.isEmpty() ? new Baseline2PolygonParser(b2p) : null;
        if (xml_in == null || xml_in.isEmpty()) {
            throw new RuntimeException("xml_in is null or empty.");
        }
        if (xml_out == null || xml_out.isEmpty()) {
            throw new RuntimeException("xml_out is null or empty.");
        }
    }

    public void run() throws MalformedURLException, IOException, JAXBException {
        File lrName = lr_path.isEmpty() ? null : new File(lr_path);
        File folderOut = xml_out.isEmpty() ? null : new File(xml_out);;
        File htrName = htr_path.isEmpty() ? null : new File(htr_path);
        Collection<File> listFiles = FileUtil.getFilesListsOrFolders(xml_in, FileUtil.IMAGE_SUFFIXES, true);
        File folderIn = FileUtil.getSourceFolderListsOrFolders(xml_in, FileUtil.IMAGE_SUFFIXES, true);
        FileUtil.deleteFilesInPageFolder(listFiles);
        folderOut.mkdirs();
        int i = 1;
        for (File srcImg : listFiles) {
            LOG.info("process image {} / {} [{}]", i++, listFiles.size(), srcImg);
            Path pathFull = Paths.get(srcImg.getAbsolutePath());
            Path pathPart = Paths.get(folderIn.getAbsolutePath());
            Path p = pathPart.relativize(pathFull);
            File tgtImg = new File(folderOut, p.toString());
            tgtImg.getParentFile().mkdirs();
            if (!srcImg.equals(tgtImg)) {
                if (link) {
                    FileUtils.deleteQuietly(tgtImg);
                    Path toPath = srcImg.getAbsoluteFile().toPath();
                    try {
                        while (true) {
                            toPath = Files.readSymbolicLink(toPath);
                        }
                    } catch (NotLinkException ex) {
                    }
                    Files.createSymbolicLink(tgtImg.toPath(), srcImg.getAbsoluteFile().toPath());
                } else {
                    FileUtils.copyFile(srcImg, tgtImg);
                }
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
//                String[] props = PropertyUtil.setProperty(null, Key.LA_DELETESCHEME, "default");
//                props = PropertyUtil.setProperty(props, Key.LA_SEPSCHEME, "default");
                la.process(img, tgtXml.getAbsolutePath(), null, props);
//                la.process(img, tgtXml.getAbsolutePath(), null, null);
            }
            if (b2pInstance != null) {
                b2pInstance.process(img, tgtXml.getAbsolutePath(), null, null);
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
            if (debug) {
                BufferedImage imageBufferedImage = img.getImageBufferedImage(true);
//                ImageUtil.printPolygons(imageBufferedImage, PageXmlUtils.unmarshal(tgtXml), false, true, true);
                BufferedImage debugImage1 = ImageUtil.getDebugImage(imageBufferedImage, PageXmlUtils.unmarshal(tgtXml), 0, false, true, true, true, true);
                LOG.debug("create debug images in folder {}", tgtXml.getParent());
                ImageIO.write(debugImage1, "jpg", new File(tgtXml.getAbsoluteFile() + ".jpg"));
                if (htrName != null) {
                    BufferedImage debugImage = ImageUtil.getDebugImage(imageBufferedImage, PageXmlUtils.unmarshal(tgtXml), 1.0, false, false, true, true, true);
                    ImageIO.write(debugImage, "jpg", new File(tgtXml.getAbsoluteFile() + "_txt.jpg"));
                }
            }
            if(img.hasType(Image.Type.OPEN_CV)){
                img.getImageOpenCVImage().release();
            }
        }
    }
    
    public static void main(String[] args) throws InvalidParameterException, MalformedURLException, IOException, JAXBException {
        ArgumentLine al = new ArgumentLine();
        al.addArgument("xml_in", HomeDir.getFile("data/002/la"));
        al.addArgument("xml_out", HomeDir.getFile("data/002/la"));
        al.addArgument("b2p", B2PSeamMultiOriented.class.getName());
//        al.setHelp();
        args=al.getArgs();

//        String folder = "/home/tobias/devel/projects/CitlabModule/raw4/", folderOut = "/home/tobias/devel/projects/CitlabModule/out/", htr = "", lr = "", la = "", b2p = "";
        String folder = "", folderOut = "", htr = "", lr = "", la = "", b2p = "";
        String[] props = null;
//        props = PropertyUtil.setProperty(props, Key.LA_DELETESCHEME, LayoutAnalysisURO_ML.DEL_REGIONS);
//        props = PropertyUtil.setProperty(props, Key.LA_ROTSCHEME, LayoutAnalysisURO_ML.ROT_HOM);
//        props = PropertyUtil.setProperty(props, Key.LA_SEPSCHEME, LayoutAnalysisURO_ML.SEP_NEVER);
        Apply2Folder_ML instance = new Apply2Folder_ML(htr, lr, la, folder, folderOut, b2p, false, false, props);
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.ACCEPT); // be strict, don't accept generic parameter
        String[] remainingArgumentList = ps.getRemainingArgumentList();
        if (remainingArgumentList != null && remainingArgumentList.length > 0) {
            LOG.warn("parameters {} are used for properties", Arrays.asList(remainingArgumentList));
        }
//        System.out.println(ps);
//        System.out.println(Arrays.toString(remainingArgumentList));
        props = ArgumentLine.getPropertiesFromArgs(remainingArgumentList, props);
        LOG.info("set properties {}", Arrays.toString(props)); //        System.out.println("==>" + Arrays.toString(props));
        instance = new Apply2Folder_ML(htr, lr, la, folder, folderOut, b2p, false, false, props);
        ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.ACCEPT); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
