/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.ArrayUtils;

import com.achteck.misc.log.Logger;

import de.uros.citlab.module.types.PageStruct;
import eu.transkribus.core.model.beans.customtags.CssSyntaxTag;
import eu.transkribus.core.model.beans.customtags.ReadingOrderTag;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextEquivType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.util.PageXmlUtils;

/**
 *
 * @author gundram
 */
public class PageXmlUtil {

    public static Logger LOG = Logger.getLogger(PageXmlUtil.class.getName());

    public static File getFirstFileWithSuffix(File baseDir, String filename, String[] suffixes, boolean caseSensitve) {
        if (suffixes == null || filename == null || baseDir == null || !baseDir.isDirectory()) {
            return null;
        }

        String bn = FilenameUtils.getBaseName(filename);

        for (File file : baseDir.listFiles()) {
            for (String s : suffixes) {
                String check = bn + "." + s;
                if (!caseSensitve) {
                    check = check.toLowerCase();
                }

                String filename2 = file.getName();
                if (!caseSensitve) {
                    filename2 = filename2.toLowerCase();
                }

                if (check.equals(filename2)) {
                    return file;
                }
            }
        }
        return null;
    }

    public static File getImagePath(File xmlFile, boolean forceExistance) {
        File fileSameFolder = getFirstFileWithSuffix(xmlFile.getParentFile(), xmlFile.getName(), FileUtil.IMAGE_SUFFIXES, false);
        if (fileSameFolder != null) {
            return fileSameFolder;
        }

        File fileParentFolder = getFirstFileWithSuffix(xmlFile.getParentFile().getParentFile(), xmlFile.getName(), FileUtil.IMAGE_SUFFIXES, false);
        if (fileParentFolder != null) {
            return fileParentFolder;
        }

//        for (int i = 0; i < suffixes.length; i++) {
//            File fileSameFolder = new File(xmlFile.getAbsolutePath().replace(".xml", "." + suffixes[i]));
//            if (fileSameFolder.exists()) {
//                return fileSameFolder;
//            }
//        }
//        for (int i = 0; i < suffixes.length; i++) {
//            File fileParentFolder = new File(xmlFile.getAbsoluteFile().getParentFile().getParentFile()
//                    + File.separator + xmlFile.getName().replace(".xml", "." + suffixes[i]));
//            if (fileParentFolder.exists()) {
//                return fileParentFolder;
//            }
//        }
        File parentFile = xmlFile.getAbsoluteFile().getParentFile().getParentFile();
        Iterator<File> iterateFiles = FileUtils.iterateFiles(parentFile, new SuffixFileFilter(FileUtil.IMAGE_SUFFIXES, IOCase.INSENSITIVE), FalseFileFilter.FALSE);
        File result = null;
        String prefix = xmlFile.getName();
        prefix = prefix.substring(0, prefix.lastIndexOf(".") + 1);
        while (iterateFiles.hasNext()) {
            File candidate = iterateFiles.next();
            if (candidate.getName().startsWith(prefix)) {
                if (result != null) {
                    throw new RuntimeException("found at least two possible matches for xml-file '" + xmlFile.getName() + "': '" + result.getAbsolutePath() + "' and '" + candidate.getAbsolutePath() + "'.");
                }
                result = candidate;
            }
        }
        if (result != null) {
            LOG.log(forceExistance ? Logger.WARN : Logger.DEBUG, "found image '" + result + "' for xml-file '" + xmlFile.getAbsolutePath() + "' - image has not a common suffix (" + Arrays.toString(FileUtil.IMAGE_SUFFIXES) + ")");
            return result;
        }
        if (forceExistance) {
            throw new RuntimeException("cannot find image for corresponding xml-file'" + xmlFile + "'.");
        }
        return null;
    }

    public static PcGtsType unmarshal(File file) {
        try {
            return PageXmlUtils.unmarshal(file);
        } catch (JAXBException ex) {
            throw new RuntimeException("cannot load file '" + file == null ? "null" : file.getAbsolutePath() + "'.", ex);
        }
    }

    public static void marshal(PcGtsType page, File file) {
        try {
            PageXmlUtils.marshalToFile(page, file);
        } catch (JAXBException | IOException ex) {
            throw new RuntimeException("cannot save file '" + file == null ? "null" : file.getAbsolutePath() + "'.", ex);
        }
    }

    public static File getXmlPath(File imgFile) {
        return getXmlPath(imgFile, true);
    }

    /**
     * Deletes all custom tags. If specific tags should not be deleted, their
     * names can be given as argument list to keepTags
     *
     * @see eu.transkribus.core.model.beans.customtags.CustomTag
     * @see eu.transkribus.core.model.beans.customtags.ReadingOrderTag#TAG_NAME
     * @param tlt
     * @param keepTags
     */
    public static void deleteCustomTags(TextLineType tlt, String... keepTags) {
        String custom = tlt.getCustom();
        if (custom == null || custom.isEmpty()) {
            return;
        }
        List<CssSyntaxTag> parseTags = CssSyntaxTag.parseTags(custom);
        for (int i = parseTags.size() - 1; i >= 0; i--) {
            CssSyntaxTag tag = parseTags.get(i);
            if (ArrayUtils.indexOf(keepTags, tag.getTagName()) >= 0) {
                continue;
            }
            parseTags.remove(i);
        }
        tlt.setCustom(CssSyntaxTag.getCssString(parseTags));
    }

    public static File getXmlPath(File imgFile, boolean forceExistance) {
        String absolutePath = imgFile.getPath();
        absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("."));
        File fileSameFolder = new File(absolutePath + ".xml");
        if (fileSameFolder.exists() && fileSameFolder.canRead()) {
            return fileSameFolder;
        }
        File parentFile = new File(imgFile.getAbsoluteFile().getParentFile(), "page/");
        if (!parentFile.exists() && forceExistance) {
            throw new RuntimeException("cannot find folder 'page/' for image '" + imgFile.getAbsolutePath() + "'.");
        }
        File file = new File(parentFile, fileSameFolder.getName());
        if (forceExistance && (!file.exists() || !file.canRead())) {
            throw new RuntimeException("cannot find xml-file for image '" + imgFile.getAbsolutePath() + "'.");
        }
        return file;
    }

    public static void copyTextEquivLine2Region(PcGtsType xmlFile) {
        for (TextRegionType textRegion : PageXmlUtils.getTextRegions(xmlFile)) {
            List<TextLineType> linesInRegion1 = textRegion.getTextLine();
            StringBuilder sb = new StringBuilder();
            sb.append("\n");
            for (TextLineType textLineType : linesInRegion1) {
                TextEquivType textEquiv = textLineType.getTextEquiv();
                if (textEquiv != null) {
                    sb.append(textEquiv.getUnicode()).append("\n");
                }
            }
            TextEquivType textEquiv = textRegion.getTextEquiv();
            if (textEquiv == null) {
                textEquiv = new TextEquivType();
                textRegion.setTextEquiv(textEquiv);
            }
            String ref = sb.substring(1);
            if (!ref.isEmpty()) {
                ref = ref.substring(0, ref.length() - 1);
            }
            textEquiv.setUnicode(ref);
        }

    }

    /**
     *
     * @param tlt
     * @return the VISUAL order from left to right and null if no textEquiv
     * given
     */
    public static String getTextEquiv(TextLineType tlt) {
        TextEquivType textEquiv = tlt.getTextEquiv();
        if (textEquiv == null) {
            return null;
        }
        String unicode = textEquiv.getUnicode();
        unicode = unicode.replace("\n", "");
        return BidiUtil.logical2visual(unicode);
    }

    public static void setTextEquiv(TextLineType tlt, String unicode, Double... confidences) {
        if (unicode == null) {
            tlt.setTextEquiv(null);
            deleteCustomTags(tlt, ReadingOrderTag.TAG_NAME);
            return;
        }
        TextEquivType textEquiv = tlt.getTextEquiv();
        if (textEquiv == null) {
            textEquiv = new TextEquivType();
            tlt.setTextEquiv(textEquiv);
        } else {
            deleteCustomTags(tlt, ReadingOrderTag.TAG_NAME);
            tlt.getWord().clear();
        }
        textEquiv.setUnicode(BidiUtil.visual2logical(unicode));
        textEquiv.setPlainText(null);
        if (confidences != null && confidences.length > 0) {
            if (confidences.length > 1) {
                throw new RuntimeException("more than one cost values given to set as confidence.");
            }
            textEquiv.setConf(new Float(confidences[0]));
        } else {
            textEquiv.setConf(null);
        }
    }

    public static List<TextLineType> getTextLines(PcGtsType page) {
        List<TextLineType> res = new LinkedList<>();
        List<TextRegionType> trt = PageXmlUtils.getTextRegions(page);
        for (TextRegionType textRegionType : trt) {
            res.addAll(textRegionType.getTextLine());
        }
        return res;
    }

    public static List<String> getText(PcGtsType page) {
        List<String> res = new LinkedList<>();
        List<TextRegionType> trt = PageXmlUtils.getTextRegions(page);
        for (TextRegionType textRegionType : trt) {
            for (TextLineType textLineType : textRegionType.getTextLine()) {
                String textEquiv = PageXmlUtil.getTextEquiv(textLineType);
                if (textEquiv != null) {
                    res.add(textEquiv);
                }
            }
        }
        return res;
    }

    public static List<PageStruct> getPages(String[] images, String[] xmls) {
        List<PageStruct> res = new LinkedList<>();
        boolean imgValid = images != null;
        boolean xmlValid = xmls != null;
        if (!imgValid && !xmlValid) {
            throw new RuntimeException("both lists are null");
        }
        if (xmlValid && imgValid && images.length != xmls.length) {
            throw new RuntimeException("number of images (" + images.length + ") and number of xmls (" + xmls.length + ") have to be equal.");
        }
        int len = imgValid ? images.length : xmls.length;
        for (int i = 0; i < len; i++) {
            res.add(new PageStruct(xmlValid ? new File(xmls[i]) : null, imgValid ? new File(images[i]) : null));
        }
        return res;
    }

}
