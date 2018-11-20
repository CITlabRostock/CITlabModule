/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import com.achteck.misc.log.Logger;
import com.achteck.misc.util.StringIO;
import de.planet.imaging.types.HybridImage;
import de.planet.itrtech.types.ImagePropertyIDs;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.util.PolygonHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author gundram
 */
public class FeatureUtil {

    public static Logger LOG = Logger.getLogger(FeatureUtil.class.getName());

    public static String loadReference(String path) throws IOException {
        try {
            return StringIO.readString(path.substring(0, path.lastIndexOf(".")) + ".txt");
        } catch (Exception e) {
            try {
                path += ".txt";
                return StringIO.readString(path);
            } catch (Exception e2) {
                throw e;
            }
        }
    }

    public static HashMap<String, Object> loadInfo(String path, HybridImage hi) throws IOException {
        HashMap<String, Object> info = new HashMap<>();
        info.put("img_dir", path);

        File infofile = new File(path + ".info");
        if (infofile.exists()) {
            try {
                List<String> list = FileUtil.readLines(infofile);
                for (int i = 0; i < list.size() - 1; i += 2) {
                    String key = list.get(i);
                    String value = list.get(i + 1);
                    if (key.equals(ImagePropertyIDs.MASK.toString())) {
                        String[] split = value.split(":");
                        ArrayList<Polygon2DInt> mask = new ArrayList<>();
                        for (String stringPolygon : split) {
                            mask.add(PolygonHelper.fromString(stringPolygon));
                        }
                        if (hi != null) {
                            hi.setProperty(key, mask);
                        }
                        info.put(key, mask);
                    } else if (key.equals(ImagePropertyIDs.DEBUG_DESCR.toString())) {
                        if (hi != null) {
                            hi.setProperty(key, value);
                        }
                        info.put(key, value);
                    } else if (key.toUpperCase().equals(ImagePropertyIDs.TRAFO.toString())) {
                        info.put("trafo_orig", value);
                    } else {
                        info.put(key, value);
                    }
                }
            } catch (Throwable ex) {
                LOG.log(Logger.WARN, "cannot load info-file", ex);
            }
        }
        return info;
    }

    public static double[][][] makeDeep(double[][] featureFlat, int s) {
        if (s < 1) {
            throw new RuntimeException("parameter s have to be 1 or bigger");
        }
        if (s == 1) {
            return new double[][][]{featureFlat};
        }
        if (featureFlat.length % s != 0) {
            throw new RuntimeException("height of image (" + featureFlat.length + ") have to be a multiply of parameter s (" + s + ").");
        }
        final int dimY = featureFlat.length / s;
        double[][][] res = new double[s][dimY][];
        int idx = 0;
        for (double[][] re : res) {
            for (int j = 0; j < re.length; j++) {
                re[j] = featureFlat[idx++];
            }
        }
        return res;
    }

    public static double[][] makeFlat(double[][][] featureFlat) {
        LOG.log(Logger.DEBUG, "dim = " + featureFlat.length + " and " + featureFlat[0].length + " and " + featureFlat[0][0].length);
        double[][] res = new double[featureFlat.length * featureFlat[0].length][];
        int idx = 0;
        for (double[][] re : featureFlat) {
            for (double[] re1 : re) {
                res[idx++] = re1;
            }
        }
        return res;
    }

}
