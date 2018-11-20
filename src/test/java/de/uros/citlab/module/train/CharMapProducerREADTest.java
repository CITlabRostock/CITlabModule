/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.train;

import com.achteck.misc.types.CharMap;
import com.achteck.misc.types.ConfMat;
import de.uros.citlab.module.TestFiles;
import de.uros.citlab.module.util.CharMapUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gundram
 */
public class CharMapProducerREADTest {

    public static File folder = new File(TestFiles.getPrefix(), "test_charmap_tmp");
    public static File f = new File(folder, "confmat.txt");

    @BeforeClass
    public static void setUpClass() {
        folder.mkdirs();
        String file = "a=0\n"
                + " =6\n"
                + "b=1\n"
                + "c=2\n"
                + "A=0\n"
                + "d=3\n"
                + "\\\\=5\n"
                + "\\==4\n"
                + "\\u0308=7\n"
                + "B=1\n"
                + "ß=1\n"
                + "C=2\n"
                + "D=3";
        try {
            try (FileOutputStream fileOutputStream = new FileOutputStream(f)) {
                IOUtils.write(file, fileOutputStream);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @AfterClass
    public static void tearDownClass() {
        FileUtils.deleteQuietly(folder);
    }

    /**
     * Test of getCharMap method, of class CharMapProducerREAD.
     */
    @Test
    public void testGetCharMap() {
        System.out.println("getCharMap");
        CharMapProducerREAD instance = new CharMapProducerREAD();
        CharMap<Integer> expResult = null;
        CharMap<Integer> result = CharMapUtil.loadCharMap(f);
        assertEquals(result.get(0), "" + ConfMat.NaC);
        char[] exp = "aA".toCharArray();
        assertArrayEquals(result.get(1).toCharArray(), exp);
        // TODO review the generated test code and remove the default call to fail.
    }

    @Test
    public void testGetCharMap2() {
        System.out.println("getCharMap2");
        CharMapProducerREAD instance = new CharMapProducerREAD();
        CharMap<Integer> result = CharMapUtil.loadCharMap(f);
        assertEquals(result.get(0), "" + ConfMat.NaC);
        char[] exp = "aA".toCharArray();
        assertArrayEquals(result.get(1).toCharArray(), exp);
        // TODO review the generated test code and remove the default call to fail.
    }

}
