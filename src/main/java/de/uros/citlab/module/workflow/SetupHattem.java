/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author gundram
 */
public class SetupHattem {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(SetupHattem.class.getName());

    private final File f;
    private final File o;
    private final boolean useAbbr;
    private String normalform;
    private boolean deletedots = true;
    Normalizer.Form form;
    boolean xml = true;

    public SetupHattem(File f, File o, boolean useAbbr) {
        this.f = f;
        this.o = o;
        this.useAbbr = useAbbr;
    }

    public void setDeletedots(boolean deletedots) {
        this.deletedots = deletedots;
    }

    public void setNormalform(String normalform) {
        this.normalform = normalform;
        form = normalform.isEmpty() ? null : Normalizer.Form.valueOf(normalform);
    }

    public void setXml(boolean xml) {
        this.xml = xml;
    }

    Pattern patternChoose = Pattern.compile("(<choose n=\"?[0-9]+\"?><abbrev>([^<]+)</abbrev><expan>([^<]+)</expan></choose>)");
    Pattern patternExpand = Pattern.compile("(<expan>([^<]+)</expan>)");
    Pattern patternEscaped = Pattern.compile("&#x([0-9a-zA-Z]+);");
    private Map<String, String> abbrMap = new LinkedHashMap<>();

    private String normalize(String line) {
        String text = line.trim();
        if (deletedots) {
            text = text.replaceAll("\\[...\\]", "").trim();
        }

        Matcher matcherChoose = patternChoose.matcher(text);
        StringBuilder sb = new StringBuilder();
        int startChoose = 0;
        int endChoose = text.length();
        while (matcherChoose.find()) {
            String abbr = matcherChoose.group(2);
            Matcher matcherEscaped = patternEscaped.matcher(abbr);
            int startEscaped = 0;
            int endEscaped = abbr.length();
            StringBuilder sbEscaped = new StringBuilder();
            while (matcherEscaped.find()) {
                int integer = Integer.valueOf(matcherEscaped.group(1), 16);
                sbEscaped.append(abbr, startEscaped, matcherEscaped.start());
                sbEscaped.append(Character.toChars(integer));
                startEscaped = matcherEscaped.end();
            }
            sbEscaped.append(abbr, startEscaped, endEscaped);
            abbr = sbEscaped.toString();
            String expan = matcherChoose.group(3);

            sb.append(text, startChoose, matcherChoose.start());
            if (useAbbr) {
                sb.append(abbr);
            } else {
                sb.append(expan);
            }
            startChoose = matcherChoose.end();
        }
        String res = sb.append(text, startChoose, endChoose).toString();
        Matcher matcherExpand = patternExpand.matcher(res);
        int startExpand = 0;
        int endExpand = res.length();
        StringBuilder sbExpand = new StringBuilder();
        while (matcherExpand.find()) {
            sbExpand.append(res, startExpand, matcherExpand.start());
            if (!useAbbr) {
                String expand = matcherExpand.group(2);
                sbExpand.append(expand);
            }
            startExpand = matcherExpand.end();
        }
        sbExpand.append(res, startExpand, endExpand);
        res = sbExpand.toString();
        if (form != null) {
            res = Normalizer.normalize(res, form);
        }
        res = res.replaceAll("<lb/>", "");
        if (res.contains("<")) {
            throw new RuntimeException("still tags (<) in line: '" + res + "'");
        }
        LOG.debug("normalize '{}' to '{}'", line, res);
        return res;
    }

    public void run() {
        if (o != null && !o.equals(f)) {
            FileUtils.deleteQuietly(o);
        }
        if (xml) {
            List<File> filesListsOrFolders = FileUtil.getFilesListsOrFolders(f.getAbsolutePath(), "xml".split(" "), true);
            FileUtil.deleteMetadataAndMetsFiles(filesListsOrFolders);
            for (File filesListsOrFolder : filesListsOrFolders) {
                LOG.debug("##### process file {} ...", filesListsOrFolder);
                PcGtsType unmarshal = PageXmlUtil.unmarshal(filesListsOrFolder);
                List<TextLineType> textLines = PageXmlUtil.getTextLines(unmarshal);
                for (int i = 0; i < textLines.size(); i++) {
                    TextLineType textLineType = textLines.get(i);
                    String corrected = normalize(textLineType.getTextEquiv().getUnicode());
                    LOG.debug("##### process line {} ...", textLineType.getId());
                    textLineType.getTextEquiv().setUnicode(corrected);
                }
                File tgtFile = FileUtil.getTgtFile(f, o, filesListsOrFolder);
                tgtFile.getParentFile().mkdirs();
                PageXmlUtil.marshal(unmarshal, tgtFile);
                File imagePath = PageXmlUtil.getImagePath(filesListsOrFolder, true);
                File tgtFileImg = FileUtil.getTgtFile(f, o, imagePath);
                FileUtil.copyFile(imagePath, tgtFileImg);

            }
        } else {
            List<File> filesListsOrFolders = FileUtil.getFilesListsOrFolders(f.getAbsolutePath(), "txt".split(" "), true);
            for (File txts : filesListsOrFolders) {
                LOG.debug("##### process file {} ...", txts);
                List<String> lines = FileUtil.readLines(txts);
                for (int i = 0; i < lines.size(); i++) {
                    lines.set(i, normalize(lines.get(i)));
                }
                String base = txts.getName().substring(0, txts.getName().lastIndexOf("."));
                File folder = new File(o, base);
                folder.mkdirs();
                File img = new File(txts.getParentFile(), base + ".jpg");
                File txt = new File(folder, base + ".txt");
                FileUtil.writeLines(txt, lines);
                FileUtil.copyFile(img, new File(folder, img.getName()));
            }

        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidParameterException {
        for (String form : new String[]{"NFD", "NFC", null})
            for (boolean abbr : new boolean[]{true, false}) {
                ArgumentLine al = new ArgumentLine();
                al.addArgument("useAbbr", abbr);
                al.addArgument("f", HomeDir.getFile("data/GTwithAbbreviations"));
                al.addArgument("o", HomeDir.getFile("data/GT_" + (abbr ? "Abbr" : "Expand") + (form == null ? "" : "_" + form)));
//        al.setHelp();
                args = al.getArgs();
                {
                    SetupHattem instance = new SetupHattem(
                            HomeDir.getFile("data/GTwithAbbreviations"),
                            HomeDir.getFile("data/GT_" + (abbr ? "Abbr" : "Expand") + (form == null ? "" : "_" + form)),
                            abbr);
                    if (form != null) {
                        instance.setNormalform(form);
                    }
                    instance.run();
                }

                al.addArgument("f", HomeDir.getFile("data/Hattem/AdditionalMaterial"));
                al.addArgument("o", HomeDir.getFile("data/T2I_" + (abbr ? "Abbr" : "Expand") + (form == null ? "" : "_" + form)));
                al.addArgument("xml", false);
                args = al.getArgs();
                {
                    SetupHattem instance = new SetupHattem(
                            HomeDir.getFile("data/Hattem/AdditionalMaterial"),
                            HomeDir.getFile("data/T2I_" + (abbr ? "Abbr" : "Expand") + (form == null ? "" : "_" + form)),
                            abbr);
                    instance.setXml(false);
                    if (form != null) {
                        instance.setNormalform(form);
                    }
                    instance.run();
                }
            }
    }

}
