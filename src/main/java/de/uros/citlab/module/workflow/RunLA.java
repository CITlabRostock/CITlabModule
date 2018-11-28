/*
 * @a
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.planet.imaging.types.StdFrameAppender;
import de.uros.citlab.module.baseline2polygon.B2PSeamMultiOriented;
import de.uros.citlab.module.baseline2polygon.Baseline2PolygonParser;
import de.uros.citlab.module.la.LayoutAnalysisParser;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.util.PropertyUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.IBaseline2Polygon;
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
public class RunLA extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(RunLA.class.getName());

    private ILayoutAnalysis la;
    private IBaseline2Polygon b2p;
    private File folderIn;
    private File folderOut;
    private boolean createDebug;
    private boolean applyB2P = false;

    public RunLA(File la, File folderIn, File folderOut, boolean createDebugImg, boolean applyB2P) {
        b2p = new Baseline2PolygonParser(B2PSeamMultiOriented.class.getName());
        if (la != null) {
            this.la = new LayoutAnalysisParser(la.getAbsolutePath(), null);
        }
        this.folderIn = folderIn;
        this.folderOut = folderOut;
        this.createDebug = createDebugImg;
        this.applyB2P = applyB2P;
    }

    public void run() throws MalformedURLException, IOException, JAXBException {
        Collection<File> listFiles = FileUtil.listFiles(folderIn, FileUtil.IMAGE_SUFFIXES, true);
        FileUtil.deleteFilesInPageFolder(listFiles);
        folderOut.mkdirs();
        LOG.log(Logger.INFO, "found " + listFiles.size() + " images...");
        for (File srcImg : listFiles) {
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
                String[] props = PropertyUtil.setProperty(null, Key.DELETE, "true");
                //                props = PropertyUtil.setProperty(props, Key.METHOD_LA, "line");
                la.process(img, tgtXml.getAbsolutePath(), null, props);
            }
            if (applyB2P) {
                b2p.process(img, tgtXml.getAbsolutePath(), null, null);
            }
            if (createDebug) {
                BufferedImage imageBufferedImage = img.getImageBufferedImage(true);
                BufferedImage debug = ImageUtil.getDebugImage(imageBufferedImage, PageXmlUtils.unmarshal(tgtXml), -1, false, true, true, false, false);
                ImageIO.write(debug, "jpg", new File(tgtXml.getAbsoluteFile() + ".jpg"));
            }
        }
    }

    public static void main(String[] args) throws InvalidParameterException, MalformedURLException, IOException, JAXBException, ClassNotFoundException {
        File folder = null, folderOut = null, la = null;
//        folder = new File("/home/gundram/devel/projects/barlach/T2I_la/");//folder containing images and PAGE-XML with baselines
//        folder = new File("/home/gundram/devel/src/git/CITlabModule/src/test/resources/");//folder containing images and PAGE-XML with baselines
        folder = HomeDir.getFile("data/T2I");
        folderOut = new File(HomeDir.PATH_FILE, "data/T2I");
//        htr = new File("/home/gundram/devel/projects/barlach/nets/barlach_v3_unsorted.sprnn");//path to htr model
//        lr = new File("/home/gundram/devel/projects/barlach/dicts/dict_v1.csv");//path to htr model
//        la = new File("/home/gundram/devel/projects/barlach/configs/historical_90_dft_20161011.bin"); //path to la - if no la is given, the PAGE-XML should have baselines.
//        la = new File("src/main/resources/planet/la/historical_90_dft_20161011.bin"); //path to la - if no la is given, the PAGE-XML should have baselines.
        RunLA instance = new RunLA(la, folder, folderOut, true, true);
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
        LOG.log(Logger.INFO, new StdFrameAppender.AppenderContent(true));
    }

}
