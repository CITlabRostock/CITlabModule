package de.uros.citlab.module.util;

import com.achteck.misc.types.CharMap;
import com.achteck.misc.types.ConfMat;
import de.planet.citech.trainer.loader.IImageLoader;
import de.planet.imaging.types.HybridImage;
import de.planet.imaging.types.SubImage;
import de.planet.util.LoaderIO;
import de.planet.util.types.XmlParser;
import de.uros.citlab.module.workflow.HomeDir;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class SetUpRimes {
    public final String dir_img;
    public final File dir_out;
    public final File path_xml;
    public final File set_lst;
    public final File charMapFile;

    public SetUpRimes(String dir_img, File dir_out, File path_xml, File set_lst) {
        this(dir_img, dir_out, path_xml, set_lst, null);
    }

    public SetUpRimes(String dir_img, File dir_out, File path_xml, File set_lst, File charMapFile) {
        this.charMapFile = charMapFile;
        this.dir_img = dir_img;
        this.dir_out = dir_out;
        this.path_xml = path_xml;
        this.set_lst = set_lst;
    }

    public void run() {
        Document loadXML = XmlParser.loadXML(path_xml.getAbsolutePath());
        NodeList pages = loadXML.getElementsByTagName("SinglePage");
        List<String> strings = FileUtil.readLines(set_lst);
        HashSet<String> pageTrue = new LinkedHashSet<>();
        for (String str : strings) {
            pageTrue.add(str.substring(0, str.lastIndexOf("-")));
        }
        int idxSkips = 0;
        int cntCorrect = 0;
        HashSet<Character> chars = new HashSet<>();
        for (int idxPages = 0; idxPages < pages.getLength(); idxPages++) {
            Node page = pages.item(idxPages);
            String filename = page.getAttributes().getNamedItem("FileName").getNodeValue();
            String substring = filename.substring(filename.lastIndexOf("/") + 1);
            substring = substring.substring(0, substring.lastIndexOf("."));
            if (!pageTrue.contains(substring)) {
                System.out.println("skip " + filename);
                idxSkips++;
                continue;
            }
            HybridImage imagePage = HybridImage.newInstance(HomeDir.getFile(filename.replace("images/", dir_img)));
            Rectangle imageBB = new Rectangle(imagePage.getWidth(), imagePage.getHeight());
//            System.out.println(filename);
            Node paragraph = XmlParser.getChild(page, "Paragraph");
            NodeList childNodes = paragraph.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node line = childNodes.item(i);
                if (line.getNodeName().equals("Line")) {
                    NamedNodeMap attributes = line.getAttributes();
                    int top = Integer.parseInt(attributes.getNamedItem("Top").getNodeValue());
                    int bottom = Integer.parseInt(attributes.getNamedItem("Bottom").getNodeValue());
                    int left = Integer.parseInt(attributes.getNamedItem("Left").getNodeValue());
                    int right = Integer.parseInt(attributes.getNamedItem("Right").getNodeValue());
                    Rectangle r = new Rectangle(left, top, right - left + 1, bottom - top + 1);
                    r = r.intersection(imageBB);
                    HybridImage imageLine = HybridImage.newInstance(SubImage.get(imagePage.getAsByteImage(), r.x, r.y, r.width, r.height));
                    String ref = attributes.getNamedItem("Value").getNodeValue();
//                    System.out.println(ref);
                    String imageName = new File(dir_out, filename.replace(".png", "_" + i + ".jpg")).getAbsolutePath();
                    HashMap<String, Object> info = new HashMap<>();
                    info.put("FileName", filename);
                    info.put("LineNumber", i);
                    info.put("BoundingBox", r);
                    LoaderIO.saveImageHolder(imageName, new IImageLoader.ImageHolderDft(imageLine, ref, info));
                    if (charMapFile != null) {
                        for (char c : ref.toCharArray()) {
                            chars.add(c);
                        }
                    }
                    cntCorrect++;
                }
            }
        }
        if (charMapFile != null) {
            CharMap<Integer> charMap = new CharMap<>();
            List<Character> charList = new ArrayList<>(chars);
            charList.sort((o1, o2) -> Character.compare(o1, o2));
            charMap.put(charMap.keySet().size(), ConfMat.NaC);
            for (int i = 0; i < charList.size(); i++) {
                charMap.put(charMap.keySet().size(), charList.get(i));
            }
            CharMapUtil.saveCharMap(charMap, charMapFile);
        }
        System.out.println(this.path_xml + " " + this.set_lst);
        System.out.println("skip " + idxSkips + " pages");
        System.out.println("found " + cntCorrect + " lines");
    }

    public static void main(String[] args) {
        HomeDir.setPath("/home/gundram/devel/projects/rimes/data");
        new SetUpRimes(
                "images_gray/",
                HomeDir.getFile("vol_test"),
                HomeDir.getFile("eval_2011_annotated.xml"),
                HomeDir.getFile("lists/te.lst")
        ).run();
        File trainXml = HomeDir.getFile("training_2011.patched.xml");
        if (!trainXml.exists()) {
            throw new RuntimeException(" no patch applied, run 'patch training_2011.xml training_2011.patch -o training_2011.patched.xml'");
        }
        new SetUpRimes(
                "images_gray/",
                HomeDir.getFile("vol_train"),
                trainXml,
                HomeDir.getFile("lists/tr.lst"),
                HomeDir.getFile("cm_rimes.txt")
        ).run();
        new SetUpRimes(
                "images_gray/",
                HomeDir.getFile("vol_val"),
                trainXml,
                HomeDir.getFile("lists/va.lst")
        ).run();
    }


}
