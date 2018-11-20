////////////////////////////////////////////////
/// File:       LangModConfigurator.java
/// Created:    03.06.2016  14:56:17
/// Encoding:   UTF-8
////////////////////////////////////////////////
package de.uros.citlab.module.util;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.types.CharMap;
import com.achteck.misc.types.ConfMat;
import de.planet.itrtech.types.IDictOccurrence;
import de.planet.citech.types.IDecodingType;
import de.planet.langmod.LangMod;
import de.planet.util.types.DictOccurrence;
import de.uros.citlab.tokenizer.TokenizerCategorizer;
import de.uros.citlab.tokenizer.categorizer.CategorizerWordMergeGroups;
import de.uros.citlab.tokenizer.interfaces.ICategorizer;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Desciption of LangModConfigurator
 *
 *
 * Since 03.06.2016
 *
 * @author Tobias (tobias.strauss@uni-rostock.de)
 */
public class LangModConfigurator {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(LangModConfigurator.class.getName());
    private IDictOccurrence dict;
    private Set<Character> chars;
    private final ICategorizer cat;
    private final TokenizerCategorizer tok;
    private String numberChars = "";
    private String preNumberChars = "";
    private String postNumberChars = "";
    private String preWordChars = "";
    private String postWordChars = "";
    private String wordchars = "";
    private final String maxNumOfWords = "15";
    private String separatorChars = "";
    private String regexNoDict;
    private String regexDict;
    private Pattern pattern;
    private final String dictname;
    private CharMap<Integer> charMap;
    private final boolean expandCharMap = true;
    boolean isPrepaired = false;

    public static class Error {

        public String type;
        public String message;

        public Error(String type, String message) {
            this.type = type;
            this.message = message;
        }

        @Override
        public String toString() {
            return "Error{" + "type=" + type + ", message=" + message + '}';
        }

    }

    public LangModConfigurator(IDictOccurrence dict, CharMap<Integer> cm, String dictname) {
        this.dict = dict;
        this.charMap = cm;
        this.dictname = dictname;
        cat = new CategorizerWordMergeGroups();
        tok = new TokenizerCategorizer(cat);
    }

    private void initRegex() {
        createRegex();
        pattern = Pattern.compile("^" + regexNoDict.replaceAll("\\?<[A-Z_]*>", "").replaceAll("\\[\\][+*?]", "").replaceAll("\\[\\]\\{[^}]+\\}", "") + "$");
    }

    public final List<Error> prepare() throws EmptyDictException {
        if (!isPrepaired) {
//        processDict(dict.getDict());
            List<Error> errors = categorizeCharacters();
            errors.addAll(cleanupDict());
            checkDict();
            initRegex();
            isPrepaired = true;
            return errors;
        }
        return null;
    }

    public void initLangMod(LangMod lm) throws EmptyDictException {
        prepare();
        lm.setRegex(regexNoDict);
        if ((dictname != null && dict == null)) {
            throw new RuntimeException("dictname set but no dict");
        }
        if (dictname == null && dict != null) {
            throw new RuntimeException("dict set but no dictname");
        }
        if (dictname != null) {
            lm.setDict(dictname, dict);
        }
    }

    /**
     * should be moved to test
     */
    @Deprecated
    private void checkDict() {
        if (dict != null) {
            for (String string : dict.getDict()) {
                for (char c : string.toCharArray()) {
                    if (!chars.contains(c)) {
                        LOG.info("dictionary entry '" + string + "' cannot be found because character '" + c + "' is not in CharMap.");
                    }
                }
            }
        }
    }

    public final void createRegex() {
        String preWord = "(?<" + IDecodingType.Label.PRECHAR + ">[" + preWordChars + "])";
        String postWord = "(?<" + IDecodingType.Label.POSTCHAR + ">[" + postWordChars + "])";
        String word = "(?<" + IDecodingType.Label.RAW_ENTRY + ">[" + wordchars + "]+)";
        String number = null;
        if (!numberChars.isEmpty()) {
            number = "(?<" + IDecodingType.Label.NUMBER + ">[" + numberChars + "]+)";
            if (!postNumberChars.isEmpty()) {
                number += "(?<" + IDecodingType.Label.UNDEFINED + ">[" + postNumberChars + "])*";
            }
            if (!preNumberChars.isEmpty()) {
                number = "(?<" + IDecodingType.Label.UNDEFINED + ">[" + preNumberChars + "])*" + number;
            }
        }
        separatorChars = separatorChars.contains("-") ? separatorChars.replaceAll("-", "") + "-" : separatorChars;
        String separator = "(?<" + IDecodingType.Label.SPACE + ">[" + separatorChars + "])";
        String unit = number == null
                ? preWord + "{0,3}" + separator + "?(" + word + ")" + separator + "?" + postWord + "{0,3}"
                : preWord + "{0,3}" + separator + "?(" + word + "|" + number + ")" + separator + "?" + postWord + "{0,3}";

        regexNoDict = separator + "{0,2}" + "(" + unit + separator + "{1,3}){0," + maxNumOfWords + "}" + unit + separator + "{0,2}";
        unit = number == null
                ? preWord + "*" + separator + "?(" + word
                + "|(?<" + IDecodingType.Label.DICT_ENTRY + ">[[:" + dictname + ":]])"
                + ")" + separator + "?" + postWord + "*"
                : preWord + "*" + separator + "?(" + word + "|" + number
                + "|(?<" + IDecodingType.Label.DICT_ENTRY + ">[[:" + dictname + ":]])"
                + ")" + separator + "?" + postWord + "*";
        regexDict = separator + "*" + "(" + unit + separator + ")*" + unit + separator + "*";
        LOG.debug("use regex '{}' for this langMod", regexNoDict);
    }

    public String getRegexNoDict() {
        return regexNoDict;
    }

    public void setRegexNoDict(String regexNoDict) {
        this.regexNoDict = regexNoDict;
    }

    public String getRegexDict() {
        return regexDict;
    }

    public void setRegexDict(String regexDict) {
        this.regexDict = regexDict;
    }

    /**
     * should be moved to test
     *
     * @param lines
     * @return
     * @deprecated
     */
    @Deprecated
    public boolean checkLines(List<String> lines) {
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (!matcher.find()) {
                char[] toCharArray = line.toCharArray();
                for (int i = 0; i < toCharArray.length; i++) {
                    char c = toCharArray[i];
                    if (isUnknown(c)) {
                        LOG.error("unexpected sign '{}'", c);
                        handleUnk(i > 0 ? toCharArray[i - 1] : null, c, i + 1 < toCharArray.length ? toCharArray[i + 1] : null);
                    } else if (i > 0 && numberHas2beExtendet(toCharArray[i - 1], c)) {
                        if (!preNumberChars.contains(String.valueOf(c))) {
                            preNumberChars += c;
                        }
                        if (!postNumberChars.contains(String.valueOf(c))) {
                            postNumberChars += c;
                        }
                    }
                }
                initRegex();
                matcher = pattern.matcher(line);
                if (!matcher.find()) {
                    LOG.error("Regex does not fit: " + line);
                    return false;
                }
            }
        }
        return true;
    }

    private String set2String(Set<Character> chars) {
        StringBuilder sb = new StringBuilder();
        boolean foundMinus = false;
        for (Character aChar : chars) {
            if (aChar.equals('-')) {
                foundMinus = true;
            } else {
                //escape for later use in groups  e.g. [A\[\]\\BCD]
                if (aChar.equals('[') || aChar.equals(']') || aChar.equals('\\')) {
                    sb.append('\\');
                }
                sb.append(aChar);
            }
        }
        if (foundMinus) {
            sb.append('-');
        }
        return sb.toString();
    }

    private List<Error> categorizeCharacters() {
        chars = new HashSet<>(charMap.getValues());
        chars.remove(ConfMat.NaC);
        List<Error> res = new LinkedList<>();
        HashMap<String, Set<Character>> map = new HashMap<>();
        for (Character aChar : chars) {
            String category = cat.getCategory(aChar);
            switch (category) {
                case "L":
                case "N":
                case "Z":
                case "P":
                    break;
                default:
                    res.add(new Error("category", "for character '" + aChar + "' unexpected category '" + category + "' found."));
                    LOG.warn("for character '{}' unexpected category '{}' found, will be ignored.", aChar, category);
            }

            Set<Character> get = map.get(category);
            if (get == null) {
                get = new HashSet<>();
                map.put(category, get);
            }
            get.add(aChar);
        }
//        for (String string : map.keySet()) {
//            System.out.println(string + "=>" + map.get(string));
//        }
        {
            Set<Character> set = map.get("P");
            if (set != null) {
                preWordChars = set2String(set);
                postWordChars = preWordChars;
                preNumberChars = preWordChars;
                postNumberChars = preWordChars;
                map.remove("P");
            }
        }
        {
            Set<Character> set = map.get("L");
            if (set != null) {
                wordchars = set2String(set);
                map.remove("L");
            }
        }
        {
            Set<Character> set = map.get("N");
            if (set != null) {
                numberChars = set2String(set);
                map.remove("N");
            }
        }
        {
            Set<Character> set = map.get("Z");
            if (set != null) {
                separatorChars = set2String(set);
                map.remove("Z");
            }
        }
        if (!map.isEmpty()) {
            res.add(new Error("category", "ignore categories '" + map.keySet() + "'."));
            LOG.error("ignore categories '{}'.", map.keySet());
        }
        return res;
    }

    private void handleUnk(Character preUnk, Character unk, Character postUnk) {
        if ((preUnk == null || separatorChars.contains("" + preUnk) || preWordChars.contains("" + preUnk)) && (!postWordChars.contains("" + unk))) {
            preWordChars += unk;
            return;
        }
        if ((postUnk == null || separatorChars.contains("" + postUnk) || postWordChars.contains("" + postUnk)) && !postWordChars.contains("" + unk)) {
            postWordChars += unk;
            return;
        }
        if (wordchars.contains("" + preUnk) && !wordchars.contains("" + unk)) {
            wordchars += unk;
            return;
        }
        if (numberChars.contains("" + preUnk) && !numberChars.contains("" + unk)) {
            numberChars += unk;
            return;
        }
        throw new RuntimeException("Unknown character cannot be assigned: " + "\\u" + Integer.toHexString(unk | 0x10000).substring(1) + " = " + unk + "   context = " + preUnk + unk + postUnk);
    }

    /**
     * should be moved to test
     */
    @Deprecated
    private boolean isUnknown(char c) {
        switch (Character.getType(c)) {
            case Character.CONTROL:
            case Character.FORMAT:
            case Character.UNASSIGNED:
            case Character.PRIVATE_USE:
            case Character.SURROGATE:
            case Character.COMBINING_SPACING_MARK:
            case Character.ENCLOSING_MARK:
            case Character.OTHER_SYMBOL:        //©¦®
            case Character.MODIFIER_SYMBOL:     // ^`¨˜ Hier richtig???
            case Character.LINE_SEPARATOR:
            case Character.PARAGRAPH_SEPARATOR:
            case Character.MATH_SYMBOL:         // + <=>
                return !wordchars.contains(String.valueOf(c));
            default:
                return false;
        }
    }

    private boolean numberHas2beExtendet(char pre, char c) {
        if (numberChars.contains("" + pre) && (wordchars.contains("" + c) || preWordChars.contains("" + c)) && !numberChars.contains("" + c)) {
            return true;
        }
        return false;
    }

    private List<Error> cleanupDict() throws EmptyDictException {
        ArrayList<String> pDict = dict.getPDict();
        int cntDeletes = 0;
        int cntNotValid = 0;
        int cntNotCatL = 0;
        List<Error> res = new LinkedList<>();
        for (int i = pDict.size() - 1; i >= 0; i--) {
            String get = pDict.get(i);
            int lastIndexOf = get.lastIndexOf(';');
            if (lastIndexOf < 0) {
                throw new RuntimeException("expect dictionary with ';' as seperator, but line is '" + get + "'");
            }
            String word = get.substring(0, lastIndexOf);
            boolean isValid = true;
            for (char c : word.toCharArray()) {
                if (!chars.contains(c)) {
                    isValid = false;
                    if (expandCharMap) {
                        Normalizer.Form[] forms = new Normalizer.Form[]{Normalizer.Form.NFKC, Normalizer.Form.NFD, Normalizer.Form.NFKD};
                        for (Normalizer.Form form : forms) {
                            char[] charsAlternatives = Normalizer.normalize(String.valueOf(c), form).toCharArray();
                            if (chars.contains(charsAlternatives[0])) {
                                boolean isEnclosing = true;
                                for (int j = 1; j < charsAlternatives.length; j++) {
                                    if (!cat.getCategory(charsAlternatives[j]).equals("M")) {
                                        isEnclosing = false;
                                        break;
                                    }
                                }
                                if (!isEnclosing) {
                                    continue;
                                }
                                char charBase = charsAlternatives[0];
                                if (!cat.getCategory(c).equals(cat.getCategory(charsAlternatives[0]))) {
                                    continue;
                                }
                                Integer key = charMap.getKey(charBase);
                                LOG.trace("add character '{}' to channel with values '{}'.", c, charMap.get(key));
//                                res.add(new Error("add character '" + c + "' to channel with values '" + charMap.get(key) + "'."));
                                charMap.put(key, charMap.get(key) + c);
                                chars.add(c);
                                isValid = true;
                                break;
                            }
                        }
                    }
                    if (!isValid) {
                        res.add(new Error("charmap", "item '" + word + "' is invalid because character '" + c + "' is not in charMap"));
                        LOG.trace("delete item '{}' because character '{}' is not in charMap", word, c);
                    }
                    break;
                }
            }
            if (!isValid) {
                pDict.remove(i);
                cntDeletes++;
                continue;
            }
            List<String> tokenize = tok.tokenize(word);
            if (tokenize.size() != 1) {
                tok.tokenize(word);
                res.add(new Error("tokenizer", "item '" + word + "' is invalid, because it can be divided into " + tokenize.size() + " items"));
                LOG.trace("delete item '{}', because it can be divided into {} items", word, tokenize.size());
                pDict.remove(i);
                cntNotValid++;
                continue;
            }
            if (!cat.getCategory(tokenize.get(0).charAt(0)).startsWith("L")) {
                res.add(new Error("category", "item '" + word + "' is invalid, because category is '" + cat.getCategory(tokenize.get(0).charAt(0)) + "' instead of 'L'"));
                LOG.trace("delete item '{}', because category is '{}' instead of 'L'", word, cat.getCategory(tokenize.get(0).charAt(0)));
                pDict.remove(i);
                cntNotCatL++;
            }
        }
        LOG.debug("delete {} from {} entries because characters are not in CharMap.", cntDeletes, pDict.size() + cntDeletes + cntNotValid + cntNotCatL);
        LOG.debug("delete {} from {} entries because word is more than one token.", cntNotValid, pDict.size() + cntNotValid + cntNotCatL);
        LOG.debug("delete {} from {} entries because word is not of type 'Letter'.", cntNotCatL, pDict.size() + cntNotCatL);
        if (pDict.isEmpty()) {
            throw new EmptyDictException("dictionary is empty or all entries are invalid");
        }
        if (cntDeletes + cntNotValid + cntNotCatL > 0) {
            dict = new DictOccurrence(pDict);
        }
        return res;
    }

    public static class EmptyDictException extends Exception {

        public EmptyDictException(String message) {
            super(message);
        }

    }

//    private void processDict(ArrayList<String> dict) {
//        HashSet<Character> set = new HashSet<>();
//        for (String string : dict) {
//            for (char c : string.toCharArray()) {
//                set.add(c);
//            }
//        }
//        StringBuilder sb = new StringBuilder();
//        ArrayList<Character> l = new ArrayList<>(set);
//        Collections.sort(l);
//        for (Character character : set) {
//            if (character == '[' || character == ']' || character == '\\') {
//                sb.append('\\');
//            }
//            sb.append(character);
//        }
//        wordchars = sb.toString();
//        if (wordchars.contains("-")) {
//            wordchars = wordchars.replaceAll("-", "");
//            wordchars += "-";
//        }
//    }
//    private static class Token {
//
//        private final List<String> categories;
//        private final LinkedList<Token> pre;
//        private final LinkedList<Token> post;
//
//        public Token(List<String> categories) {
//            this.categories = categories;
//            pre = new LinkedList<Token>();
//            post = new LinkedList<Token>();
//        }
//
//        public void addPre(Token t) {
//            pre.add(t);
//        }
//
//        public void addPost(Token t) {
//            post.add(t);
//        }
//
//        public boolean isValid(String s) {
//            return categories.contains(s);
//        }
//
//    }
}
