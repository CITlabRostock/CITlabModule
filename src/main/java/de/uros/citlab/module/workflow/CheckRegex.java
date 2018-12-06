/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ConfMat;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.uros.citlab.errorrate.HtrError;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.module.baseline2polygon.B2PSeamMultiOriented;
import de.uros.citlab.module.baseline2polygon.Baseline2PolygonParser;
import de.uros.citlab.module.htr.HTRParser;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.types.HTR;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.interfaces.IBaseline2Polygon;
import eu.transkribus.interfaces.IHtr;
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
import java.util.LinkedList;
import java.util.List;

/**
 * @author gundram and max
 */
public class CheckRegex extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(CheckRegex.class.getName());

    @ParamAnnotation(descr = "path to dictionary")
    private String lr;
    @ParamAnnotation(descr = "path to folder containing images and pageXML files")
    private String gt;
    @ParamAnnotation(descr = "path to folder where changed pageXML files should be saved")
    private String out;
    @ParamAnnotation(descr = "calculate wer")
    private boolean wer = false;

    @ParamAnnotation(descr = "create debug images")
    private boolean debug;

    private IHtr htrInstance;
    private IBaseline2Polygon b2p;
    private File folderIn;
    private File folderOut;
//    private boolean createDebug;

    public CheckRegex() {
        this("", "", "", "", false);
    }

    public CheckRegex(String htr, String lr, String gt, String out, boolean createDebugImg) {
        this.htrInstance = new HTRParser();
        b2p = new Baseline2PolygonParser(B2PSeamMultiOriented.class.getName());
        this.gt = gt;
        this.out = out;
        this.lr = lr;
        debug = createDebugImg;
        addReflection(this, CheckRegex.class);
    }

    @Override
    public void init() {
        super.init();
        if (gt.isEmpty()) {
            throw new RuntimeException("no input folder given");
        }
        if (out.isEmpty()) {
            throw new RuntimeException("no output folder given");
        }
        folderIn = new File(gt);
        folderOut = new File(out);
    }

    private ConfMat getConfMat(String reference) {
        return null;
    }

    public void run() throws MalformedURLException, IOException, JAXBException {
        Collection<File> listFiles = FileUtils.listFiles(folderIn, FileUtil.IMAGE_SUFFIXES, true);
        folderOut.mkdirs();
        List<String> refs = new LinkedList<>();
        List<String> recos = new LinkedList<>();
        LOG.log(Logger.INFO, "found " + listFiles.size() + " images...");
        for (File pathImg : listFiles) {
            Path pathFull = Paths.get(pathImg.getAbsolutePath());
            Path pathPart = Paths.get(folderIn.getAbsolutePath());
            Path p = pathPart.relativize(pathFull);
            File outImg = new File(folderOut, p.toString());
            outImg.getParentFile().mkdirs();
            FileUtils.copyFile(pathImg, outImg);
            System.out.println(p);
            Image img = new Image(outImg.toURI().toURL());

            File parent = outImg.getParentFile();
            File out = new File(parent, outImg.getName().substring(0, outImg.getName().lastIndexOf(".")) + ".xml");
            File xmlPath = PageXmlUtil.getXmlPath(pathImg);
            FileUtils.copyFile(xmlPath, out);
            refs.add(xmlPath.getAbsolutePath());
            recos.add(out.getAbsolutePath());
            b2p.process(img, out.getAbsolutePath(), null, null);

            HTR htr = new HTR(null, HTRParser.getLangMod(lr, null, null), null, lr, null, null);
            PcGtsType page = PageXmlUtil.unmarshal(out);
            List<TextLineType> textLines = PageXmlUtil.getTextLines(page);
            for (TextLineType textLine : textLines) {
                if (textLine.getTextEquiv() != null) {
                    String ref = PageXmlUtil.getTextEquiv(textLine);
                    ConfMat cm = getConfMat(ref);
                    List<HTR.ConfMatPart> cofMatParts = htr.getCofMatParts(cm, null);
                    StringBuilder sb = new StringBuilder();
                    for (HTR.ConfMatPart cofMatPart : cofMatParts) {
                        sb.append(cofMatPart.getConfMat().toString());
                    }
                    PageXmlUtil.setTextEquiv(textLine, sb.toString());
                }
            }

            PageXmlUtil.marshal(page, out);
            htrInstance.process(null, lr, null, img, out.getAbsolutePath(), null, null, null);
            if (debug) {
                BufferedImage imageBufferedImage = img.getImageBufferedImage(true);
                imageBufferedImage = ImageUtil.getDebugImage(imageBufferedImage, PageXmlUtil.unmarshal(out), 1.0, false, false, false, true, true);
                ImageIO.write(imageBufferedImage, "png", new File(out.getAbsoluteFile() + ".png"));
            }
        }
        File fileRef = new File("ref.lst");
        File fileReco = new File("reco.lst");
        FileUtil.writeLines(fileRef, refs);
        FileUtil.writeLines(fileReco, recos);
        HtrError erp = new HtrError();
        ObjectCounter<Count> map = erp.run(("ref.lst reco.lst" + (wer ? " -w" : "")).split(" ")).getCounts();
        FileUtils.deleteQuietly(fileRef);
        FileUtils.deleteQuietly(fileReco);
        long gt = map.get(Count.GT);
        long errors = map.get(Count.INS) + map.get(Count.DEL) + map.get(Count.SUB);
        System.out.println("ERROR = " + String.format("%.2f%s", ((double) errors) * 100.0 / ((double) gt), "%"));
    }

    public static void main(String[] args) throws InvalidParameterException, MalformedURLException, IOException, JAXBException, InterruptedException {
        if (args.length == 0) {
            ArgumentLine al = new ArgumentLine();
            al.addArgument("gt", HomeDir.getFile("data/val/"));//folder containing images and PAGE-XML with baselines
            al.addArgument("out", HomeDir.getFile("datatest/")); //folder where recognition should be saved
//            al.addArgument("htr", "/home/gundram/devel/projects/barlach/nets/barlach_20160930_both_v1.1.sprnn");//path to htr model
            al.addArgument("htr", HomeDir.getFile("nets/barlach_20161118_trptrain.sprnn"));//path to htr model
            al.addArgument("lr", HomeDir.getFile("dicts/dict_g1.csv"));//path to htr model
            args = al.getArgs();
        }
        CheckRegex instance = new CheckRegex();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
