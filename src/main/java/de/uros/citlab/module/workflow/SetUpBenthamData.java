package de.uros.citlab.module.workflow;

import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.XmlParserTei;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SetUpBenthamData {

    private String toOneString(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(" ").append(line);
        }
        return sb.toString().trim();
    }

    public void run(File folderImg, File folderXml, File out, boolean delete) throws IOException {
        out.mkdirs();
        List<File> xmls = FileUtil.listFiles(folderXml, "xml", true);
        for (File xml : xmls) {
            try {
                XmlParserTei.Doc doc = XmlParserTei.loadXML(xml);
                if (doc == null) {
                    continue;
                }
                XmlParserTei xmlParserTei = new XmlParserTei();
                ArrayList<XmlParserTei.Page> pages = xmlParserTei.getPages(doc);
                List<String> txt = new LinkedList<>();
                for (XmlParserTei.Page page : pages) {
                    txt.add(toOneString(page.getTargets()));
                }
                String baseName = xml.getName().substring(0, xml.getName().lastIndexOf("."));
                if (baseName.startsWith("JB_")) {
                    baseName = baseName.substring(3);
                }
                File folder = new File(out, baseName);
                FileUtil.writeLines(new File(folder, folder.getName() + ".txt"), txt);
            } catch (RuntimeException ex) {
                throw new RuntimeException("exception in parsing file '" + xml + "'", ex);
            }
        }
        List<File> imgs = FileUtil.listFiles(folderImg, FileUtil.IMAGE_SUFFIXES, true);
        for (File img : imgs) {
            String baseName = img.getName().substring(0, img.getName().lastIndexOf("."));
            File folder = new File(out, baseName);
            if (delete) {
                FileUtils.moveFile(img, new File(folder, img.getName()));
            } else {
                FileUtil.copyFile(img, new File(folder, img.getName()));
            }
        }
        ObjectCounter<String> oc = new ObjectCounter<>();
        File[] files = out.listFiles();
        for (File folder : files) {
            boolean hasTxt = !FileUtil.listFiles(folder, "txt", false).isEmpty();
            boolean hasImg = !FileUtil.listFiles(folder, FileUtil.IMAGE_SUFFIXES, false).isEmpty();
            boolean deleteForce = hasImg != hasTxt;
            if (hasImg && hasTxt) {
                oc.add("both");
            }
            if (hasImg && !hasTxt) {
                oc.add("only img");
            }
            if (!hasImg && hasTxt) {
                oc.add("only txt");
            }
            if (deleteForce) {
                FileUtils.deleteQuietly(folder);
            }
        }
        System.out.println(oc);
        if (delete) {
            if (!out.getAbsolutePath().startsWith(folderImg.getAbsolutePath())) {
                FileUtils.deleteQuietly(folderImg);
            } else {
                File d = new File(out, "Thumbs.db");
                if (d.exists()) {
                    FileUtils.deleteQuietly(d);
                }
            }
            if (!out.getAbsolutePath().startsWith(folderXml.getAbsolutePath())) {
                FileUtils.deleteQuietly(folderXml);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            args = new String[]{"data/009", "data/Box 009", "data/009/la", "true"};
        }
        SetUpBenthamData instance = new SetUpBenthamData();
        File folderImg = HomeDir.getFile(args[0]);
        File folderXml = HomeDir.getFile(args[1]);
        File out = HomeDir.getFile(args[2]);
        boolean delete = Boolean.parseBoolean(args[3]);
        instance.run(folderImg, folderXml, out, delete);
    }
}
