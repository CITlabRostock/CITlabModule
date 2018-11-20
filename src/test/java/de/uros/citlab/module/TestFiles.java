/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * @author gundram
 */
public class TestFiles {

    private final static List<File> files;
    private final static File prefix;
    private final static List<File> htrs = new LinkedList<>();
    private final static List<File> layoutAnalysis = new LinkedList<>();
    private final static List<File> snipets = new LinkedList<>();
    private final static boolean skipLargeTests = false;

    public static boolean skipLargeTests() {
        return skipLargeTests;
    }

    static {
        prefix = new File("src/test/resources/");
        files = new LinkedList<>();
        files.add(new File(prefix, "test_workflow/00000073.tif"));
        htrs.add(new File(prefix, "test_htr/meganet_37"));
        htrs.add(new File(prefix, "test_htr/Konzilsprotokolle_M4"));
        htrs.add(new File(prefix, "test_htr/Bentham"));
        layoutAnalysis.add(new File(prefix, "test_la/historical_90_dft_20161011.bin"));
        snipets.add(new File(prefix, "test_charmap/CO_5_403_0122_line_1455785607636_879.jpg"));
    }

    public static List<File> getSnipets() {
        return snipets;
    }

    public static File getHtrDft() {
        return htrs.get(0);
    }

    public static List<File> getHtrs() {
        return htrs;
    }

    public static List<File> getLayoutAnalysis() {
        return layoutAnalysis;
    }

    public static List<File> getTestFiles() {
        return files;
    }

    public static File getPrefix() {
        return prefix;
    }

}
