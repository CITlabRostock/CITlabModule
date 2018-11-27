/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.t2i;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.uros.citlab.errorrate.HtrError;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.types.Result;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.workflow.HomeDir;
import org.apache.commons.io.FileUtils;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author gundram
 */
public class EvaluateT2I extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(EvaluateT2I.class.getName());

    @ParamAnnotation(descr = "path to folder containing images and pageXML files")
    private String gt;
    @ParamAnnotation(descr = "path to folder containing images and pageXML files")
    private String hyp;
    @ParamAnnotation(descr = "calculate wer")
    private boolean wer = false;

    @ParamAnnotation(descr = "create debug images")
    private boolean debug;

    private File folderGt;
    private File folderHyp;
//    private boolean createDebug;

    public EvaluateT2I() {
        this("", "", false);
    }

    static {
        HomeDir.setPath("/home/gundram/devel/projects/read");
    }

    public EvaluateT2I(String gt, String hyp, boolean createDebugImg) {
        this.gt = gt;
        this.hyp = hyp;
        debug = createDebugImg;
        addReflection(this, EvaluateT2I.class);
    }

    @Override
    public void init() {
        super.init();
        if (gt.isEmpty()) {
            throw new RuntimeException("no input folder given");
        }
        if (hyp.isEmpty()) {
            throw new RuntimeException("no output folder given");
        }
        folderGt = new File(gt);
        folderHyp = new File(hyp);
    }

    public double run(String[] props) throws MalformedURLException, IOException, JAXBException {
        Collection<File> listFilesGT = FileUtil.listFiles(folderGt, new String[]{"xml"}, true);
        Collection<File> listFilesHyp = FileUtil.listFiles(folderHyp, new String[]{"xml"}, true);
        if (listFilesGT.size() != listFilesHyp.size()) {
            throw new RuntimeException("size of gt folder (" + listFilesGT.size() + ") and hyp folder(" + listFilesHyp.size() + ") have to be the same");
        }
        List<String> refs = new LinkedList<>();
        List<String> recos = new LinkedList<>();
        LOG.log(Logger.INFO, "found " + listFilesGT.size() + " xml-files...");
        for (File file : listFilesGT) {
            refs.add(file.getPath());
        }
        for (File file : listFilesHyp) {
            recos.add(file.getPath());
        }
        File fileRef = new File("ref.lst");
        File fileReco = new File("reco.lst");
        FileUtil.writeLines(fileRef, refs);
        FileUtil.writeLines(fileReco, recos);
        HtrError erp = new HtrError();
        Result run = erp.run(("ref.lst reco.lst" + (wer ? " -w" : "")).split(" "));
        FileUtils.deleteQuietly(fileRef);
        FileUtils.deleteQuietly(fileReco);
        ObjectCounter<Count> map = run.getCounts();
        long gt = map.get(Count.GT);
        long errors = map.get(Count.INS) + map.get(Count.DEL) + map.get(Count.SUB);
        System.out.println("ERROR = " + String.format("%.2f%s", ((double) errors) * 100.0 / ((double) gt), "%"));
        return ((double) errors) * 100.0 / ((double) gt);
    }

    public static void main(String[] args) throws InvalidParameterException, MalformedURLException, IOException, JAXBException, InterruptedException {
        ArgumentLine al = new ArgumentLine();
        al.addArgument("gt", HomeDir.PATH + "data/HTRTS14/gt");
        al.addArgument("hyp", HomeDir.PATH + "data/HTRTS14/t2i_1");
        args = al.getArgs();
        EvaluateT2I instance = new EvaluateT2I();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run(null);
    }

}
