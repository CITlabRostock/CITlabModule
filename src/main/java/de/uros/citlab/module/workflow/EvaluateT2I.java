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
import de.uros.citlab.errorrate.htr.end2end.ErrorModuleEnd2End;
import de.uros.citlab.errorrate.interfaces.ILine;
import de.uros.citlab.errorrate.types.Method;
import de.uros.citlab.errorrate.types.Result;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.util.PolygonUtil;
import de.uros.citlab.tokenizer.categorizer.CategorizerCharacterDft;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextEquivType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author gundram
 */
public class EvaluateT2I extends ParamTreeOrganizer implements Runnable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(EvaluateT2I.class.getName());

    @ParamAnnotation(descr = "folder with results")
    private String hyp = "";
    @ParamAnnotation(descr = "folder which contains images, pageXMLs(GT) (LA and HTR)")
    private String gt = "";
    @ParamAnnotation(descr = "use polygons")
    private boolean g = true;
    @ParamAnnotation(descr = "ignore Readingorder errors")
    private boolean no_ro = true;
    @ParamAnnotation(descr = "ignore segmentation errors")
    private boolean seg = true;
    @ParamAnnotation(descr = "threshold for confidence")
    private double t = 0.0;

    public EvaluateT2I() {
        addReflection(this, EvaluateT2I.class);
    }

    @Override
    public void init() {
        super.init(); //To change body of generated methods, choose Tools | Templates.
        if (gt.isEmpty()) {
            throw new RuntimeException("no GT folder given");
        }
    }


    @Override
    public void run() {
        File fGT = new File(this.gt);
        File fHyp = new File(this.hyp);
        //find folders to execute
        ErrorModuleEnd2End.Mode mode =
                no_ro ?
                        seg ?
                                ErrorModuleEnd2End.Mode.NO_RO_SEG :
                                ErrorModuleEnd2End.Mode.NO_RO :
                        seg ?
                                ErrorModuleEnd2End.Mode.RO_SEG :
                                ErrorModuleEnd2End.Mode.RO;
        ErrorModuleEnd2End measure = new ErrorModuleEnd2End(
                new CategorizerCharacterDft(),
                null,
                mode,
                g,
                null);
        List<File> xmlsGT = FileUtil.listFiles(fGT, "xml", true);
        List<File> xmlsHyps = FileUtil.listFiles(fHyp, "xml", true);
        FileUtil.deleteMetadataAndMetsFiles(xmlsGT);
        FileUtil.deleteMetadataAndMetsFiles(xmlsHyps);
        if (xmlsGT.size() != xmlsHyps.size()) {
            throw new RuntimeException("the number of xml between gt (" + xmlsGT.size() + ") and hyp (" + xmlsHyps.size() + ") differs.");
        }
        HashMap<String, File> xmlMap = new LinkedHashMap<>();
        for (File xmlHyp : xmlsHyps) {
            xmlMap.put(xmlHyp.getName(), xmlHyp);
        }
        if (xmlMap.size() != xmlsHyps.size()) {
            throw new RuntimeException("at least 1 file in hyp folder has the same name");
        }
        for (int i = 0; i < xmlsGT.size(); i++) {
            File xmlGT = xmlsGT.get(i);
            System.out.println(xmlGT);
            if(!xmlGT.getName().contains("071_085_002")){
                continue;
            }
            File xmlHyp = xmlMap.get(xmlGT.getName());
            if (xmlHyp == null) {
                throw new RuntimeException("cannot find hypothese for file " + xmlGT);
            }
            List<ILine> linesT2I = getLines(xmlHyp);
            List<ILine> linesRef = getLines(xmlGT);
            if (linesRef.size() == 0 && linesT2I.size() == 0) {
                continue;
            }
            measure.calculateWithSegmentation(linesT2I, linesRef);
        }
        Result result = new Result(Method.CER);
        result.addCounts(measure.getCounter());
        System.out.println(measure.getResults());
        System.out.println(result.getMetrics());
    }

    private List<ILine> getLines(File file) {
        List<ILine> res = new LinkedList<>();
        PcGtsType xml = PageXmlUtil.unmarshal(file);
        List<TextLineType> textLines = PageXmlUtil.getTextLines(xml);
        for (int i = 0; i < textLines.size(); i++) {
            TextLineType line = textLines.get(i);
            Polygon baseline = PolygonUtil.getBaseline(line);
            TextEquivType textEquiv = line.getTextEquiv();
            if (textEquiv == null) {
                continue;
            }
            String text = textEquiv.getUnicode();
            Float conf = textEquiv.getConf();
            if (conf != null && conf.doubleValue() < t) {
                continue;
            }
            res.add(new ILine() {
                @Override
                public String getText() {
                    return text;
                }

                @Override
                public Polygon getBaseline() {
                    return baseline;
                }
            });
        }
        return res;
    }

    public static void main(String[] args) throws InvalidParameterException, MalformedURLException, IOException, JAXBException {
        if (args.length == 0) {
            ArgumentLine al = new ArgumentLine();
            al.addArgument("gt", HomeDir.getFile("data/T2I_valid"));//004/004_070_015
            al.addArgument("hyp", HomeDir.getFile("data/T2I_LA_valid_out/NO_RO/geo"));
//            al.addArgument("t",0.02);
            args = al.getArgs();
        }
        EvaluateT2I instance = new EvaluateT2I();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
