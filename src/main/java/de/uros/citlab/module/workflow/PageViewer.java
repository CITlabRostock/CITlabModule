/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.planet.imaging.types.HybridImage;
import de.planet.imaging.util.GraphicalMonPanelFrame;
import de.planet.imaging.util.StdGraphicalMonPanelFrame;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.interfaces.types.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author gundram
 */
public class PageViewer extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(PageViewer.class.getName());

    @ParamAnnotation(descr = "path or file of image(s)")
    private String i = "";

    public PageViewer() {
        addReflection(this, PageViewer.class);
    }

    public void run() throws MalformedURLException {
        StdGraphicalMonPanelFrame fr = new StdGraphicalMonPanelFrame(GraphicalMonPanelFrame.PANEL_MODE_RESULT);
        fr.enable(true);
        File f = new File(i);
        List<File> files;
        if (f.isDirectory()) {
            files = FileUtil.listFiles(f, FileUtil.IMAGE_SUFFIXES, true);
            FileUtil.deleteFilesInPageFolder(files);
            Collections.sort(files);
        } else {
            files = new LinkedList<>();
            files.add(f);
        }
        for (File file : files) {
            Image img = new Image(file.toURI().toURL());
            BufferedImage imageBufferedImage = img.getImageBufferedImage(true);
            PcGtsType unmarshal = PageXmlUtil.unmarshal(PageXmlUtil.getXmlPath(file, true));
            BufferedImage debugImage = ImageUtil.getDebugImage(imageBufferedImage, unmarshal, 1.0, false, false, true, true, true);
            fr.addImage(HybridImage.newInstance(debugImage), file.getPath(), null, file.getPath());
            fr.next();
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidParameterException, MalformedURLException {
        ArgumentLine al = new ArgumentLine();
        al.addArgument("i", HomeDir.getFile("data/GTwithAbbreviations"));
        args=al.getArgs();
//        args=("-i "+HomeDir.getFile("tmp_20170308/xml_semi_0/")).split(" ");
        PageViewer instance = new PageViewer();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
