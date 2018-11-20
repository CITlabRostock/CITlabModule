/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import com.achteck.misc.log.Logger;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author gundram
 */
public class XmlUtil implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(XmlUtil.class.getName());

    public static Doc loadXML(String filename) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Doc document = new Doc(builder.parse(filename));
            return document;
        } catch (ParserConfigurationException | SAXException ex) {
            throw new RuntimeException("unexpected syntax error.", ex);
        } catch (IOException ex) {
            throw new RuntimeException("cannot find or open file", ex);
        }
    }

    public static void saveXML(Doc dokument, String filename) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(dokument.getDocument());
            StreamResult streamResult = new StreamResult(new File(filename));
            transformer.transform(source, streamResult);
        } catch (TransformerException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Node getChild(Node parent, String name) {
        NodeList childNodes = parent.getChildNodes();
        Node res = null;
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeName().equals(name)) {
                if (res != null) {
                    throw new RuntimeException("there are more than one child with this name");
                }
                res = child;
            }
        }
        return res;
    }

    public static List<Node> getChildren(Node parent, String name) {
        List<Node> res = new LinkedList<>();
        NodeList childNodes = parent.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeName().equals(name)) {
                res.add(child);
            }
        }
        return res;
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

}
