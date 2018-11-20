/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import com.achteck.misc.types.CharMap;
import com.achteck.misc.types.ConfMat;
import de.planet.itrtech.types.IDictOccurrence;
import de.planet.util.types.DictOccurrence;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

/**
 *
 * @author gundram
 */
public class DictionaryTest {

    public static enum TestType {

        /**
         * does the category matches to L? this does not have to be tested
         */
        CATEGORY,
        /**
         * are there characters in the dictionary, which are not in the charMap?
         * A retraining should be done with expanded charMap
         */
        CHARMAP,
        /**
         * are there tokens, which were interpreted as more than 1 token? This
         * should not happen.
         */
        TOKENIZER;

    }

    public DictionaryTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
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

    /**
     *
     * @param pathToDict path to dictionary
     * @param pathToCharMap can be null, creates chaMap from dictionary
     * @param tests what should be tested
     * @throws de.uros.citlab.module.util.LangModConfigurator.EmptyDictException if dictionary is empty after deleting all invalid entries
     */
    public void testDict(File pathToDict, File pathToCharMap, TestType... tests) throws LangModConfigurator.EmptyDictException {
        IDictOccurrence dict = null;
        dict = new DictOccurrence(pathToDict.getAbsolutePath(), ";", 1, 0, true);
        dict.setParamSet(dict.getDefaultParamSet(null));
        dict.init();
        CharMap<Integer> cm = null;
        if (pathToCharMap != null) {
            cm = CharMapUtil.loadCharMap(pathToCharMap);
        } else {
            cm = getCharMap(dict);
        }

        LangModConfigurator lmConfig = new LangModConfigurator(dict, cm, "dict");
        List<LangModConfigurator.Error> prepare = lmConfig.prepare();
        List<String> types = new LinkedList<>();
        for (TestType test : tests) {
            types.add(test.toString().toLowerCase());
        }
        for (int i = prepare.size() - 1; i >= 0; i--) {
            LangModConfigurator.Error get = prepare.get(i);
            if (!types.contains(get.type)) {
                prepare.remove(i);
            }
        }
//        LangModConfigurator.Error[] toArray = prepare.toArray(new LangModConfigurator.Error[0]);
        Assert.assertNull("errors while testing dictionary '" + pathToDict + "'", prepare.isEmpty() ? null : prepare.get(0).toString());
    }
}
