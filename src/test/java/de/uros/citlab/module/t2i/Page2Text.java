/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.t2i;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.workflow.HomeDir;
import de.uros.citlab.module.workflow.Page2PlainText;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author gundram
 */
public class Page2Text extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(Page2Text.class.getName());
//    private String dirImg = HomeDir.getFile("data/HTRTS14/gt").getAbsolutePath();
    private String dirImg
            = ""
            + HomeDir.getFile("data/HTRTS14/gt").getAbsolutePath() + File.pathSeparator //            + HomeDir.getFile("data/HTRTS14/t2i_nohyp").getAbsolutePath() + File.pathSeparator
            + HomeDir.getFile("data/HTRTS14/t2i").getAbsolutePath() + File.pathSeparator //            + HomeDir.getFile("data/HTRTS14/t2i_nohyp").getAbsolutePath() + File.pathSeparator
            + HomeDir.getFile("data/HTRTS14/t2i_1").getAbsolutePath() + File.pathSeparator //            + HomeDir.getFile("data/HTRTS14/t2i_nohyp").getAbsolutePath() + File.pathSeparator
            + HomeDir.getFile("data/HTRTS14/t2i_hyp").getAbsolutePath() + File.pathSeparator //            + HomeDir.getFile("data/HTRTS14/t2i_nohyp").getAbsolutePath() + File.pathSeparator
            + HomeDir.getFile("data/HTRTS14/t2i_hyp_ro").getAbsolutePath() + File.pathSeparator //            + HomeDir.getFile("data/HTRTS14/t2i_nohyp").getAbsolutePath() + File.pathSeparator
            + HomeDir.getFile("data/HTRTS14/t2i_hyp_ro_ws").getAbsolutePath() + File.pathSeparator //            + HomeDir.getFile("data/HTRTS14/t2i_nohyp").getAbsolutePath() + File.pathSeparator
            + HomeDir.getFile("data/HTRTS14/t2i_hyp_ws").getAbsolutePath() + File.pathSeparator //            + HomeDir.getFile("data/HTRTS14/t2i_nohyp").getAbsolutePath() + File.pathSeparator
            //            + HomeDir.getFile("data/HTRTS14/t2i_alvermann").getAbsolutePath() + File.pathSeparator
            //            + HomeDir.getFile("data/HTRTS14/t2i_chPath").getAbsolutePath() + File.pathSeparator
            //            + HomeDir.getFile("data/HTRTS14/t2i_alvermann_nac-1").getAbsolutePath() + File.pathSeparator
            //            + HomeDir.getFile("data/HTRTS14/t2i_alvermann_nac-2").getAbsolutePath() + File.pathSeparator
            //            + HomeDir.getFile("data/HTRTS14/t2i_alvermann_nonac").getAbsolutePath() + File.pathSeparator
            //            + HomeDir.getFile("data/HTRTS14/t2i_alvermann").getAbsolutePath() + File.pathSeparator
            //            + HomeDir.getFile("data/HTRTS14/t2i_hyp").getAbsolutePath();
            //    private String dirList = HomeDir.getFile("data/HTRTS14/lists").getAbsolutePath();
            //    private String dirNets = HomeDir.getFile("data/HTRTS14/nets").getAbsolutePath();
            //    private String dirRefs = HomeDir.getFile("data/HTRTS14/refs").getAbsolutePath();
            //    private final String[] hyphenSuffixes = new String[]{":", "-", "=", "¬"};
            //    private final String[] hyphenPrefixes = new String[]{":", "=", "-"};
            ;

    static {
        HomeDir.setPath("/home/gundram/devel/projects/read");
    }

    public Page2Text() {
        addReflection(this, Page2Text.class);
    }

    public void run() throws InvalidParameterException, IOException {
        if (dirImg.endsWith(File.pathSeparator)) {
            dirImg = dirImg.substring(0, dirImg.length() - 1);
        }
        String[] folders = dirImg.split(File.pathSeparator);
        for (String folder : folders) {
            try {
                ArgumentLine al = new ArgumentLine();
                al.addArgument("f", folder);
                al.addArgument("o", new File(folder + "_txt").getPath());
                String[] args = al.getArgs();
                Page2PlainText page2PlainText = new Page2PlainText();
                ParamSet ps = new ParamSet();
                ps.setCommandLineArgs(args);    // allow early parsing
                ps = page2PlainText.getDefaultParamSet(ps);
                ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
                page2PlainText.setParamSet(ps);
                page2PlainText.init();
                page2PlainText.run();
            } catch (Throwable ex) {
            }
        }
    }

    /**
     * @param args the command line arguments
     * @throws com.achteck.misc.exception.InvalidParameterException
     */
    public static void main(String[] args) throws InvalidParameterException, IOException {
        Page2Text instance = new Page2Text();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
