/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.uros.citlab.module.util.FileUtil;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author gundram
 */
public class CreateGroundTruthStaZh extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(CreateGroundTruthStaZh.class.getName());
    private String i_txt = "data/StaZh_FirstTestCollection/Testdaten_TKR/Texte_Word/";
    private String i_img = "data/StaZh_FirstTestCollection/Testdaten_TKR/Bilder_PDF/";
    private String o_xml = "data/StaZh_FirstTestCollection/Testdaten_TKR/T2I/";

    public CreateGroundTruthStaZh() {
        addReflection(this, CreateGroundTruthStaZh.class);
    }

    private void prepareCollections() {
        List<File> filesTxt = FileUtil.listFiles(HomeDir.getFile(i_txt), "txt".split(" "), true);
        List<File> filesImg = FileUtil.listFiles(HomeDir.getFile(i_img), "jpg".split(" "), true);
        HashMap<String, Set<File>> nameMap = new HashMap<>();
        for (File file : filesImg) {
            String name = file.getName();
            String base = name.substring(0, name.lastIndexOf(".jpg") - 5);
            LOG.log(Logger.DEBUG, "found file '" + name + "'");
            LOG.log(Logger.DEBUG, "found basename '" + base + "'");
            Set<File> set = nameMap.get(base);
            if (set == null) {
                set = new LinkedHashSet<>();
                nameMap.put(base, set);
            }
            set.add(file);
        }
        File folderOut = HomeDir.getFile(o_xml);
        if (folderOut.mkdirs()) {
            LOG.log(Logger.DEBUG, "create folder " + folderOut);
        }
        for (File fileTxt : filesTxt) {
            String name = fileTxt.getName();
            LOG.log(Logger.DEBUG, "found file '" + name + "'");
            String base = name.substring(0, name.lastIndexOf(".txt"));
            LOG.log(Logger.DEBUG, "found basename '" + base + "'");
            Set<File> imgList = nameMap.get(base);
            if (imgList == null) {
                throw new RuntimeException("cannot find images for file " + fileTxt);
            }
            File folderCollection = new File(folderOut, base);
            folderCollection.mkdirs();
            FileUtil.copyFile(fileTxt, new File(folderCollection, fileTxt.getName()));
            for (File fileImg : imgList) {
                FileUtil.copyFile(fileImg, new File(folderCollection, fileImg.getName()));
            }
        }

    }

    private void prepareGT() throws IOException {
        List<File> listFiles = FileUtil.listFiles(HomeDir.getFile(i_txt), "txt".split(" "), true);
        for (File listFile : listFiles) {
            LOG.log(Logger.INFO, "##########################################" + listFile.getName());
            List<String> in;
            try (FileReader fs = new FileReader(listFile)) {
                in = IOUtils.readLines(fs);
            }
            List<String> out = new LinkedList<>();
            boolean start = false;
            boolean end = false;
            for (String line : in) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                Pattern p = Pattern.compile("(//[   ]{1,4})?\\[p\\.[   ]+[0-9]{1,2}\\]");
                boolean found = p.matcher(line).matches();
                String[] split = p.split(line);
                if (found || split.length > 1) {
                    start = true;
                }
                if (line.startsWith("[Transkript:")) {
                    end = true;
                }
                if (start && !end) {
//                    System.out.println((split.length > 1) + "=> " + line);
                    for (String string : split) {
                        string = string.trim();
                        if (!string.isEmpty()) {
//                            System.out.println(string);
                            out.add(string);
                        }
                    }
                }
            }
            for (String string : out) {
                LOG.log(Logger.INFO, string);
            }
            if (out.isEmpty()) {
                for (String string : in) {
                    LOG.log(Logger.INFO, "###" + string);
                }
                throw new RuntimeException("did not find ground truth for file " + listFile.getPath());
            }
            LOG.log(Logger.INFO, "##########################################" + listFile.getName());
            try (FileWriter fw = new FileWriter(listFile)) {
                IOUtils.writeLines(out, null, fw);
            }
        }
    }

    public void run() throws IOException {
        prepareCollections();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidParameterException, IOException {
        CreateGroundTruthStaZh instance = new CreateGroundTruthStaZh();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
