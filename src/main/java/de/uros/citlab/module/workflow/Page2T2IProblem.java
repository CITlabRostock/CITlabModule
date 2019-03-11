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
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PageXmlUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author gundram
 */
public class Page2T2IProblem extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(Page2T2IProblem.class.getName());

    @ParamAnnotation(descr = "folder with typical xml structure or path to list-file (use ':' for more folders/list-files)")
    private String f = "";
    @ParamAnnotation(descr = "if given, write output plain text to this path")
    private String o = "";

    public Page2T2IProblem() {
        addReflection(this, Page2T2IProblem.class);
    }

    public void run() {
        List<File> filesListsOrFolders = FileUtil.getFilesListsOrFolders(f, FileUtil.IMAGE_SUFFIXES, true);
        FileUtil.deleteMetadataAndMetsFiles(filesListsOrFolders);
        File out = o.isEmpty() ? null : new File(o);
        for (File imageFile : filesListsOrFolders) {
            File xmlFile = PageXmlUtil.getXmlPath(imageFile, true);
            String text = String.join(" ", PageXmlUtil.getText(PageXmlUtil.unmarshal(xmlFile)));
            String name = xmlFile.getName().replace(".xml", "");
            File outFolder = FileUtil.getTgtFile(new File(f), out, imageFile.getParentFile());
            outFolder = new File(outFolder, name);
            outFolder.mkdirs();
            File outPageFolder = new File(outFolder, "page");
            outPageFolder.mkdirs();
            FileUtil.copyFile(imageFile, new File(outFolder, imageFile.getName()));
            FileUtil.copyFile(xmlFile, new File(outPageFolder, xmlFile.getName()));
            FileUtil.writeLines(new File(outFolder, name + ".txt"), Arrays.asList(text));
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidParameterException {
//        List<File> jpg = FileUtil.listFiles(HomeDir.getFile("data/sets_b2p_plaintext"), "xml", true);
//        for (File img : jpg) {
        ArgumentLine al = new ArgumentLine();
        al.addArgument("f", HomeDir.getFile("data/sets_b2p"));
        al.addArgument("o", HomeDir.getFile("data/sets_b2p_plaintext"));
//        al.setHelp();
        args = al.getArgs();
        Page2T2IProblem instance = new Page2T2IProblem();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
//        }
    }

}
