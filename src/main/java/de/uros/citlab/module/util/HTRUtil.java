package de.uros.citlab.module.util;

import de.uros.citlab.module.train.TrainHtr;

import java.io.File;

public class HTRUtil {

    public static boolean isHtrPlus(File folder) {
        try {
            TrainHtr.getNetBestOrLast(folder);
            return false;
        } catch (RuntimeException ex) {
            return true;
        }
    }
}
