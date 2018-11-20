/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import com.achteck.misc.log.Logger;
import com.achteck.misc.types.CharMap;
import com.achteck.misc.types.ConfMat;
import de.planet.reco.types.SNetwork;
import de.planet.trainer.factory.CharMapFactoryConstructors;
import de.uros.citlab.module.TestFiles;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 *
 * @author gundram
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CharMapUtilTest {

    public static Logger LOG = Logger.getLogger(CharMapUtilTest.class.getName());
    static SNetwork net;
    static File netPath = null;
    static private List<CharMap<Integer>> testCM;
    static private List<String> output;
    static File testDirTmp = new File(TestFiles.getPrefix(), "test_charmap_tmp");
    static File testDir = new File(TestFiles.getPrefix(), "test_charmap");
    static File testFileTmp = new File(testDirTmp, "cm.txt");

    public CharMapUtilTest() {
    }

    @BeforeClass
    public static void setUp() {
        testCM = new LinkedList<>();
        output = new LinkedList<>();
        testCM.add(CharMapFactoryConstructors.getCharMapNum());
        output.add("1503");
        CharMap<Integer> charMapAlpha = CharMapFactoryConstructors.getCharMapAlpha();
        charMapAlpha.put(charMapAlpha.getKey('e'), "ée");
        testCM.add(charMapAlpha);
        output.add("ofthésoofJeptembérandSoP");
//        output.add("oftheioofSeptemberandSop");
        CharMap<Integer> charMapAlnum = CharMapFactoryConstructors.getCharMapAlnum();
        charMapAlnum.put(charMapAlnum.getKey('e'), "ée");
        testCM.add(charMapAlnum);
        output.add("ofthé15ofJeptembérand3oP");
        netPath = TestFiles.getHtrDft();
//        netPath = TestFiles.getHtrs().get(1);
        System.out.println("###############" + netPath);
//        System.out.println(testCM.toString().replace("" + ConfMat.NaC, "NAC"));
        testDirTmp.mkdirs();
    }

    @AfterClass
    public static void tearDown() {
        FileUtils.deleteQuietly(testDirTmp);
    }

    /**
     * Test of loadCharMap method, of class CharMapUtil.
     */
    @Test
    public void testBLoadCharMap() {
        System.out.println("loadCharMaCSep");
        CharMap<Integer> result = CharMapUtil.loadCharMap(testFileTmp);
        assertTrue("charmaps after saving and loading differs", CharMapUtil.equals(result, testCM.get(0)));
    }

    /**
     * Test of saveCharMap method, of class CharMapUtil.
     */
    @Test
    public void testASaveCharMap() {
        System.out.println("saveCharMap");
        CharMapUtil.saveCharMap(testCM.get(0), testFileTmp);
    }

    /**
     * Test of saveCharMap method, of class CharMapUtil.
     */
    @Test
    public void testGetCharMap() {
        System.out.println("getCharMap");
        Set<Character> chars = new HashSet<>();
        chars.add('a');
        chars.add('ä');
        chars.add('Ä');
        chars.add('A');
        CharMap<Integer> charMap1 = CharMapUtil.getCharMap(chars, true);
        Assert.assertEquals("index have to be the same", charMap1.getKey('a'), charMap1.getKey('ä'));
        CharMap<Integer> charMap = CharMapUtil.getCharMap(chars, false);
        Assert.assertNotEquals("index have to be the same", charMap.getKey('a'), charMap.getKey('ä'));

    }

    private static void printConfMat(String key, ConfMat cm) {
        LOG.log(Logger.DEBUG, key + "'" + cm.toString() + "' '" + cm.getBestPath().replace(ConfMat.NaC, '*') + "'");
        LOG.log(Logger.DEBUG, key + cm.getCharMap().toString().replace(ConfMat.NaC, '*') + "'");
    }

}
