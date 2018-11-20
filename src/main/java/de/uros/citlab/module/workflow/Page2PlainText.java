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
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.util.PageXmlUtils;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author gundram
 */
public class Page2PlainText extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(Page2PlainText.class.getName());

    @ParamAnnotation(descr = "folder with xmls or path to list-file (use ':' for more folders/list-files)")
    private String f = "";
    @ParamAnnotation(descr = "if given, write output plain text to this path")
    private String o = "";

    public Page2PlainText() {
        addReflection(this, Page2PlainText.class);
    }

    public void run() {
        List<File> filesListsOrFolders = FileUtil.getFilesListsOrFolders(f, "xml".split(" "), true);
        FileUtil.deleteMetadataAndMetsFiles(filesListsOrFolders);
        File out = o.isEmpty() ? null : new File(o);
        if (out != null) {
            FileUtils.deleteQuietly(out);
        }
        for (File filesListsOrFolder : filesListsOrFolders) {
            PcGtsType unmarshal = PageXmlUtil.unmarshal(filesListsOrFolder);
            List<String> textLines = PageXmlUtil.getText(unmarshal);
            if (out == null) {
                FileUtil.writeLines(new File(filesListsOrFolder.getAbsolutePath().replace(".xml", ".txt")), textLines);
            } else {
                File file = new File(out, filesListsOrFolder.getName().replace(".xml", ".txt"));
                if (file.exists()) {
                    throw new RuntimeException("file " + file.getAbsolutePath() + " is written two times.");
                }
                FileUtil.writeLines(file, textLines, false);
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidParameterException {
        ArgumentLine al = new ArgumentLine();
        al.addArgument("f", "/home/gundram/devel/projects/racetrack_stazh/data/train_b2p");
        al.addArgument("o", "/home/gundram/devel/projects/racetrack_stazh/data/lr_stazh_320.txt");
//        al.setHelp();
        args = al.getArgs();
        Page2PlainText instance = new Page2PlainText();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
