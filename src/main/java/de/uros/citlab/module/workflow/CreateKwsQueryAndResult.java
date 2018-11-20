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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PolygonUtil;
import de.uros.citlab.errorrate.kws.KeywordExtractor;
import de.uros.citlab.errorrate.normalizer.StringNormalizerLetterNumber;
import de.uros.citlab.errorrate.types.KWS;
import de.uros.citlab.errorrate.util.ObjectCounter;
import eu.transkribus.interfaces.IStringNormalizer;
import eu.transkribus.interfaces.ITokenizer;
import de.uros.citlab.tokenizer.TokenizerCategorizer;
import de.uros.citlab.tokenizer.categorizer.CategorizerWordMergeGroups;
import de.uros.citlab.tokenizer.interfaces.ICategorizer;
import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gundram
 */
public class CreateKwsQueryAndResult extends ParamTreeOrganizer {

    private static Logger LOG = LoggerFactory.getLogger(CreateKwsQueryAndResult.class);
    @ParamAnnotation(descr = "folder with xml-files")
    private String xml_in;

    @ParamAnnotation(descr = "folder with image-files (if empty use index of xml-files")
    private String img_in;

    @ParamAnnotation(descr = "path to output groundtruth file")
    private String o;

    @ParamAnnotation(descr = "path to input query file (if empty, take all keywords from xml-file")
    private String q_in;

    @ParamAnnotation(descr = "path to output query file (if empty, no query file saved")
    private String q_out;

    @ParamAnnotation(descr = "minimal length of keyword")
    private int minlen;

    @ParamAnnotation(descr = "maximal length of keyword")
    private int maxlen;

    @ParamAnnotation(descr = "minimal occurance of keyword")
    private int minocc;

    @ParamAnnotation(descr = "maximal occurance of keyword (<0 for any)")
    private int maxocc;

    @ParamAnnotation(descr = "take only apha-channels (ignore numerical)")
    private boolean alpha;

    @ParamAnnotation(descr = "make everything upper")
    private boolean upper;

    @ParamAnnotation(descr = "find keyword also as part of longer words (substring)")
    private boolean part;
//    @ParamAnnotation(descr = "use index for images instead of absolute path")
//    private boolean index = false;

    @Override
    public void init() {
        super.init();
    }

    public CreateKwsQueryAndResult(String xml_in, String img_in, String o, String q_in, String q_out, int minlen, int maxlen, int minocc, int maxocc, boolean alpha, boolean upper, boolean part) {
        this.xml_in = xml_in;
        this.img_in = img_in;
        this.o = o;
        this.q_in = q_in;
        this.q_out = q_out;
        this.minlen = minlen;
        this.maxlen = maxlen;
        this.minocc = minocc;
        this.maxocc = maxocc;
        this.alpha = alpha;
        this.upper = upper;
        this.part = part;
        super.addReflection(this, CreateKwsQueryAndResult.class);
    }

    public CreateKwsQueryAndResult() {
        this("", "", "", "", "", 1, -1, 1, -1, false, false, false);
    }

    public boolean isPart() {
        return part;
    }

    public boolean isUpper() {
        return upper;
    }

    public String getImgIn() {
        return img_in;
    }

    public String getOutput() {
        return o;
    }

    public String getQuery() {
        return q_out;
    }

    final ICategorizer cat = new CategorizerWordMergeGroups();
    final ITokenizer tokIntern = new TokenizerCategorizer(cat);

    private final List<String> filter(String line) {
        List<String> resAll = tokIntern.tokenize(line);
        List<String> res = new LinkedList<>();
        if (alpha) {
            for (String string : resAll) {
                String category = cat.getCategory(string.charAt(0));
                if (category.equals("L")) {
                    res.add(upper ? string.toUpperCase() : string);
                }
            }
        } else {
            for (String string : resAll) {
                String category = cat.getCategory(string.charAt(0));
                if (category.equals("L") || category.equals("N")) {
                    res.add(upper ? string.toUpperCase() : string);
                }
            }
        }
        return res;
    }

    public void run() throws IOException {
        List<File> filesListsOrFolders = FileUtil.getFilesListsOrFolders(xml_in, "xml".split(" "), true);
        FileUtil.deleteMetadataAndMetsFiles(filesListsOrFolders);
        Collections.sort(filesListsOrFolders);
        String[] files = FileUtil.getStringList(filesListsOrFolders).toArray(new String[0]);
        LOG.debug("take {} xml-files.", filesListsOrFolders.size());
        String[] imgPathes = null;
        if (img_in != null && !img_in.isEmpty()) {
            List<File> filesImg = FileUtil.getFilesListsOrFolders(xml_in, FileUtil.IMAGE_SUFFIXES, true);
            Collections.sort(filesImg);
            imgPathes = FileUtil.asStringList(filesImg);
        }
        KeywordExtractor kwe = new KeywordExtractor(false, upper);
        KWS.GroundTruth keywordGroundTruth = null;
        KWS.GroundTruth keywordGroundTruthFinal = null;
        if (q_in.isEmpty()) {
            ITokenizer tok = new ITokenizer() {
                @Override
                public List<String> tokenize(String string) {
                    return filter(string);
                }
            };
            keywordGroundTruth = kwe.getKeywordGroundTruth(files, imgPathes, tok);
        } else {
            keywordGroundTruth = kwe.getKeywordGroundTruth(files, imgPathes, FileUtil.readLines(new File(q_in)));
        }
        ObjectCounter<String> kws = new ObjectCounter<>();
        for (KWS.Page page : keywordGroundTruth.getPages()) {
            for (KWS.Line line : page.getLines()) {
                for (Map.Entry<String, List<Polygon>> entry : line.getKeyword2Baseline().entrySet()) {
                    kws.add(entry.getKey(), entry.getValue().size());
                }
            }
        }
        LinkedList<String> queries = new LinkedList<>();
        long cntAll = 0;
        long cntQuery = 0;
        for (Pair<String, Long> pair : kws.getResultOccurrence()) {
            cntAll += pair.getSecond();
            String kw = pair.getFirst();
            if (kw.length() < minlen) {
                continue;
            }
            if (maxlen > 0 && kw.length() > maxlen) {
                continue;
            }
            if (maxocc >= 0 && pair.getSecond() > maxocc) {
                continue;
            }
            if (pair.getSecond() < minocc) {
                continue;
            }
            cntQuery += pair.getSecond();
            queries.add(kw);
        }
        LOG.debug("take {}/{} queries which have {}/{} entries", queries.size(), kws.getMap().size(), cntQuery, cntAll);
        for (KWS.Page page : keywordGroundTruth.getPages()) {
            for (KWS.Line line : page.getLines()) {
                Set<String> keySet = new HashSet<>(line.getKeyword2Baseline().keySet());
                for (String string : keySet) {
                    if (!queries.contains(string)) {
                        line.removeKeyword(string);
                    }
                }
            }
        }

        if (!q_out.isEmpty()) {
            if (!q_in.isEmpty()) {
                int sizeBefore = FileUtil.readLines(new File(q_in)).size();
                if (sizeBefore != queries.size()) {
                    LOG.warn("output queries have length {} but input has {}.", queries.size(), sizeBefore);
                }
            }
            FileUtil.writeLines(new File(q_out), queries);
        }
        kwe = new KeywordExtractor(part, upper);
        keywordGroundTruthFinal = kwe.getKeywordGroundTruth(files, imgPathes, queries);
        int cntQueryFinal = 0;
        for (KWS.Page page : keywordGroundTruthFinal.getPages()) {
            for (KWS.Line line : page.getLines()) {
                for (String string : line.getKeyword2Baseline().keySet()) {
                    cntQueryFinal += line.getKeyword2Baseline().get(string).size();
                }
            }
        }
        if (cntQueryFinal != cntQuery) {
            LOG.warn("second run pruduced more entries");
            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
//            FileUtils.write(new File("pre.json"), gson.toJson(keywordGroundTruth, KWS.GroundTruth.class));
//            FileUtils.write(new File("post.json"), gson.toJson(keywordGroundTruthFinal, KWS.GroundTruth.class));

        }
        LOG.debug("take {}/{} queries which have {} entries with eventually parts", queries.size(), kws.getMap().size(), cntQueryFinal);
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
        FileUtils.write(new File(o), gson.toJson(keywordGroundTruthFinal, KWS.GroundTruth.class));
    }

    public static void main(String[] args) throws InvalidParameterException, IOException {
        if (args.length == 0) {
            ArgumentLine al = new ArgumentLine();
            int minocc = 3;
            int maxocc = 10;
            int minlen = 3;
            int maxlen = 10;
            boolean part = true;
            boolean alpha = false;
            boolean upper = true;
            String file = "part_" + part + "_alpha_" + alpha + "_upper_" + upper + "_len_" + minlen + "-" + maxlen + "_occ_" + minocc + "-" + maxocc;
            al.addArgument("xml_in", HomeDir.getFile("data/val_b2p/val_a"));
            al.addArgument("img_in", HomeDir.getFile("data/val_b2p/val_a"));
            al.addArgument("o", HomeDir.getFile("kws_gt/val_a/" + file + ".json"));
            al.addArgument("q_out", HomeDir.getFile("kws_gt/val_a/" + file + ".txt"));
            al.addArgument("minocc", minocc);
            al.addArgument("maxocc", maxocc);
            al.addArgument("minlen", minlen);
            al.addArgument("maxlen", maxlen);
            al.addArgument("part", part);
            al.addArgument("alpha", alpha);
            al.addArgument("upper", upper);
//            al.setHelp();
            args = al.getArgs();
        }
        CreateKwsQueryAndResult instance = new CreateKwsQueryAndResult();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.STRICT); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();

    }
}
