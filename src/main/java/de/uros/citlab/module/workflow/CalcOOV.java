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
import de.planet.itrtech.types.IDictOccurrence;
import de.planet.util.types.DictOccurrence;
import de.uros.citlab.module.types.DictOccurenceFromText;
import de.uros.citlab.module.util.OOVUtil;
import java.io.IOException;

/**
 *
 * @author tobias
 */
public class CalcOOV extends ParamTreeOrganizer {

//    @ParamAnnotation
//    String lref;
//    @ParamAnnotation
//    String leval;
    @ParamAnnotation(member = "dictR")
    String dref = DictOccurrence.class.getCanonicalName();
    IDictOccurrence dictR;
    @ParamAnnotation(member = "dictE")
    String deval = DictOccurrence.class.getCanonicalName();
    IDictOccurrence dictE;

    public CalcOOV() {
        addReflection(this, CalcOOV.class);
    }

    @Override
    public void init() {

        super.init();
    }

    public void run() {

        System.out.println("OOV rate of deval: " + OOVUtil.calcOOVRate(dictR, dictE));
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidParameterException, IOException {
        if (args.length == 0) {
            String arg = ""
                    + "-deval " + DictOccurenceFromText.class.getCanonicalName() + " "
                    + "-dref " + DictOccurenceFromText.class.getCanonicalName() + " "
                    + "-deval/l /home/tobias/devel/src/git/stazh/transcripts_val.txt "
                    + "-dref/l /home/tobias/devel/src/git/stazh/languageresources_StaZh/transcripts_incl_val_0.001.txt "
//                    + "--help"
                    + "";
            args = arg.split(" ");
        }
        CalcOOV instance = new CalcOOV();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
