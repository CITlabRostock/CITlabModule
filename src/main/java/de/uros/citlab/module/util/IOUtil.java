package de.uros.citlab.module.util;

import com.achteck.misc.util.IO;

import java.io.File;
import java.io.IOException;

public class IOUtil {

    public static Object load(String file){
        return load(new File(file));
    }
    public static Object load(File file) {
        try {
            return IO.load(file, "de.planet.tech", "de.planet");
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("cannot load file '" + file + "'", e);
        }
    }
}
