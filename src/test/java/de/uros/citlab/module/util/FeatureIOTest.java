/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import de.uros.citlab.module.TestFiles;
import static de.uros.citlab.module.util.FeatureIO.getAvgError;
import static de.uros.citlab.module.util.FeatureIO.getExp;
import static de.uros.citlab.module.util.FeatureIO.getLLH;
import static de.uros.citlab.module.util.FeatureIO.loadBinary;
import static de.uros.citlab.module.util.FeatureIO.saveBinaryAsByte;
import static de.uros.citlab.module.util.FeatureIO.saveBinaryAsFloat;
import static de.uros.citlab.module.util.FeatureIO.saveBinaryAsShort;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static de.uros.citlab.module.util.FeatureIO.saveBinaryAsByte;

/**
 *
 * @author gundram
 */
public class FeatureIOTest {

    private static java.io.File dir;
    private static java.io.File fileByte;
    private static java.io.File fileFloat;
    private static java.io.File fileLLH;
    private static double[][] feature;

    static {
        try {
            dir = new File(TestFiles.getPrefix(), "test_feature_io");
            fileByte = new File(dir, "fileByte.feat");
            fileFloat = new File(dir, "fileFloat.feat");
            fileLLH = new File(dir, "fileLLH.feat");
            Random r = new Random();
            feature = new double[40][50];
            for (double[] ds : feature) {
                for (int i = 0; i < ds.length; i++) {
                    ds[i] = r.nextDouble();
                }
            }
        } catch (Throwable ex) {
            Assert.fail("cannot initialize test " + FeatureIOTest.class.getName() + ": " + ex);
        }
    }

    public FeatureIOTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        System.out.println("setUp Class by creating " + dir.getAbsolutePath());
        dir.mkdirs();
    }

    @AfterClass
    public static void tearDownClass() {
        System.out.println("tearDown Class");
        try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testSaveBinaryAsByte() {
        try {
            System.out.println("saveBinaryAsByte");
            saveBinaryAsByte(feature, new FileOutputStream(fileByte));
            double[][] featureAsByte = loadBinary(new FileInputStream(fileByte));
            assertEquals(0, getAvgError(feature, featureAsByte), 2e-3);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testsaveBinaryAsShort() {
        try {
            System.out.println("saveBinaryAsShort");
            double[][] featureLLH = getLLH(feature);
            saveBinaryAsShort(featureLLH, new FileOutputStream(fileLLH), -50);
            double[][] featureAsShort = loadBinary(new FileInputStream(fileLLH));
            assertEquals(0, getAvgError(feature, getExp(featureAsShort)), 2e-4);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testsaveBinaryAsFloat() {
        try {
            System.out.println("saveBinaryAsFloat");
            saveBinaryAsFloat(feature, new FileOutputStream(fileFloat));
            double[][] featureAsFloat = loadBinary(new FileInputStream(fileFloat));
            assertEquals(0, getAvgError(feature, featureAsFloat), 2e-8);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
