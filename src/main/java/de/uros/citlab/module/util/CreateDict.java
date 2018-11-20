////////////////////////////////////////////////
/// File:       CreateDict.java
/// Created:    26.02.2015  12:21:37 
/// Encoding:   UTF-8 
//////////////////////////////////////////////// 
package de.uros.citlab.module.util;

import com.achteck.misc.log.Logger;
import com.achteck.misc.util.StringIO;
import de.uros.citlab.tokenizer.categorizer.CategorizerWordDft;
import de.uros.citlab.tokenizer.TokenizerCategorizer;
import de.uros.citlab.errorrate.util.ObjectCounter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.math3.util.Pair;

/**
 * Description of CreateDict
 *
 *
 * Since 26.02.2015
 *
 * @author tobias
 */
public class CreateDict {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(CreateDict.class.getName());
    private TokenizerCategorizer tok;
    private CategorizerWordDft cat;
    ObjectCounter<String> pDict;
    ObjectCounter<Character> charDict;
    private final String hyphenChars;
    private String lastWord;

    /**
     *
     * @param hyphenChars
     */
    public CreateDict(String hyphenChars) {
        init();
        this.hyphenChars = hyphenChars;
    }

    private void init() {
        cat = new CategorizerWordDft();
        tok = new TokenizerCategorizer(cat);
        pDict = new ObjectCounter<>();
        charDict = new ObjectCounter<>();
    }

    public void processLines(List<String> lines) {
        if (hyphenChars != null) {
//            Hyphens nicht Page-übergreifend
            if (lastWord != null) {
                addTokenOnly(lastWord);
                lastWord = null;
            }
            for (String line : lines) {
                for (char c : line.toCharArray()) {
                    charDict.add(c);
                }
                List<String> tokenize = tok.tokenize(line);
                int size = tokenize.size();

                int start = 0;
//                Combine hyphens
                if (lastWord != null) {
                    if (tokenize.get(start).matches("[" + hyphenChars + "]+")) {
                        start++;
                    }
                    if (cat.getCategory(tokenize.get(start).charAt(0)).equals("L")) {
                        addTokenOnly(lastWord + tokenize.get(start));
                    } else {
                        LOG.log(Logger.WARN, "tried to connect " + lastWord + " and " + tokenize.get(start) + " but this seems to be false.");
                        addTokenOnly(lastWord);
                        addTokenOnly(tokenize.get(start));
                        lastWord = null;
                    }
                }

                for (int i = start; i < (size > 1 ? size - 2 : size); i++) {
                    String token = tokenize.get(i);
                    addTokenOnly(token);
                }
//                Save hyphens
                if (size > 1) {
                    if (tokenize.get(size - 1).matches("[" + hyphenChars + "]+") && cat.getCategory(tokenize.get(size - 2).charAt(0)).equals("L")) {
                        lastWord = tokenize.get(size - 2);
                    } else {
                        lastWord = null;
                        addTokenOnly(tokenize.get(size - 2));
                        addTokenOnly(tokenize.get(size - 1));
                    }
                }
            }
        } else {
            for (String line : lines) {
                for (char c : line.toCharArray()) {
                    charDict.add(c);
                }
                List<String> tokenize = tok.tokenize(line);
                for (String token : tokenize) {
                    if (cat.getCategory(token.charAt(0)).equals("L")) {
                        pDict.add(token);
                    }
                }
            }
        }
    }

    public void savePDict(String fileOut) {
        List<Pair<String, Long>> resultOccurrence = pDict.getResultOccurrence();
        LinkedList<String> list = new LinkedList<>();
        list.add("value;occurence");
        for (Pair<String, Long> pair : resultOccurrence) {
            list.add(pair.getFirst() + ";" + pair.getSecond());
        }
        try {
            StringIO.saveLineList(fileOut, list);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void saveCharDict(String fileOut) {
        LinkedList<String> list = new LinkedList<>();
        for (Character c : charDict.getResult()) {
            list.add(String.valueOf(c));
        }
        try {
            FileWriter fw = new FileWriter(new File(fileOut));
            for (String string : list) {
                fw.write(string + "\n");
                LOG.log(Logger.TRACE, "write \"" + string + "\" with length " + string.length() + ".");
            }
//            fw.flush();
            fw.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

//    public void run() {
//        for (String file : filesXML) {
//            try {
//
//                PageType pageType = XMLHelper20100319.readXML(file);
//                for (Object region : pageType.getTextRegionOrImageRegionOrLineDrawingRegion()) {
//                    if (region instanceof TextRegionType) {
//                        TextRegionType t = (TextRegionType) region;
//                        List<TextLineType> textLines = t.getTextLine();
////                    numOflines += textLines.size();
//                        Collections.sort(textLines, new Comparator<TextLineType>() {
//
//                            @Override
//                            public int compare(TextLineType o1, TextLineType o2) {
//                                return Integer.compare(o1.getCoords().getPoint().get(0).getY(), o2.getCoords().getPoint().get(0).getY());
//                            }
//                        });
//                        for (TextLineType textLine : textLines) {
//                            String[] words = PageXmlUtil.getTextEquiv(textLine).replaceAll("[.,;_\"\':?!#*<>+£§&$|0-9]|( - )", "").split(" |\\(|\\)|\\[|\\]");
//                            extractDict(words);
//                        }
//                    }
//
//                }
//            } catch (Exception ex) {
//                try {
//                    de.planet.ted_base.xml_20130715.PageType pageType = XMLHelper20130715.readXML(file);
//                    for (Object region : pageType.getTextRegionOrImageRegionOrLineDrawingRegion()) {
//                        if (region instanceof de.planet.ted_base.xml_20130715.TextRegionType) {
//                            de.planet.ted_base.xml_20130715.TextRegionType t = (de.planet.ted_base.xml_20130715.TextRegionType) region;
//                            List<de.planet.ted_base.xml_20130715.TextLineType> textLines = t.getTextLine();
////                    numOflines += textLines.size();
//                            for (de.planet.ted_base.xml_20130715.TextLineType textLine : textLines) {
//                                String[] words = PageXmlUtil.getTextEquiv(textLine).replaceAll("[.,;_\"\':?!#*<>+£§&$|0-9]|( - )", "").split(" |\\(|\\)|\\[|\\]");
//                                extractDict(words);
//                            }
//                        }
//
//                    }
//                } catch (Exception ex2) {
//                    LOG.log(Logger.ERROR, "file " + file + " not found.");
//                }
//            }
//
//        }
//        for (String file : filesTXT) {
//            ArrayList<String> lines = null;
//            try {
//                lines = IO.loadLineArrayList(file);
//            } catch (IOException ex) {
//                java.util.logging.Logger.getLogger(CreateDict.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            for (String line : lines) {
//                String[] words = line.replaceAll("[.,;_\"\':?!#*<>+£§&$|0-9]|( - )", "").split(" |\\(|\\)|\\[|\\]");
//                extractDict(words);
//            }
//
//        }
//
//        ArrayList<String> result = new ArrayList<String>();
//        Object[] set = wordsAndRate.entrySet().toArray();
//        Comparator<Object> comparator = new Comparator<Object>() {
//            @Override
//            public int compare(Object o1, Object o2) {
//                return -Integer.compare(((Map.Entry<String, Integer>) o1).getValue(), ((Map.Entry<String, Integer>) o2).getValue());
//            }
//        };
//        Arrays.sort(set, comparator);
//        for (Object entry : set) {
//            Map.Entry<String, Integer> e = (Map.Entry<String, Integer>) entry;
//            result.add(e.getKey() + ";" + e.getValue());
//        }
//        try {
//            IO.saveLineArrayList(result, file_out);
//        } catch (IOException ex) {
//            java.util.logging.Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
//        }
////        LOG.log(Logger.INFO, "Stat: " + cntpage + " pages with " + cntregion + " regions and " + cntline + " lines.S");
//    }
//
//    private void putAndExt(String string, HashMap<String, Integer> wordsAndRate) {
//        if (!string.matches("\\p{Alpha}+")) {
//            return;
//        }
//        if (!wordsAndRate.containsKey(string)) {
//            wordsAndRate.put(string, 1);
//        } else {
//            wordsAndRate.put(string, wordsAndRate.get(string).intValue() + 1);
//        }
//        if (useInflections) {
//            if (string.endsWith("ing") && string.length() > 3) {
//                String base = string.substring(0, string.length() - 3);
//                String past = base + "ed";
//                Logger.getLogger(this.getClass().getName()).log(Logger.INFO, "Found: " + string + " Adding: " + base + " and " + past);
//                if (!wordsAndRate.containsKey(base)) {
//                    wordsAndRate.put(base, 1);
//                } else {
//                    wordsAndRate.put(base, wordsAndRate.get(base).intValue() + 1);
//                }
//                if (!wordsAndRate.containsKey(past)) {
//                    wordsAndRate.put(past, 1);
//                } else {
//                    wordsAndRate.put(past, wordsAndRate.get(past).intValue() + 1);
//                }
//            }
//            if (string.endsWith("ed") && string.length() > 2) {
//                String base = string.substring(0, string.length() - 2);
//                String patriciple = base + "ing";
//                Logger.getLogger(this.getClass().getName()).log(Logger.INFO, "Found: " + string + " Adding: " + base + " and " + patriciple);
//                if (!(wordsAndRate.containsKey(base))) {
//                    if (!wordsAndRate.containsKey(base + "e")) {
//                        wordsAndRate.put(base, 1);
//                    } else {
//                        wordsAndRate.put(base + "e", 1);
//                    }
//                } else {
//                    wordsAndRate.put(base, wordsAndRate.get(base).intValue() + 1);
//                }
//                if (!wordsAndRate.containsKey(patriciple)) {
//                    wordsAndRate.put(patriciple, 1);
//                } else {
//                    wordsAndRate.put(patriciple, wordsAndRate.get(patriciple).intValue() + 1);
//                }
//            }
//        }
//    }
//
//    private void extractDict(String[] words) {
//        String string;
//        if (words.length == 0) {
//            lastWord = "";
//            return;
//        }
//        if (mergeHyphens && (lastWord.matches(".*=$") || lastWord.matches(".*-$") || words[0].matches("^=.*") || words[0].matches("^-.*"))) {
//            string = (lastWord + words[0]).replaceAll("=|-| ", "");
//            Logger.getLogger(this.getClass().getName()).log(Logger.INFO, "word:  " + string);
//            Logger.getLogger(this.getClass().getName()).log(Logger.INFO, "last:  " + lastWord);
//            Logger.getLogger(this.getClass().getName()).log(Logger.INFO, "first: " + words[0]);
//            Logger.getLogger(this.getClass().getName()).log(Logger.INFO, "");
//            putAndExt(string, wordsAndRate);
//        } else {
//            String[] ss = new String[]{lastWord, words[0]};
//            for (String word : ss) {
//                putAndExt(word.replaceAll("[=-]", ""), wordsAndRate);
//            }
//        }
//        for (int i = 1; i < words.length - 1; i++) {
//            String word = words[i];
//            if (!word.isEmpty()) {
//                putAndExt(word, wordsAndRate);
//            }
//        }
//        lastWord = words[words.length - 1];
//    }
//
//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String[] args) throws InvalidParameterException {
//        CreateDict app = new CreateDict();
////        Create htrts_train_xml.lst by:
////        sed 's/\([0-9_]\+\)/data\/icdar15\/contestHTRtS\/BenthamData\/1stBatch\/PAGE\/\1.xml/g' data/icdar15/contestHTRtS/BenthamData/1stBatch/Partitions/Train.lst 
//        if (args.length == 0) {
//            String arg
//                    = //                    "-lxml lists/htrts/htrts_train_xml.lst "
//                    "-lxml lists/iclef16/all_xml.lst "
//                    + "-infl false "
//                    + "-hyph false "
//                    //                    + "-ltxt lists/htrts/htrts_txt.lst "
//                    + "-o dicts/iclef/all_no_hyphens.dict";
//            args = arg.split(" ");
//        }
//        ParamSet ps = new ParamSet();
//        ps.setCommandLineArgs(args);
//        ps = app.getDefaultParamSet(ps);
//        ParamSet.parse(ps, args, ParamSet.ParseMode.STRICT);
//        app.setParamSet(ps);
////        }
//
//        app.init();
//        app.run();
//    }
    private void addTokenOnly(String token) {
        if (cat.getCategory(token.charAt(0)).equals("L")) {
            pDict.add(token);
        }
    }
}
