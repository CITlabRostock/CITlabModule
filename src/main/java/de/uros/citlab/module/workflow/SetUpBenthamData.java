package de.uros.citlab.module.workflow;

import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.module.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class SetUpBenthamData {
    private static Logger LOG = LoggerFactory.getLogger(SetUpBenthamData.class);

    private String toOneString(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(" ").append(line);
        }
        return sb.toString().trim();
    }

    private static Pattern patternBaseName = Pattern.compile("[0-9]{2,3}_[0-9]{3}_[0-9]{3}");

    private String box(String name) {
        String part = name.substring(0, name.indexOf("_"));
        return part.length() == 3 ? part : ("0" + part);
    }

    public void run(File folderImg, File folderXml, File out, File blackListFile, boolean delete) throws IOException {
        List<String> blackList = null;
        if (blackListFile != null) {
            blackList = FileUtil.readLines(blackListFile);
        }
        int maxCount = 0;
        File file = null;
        out.mkdirs();
        List<File> xmls = FileUtil.listFiles(folderXml, "xml", true);
        ObjectCounter<String> oc = new ObjectCounter<>();
//        for (File xml : xmls)
//            try {
//                String baseName = xml.getName().substring(0, xml.getName().lastIndexOf("."));
//                if (baseName.startsWith("JB_")) {
//                    baseName = baseName.substring(3);
//                }
//                if (blackList != null && blackList.contains(baseName)) {
//                    oc.add("XML blacklist");
//                    continue;
//                }
//                if (!patternBaseName.matcher(baseName).matches()) {
//                    LOG.info("skip file '{}' because pattern does not match for name {}.", xml, baseName);
//                    oc.add("XML pattern");
//                    continue;
//                }
//                XmlParserTei.Doc doc = XmlParserTei.loadXML(xml);
//                if (doc == null) {
//                    oc.add("XML load");
//                    continue;
//                }
//                XmlParserTei xmlParserTei = new XmlParserTei();
//                ArrayList<XmlParserTei.Page> pages = xmlParserTei.getPages(doc);
//                List<String> txt = new LinkedList<>();
//                for (XmlParserTei.Page page : pages) {
//                    txt.add(toOneString(page.getTargets()));
//                }
//                File folder = new File(new File(out, box(baseName)), baseName);
//                FileUtil.writeLines(new File(folder, folder.getName() + ".txt"), txt);
//                int cnt = txt.toString().length();
//                if (maxCount < cnt) {
//                    maxCount = cnt;
//                    file = (new File(folder, folder.getName() + ".txt"));
//                    System.out.println(file);
//                }
//                oc.add("XML OK");
//            } catch (RuntimeException ex) {
//                throw new RuntimeException("exception in parsing file '" + xml + "'", ex);
//            }
//        System.out.println(oc);
//        List<File> imgs = FileUtil.listFiles(folderImg, FileUtil.IMAGE_SUFFIXES, true);
//        LOG.warn("start Images!");
//        for (File img : imgs) {
//            String baseName = img.getName().substring(0, img.getName().lastIndexOf("."));
//            if (baseName.startsWith("JB_")) {
//                baseName = baseName.substring(3);
//            }
//            if (blackList != null && blackList.contains(baseName)) {
//                oc.add("IMG blacklist");
//                continue;
//            }
//            if (!patternBaseName.matcher(baseName).matches()) {
//                LOG.info("skip file '{}' because pattern does not match for name {}.", img, baseName);
//                oc.add("IMG pattern");
//                continue;
//            }
//            File folderOut = new File(out, box(baseName));
//            File folder = new File(folderOut, baseName);
//            File[] files = folder.listFiles();
//            if (files == null || files.length == 0) {
//                //does not exist or is empty
//                oc.add("IMG OK - no XML");
//                continue;
//            }
//            if (delete) {
//                FileUtils.moveFile(img, new File(folder, img.getName()));
//            } else {
//                FileUtil.copyFile(img, new File(folder, img.getName()));
//            }
//            oc.add("IMG OK");
//            System.out.println(oc);
//
//        }
        Collection<File> files = FileUtil.listFilesAndDirs(out, new AbstractFileFilter() {
            final Pattern p = Pattern.compile("[0-9]{2,3}_[0-9]{3}_[0-9]{3}");

            @Override
            public boolean accept(File file) {
                return p.matcher(file.getName()).matches();
            }
        }, TrueFileFilter.INSTANCE);
        final Pattern p = Pattern.compile("[0-9]{2,3}_[0-9]{3}_[0-9]{3}");
        files.removeIf(file1 -> !p.matcher(file1.getName()).matches() || file1.listFiles().length <= 1);
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
//        HomeDir.setPath("/home/gundram/old/");
        if (args.length == 0) {
            args = new String[]{"data/UCL_URO", "data/UCL_URO", "data/LA", "partitions/all.lst", "false"};
        }
        SetUpBenthamData instance = new SetUpBenthamData();
        File folderImg = HomeDir.getFile(args[0]);
        File folderXml = HomeDir.getFile(args[1]);
        File out = HomeDir.getFile(args[2]);
        File blackList = new File(args[3]);
        boolean delete = Boolean.parseBoolean(args[4]);
        instance.run(folderImg, folderXml, out, blackList, delete);
    }
}