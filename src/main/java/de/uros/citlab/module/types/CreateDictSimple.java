////////////////////////////////////////////////
/// File:       CreateDict.java
/// Created:    26.02.2015  12:21:37 
/// Encoding:   UTF-8 
//////////////////////////////////////////////// 
package de.uros.citlab.module.types;

import com.achteck.misc.log.Logger;
import com.achteck.misc.util.StringIO;
import de.uros.citlab.tokenizer.categorizer.CategorizerWordDft;
import de.uros.citlab.tokenizer.TokenizerCategorizer;
import de.uros.citlab.errorrate.util.ObjectCounter;
import eu.transkribus.interfaces.ITokenizer;
import de.uros.citlab.tokenizer.categorizer.CategorizerWordMergeGroups;
import de.uros.citlab.tokenizer.interfaces.ICategorizer;
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
 * Since 25.07.2017
 *
 * @author tobias
 */
public class CreateDictSimple {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(CreateDictSimple.class.getName());
    private ITokenizer tok;
    private ICategorizer cat;
    ObjectCounter<String> pDict;
    ObjectCounter<Character> charDict;
    String sep;

    /**
     * @param sep
     */
    public CreateDictSimple(String sep) {
        init();
        this.sep = sep;
    }

    private void init() {
        cat = new CategorizerWordMergeGroups();
        tok = new TokenizerCategorizer(cat);
        pDict = new ObjectCounter<>();
        charDict = new ObjectCounter<>();
    }

    public void setTok(ITokenizer tok) {
        this.tok = tok;
    }

    public void setCat(ICategorizer cat) {
        this.cat = cat;
    }

    public void processLines(List<String> lines) {
        for (String line : lines) {
            for (char c : line.toCharArray()) {
                charDict.add(c);
            }
            List<String> tokenize = tok.tokenize(line);
            for (String token : tokenize) {
                if (!token.contains(" ")) {
                    if (cat.getCategory(token.charAt(0)).equals("L")) {
                        pDict.add(token);
                    } else {
                        for (char c : token.toCharArray()) {
                            pDict.add(String.valueOf(c));
                        }
                    }
                }
            }
        }
    }

    public void savePDict(String fileOut) {
        List<Pair<String, Long>> resultOccurrence = pDict.getResultOccurrence();
        LinkedList<String> list = new LinkedList<>();
        list.add("value" + sep + "occurence");
        for (Pair<String, Long> pair : resultOccurrence) {
            list.add(pair.getFirst() + sep + pair.getSecond()
            );
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

}
