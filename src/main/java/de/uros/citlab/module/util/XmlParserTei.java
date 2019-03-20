/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import de.uros.citlab.module.workflow.HomeDir;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * @author gundram
 */
public class XmlParserTei implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(XmlParserTei.class.getName());
    private Decision tagDel = Decision.CONTINUE;
    private Decision tagAdd = Decision.SEPARATE;
    private Decision tagSic = Decision.CONTINUE;
    private Decision tagUnclear = Decision.INVALID;
    private Decision tagForeign = Decision.CONTINUE;
    private Decision taghi = Decision.UNKNOWN; //any other special case (except underline and superscript)
    private Decision taghi_superscript = Decision.CONTINUE;//superscirpt words
    private Decision taghi_underline = Decision.CONTINUE;//underline words
    private Decision taghi_italic = Decision.CONTINUE;//also underline or kursive words

    public enum Decision {
        /**
         * If tag occures, the children will be used as transcription
         */
        CONTINUE,
        /**
         * if tag occures, the line, where this tag occures, will not be used
         */
        INVALID,
        /**
         * the text in this tag will be interpreted as seperate line
         */
        SEPARATE,
        /**
         * the text of this tag will not be used neither as seperate line or in the actual line
         */
        SKIP,
        /**
         * should be used for situations, which should not occure in your documents - throws an exception
         */
        UNKNOWN
    }

    private static class LineParser {

        StringBuilder sb = new StringBuilder();
        private boolean isValid = true;

        private LineParser() {
        }

        private String getLine() {
            return sb.toString().trim();
        }

        public void setValid(boolean isValid) {
            this.isValid = isValid;
        }

        public boolean isValid() {
            return isValid;
        }

        private void appendPart(Node node) {
            sb.append(node.getNodeValue().replace("\n", ""));
        }

        private void clear() {
            sb = new StringBuilder();
            isValid = true;
        }

        @Override
        public String toString() {
            return sb.toString();
        }

    }

    private static class PageNameHolder {

        Node page = null;

        private PageNameHolder() {
        }

        public void init(Node node) {
            this.page = node;
        }

        private String getPageName() {
            if (page == null) {
                return "???";
            }
            NamedNodeMap attributes = page.getAttributes();
            if (attributes.getNamedItem("facs") != null) {
                return attributes.getNamedItem("facts").getNodeValue();
            }
            if (attributes.getNamedItem("n") != null) {
                String nodeValue = attributes.getNamedItem("n").getNodeValue();
                try {
                    int val = Integer.parseInt(nodeValue);
                } catch (Exception ex) {
                    LOG.warn("cannot parse '{}' to pagenumber, which should be an integer", nodeValue);
                    throw new RuntimeException("cannot parse '" + nodeValue + "' to integer");
                }
                return nodeValue;
            }
            LOG.warn("cannot find identifier for pagebreak - return '???'");
            return "???";
        }

        @Override
        public String toString() {
            return getPageName() == null ? "?" : getPageName();
        }

    }

    private void linebreak(ArrayList<String> lines, LineParser line) {
        String l = line.getLine();
        if (!l.isEmpty() && line.isValid) {
            l = l.replaceAll(" +", " ");
            l = l.replaceAll("\t+", "\t");
            lines.add(l);
        }
        line.clear();
    }

    private void pagebreak(ArrayList<Page> pages, ArrayList<String> lines, LineParser line, PageNameHolder page, Node pageNode) {
        linebreak(lines, line);
        if (page.getPageName() != null) {
            pages.add(new Page(page.getPageName(), new ArrayList<>(lines)));
        }
        lines.clear();
        page.init(pageNode);
    }

    private void parseNodeNote(Node node, ArrayList<Page> pages, ArrayList<String> lines, LineParser line, PageNameHolder page) {
        NodeList childNodes = node.getChildNodes();
        LineParser line_note = new LineParser();
        for (int j = 0; j < childNodes.getLength(); j++) {
            parseNode(childNodes.item(j), pages, lines, line_note, page);
        }
        linebreak(lines, line_note);
    }

    private void parseNodeChoice(Node node, ArrayList<Page> pages, ArrayList<String> lines, LineParser line, PageNameHolder page) {
        Node child_important = null;
        NodeList childNodes = node.getChildNodes();
        for (int j = 0; j < childNodes.getLength(); j++) {
            Node child = childNodes.item(j);
            if (child.getNodeName().equals("sic") || child.getNodeName().equals("abbr") || child.getNodeName().equals("orig")) {
                if (child_important != null) {
                    LOG.error("found two important nodes: " + child_important + " and " + child + ". Delete whole choice.");
                    child_important = null;
                    j = childNodes.getLength();
                }
                child_important = child;
            }
        }
        if (child_important == null) {
            LOG.error("found no (or two) important node in " + node + ". ignore line completely.");
            line.setValid(false);
        } else {
            parseNode(child_important, pages, lines, line, page);
        }
    }

    private void parseNodeTagContiue(Node node, ArrayList<Page> pages, ArrayList<String> lines, LineParser line, PageNameHolder page) {
        if (node.hasChildNodes()) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                parseNode(children.item(i), pages, lines, line, page);
            }
        }
    }

    private void parseNode(Node node, ArrayList<Page> pages, ArrayList<String> lines, LineParser line, PageNameHolder page) {
        switch (node.getNodeName()) {
//zeile wird später ignoriert
            case "foreign":
                if (tagForeign == Decision.CONTINUE) {
                    parseNodeTagContiue(node, pages, lines, line, page);
                    break;

                } else if (tagForeign == Decision.INVALID) {
                    line.setValid(false);
                    parseNodeTagContiue(node, pages, lines, line, page);
                    break;
                }
                throw new RuntimeException("cannot interprete decision " + tagDel + " for tagDel.");
            case "g":
            case "fw":
                LOG.warn("tag " + node + " is not clear how to handle - ignore line.");
                line.setValid(false);
                parseNodeTagContiue(node, pages, lines, line, page);
                break;

            case "sic":
                if (tagSic == Decision.INVALID) {
                    line.setValid(false);
                    parseNodeTagContiue(node, pages, lines, line, page);
                    break;
                } else if (tagSic == Decision.SKIP) {
                    break;
                } else if (tagSic == Decision.CONTINUE) {
                    parseNodeTagContiue(node, pages, lines, line, page);
                    break;
                } else {
                    throw new RuntimeException("cannot interprete decision " + tagSic + " for tagSic.");
                }
                //els: handle like abbr and orig in choice
            case "abbr":
            case "orig":
                if (node.getParentNode().getNodeName().equals("choice")) {
                    parseNodeTagContiue(node, pages, lines, line, page);
                } else {
                    LOG.warn("ignore tag " + node + " for page " + page.getPageName() + ", because parent is not choice, ignore line.");
                    line.setValid(false);
                }
                break;
            case "hi":
                Node rendNode = node.getAttributes().getNamedItem("rend");
                if (rendNode == null) {
                    if (taghi == Decision.CONTINUE) {
                        parseNodeTagContiue(node, pages, lines, line, page);
                        break;
                    } else if (taghi == Decision.INVALID) {
                        line.setValid(false);
                        parseNodeTagContiue(node, pages, lines, line, page);
                    }
                    throw new RuntimeException("cannot interprete decision " + taghi + " for taghi.");
                }
                String rend = rendNode.getTextContent();
                if (rend.startsWith("superscr")) {
                    if (taghi_superscript == Decision.CONTINUE) {
                        parseNodeTagContiue(node, pages, lines, line, page);
                        break;
                    } else if (taghi_superscript == Decision.INVALID) {
                        line.setValid(false);
                        parseNodeTagContiue(node, pages, lines, line, page);
                        break;
                    } else if (taghi_superscript == Decision.SKIP) {
                        break;
                    }
                    throw new RuntimeException("cannot interprete decision " + taghi_superscript + " for taghi_superscript.");
                }
                if (rend.startsWith("italic")) {
                    if (taghi_italic == Decision.CONTINUE) {
                        parseNodeTagContiue(node, pages, lines, line, page);
                        break;
                    } else if (taghi_italic == Decision.INVALID) {
                        line.setValid(false);
                        parseNodeTagContiue(node, pages, lines, line, page);
                        break;
                    } else if (taghi_italic == Decision.SKIP) {
                        break;
                    }
                    throw new RuntimeException("cannot interprete decision " + taghi_italic + " for taghi_superscript.");
                }
                if (rend.startsWith("underlin")) {
                    if (taghi_underline == Decision.CONTINUE) {
                        parseNodeTagContiue(node, pages, lines, line, page);
                        break;
                    } else if (taghi_underline == Decision.INVALID) {
                        line.setValid(false);
                        parseNodeTagContiue(node, pages, lines, line, page);
                        break;
                    } else if (taghi_underline == Decision.SKIP) {
                        break;
                    }
                    throw new RuntimeException("cannot interprete decision " + taghi_underline + " for taghi_underline.");
                }
                throw new RuntimeException("cannot interprete attribute '" + rend + "' in tag <hi>.");
            case "gap":
                line.setValid(false);
                parseNodeTagContiue(node, pages, lines, line, page);
                break;
            case "front":
            case "body":
            case "back":
            case "div":
            case "milestone":
            case "lg":
            case "l":
            case "quote":
            case "cit":
            case "argument":
            case "sp":
            case "speaker":
            case "listBibl":
            case "bibl":
            case "titlePage":
            case "epigraph":
            case "closer":
            case "salute":
            case "dateline":
            case "postscript":
            case "trailer":
            case "opener":
            case "titlePart":
            case "byline":
            case "imprimatur":
            case "docTitle":
            case "docImprint":
            case "docAuthor":
            case "pubPlace":
            case "docDate":
            case "docEdition":
            case "name":
            case "spGrp":
            case "date":
            case "actor":
            case "placeName":
            case "surname":
            case "forename":
            case "persName":
            case "publisher":
            case "floatingText":
            case "cb":
            case "space":
            case "signed":
            case "ref":
            case "castList":
            case "castItem":
            case "role":
            case "stage":
            case "castGroup":
            case "roleDesc":
            case "text":
            case "orgName":
                //skip node, does not contain information- but children maybe
                parseNodeTagContiue(node, pages, lines, line, page);
                break;
            case "teiHeader":
            case "#comment":
            case "supplied":
            case "list":
            case "figure":
            case "table":
            case "formula":
                //skip node and its context, because the context is irrelevant
                break;
            case "choice":
                parseNodeChoice(node, pages, lines, line, page);
                break;
            case "note":
                parseNodeNote(node, pages, lines, line, page);
                break;
            case "pb": {
                pagebreak(pages, lines, line, page, node);
                break;
            }
            case "lb": {
                linebreak(lines, line);
                break;
            }
            case "head":
            case "p": {
                linebreak(lines, line);
                parseNodeTagContiue(node, pages, lines, line, page);
                linebreak(lines, line);
                break;
            }
            case "#text": {
                line.appendPart(node);
                break;
            }
            case "del": {
                if (tagDel == Decision.INVALID) {
                    line.setValid(false);
                    parseNodeTagContiue(node, pages, lines, line, page);
                    break;
                } else if (tagDel == Decision.CONTINUE) {
                    parseNodeTagContiue(node, pages, lines, line, page);
                    break;
                } else if (tagDel == Decision.SKIP) {
                    break;
                }
                throw new RuntimeException("cannot interprete decision " + tagDel + " for tagDel.");
            }
            case "add": {
                if (tagAdd == Decision.SEPARATE) {
                    parseNodeNote(node, pages, lines, line, page);
                    break;
                } else if (tagAdd == Decision.CONTINUE) {
                    parseNodeTagContiue(node, pages, lines, line, page);
                    break;
                } else if (tagAdd == Decision.SKIP) {
                    break;
                } else if (tagAdd == Decision.INVALID) {
                    line.setValid(false);
                    parseNodeTagContiue(node, pages, lines, line, page);
                    break;
                }
                throw new RuntimeException("cannot interprete decision " + tagAdd + " for tagAdd.");
            }
            case "unclear": {
                if (tagUnclear == Decision.INVALID) {
                    line.setValid(false);
                    parseNodeTagContiue(node, pages, lines, line, page);
                    break;
                } else if (tagUnclear == Decision.CONTINUE) {
                    parseNodeTagContiue(node, pages, lines, line, page);
                    break;
                }
                throw new RuntimeException("cannot interprete decision " + tagUnclear + " for tagUnclear.");
            }
            default: {
                LOG.warn("ignore tag " + node + ", ignore line.");
                line.setValid(false);
                parseNodeTagContiue(node, pages, lines, line, page);
            }

        }
    }

    public static class Page implements Comparable<Page> {

        private final String page;
        private ArrayList<String> targets;

        @Override
        public int compareTo(Page o) {
            return page.compareTo(o.page);
        }

        public ArrayList<String> getTargets() {
            return targets;
        }

        public void setTargets(ArrayList<String> targets) {
            this.targets = targets;
        }

        public String getPage() {
            return page;
        }

        public Page(String page, ArrayList<String> targets) {
            try {
                int idx = Integer.parseInt(page);
                page = String.format("%04d", idx);
            } catch (RuntimeException ex) {
            }
            this.page = page;
            this.targets = targets;
        }
    }

    public ArrayList<Page> getPages(Doc document) {
        NodeList elementsByTagName = document.getDocument().getElementsByTagName("text");
        if (elementsByTagName.getLength() != 1) {
            LOG.warn("expect only one tag with name 'body'.");
//            throw new RuntimeException("expect only one tag with name 'body'.");
        }
        Node text = elementsByTagName.item(0);
        ArrayList<String> lines = new ArrayList<>();
        ArrayList<Page> pages = new ArrayList<>();
        {
            PageNameHolder page = new PageNameHolder();
            LineParser line = new LineParser();
            parseNode(text, pages, lines, line, page);
            pagebreak(pages, lines, line, page, null);
        }
        return pages;
    }

    public static Doc loadXML(File filename) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Doc document = new Doc(builder.parse(filename));
            return document;
        } catch (ParserConfigurationException | SAXException ex) {
            LOG.error("unexpected syntax error.", ex);
        } catch (IOException ex) {
            LOG.error("cannot find or open file", ex);
        }
        return null;
    }

    public static void saveXML(Doc dokument, File filename) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(dokument.getDocument());
            StreamResult streamResult = new StreamResult(filename);
            transformer.transform(source, streamResult);
        } catch (TransformerException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static class Doc {

        private final Document doc_intern;

        public Doc(Document doc_intern) {
            this.doc_intern = doc_intern;
        }

        public Document getDocument() {
            return doc_intern;
        }

    }

    public static void main(String[] args) {
        File out = HomeDir.getFile("data/StaZh/MM_2_254/MM_2_254_merged");
        List<File> folderXml = FileUtil.listFiles(HomeDir.getFile("data/StaZh/MM_2_254/MM_2_254_xml"), "xml".split(" "), true);
        List<File> folderImg = FileUtil.listFiles(HomeDir.getFile("data/StaZh/MM_2_254/MM_2_254_jpg"), "jpg".split(" "), false);
        for (int i = folderImg.size() - 1; i >= 0; i--) {
            if (!folderImg.get(i).isFile()) {
                folderImg.remove(i);
            }
        }
        Collections.sort(folderXml);
        HashMap<Integer, File> imgMap = new HashMap<>();
        for (File file : folderImg) {
            System.out.println(file.getName());
            String name = file.getName().split("(__)|( )")[0];
            System.out.println(name);
            int pageNo = Integer.parseInt(name);
            imgMap.put(pageNo, file);
        }
        LinkedList<Pair<String, String>> text = new LinkedList<>();
        for (File file : folderXml) {
            if (file.getParentFile().getName().equals("page")) {
                continue;
            }
            System.out.println("File: " + file.getAbsolutePath());
            Doc loadXML = XmlParserTei.loadXML(file);
            if (loadXML == null) {
                continue;
            }
            ArrayList<Page> pages = null;
            try {
                XmlParserTei parser = new XmlParserTei();
                pages = parser.getPages(loadXML);
            } catch (Throwable ex) {
                LOG.error("found error in xml " + file + " - skip xml.", ex);
                continue;
            }
            for (Page page : pages) {
                for (String target : page.targets) {
//                    if (page.getPage().equals("0045")) {
//                        LOG.info( "stop");
//                    }
                    text.add(new Pair<>(page.getPage(), target));
//                    System.out.println(target);
                }
            }
//            System.out.println("");
        }
//        for (int i = 0; i < text.size() - 1; i++) {
//            Pair<String, String> first = text.get(i);
//            Pair<String, String> second = text.get(i + 1);
//            if (1 == first.getFirst().compareTo(second.getFirst())) {
//                throw new RuntimeException("unexpected order on index " + first.getFirst());
//            }
//
//        }
        List<Pair<String, List<String>>> grouping = GroupUtil.getGrouping(text, new GroupUtil.Joiner<Pair<String, String>>() {
            @Override
            public boolean isGroup(List<Pair<String, String>> group, Pair<String, String> element) {
                int cmp = group.get(0).getFirst().compareTo(element.getFirst());
                if (cmp == 0) {
                    return true;
                } else if (cmp > 0) {
                    LOG.warn("wrong order on images: " + group.get(0).getFirst() + ", " + element.getFirst() + ".");
                    return true;
//                    throw new RuntimeException("wrong oder of files: " + group.get(0).getFirst());
                }
                return false;
            }

            @Override
            public boolean keepElement(Pair<String, String> element) {
                return true;
            }
        }, new GroupUtil.Mapper<Pair<String, String>, Pair<String, List<String>>>() {
            @Override
            public Pair<String, List<String>> map(List<Pair<String, String>> elements) {
                StringBuilder sb = new StringBuilder();
                String last = elements.get(0).getFirst();
                sb.append(last);
                for (int i = 1; i < elements.size(); i++) {
                    Pair<String, String> current = elements.get(i);
                    if (!current.getFirst().equals(last)) {
                        sb.append('_').append(current.getFirst());
                    }
                    last = current.getFirst();
                }
                List<String> res = new LinkedList<>();
                for (Pair<String, String> element : elements) {
                    res.add(element.getSecond());
                }
                return new Pair<>(sb.toString(), res);
            }
        });
        for (int i = 0; i < grouping.size(); i++) {
            Pair<String, List<String>> xml = grouping.get(i);
            String[] images = xml.getFirst().split("_");
            if (images.length > 1) {
                throw new RuntimeException("wrong order in files");
            }
            File subFolder = new File(out, xml.getFirst());
            subFolder.mkdirs();
            boolean isValid = true;
            for (String image : images) {
                int idx = Integer.parseInt(image);
                File imgOld = imgMap.get(idx);
                if (imgOld == null) {
                    LOG.error("cannot find image for index {}. skip record {}", idx, xml.getFirst());
                    isValid = false;
                }
            }
            if (!isValid) {
                continue;
            }
            for (String image : images) {
                int idx = Integer.parseInt(image);
                File imgOld = imgMap.get(idx);
                File imgNew = new File(subFolder, imgOld.getName());
                FileUtil.copyFile(imgOld, imgNew);
            }
            FileUtil.writeLines(new File(subFolder, "ref.txt"), xml.getSecond());
        }
    }
}
