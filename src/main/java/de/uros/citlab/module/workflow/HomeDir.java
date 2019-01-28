/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import java.io.File;

/**
 *
 * @author gundram
 */
public class HomeDir {

    public static String PATH = "/home/gundram/devel/projects/bentham_academical/";
    public static File PATH_FILE = new File(PATH);

    public static File getFile(String file) {
        return new File(PATH_FILE, file);
    }

    public static void setPath(String path) {
        HomeDir.PATH = path;
        if (!HomeDir.PATH.endsWith(File.separator)) {
            HomeDir.PATH += File.separator;
        }
        PATH_FILE = new File(HomeDir.PATH);
    }

}
