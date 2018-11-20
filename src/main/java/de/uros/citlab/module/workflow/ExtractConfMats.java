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
import de.uros.citlab.module.htr.HTRParser;
import de.uros.citlab.module.util.CharMapUtil;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.util.TrainDataUtil;
import eu.transkribus.interfaces.IHtr;
import eu.transkribus.interfaces.types.Image;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import javax.xml.bind.JAXBException;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author gundram
 */
public class ExtractConfMats extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ExtractConfMats.class.getName());

    private File htrName;
    private IHtr htr;
    private File folderIn;
    private File folderOut;

    public ExtractConfMats(File htr, File folderIn, File folderOut) {
        this.htr = new HTRParser();
        htrName = htr;
        this.folderIn = folderIn;
        this.folderOut = folderOut;
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
            String name = tgtImg.getPath();
            name = name.substring(0, name.lastIndexOf("."));
            File folderTraindata = new File(name);
            folderTraindata.mkdirs();
            Image img = new Image(tgtImg.toURI().toURL());
            File srcXml = PageXmlUtil.getXmlPath(srcImg, false);
            File tgtXml = PageXmlUtil.getXmlPath(tgtImg, false);
            if (!srcXml.exists()) {
                throw new RuntimeException("cannot find file '" + srcXml + "'.");
            }
            if (!tgtXml.equals(srcXml)) {
                FileUtils.copyFile(srcXml, tgtXml);
            }
            TrainDataUtil.createTrainData(new String[]{PageXmlUtil.getXmlPath(srcImg).getAbsolutePath()}, folderTraindata.getAbsolutePath(), null);
            htr.process(htrName.getAbsolutePath(), null, null, img, tgtXml.getAbsolutePath(), folderTraindata.getAbsolutePath(), null, null);
        }
    }

}
