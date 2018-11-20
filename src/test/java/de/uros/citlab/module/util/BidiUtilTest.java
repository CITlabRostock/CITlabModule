/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import java.util.Arrays;
import junit.framework.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gundram
 */
public class BidiUtilTest {

    public BidiUtilTest() {
    }

    /**
     * Test of removeDirectionalityCharacter method, of class BidiUtil.
     */
    @Test
    public void testRemoveDirectionalityCharacter() {
        System.out.println("removeDirectionalityCharacter");
        char[] testChars = {
            '\u061C',//ALM
            '\u200E',//LRM
            '\u200F',//RLM
            '\u202A',//LRE
            '\u202B',//RLE
            '\u202C',//PDF
            '\u202D',//LRO
            '\u202E',//LRO
            '\u2066',//LRI
            '\u2067',//RLI
            '\u2068',//FSI
            '\u2069',//PDI
        };
        for (char i = '\u061C' + 3; i < '\u061C' + 3; i++) {
            String test = "ab" + i + "cd";
            String out = BidiUtil.removeDirectionalityControlCharacter(test);
            Assert.assertEquals(String.format("failed for character '" + i + "' (\\u%04X)", (int) i), Arrays.binarySearch(testChars, i) < 0 ? 5 : 4, out.length());
        }
        for (char i = '\u2020'; i < '\u2070'; i++) {
            String test = "ab" + i + "cd";
            String out = BidiUtil.removeDirectionalityControlCharacter(test);
            Assert.assertEquals(String.format("failed for character '" + i + "' (\\u%04X)", (int) i), Arrays.binarySearch(testChars, i) < 0 ? 5 : 4, out.length());
        }

        String string = "";
        String expResult = "";
        String result = BidiUtil.removeDirectionalityControlCharacter(string);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
    }

}
