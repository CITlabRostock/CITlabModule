/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.xml;

import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.planet.imaging.types.HybridImage;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.util.PolygonUtil;
import de.uros.citlab.module.util.XmlUtil;
import eu.transkribus.core.model.beans.pagecontent.PageType;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextEquivType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextRegionType;
import eu.transkribus.core.util.PageXmlUtils;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.util.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author gundram
 */
public class XmlParserEscherTest extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(XmlParserEscherTest.class.getName());

    @ParamAnnotation(descr = "folder with xml-files")
    private String xml = "";

    @ParamAnnotation(descr = "folder with images")
    private String img = "";

    @ParamAnnotation(descr = "folder to save page-sturcture")
    private String out = "";

    public XmlParserEscherTest() {
        addReflection(this, XmlParserEscherTest.class);
    }

    private StringBuilder parseString(Node node, StringBuilder sb) throws IllegalArgumentException {
        switch (node.getNodeName()) {
            case "#text":
                sb.append(node.getTextContent());
                return sb;
            //go deeper in following tags
            case "name"://name
            case "line"://zeile
            case "date"://datum
            case "hi"://überschrift
            case "sup"://kleinunten
            case "metamark"://marks to additional noted
                NodeList childNodes = node.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    sb = parseString(childNodes.item(i), sb);
                    if (sb == null) {
                        return null;
                    }
                }
                return sb;
            //delete following tags
            case "add"://add additional word - probably small an superscript
            case "del"://strike outs..
                return sb;
            case "unclear"://unclear transcription
            case "table"://table - it is parse-able but needs additional work TODO
                return null;
            default:
                throw new RuntimeException("unknown type '" + node.getNodeName() + "'.");
        }
    }

    private Object getNamedItem(String key) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static class Page {

        private HybridImage imgInner;
        private String imgPath;
        private List<Polygon2DInt> polygons = new LinkedList<>();
        private List<String> references = new LinkedList<>();
        private String name;
        private String author;
        private String year;
        private File folderOut;

        public Page(File folderOut, File urlSrc, String name, String author, String year) {
            this.imgPath = urlSrc.getAbsolutePath();
            this.name = name;
            this.author = author;
            this.year = year;
            this.folderOut = folderOut;
        }

        public void addLine(Polygon2DInt p, String reference) {
            polygons.add(p);
            references.add(reference);
        }

        public HybridImage getImg() {
            if (imgInner == null) {
                imgInner = HybridImage.newInstance(imgPath);
            }
            return imgInner;
        }

        public void clear() {
            if (imgInner != null) {
                imgInner.clear(false);
            }
        }

        public List<Polygon2DInt> getPolygons() {
            return polygons;
        }

        public List<String> getReferences() {
            return references;
        }

        public void save(boolean debugImg, boolean img, boolean xml, boolean txt) throws IOException {
            if (debugImg && img) {
                throw new RuntimeException("only img or debugImg can be true");
            }
            PcGtsType PcGts;
            PcGts = PageXmlUtils.createEmptyPcGtsType(name, getImg().getWidth(), getImg().getHeight());
            PageType page = PcGts.getPage();
            TrpTextRegionType region = new TrpTextRegionType();
            region.setId("r1");
            region.setCoords(PolygonUtil.polyon2Coords(new Polygon(new int[]{0}, new int[]{0}, 1)));
            List<TextLineType> linesTextList = region.getTextLine();
            for (int i = 0; i < polygons.size(); i++) {
                Polygon2DInt p = polygons.get(i);
                String refString = references.get(i).replace("\n", "");
                TrpTextLineType line = new TrpTextLineType();
                line.setId("r1l" + i);
                line.setCoords(PolygonUtil.polyon2Coords(p));
                PageXmlUtil.setTextEquiv(line, refString);
                linesTextList.add(line);
            }
            page.getTextRegionOrImageRegionOrLineDrawingRegion().add(region);
            File folderOut = new File(this.folderOut + File.separator + author.replace(" ", "_").replaceAll("[^a-zA-Z_]", "") + File.separator + year + File.separator);
            File imgName = new File(folderOut, name);
            if (img) {
                FileUtils.copyFile(new File(imgPath), new File(folderOut, name));
            }
            if (debugImg) {
                BufferedImage asBufferedImage = getImg().copy().getAsBufferedImage();
                ImageUtil.printPolygons(asBufferedImage, PcGts, false, false, true);
                HybridImage.newInstance(asBufferedImage).save(new File(folderOut, name).getAbsolutePath());
            }
            if (txt) {
                List<String> lines = new LinkedList<>();
                for (TextLineType textLineType : linesTextList) {
                    TextEquivType textEquiv = textLineType.getTextEquiv();
                    if (textEquiv != null) {
                        lines.add(textEquiv.getUnicode());
                    }
                }
                IOUtils.writeLines(lines, null, new FileOutputStream(new File(folderOut, name.replaceAll("\\.[a-zA-Z]{3}$", ".txt"))));
            }
            if (xml) {
                try {
                    File file = PageXmlUtil.getXmlPath(imgName, false);
                    file.getParentFile().mkdirs();
                    PageXmlUtils.marshalToFile(PcGts, file);
                } catch (JAXBException ex) {
                    java.util.logging.Logger.getLogger(XmlParserEscherTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
    }

    private static Pair<Polygon2DInt, String> getLineAndImageName(Node line) {
        NamedNodeMap attributes = line.getAttributes();
        int p1 = 0, p2 = 0, p3 = 0, p4 = 0;
        String img = "";
//                String txt = "";
        for (int j = 0; j < attributes.getLength(); j++) {
            Node att = attributes.item(j);
            switch (att.getNodeName()) {
                case "ulx":
                    p1 = Integer.parseInt(att.getNodeValue());
                    break;
                case "uly":
                    p3 = Integer.parseInt(att.getNodeValue());
                    break;
                case "lrx":
                    p2 = Integer.parseInt(att.getNodeValue());
                    break;
                case "lry":
                    p4 = Integer.parseInt(att.getNodeValue());
                    break;
                case "url":
                    img = att.getNodeValue();
                    break;
                default:
                    throw new RuntimeException("cannot interprete value '" + att.getNodeName() + "'.");
            }
        }
        return new Pair<>(new Polygon2DInt(new int[]{p1, p1, p2, p2}, new int[]{p3, p4, p4, p3}, 4), img);
    }

    public String getFrom(Document doc) {
        NodeList froms = doc.getElementsByTagName("from");
        if (froms.getLength() != 1) {
            throw new RuntimeException("more than one 'from' in xml-file.");
        }
        Node from = froms.item(0);
        List<Node> names = XmlUtil.getChildren(from, "name");
        if (names.isEmpty()) {
            return from.getFirstChild().getTextContent();
        }
        Node name = names.get(0);
        NamedNodeMap attributes = name.getAttributes();
        Node namedItem = attributes.getNamedItem("key");
        return namedItem.getNodeValue() + (names.size() > 1 ? " et al" : "");
    }

    public String getYear(Document doc) {
        Node letterdata = doc.getElementsByTagName("letterdata").item(0);
        List<Node> children = XmlUtil.getChildren(letterdata, "date");
        if (children.isEmpty()) {
            return "XXXX";
        }
        Node date = children.get(0);
        Node year = XmlUtil.getChild(date, "year");
        if (year == null || !year.hasChildNodes()) {
            return "XXXX";
        }
        Node child = year.getFirstChild();
        if (child.getNodeName().equals("#text") && child.getTextContent().length() > 3) {
            String textContent = child.getTextContent();
            try {
                Integer.parseInt(textContent);
            } catch (NumberFormatException ex) {
                LOG.log(Logger.WARN, "cannot interprete '" + textContent + "' as year");
                return "XXXX";
            }
            return child.getTextContent();
        }
        Node child1 = XmlUtil.getChild(year, "unclear");
        if (child1 == null) {
            return "XXXX";
        }
        String textContent = child1.getTextContent();
        try {
            Integer.parseInt(textContent);
        } catch (NumberFormatException ex) {
            LOG.log(Logger.WARN, "cannot interprete '" + textContent + "' as year");
            return "XXXX";
        }
        return child1.getTextContent();

    }

    public void run() throws IOException {
        File folderXML = new File(xml);
        Iterator<File> iterateFiles = FileUtils.iterateFiles(folderXML, "xml".split(" "), true);
        int id = 1;
        while (iterateFiles.hasNext()) {
            File fxml = iterateFiles.next();
            File fimg = new File(img);
            HashMap<String, Page> imgs = new LinkedHashMap<>();
//            HashMap<String, List<Polygon2DInt>> polys = new LinkedHashMap<>()
            System.out.println("processing " + (id++) + ":" + fxml);
            XmlUtil.Doc loadXML = XmlUtil.loadXML(fxml.getAbsolutePath());
            Document document = loadXML.getDocument();
            String from = getFrom(document);
            String year = getYear(document);
            System.out.println(from);
            NodeList elementsByTagName = document.getElementsByTagName("line");
            for (int i = 0; i < elementsByTagName.getLength(); i++) {
                Node line = elementsByTagName.item(i);
                StringBuilder sb = new StringBuilder();
                try {
                    sb = parseString(line, sb);
                } catch (Exception ex) {
                    throw new RuntimeException("error in image xml-file " + fxml.getPath(), ex);
                }

                if (sb != null) {
                    Pair<Polygon2DInt, String> lineAndImageName = getLineAndImageName(line);
                    String img = lineAndImageName.getSecond();
                    if (!imgs.containsKey(img)) {
                        imgs.put(img, new Page(new File(out), new File(fimg, img), img, from, year));
                    }
                    imgs.get(img).addLine(lineAndImageName.getFirst(), sb.toString().trim());
                }
            }
            for (String imgName : imgs.keySet()) {
                Page page = imgs.get(imgName);
                page.save(false, true, true, true);
                System.out.println(page.name);
                page.clear();
            }
        }
    }

    /**
     * @param args the command line arguments
     * @throws com.achteck.misc.exception.InvalidParameterException
     */
    public static void main(String[] args) throws Exception {
        ArgumentLine al = new ArgumentLine();
        al.addArgument("out", "data/");
//        al.addArgument("xml", "/home/gundram/devel/projects/read/data/alfred_escher/Alfred_Escher_Dataset/tmp/");
        al.addArgument("xml", "Alfred_Escher_Dataset/letters/");
//        al.addArgument("img", "/home/gundram/devel/projects/read/data/alfred_escher/Alfred_Escher_Dataset/tmp/");
        al.addArgument("img", "Alfred_Escher_Dataset/images/");
        args = al.getArgs();
        XmlParserEscherTest instance = new XmlParserEscherTest();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
