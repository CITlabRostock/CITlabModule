/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.CharMap;
import com.achteck.misc.types.ConfMat;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.planet.itrtech.types.IDictOccurrence;
import de.planet.regexdecoding.OptPathStruct;
import de.planet.regexdecoding.RegexDecoder;
import de.planet.util.types.ConfMatGenerator;
import de.planet.util.types.DictOccurrence;
import de.uros.citlab.module.TestFiles;
import de.uros.citlab.module.workflow.HomeDir;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author gundram
 */
public class LangModConfiguratorTest {

    public static Logger LOG = Logger.getLogger(LangModConfiguratorTest.class.getName());
    private Random rnd;
    File prefix = new File(TestFiles.getPrefix(), "test_regex");

    public LangModConfiguratorTest() {
    }

    @Before
    public void setUp() {
        rnd = new Random(1234);
    }

    @After
    public void tearDown() {
    }

//    @Test
//    public void testDictsTranskribus() {
//        DictionaryTest dictTest = new DictionaryTest();
//        File folderDicts = HomeDir.getFile("dicts/transkribus/");
//        List<File> listFiles = FileUtil.listFiles(folderDicts, "dict".split(" "), false);
//        for (File listFile : listFiles) {
//            dictTest.testDict(listFile, null, DictionaryTest.TestType.TOKENIZER, DictionaryTest.TestType.CHARMAP);
//        }
//    }
    /**
     * Test of createRegex method, of class LangModConfigurator.
     */
    @Test
    public void testCreateRegex() throws LangModConfigurator.EmptyDictException {
//        String str = "^[]{0,3}([ ]){0,2}((['·•]){0,3}([ ])?(([=ABCDEGHIJLMNOPRSTUVWXYABCDEFGHIJKLMNOPRSTUVWXYÎÜŸŊŬSƘⱮƷ̃̆̇̈̉ͣͦẺỎỦꝚꝢ=abcdeghijlmnoprstuvwxyabcdefghijklmnoprstuvwxyîüÿŋŭſƙɱʒ̃̆̇̈̉ͣͦẻỏủꝛꝣ]+)|([]+))([ ])?(['·•]){0,3}([ ]){1,3}){0,15}(['·•]){0,3}([ ])?(([=ABCDEGHIJLMNOPRSTUVWXYABCDEFGHIJKLMNOPRSTUVWXYÎÜŸŊŬSƘⱮƷ̃̆̇̈̉ͣͦẺỎỦꝚꝢ=abcdeghijlmnoprstuvwxyabcdefghijklmnoprstuvwxyîüÿŋŭſƙɱʒ̃̆̇̈̉ͣͦẻỏủꝛꝣ]+)|([]+))([ ])?(['·•]){0,3}([ ]){0,2}$";
////        String str = "^(|([]+))([ ])?(['·•]){0,3}([ ]){0,2}$";
//        str=str.replaceAll("\\[\\][+*?]", "");
//        str=str.replaceAll("\\[\\]\\{[^}]+\\}", "");
//        Pattern.compile(str);
        CharMap<Integer> cm = CharMapUtil.loadCharMap(new File(prefix, "cm_issue8.txt"));
        IDictOccurrence dict = new DictOccurrence(new File(prefix, "dict_issue8.txt").getAbsolutePath(), ",", 1, 0, false);
        dict.setParamSet(dict.getDefaultParamSet(new ParamSet()));
        dict.init();

        RegexDecoder rd = new RegexDecoder();

        MyDictocc mydict = new MyDictocc(dict.getDict(), dict.getPDict());

//        testCase(dict, chars, rd, word);
        int range = '\uF8FF' - '\uE000';
        int pos = cm.keySet().size();
        for (int i = 0; i < 10; i++) {
//        for (char c = '\uE000'; c < '\uF8FF' + 1; c += rnd.nextInt(100)) {
            char c = (char) (rnd.nextInt(range) + '\uE000');
            LOG.log(Logger.DEBUG, "char = " + "\\u" + Integer.toHexString(c | 0x10000).substring(1) + "   " + c);
            String word = c + "abc" + c;
            rd = new RegexDecoder();
            mydict.addNewWord(word);
            cm.put(pos, c);
            testCase(dict, cm, rd, word);
            mydict.removeWord(word);
            if (!contains(dict, c)) {
                testCase(dict, cm, rd, word);
            }
        }

    }

    /**
     * Test of createRegex method, of class LangModConfigurator.
     */
    @Test
    public void testREADRegex() throws LangModConfigurator.EmptyDictException {
        IDictOccurrence dict = new DictOccurrence(new File(prefix, "NA_UK_Prob11_train.txt").getAbsolutePath(), ",", 1, 0, false);
        dict.setParamSet(dict.getDefaultParamSet(new ParamSet()));
        dict.init();
        CharMap<Integer> charMap = getCharMap(dict);

        RegexDecoder rd = new RegexDecoder();

        for (int i = 0; i < 5; i++) {
            String word = dict.getNext(rnd);
            rd = new RegexDecoder();
            testCase(dict, charMap, rd, word);
        }

    }

    private void testCase(IDictOccurrence dict, CharMap<Integer> chars, RegexDecoder rd, String word) throws LangModConfigurator.EmptyDictException {

        LangModConfigurator lmConfig = new LangModConfigurator(dict, chars, "dict");
        lmConfig.prepare();
        String regexNoDict = lmConfig.getRegexNoDict();
        System.out.println(regexNoDict);
        for (char c : word.toCharArray()) {
            if (!regexNoDict.contains(String.valueOf(c))) {
                Assert.fail("Regular Expression does not fit to expectation");
            }
        }
        String word1 = dict.getNext(rnd);
        String word2 = dict.getNext(rnd);
        LinkedList<String> l = new LinkedList<>();
        String ref = word1 + " " + word2 + " " + word;
        l.add(ref);
        while (!lmConfig.checkLines(l) || !contain(chars, ref)) {
            word1 = dict.getNext(rnd);
            word2 = dict.getNext(rnd);
            l.clear();
            ref = word1 + " " + word2 + " " + word;
            l.add(ref);
        }

        for (char c : ref.toCharArray()) {
            if (!chars.containsValue(c)) {
                System.out.println("chars do not contain " + c);
            }
        }

        ConfMat confMat = ConfMatGenerator.getConfMat(ref, 20, chars);

        rd.setConfMat(confMat);
        OptPathStruct optPathStruct = rd.getOptPathStruct(regexNoDict);
        String reco = optPathStruct.getText();
        if (!ref.equals(reco)) {
            if (ref.length() != reco.length()) {
                Assert.assertEquals("reference and reconstructed recognition differ", ref, reco);
            }
            for (int i = 0; i < ref.length(); i++) {
                Assert.assertEquals("reference  '" + ref + "' and reconstruction '" + reco + "' differ in position " + i, chars.getKey(ref.charAt(i)), chars.getKey(reco.charAt(i)));
            }
        }

        Assert.assertEquals("expected almost zero costs but recieved: " + optPathStruct.getCosts()[0], 0.0, optPathStruct.getCostAbs(), 1E-5);

    }

    private boolean contains(IDictOccurrence dict, char c) {
        for (String string : dict.getDict()) {
            if (string.contains(String.valueOf(c))) {
                return true;
            }
        }
        return false;
    }

    private boolean contain(CharMap<Integer> chars, String ref) {
        for (char c : ref.toCharArray()) {
            if (!chars.containsValue(c)) {
                return false;
            }
        }
        return true;
    }

    private CharMap<Integer> getCharMap(IDictOccurrence dict) {
        CharMap<Integer> set = new CharMap<>();
        set.put(0, ConfMat.NaC);
        if (dict.getDict().size() == 1) {
            String chars = dict.getDict().iterator().next();
            for (char c : chars.toCharArray()) {
                if (!set.containsValue(c)) {
                    set.put(set.keySet().size(), c);
                }
            }
        } else {
            for (String string : dict.getDict()) {
                for (char c : string.toCharArray()) {
                    if (!set.containsValue(c)) {
                        set.put(set.keySet().size(), c);
                    }
                }
            }
        }
        for (Character c : new char[]{'-', ' '}) {
            if (!set.containsValue(c)) {
                set.put(set.keySet().size(), c);
            }
        }
        return set;
    }

    public class MyDictocc extends ParamTreeOrganizer implements IDictOccurrence {

        private final ArrayList<String> dict;

        private MyDictocc(ArrayList<String> dict, ArrayList<String> pDict) {
            this.dict = dict;
//            this.pDict = pDict;
        }

        public void addNewWord(String word) {
            dict.add(word);
        }

        public void removeWord(String word) {
            dict.remove(word);
        }

        @Override
        public double getCost(String string) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ArrayList<String> getDict() {
            return dict;
        }

        @Override
        public ArrayList<String> getPDict() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public HashMap<String, Double> getProbMap() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public long getTotalCount() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getNext(Random random) {
            int nextInt = rnd.nextInt(dict.size());
            return dict.get(nextInt);
        }

    }
}
