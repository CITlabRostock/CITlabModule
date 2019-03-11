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
import de.uros.citlab.errorrate.htr.ErrorRateCalcer;
import de.uros.citlab.errorrate.htr.end2end.ErrorModuleEnd2End;
import de.uros.citlab.errorrate.types.Method;
import de.uros.citlab.errorrate.types.Metric;
import de.uros.citlab.errorrate.types.Result;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import org.apache.commons.math3.util.Pair;
import org.primaresearch.dla.page.Page;
import org.primaresearch.dla.page.layout.physical.Region;
import org.primaresearch.dla.page.layout.physical.text.LowLevelTextObject;
import org.primaresearch.dla.page.layout.physical.text.impl.TextLine;
import org.primaresearch.dla.page.layout.physical.text.impl.TextRegion;
import org.primaresearch.io.UnsupportedFormatVersionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * @author gundram
 */
public class Page2PlainText4Joan extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(Page2PlainText4Joan.class);

    @ParamAnnotation(descr = "folder with xmls or path to list-file (use ':' for more folders/list-files)")
    private String f = "";
    @ParamAnnotation(descr = "if given, write output plain text to this path")
    private String o = "";
    double t = -0.1;

    public Page2PlainText4Joan() {
        addReflection(this, Page2PlainText4Joan.class);
    }

    public void run() {
        List<File> filesListsOrFolders = FileUtil.getFilesListsOrFolders(f, "xml".split(" "), true);
        FileUtil.deleteMetadataAndMetsFiles(filesListsOrFolders);
        File out = o.isEmpty() ? null : new File(o);
        List<String> list = new LinkedList<>();
        String s = "<space>";
        for (File filesListsOrFolder : filesListsOrFolders) {
            PcGtsType unmarshal = PageXmlUtil.unmarshal(filesListsOrFolder);
            List<TextLineType> textLines1 = PageXmlUtil.getTextLines(unmarshal);
            String name = filesListsOrFolder.getName();
            name = name.substring(0, name.lastIndexOf("."));
            for (TextLineType textLineType : textLines1) {
                if (textLineType.getTextEquiv() != null) {
                    if (t > 0.0 && textLineType.getTextEquiv().getConf() != null && textLineType.getTextEquiv().getConf().doubleValue() <= 0.1) {
                        continue;
                    }
                    String unicode = textLineType.getTextEquiv().getUnicode();
                    StringBuilder sb = new StringBuilder();
                    sb.append(name).append('.').append(textLineType.getId()).append(' ').append(s).append(' ');
                    for (char c : unicode.toCharArray()) {
                        if (c == ' ') {
                            sb.append(s);
                        } else {
                            sb.append(c);
                        }
                        sb.append(' ');
                    }
                    sb.append(s).append(' ');
                    list.add(sb.toString());
                }
            }
            FileUtil.writeLines(out, list);
        }
    }

    public static List<String> getTextFromFile(String fileName) {
        if (fileName.endsWith(".xml")) {
            Page aPage;
            try {
                List<String> res = new ArrayList<>();
//                aPage = reader.read(new FileInput(new File(fileName)));
                aPage = org.primaresearch.dla.page.io.xml.PageXmlInputOutput.readPage(fileName);
                if (aPage == null) {
                    System.out.println("Error while parsing xml-File.");
                    return null;
                }
                List<Region> regionsSorted = aPage.getLayout().getRegionsSorted();
                for (Region reg : regionsSorted)
                    if (reg instanceof TextRegion) {
                        TextRegion textregion = (TextRegion) reg;
                        List<LowLevelTextObject> textObjectsSorted = textregion.getTextObjectsSorted();
                        for (LowLevelTextObject line : textObjectsSorted) {
                            if (line instanceof TextLine) {
                                String text = line.getText();
//                                if (text == null) {
//                                    LOG.warn("transciption is null for line with id = {} - ignore line.", line.getId());
//                                    continue;
//                                }
//                                if (text.startsWith(" ")) {
//                                    LOG.warn("transciption of line id {} starts with space, trim transcription '{}'", line.getId(), text);
//                                }
//                                if (text.endsWith(" ")) {
//                                    LOG.warn("transciption of line id {} ends with space, trim transcription '{}'", line.getId(), text);
//                                }
                                res.add(text == null ? "" : text.trim());
                            }
                        }
                    }
                return res;
            } catch (UnsupportedFormatVersionException ex) {
                throw new RuntimeException("Error while parsing xml-file.", ex);
            }
        }
        return null;
    }


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidParameterException {
        File file = HomeDir.getFile("eval/");
        File fileOut = HomeDir.getFile("eval_gundram/");
        List<String> resError = new LinkedList<>();
        List<String> resErrorE2E = new LinkedList<>();
        resError.add("CER / WER configuration");
        resErrorE2E.add("CER / WER configuration");
        Map<Pair<String, String>, String> errorMap = new TreeMap<>();
        Set<String> models = new TreeSet<>();
        for (File folderSet : file.listFiles((dir, name) -> !name.contains("__"))) {
            String name_set = folderSet.getName();
            File folderGT = HomeDir.getFile("data/sets_b2p/" + name_set.replace("_lm", ""));
            for (File folderOM : folderSet.listFiles()) {
                String name_om = folderOM.getName();
                ArgumentLine al = new ArgumentLine();
//        al.addArgument("f", HomeDir.getFile("data/eval4joan"));
//        al.addArgument("o", HomeDir.getFile("RO_HYP_net4.txt"));
                al.addArgument("f", folderOM.getAbsolutePath());
                al.addArgument("o", new File(fileOut, name_set + "__" + name_om + ".txt"));
//        al.setHelp();
                args = al.getArgs();
                Page2PlainText4Joan instance = new Page2PlainText4Joan();
                ParamSet ps = new ParamSet();
                ps.setCommandLineArgs(args);    // allow early parsing
                ps = instance.getDefaultParamSet(ps);
                ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
                instance.setParamSet(ps);
                instance.init();
                instance.run();
                File[] xmlsGT = FileUtil.listFiles(folderGT, "xml", true).toArray(new File[0]);
                File[] xmlsOM = FileUtil.listFiles(folderOM, "xml", true).toArray(new File[0]);
                {
                    ErrorRateCalcer calcer = new ErrorRateCalcer();
                    Result processCER = calcer.process(xmlsOM, xmlsGT, Method.CER);
                    Result processWER = calcer.process(xmlsOM, xmlsGT, Method.WER);
                    errorMap.put(new Pair<>( folderOM.getName(),name_set), String.format("%4.1f / %4.1f",
                            processCER.getMetric(Metric.ERR) * 100,
                            processWER.getMetric(Metric.ERR) * 100));
                    models.add(folderOM.getName());
                    resError.add(String.format("%4.1f / %4.1f %s",
                            processCER.getMetric(Metric.ERR) * 100,
                            processWER.getMetric(Metric.ERR) * 100,
                            name_set + "__" + folderOM.getName()));
                    System.out.println(resError);
                    FileUtil.writeLines(HomeDir.getFile("errors_gundram_.txt"), resError);
                }
                {
                    ErrorModuleEnd2End end2EndCER = new ErrorModuleEnd2End(false, false, false, false);
                    ErrorModuleEnd2End end2EndWER = new ErrorModuleEnd2End(false, false, false, true);
                    for (int i = 0; i < xmlsGT.length; i++) {
                        List<String> gtLines = getTextFromFile(xmlsGT[i].getPath());
                        List<String> hypLines = getTextFromFile(xmlsOM[i].getPath());
                        if (gtLines.size() != hypLines.size()) {
                            throw new RuntimeException();
                        }
                        for (int j = 0; j < gtLines.size(); j++) {
                            System.out.println("1: " + hypLines.get(j) + " " + hypLines.get(j).length());
                            System.out.println("2: " + gtLines.get(j) + " " + gtLines.get(j).length());
                            end2EndCER.calculate(hypLines.get(j), gtLines.get(j), false);
                            end2EndWER.calculate(hypLines.get(j), gtLines.get(j), false);
                        }
                    }
                    resErrorE2E.add(String.format("%4.1f / %4.1f %s",
                            end2EndCER.getMetrics().get(Metric.ERR) * 100,
                            end2EndWER.getMetrics().get(Metric.ERR) * 100,
                            name_set + "__" + folderOM.getName()));
                    FileUtil.writeLines(HomeDir.getFile("errors_gundram_e2e.txt"), resErrorE2E);
                }
            }
            ArgumentLine al = new ArgumentLine();
//        al.addArgument("f", HomeDir.getFile("data/eval4joan"));
//        al.addArgument("o", HomeDir.getFile("RO_HYP_net4.txt"));
            al.addArgument("f", folderGT);
            al.addArgument("o", new File(fileOut, name_set + "__gt.txt"));
//        al.setHelp();
            args = al.getArgs();
            Page2PlainText4Joan instance = new Page2PlainText4Joan();
            ParamSet ps = new ParamSet();
            ps.setCommandLineArgs(args);    // allow early parsing
            ps = instance.getDefaultParamSet(ps);
            ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
            instance.setParamSet(ps);
            instance.init();
            instance.run();

        }
        List<String> out = new LinkedList<>();
        for (String model : models) {
            StringBuilder sb = new StringBuilder();
            sb.append(model).append(" & ");
            for (Map.Entry<Pair<String, String>, String> pairStringEntry : errorMap.entrySet()) {
                if(!pairStringEntry.getKey().getKey().equals(model)){
                    continue;
                }
                sb.append(pairStringEntry.getValue()).append(" & ");
            }
            out.add(sb.toString());

        }

    }

}
