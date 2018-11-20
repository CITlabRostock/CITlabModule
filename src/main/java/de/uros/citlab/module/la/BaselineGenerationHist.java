/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.la;

import de.uros.citlab.module.interfaces.IP2B;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import de.planet.imaging.types.ByteImage;
import de.planet.imaging.types.HybridImage;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.trafo.ITransform;
import de.planet.math.trafo.TrafoUtil;
import de.planet.math.util.PolygonHelper;
import de.planet.math.util.VectorUtil;
import de.planet.util.preproc.ContrastNormalizer6;
import de.uros.citlab.module.types.LineImage;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.PolygonUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.types.Image;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author gundram
 */
public class BaselineGenerationHist /*extends ParamTreeOrganizer*/ implements Serializable, IP2B {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(BaselineGenerationHist.class.getName());
    private ContrastNormalizer6 cn;
    final private int maxPoints = 10;
    final private int grid = 100;
    final private int step = 3;
    final private int blur = 2;
//    @ParamAnnotation(descr = "default shift in y-dimension (positiv=lower)")

    public BaselineGenerationHist() {
        cn = new ContrastNormalizer6();
        cn.setParamSet(cn.getDefaultParamSet(new ParamSet()));
        cn.init();
    }

    private static int[][] getMatShrink(byte[][] mat, int grid, int step) {
        int[][] pointsMat = new int[(mat[0].length - 1) / grid + 1][mat.length];
        for (int y = 0; y < mat.length; y++) {
            byte[] vec = mat[y];
            for (int x = 0; x < vec.length; x += step) {
                pointsMat[x / grid][y] += 255 - (vec[x] & 0xFF);
            }
        }
        return pointsMat;
    }

    private static Polygon2DInt resizeInterpolate(double[] vec_src, int resize_factor, int length_tgt) throws NullPointerException, ArrayIndexOutOfBoundsException {
        Polygon2DInt p = new Polygon2DInt();
        for (int i = 0; i < vec_src.length; i++) {
            p.addPoint(i * resize_factor, (int) (vec_src[i] + 0.5));
        }
        double slope = (vec_src[0] - vec_src[vec_src.length - 1]) / (resize_factor * vec_src.length);
        p.addPoint(length_tgt - 1, (int) (vec_src[vec_src.length - 1] + slope * (length_tgt - 1 - vec_src.length * resize_factor) + 0.5));
        return p;
    }

    private double[][] getQuantilAndSum4Hist(final int[][] mat, double q) {
        double[] borders = new double[mat.length];
        double[] weights = new double[mat.length];
        for (int i = 0; i < borders.length; i++) {
            final int[] distr = mat[i];
            for (int j = 1; j < distr.length; j++) {
                distr[j] += distr[j - 1];
            }
            final int sum = distr[distr.length - 1];
            if (sum == 0) {
                borders[i] = (distr.length - 1) * 0.5;
            } else {
                weights[i] = sum;
                int border = (int) (sum * q + 0.5);
                int j = 0;
                while (distr[j] < border) {
                    j++;
                }
                if (j == 0) {
                    borders[i] = i > 0 ? borders[i - 1] : (distr.length - 1) * 0.5;
                } else {
                    borders[i] = j + (border - distr[j]) / (distr[j] - distr[j - 1]);
                }
            }
        }
        return new double[][]{borders, weights};
    }

    private void blur(final double[] in_y, final double[] in_sum, final double[] out_y, final double[] out_sum) {
        final double sumBefore = 2 * in_sum[0] + in_sum[1];
        out_y[0] = sumBefore == 0.0
                ? (2 * in_y[0] + in_y[1]) / 3.0
                : (2 * in_y[0] * in_sum[0] + in_y[1] * in_sum[1]) / sumBefore;
        out_sum[0] = sumBefore;
        for (int i = 1; i < out_sum.length - 1; i++) {
            final double sumMiddle = in_sum[i - 1] + 2 * in_sum[i] + in_sum[i + 1];
            out_y[i] = sumMiddle == 0.0
                    ? (2 * in_y[i] + in_y[i + 1] + in_y[i - 1]) / 4.0
                    : (2 * in_y[i] * in_sum[i] + in_y[i + 1] * in_sum[i + 1] + in_y[i - 1] * in_sum[i - 1]) / sumMiddle;
            out_sum[i] = sumMiddle;
        }
        final double sumAfter = 2 * in_sum[out_sum.length - 1] + in_sum[out_sum.length - 2];
        out_y[out_sum.length - 1] = sumAfter == 0.0
                ? (2 * in_y[out_sum.length - 1] + in_y[out_sum.length - 2]) / 3.0
                : (2 * in_y[out_sum.length - 1] * in_sum[out_sum.length - 1] + in_y[out_sum.length - 2] * in_sum[out_sum.length - 2]) / sumAfter;
        out_sum[out_sum.length - 1] = sumAfter;
    }

    private Polygon2DInt calcAverageLine(HybridImage hi_orig, double q) {
        ByteImage bi_orig = hi_orig.getAsByteImage();
        byte[][] mat_orig = bi_orig.pixels[0];
        int[][] matShrink = getMatShrink(mat_orig, grid, step);
        double[][] quantilAndSum4Hist = getQuantilAndSum4Hist(matShrink, q);
        double[] quantil = quantilAndSum4Hist[0];
        double[] sum = quantilAndSum4Hist[1];
        if (blur > 0 && quantil.length > 1) {
            double[] quantilOut = new double[quantil.length];
            double[] sumOut = new double[quantil.length];
            double[] tmp;
            for (int i = 0; i < blur; i++) {
                blur(quantil, sum, quantilOut, sumOut);
                tmp = quantil;
                quantil = quantilOut;
                quantilOut = tmp;
                tmp = sum;
                sum = sumOut;
                sumOut = tmp;
            }
        }
        return resizeInterpolate(quantil, grid, mat_orig[0].length);
    }

    @Override
    public void processImage(Image image, PcGtsType page) {
        HybridImage hi = ImageUtil.getHybridImage(image, true);
        List<TextRegionType> textRegions = PageXmlUtils.getTextRegions(page);
        for (TextRegionType textRegion : textRegions) {
            processRegion(hi, textRegion);
        }
    }

    @Override
    public void processLine(HybridImage image, TextLineType tlt, TextRegionType rt) {
        LineImage li = new LineImage(image, tlt, rt);
        HybridImage subImage = li.getSubImage();
        ITransform trafo = subImage.getTrafo();
        HybridImage contrasnormalized = cn.preProcess(subImage);
//        LOG.log(Logger.WARN, new StdFrameAppender.AppenderContent(contrasnormalized, true));
//        double angle = trt != null && trt.getOrientation() != null ? trt.getOrientation().doubleValue() : 0.0;
        Polygon2DInt bl = calcAverageLine(contrasnormalized, 0.5);
        Polygon2DInt bld = calcAverageLine(contrasnormalized, 0.8);
        int shift = (VectorUtil.addElements(bld.ypoints) - VectorUtil.addElements(bl.ypoints)) / bld.npoints;
        Polygon2DInt blShort = new Polygon2DInt();
        if (bl.npoints <= maxPoints) {
            blShort = PolygonHelper.copy(bl);
            blShort.translate(0, shift);
        } else {
            for (int i = 0; i < maxPoints; i++) {
                int idx = (int) (((double) i * (bl.npoints - 1)) / (maxPoints - 1) + 0.5);
                blShort.addPoint(bl.xpoints[idx], bl.ypoints[idx] + shift);
            }
        }
//        bl.translate(0, shift);
        Polygon2DInt blTransformed = TrafoUtil.transform(trafo.invert(), blShort);
        tlt.setBaseline(PolygonUtil.polyon2TrpBaseline(blTransformed));
    }

    @Override
    public void processRegion(HybridImage img, TextRegionType trt) {
        for (TextLineType coord : trt.getTextLine()) {
            processLine(img, coord, trt);
        }
    }
}
