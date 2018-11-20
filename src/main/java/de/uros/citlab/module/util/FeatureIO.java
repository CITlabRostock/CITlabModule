/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import com.achteck.misc.io.File;
import com.achteck.misc.log.Logger;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.planet.imaging.types.HybridImage;
import de.uros.citlab.module.interfaces.IFeatureGeneratorStreamable;
import eu.transkribus.interfaces.types.Image;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author gundram
 */
public class FeatureIO extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(FeatureIO.class.getName());

    public static void process(IFeatureGeneratorStreamable instance, InputStream is, OutputStream os, String[] props) {
        try {
            double[][] loadBinary = loadBinary(is);
            double[][] process = instance.process(loadBinary, props);
            saveBinary(instance, process, os);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void process(IFeatureGeneratorStreamable instance, Image image, String pathToFileOut, String[] props) {
        HybridImage hi = ImageUtil.getHybridImage(image, props);
        double[][] process = instance.process(hi.getAsInverseGrayMatrix(), props);
        try (FileOutputStream os = new FileOutputStream(pathToFileOut)) {
            saveBinary(instance, process, os);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    public static void process(IFeatureGeneratorStreamable instance, String pathToFileIn, String pathToFileOut, String[] props) {
        double[][] hi = null;
        try {
            hi = HybridImage.newInstance(pathToFileIn).getAsInverseGrayMatrix();
        } catch (Throwable ex) {
            try {
                hi = FeatureIO.loadBinary(new FileInputStream(pathToFileIn));
            } catch (IOException ex1) {
                throw new RuntimeException("cannot load '" + pathToFileIn + "' as hybridImage or as binary feature.", ex);
            }
        }
        double[][] process = instance.process(hi, null);
        try (FileOutputStream os = new FileOutputStream(pathToFileOut)) {
            saveBinary(instance, process, os);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void saveBinary(IFeatureGeneratorStreamable instance, double[][] hi, OutputStream os) throws IOException {
        switch (instance.getSaveMode()) {
            case BYTE: {
                saveBinaryAsByte(hi, os);
                break;
            }
            case FLOAT: {
                saveBinaryAsFloat(hi, os);
                break;
            }
            case LLH: {
                saveBinaryAsShort(hi, os, instance.getLLHAccuracy());
                break;
            }
        }
    }

    public static void saveBinaryAsFloat(double[][] feature, OutputStream os) throws IOException {
        writeInt(os, feature.length);
        writeInt(os, 1);
        writeShort(os, (short) (feature[0].length * 4));
        writeShort(os, (short) 6);
        for (int i = 0; i < feature.length; i++) {
            double[] ds = feature[i];
            for (int j = 0; j < ds.length; j++) {
                writeFloat(os, (float) ds[j]);
            }
        }
    }

    public static void saveBinaryAsByte(byte[][] feature, OutputStream os) throws IOException {
        writeInt(os, feature.length);
        writeInt(os, -1);
        writeShort(os, (short) (feature[0].length));
        writeShort(os, (short) 6);
        for (int i = 0; i < feature.length; i++) {
            byte[] ds = feature[i];
            for (int j = 0; j < ds.length; j++) {
                writeByte(os, ds[j]);
            }
        }
    }

    public static void saveBinaryAsByte(double[][] feature, OutputStream os) throws IOException {
        writeInt(os, feature.length);
        writeInt(os, -1);
        writeShort(os, (short) (feature[0].length));
        writeShort(os, (short) 6);
        ByteDoubleMap bdm = new ByteDoubleMap(0.0, 1.0);
        for (int i = 0; i < feature.length; i++) {
            double[] ds = feature[i];
            for (int j = 0; j < ds.length; j++) {
                final double out = feature[i][j];
                if (out > 1.0 || out < 0.0) {
                    throw new RuntimeException("feature have to be in [0,1], but [" + i + "," + j + "] is " + out + ".");
                }
                writeByte(os, bdm.map(out));
            }
        }
    }

    public static void saveBinaryAsShort(double[][] feature, OutputStream os, int minvalue) throws IOException {
        if (minvalue >= -1) {
            throw new RuntimeException("minvalue have to be smaller than -1 but is" + minvalue + ".");
        }
        writeInt(os, feature.length);
        writeInt(os, minvalue);
        writeShort(os, (short) (feature[0].length * 2));
        writeShort(os, (short) 6);
        ShortDoubleMap sdm = new ShortDoubleMap(minvalue, 0.0);
        for (int i = 0; i < feature.length; i++) {
            double[] ds = feature[i];
            for (int j = 0; j < ds.length; j++) {
                final double out = feature[i][j];
                if (out > 0.0) {
                    throw new RuntimeException("feature have to be  <=0, but [" + i + "," + j + "] is " + out + ".");
                }
                short map = sdm.map(out);
                double diff = sdm.unmap(map) - out;
                if (Math.abs(diff) > 0.01) {
                    LOG.log(Logger.INFO, "erro");
                }
                writeShort(os, sdm.map(out));
            }
        }
    }

    public static double[][] loadBinary(InputStream is) throws IOException {
//        byte[] buffer = new byte[8];
        long dimy = readInt(is);
        long aLong2 = readInt(is);
        long dimx = readShort(is);
        long aInt2 = readShort(is);
        LOG.log(Logger.DEBUG, "load stream with prefix " + dimy + " " + aLong2 + " " + dimx + " " + aInt2);
        if (aLong2 < -1) {
            ShortDoubleMap sdm = new ShortDoubleMap(aLong2, 0.0);
            double[][] feature = new double[(int) dimy][(int) dimx / 2];
            for (int i = 0; i < feature.length; i++) {
                for (int j = 0; j < feature[0].length; j++) {
                    feature[i][j] = sdm.unmap(readShort(is));
                }
            }
            return feature;
        } else if (aLong2 < 0) {
            ByteDoubleMap bdm = new ByteDoubleMap(0.0, 1.0);
            double[][] feature = new double[(int) dimy][(int) dimx];
            for (int i = 0; i < feature.length; i++) {
                for (int j = 0; j < feature[0].length; j++) {
                    feature[i][j] = bdm.unmap(readByte(is));
                }
            }
            return feature;
        } else {
            double[][] feature = new double[(int) dimy][(int) dimx / 4];
            for (int i = 0; i < feature.length; i++) {
                for (int j = 0; j < feature[0].length; j++) {
                    feature[i][j] = readFloat(is);
                }
            }
            return feature;
        }
    }

    private static void writeShort(OutputStream fileInputStream, short value) throws IOException {
        fileInputStream.write(getShort2Bytes(value));
    }

    private static short readShort(InputStream fileInputStream) throws IOException {
        byte[] buf = new byte[2];
        fileInputStream.read(buf);
        return getBytes2Short(buf);
    }

    private static byte[] getShort2Bytes(short value) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putShort(value);
        bb.flip();
        return bb.array();
    }

    private static short getBytes2Short(byte[] value) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(value);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getShort();
    }

    private static void writeInt(OutputStream fileInputStream, int value) throws IOException {
        fileInputStream.write(getInt2Bytes(value));
    }

    private static int readInt(InputStream fileInputStream) throws IOException {
        byte[] buf = new byte[4];
        fileInputStream.read(buf);
        return getBytes2Int(buf);
    }

    private static byte[] getInt2Bytes(int value) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt((int) value);
        bb.flip();
        return bb.array();
    }

    private static int getBytes2Int(byte[] value) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(value);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getInt();
    }

    private static byte[] getFloat2Bytes(float d) {
        ByteBuffer allocate = ByteBuffer.allocate(4);
        allocate.putFloat(d);
        allocate.order(ByteOrder.BIG_ENDIAN);
        return allocate.array();
    }

    private static void writeFloat(OutputStream os, float value) throws IOException {
        os.write(getFloat2Bytes(value));
    }

    private static float getBytes2Float(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
    }

    private static float readFloat(InputStream fileInputStream) throws IOException {
        byte[] buf = new byte[4];
        fileInputStream.read(buf, 0, 4);
        return getBytes2Float(buf);
    }

    private static void writeByte(OutputStream os, byte value) throws IOException {
        os.write(value & 0xff);
    }

    private static byte readByte(InputStream fileInputStream) throws IOException {
        return (byte) fileInputStream.read();
    }

    public static void writeBinary(File outPath, double[][] feature) throws IOException {
        try (OutputStream outStream = new BufferedOutputStream(new FileOutputStream(outPath))) {
            System.out.println("size:" + outPath.length());
            writeInt(outStream, feature.length);
            writeInt(outStream, 1);
            writeShort(outStream, (short) (feature[0].length * 4));
            writeShort(outStream, (short) 1);
            System.out.println(feature.length + " " + 1 + " " + feature[0].length + " " + 1);
            for (int i = 0; i < feature.length; i++) {
                for (int j = 0; j < feature[0].length; j++) {
                    writeFloat(outStream, (float) feature[i][j]);
                }
            }
        }
    }

    private final static class ByteDoubleMap {

        private final double min_val, max_val;
        private final double a, b;   ///< y = a * x + b
        private final double ai, bi; ///< inverse

        /**
         * Define Mapping
         *
         * @param min_val mapped to Byte.MIN_VALUE
         * @param max_val mapped to Byte.MAX_VALUE
         */
        public ByteDoubleMap(double min_val, double max_val) {
            this.min_val = min_val;
            this.max_val = max_val;
            this.a = ((double) Byte.MAX_VALUE - (double) Byte.MIN_VALUE) / (max_val - min_val);
            this.b = Byte.MIN_VALUE - a * min_val + .5; // + .5 for rounding
            this.ai = 1. / a;
            this.bi = min_val - ai * Byte.MIN_VALUE;
        }

        /**
         * Map double value to byte
         *
         * @param x values < min_val are mapped to Byte.MIN_VALUE, values >
         * max_value to Byte.MAX_VALUE
         * @return
         */
        public byte map(final double x) {
            return x <= min_val ? Byte.MIN_VALUE : x >= max_val ? Byte.MAX_VALUE : (byte) Math.floor(a * x + b);
        }

        /**
         * Map byte value to double.
         *
         * @param x
         * @return
         */
        public double unmap(byte x) {
            return ai * x + bi;
        }
    }

    private final static class ShortDoubleMap {

        private final double min_val, max_val;
        private double a, b;   ///< y = a * x + b
        private double ai, bi; ///< inverse

        /**
         * Define Mapping
         *
         * @param min_val mapped to Short.MIN_VALUE
         * @param max_val mapped to Short.MAX_VALUE
         */
        public ShortDoubleMap(double min_val, double max_val) {
            this.min_val = min_val;
            this.max_val = max_val;
            this.a = ((double) Short.MAX_VALUE - (double) Short.MIN_VALUE) / (max_val - min_val);
            this.b = Short.MIN_VALUE - a * min_val + .5; // + .5 for rounding
            this.ai = 1. / a;
            this.bi = min_val - ai * Short.MIN_VALUE;
        }

        /**
         * Map double value to byte
         *
         * @param x values < min_val are mapped to Short.MIN_VALUE, values >
         * max_value to Short.MAX_VALUE
         * @return
         */
        public short map(final double x) {
            return x <= min_val ? Short.MIN_VALUE : x >= max_val ? Short.MAX_VALUE : (short) Math.floor(a * x + b);
        }

        /**
         * Map short value to double.
         *
         * @param x
         * @return
         */
        public double unmap(short x) {
            return ai * x + bi;
        }

    }

    public static double getAvgError(double[][] tgt, double[][] hyp) {
        if (tgt.length != hyp.length) {
            throw new RuntimeException("length of target (" + tgt.length + ") and hyp (" + hyp.length + ")");
        }
        if (tgt[0].length != hyp[0].length) {
            throw new RuntimeException("length in second dimension of target (" + tgt[0].length + ") and hyp (" + hyp[0].length + ")");
        }
        double sum = 0;
        for (int i = 0; i < tgt.length; i++) {
            double[] ds = tgt[i];
            double[] fs = hyp[i];
            for (int j = 0; j < fs.length; j++) {
                sum += (fs[j] - ds[j]) * (fs[j] - ds[j]);
            }
        }
        sum /= tgt.length * tgt[0].length;
        sum = Math.sqrt(sum);
        return sum;
    }

    public static double[][] getLLH(double[][] mat) {
        double[][] featureLLH = new double[mat.length][mat[0].length];
        for (int i = 0; i < featureLLH.length; i++) {
            double[] dsLLH = featureLLH[i];
            double[] ds = mat[i];
            for (int j = 0; j < ds.length; j++) {
                dsLLH[j] = Math.log(ds[j]);
            }
        }
        return featureLLH;
    }

    public static double[][] getExp(double[][] mat) {
        double[][] featureExp = new double[mat.length][mat[0].length];
        for (int i = 0; i < featureExp.length; i++) {
            double[] dsLLH = featureExp[i];
            double[] ds = mat[i];
            for (int j = 0; j < ds.length; j++) {
                dsLLH[j] = Math.exp(ds[j]);
            }
        }
        return featureExp;
    }

}
