/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.types.Pair;
import de.planet.imaging.algo.GrabQuad;
import de.planet.imaging.types.*;
import de.planet.imaging.util.ImageHelper;
import de.planet.imaging.util.Rot90;
import de.planet.itrtech.types.ImagePropertyIDs;
import de.planet.math.geom2d.types.Point2DDouble;
import de.planet.math.geom2d.types.Point2DInt;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.geom2d.types.Rectangle2DInt;
import de.planet.math.geom2d.util.Geom2DConversionUtil;
import de.planet.math.trafo.TrafoUtil;
import de.planet.math.trafo.TransformAffine;
import de.planet.math.util.MatrixUtil;
import de.planet.math.util.PolygonHelper;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextEquivType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.types.Image;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 *
 * @author gundram
 */
public class ImageUtil {

    public static Logger LOG = LoggerFactory.getLogger(ImageUtil.class);

    public static HybridImage getHybridImage(Image image, String[] props) {
        return getHybridImage(image, false, props);
    }

    public static HybridImage getHybridImage(Image image) {
        return getHybridImage(image, false, null);
    }

    public static HybridImage getHybridImage(Image image, boolean toGrey) {
        return getHybridImage(image, toGrey, null);
    }
    public static short[][] calcAbsSobelSum(Mat inp) {
        /// Generate grad_x and grad_y
        Mat sobel_x = new Mat(inp.rows(), inp.cols(), CvType.CV_16S);
        Mat sobel_y = new Mat(inp.rows(), inp.cols(), CvType.CV_16S);

        /// Gradient X
        Imgproc.Scharr(inp, sobel_x, CvType.CV_16S, 1, 0);
//        Imgproc.Sobel(inp, sobel_x, CvType.CV_16S, 1, 0, 3, 1, 0, BORDER_DEFAULT);

        /// Gradient Y
        Imgproc.Scharr(inp, sobel_y, CvType.CV_16S, 0, 1);
//        Imgproc.Sobel(inp, sobel_y, CvType.CV_16S, 0, 1, 3, 1, 0, BORDER_DEFAULT);

        short[][] sobel_x_s = convertToShort(sobel_x);
        short[][] sobel_y_s = convertToShort(sobel_y);

        short[][] sobel_s = new short[sobel_x_s.length][sobel_x_s[0].length];
        for (int i = 0; i < sobel_x_s.length; i++) {
            short[] sobel_x_s1 = sobel_x_s[i];
            short[] sobel_y_s1 = sobel_y_s[i];
            short[] sobel1 = sobel_s[i];
            for (int j = 0; j < sobel1.length; j++) {
                sobel1[j] = (short) ((Math.abs(sobel_x_s1[j]) + Math.abs(sobel_y_s1[j])) / 4);
            }
        }
        sobel_x.release();
        sobel_y.release();
        return sobel_s;
    }

    public static boolean write(RenderedImage im,
            String formatName,
            File output) {
        try {
            return ImageIO.write(im, formatName, output);
        } catch (IOException ex) {
            throw new RuntimeException("cannot save image to file '" + output == null ? "null" : output.getAbsolutePath() + "' and format '" + formatName + "'.", ex);
        }
    }

    final public static BufferedImage convertColorspace(BufferedImage image, int newType) {
        try {
            BufferedImage raw_image = image;
            image = new BufferedImage(raw_image.getWidth(), raw_image.getHeight(), newType);
            ColorConvertOp xformOp = new ColorConvertOp(null);
            xformOp.filter(raw_image, image);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return image;
    }

    public static HybridImage getHybridImage(Image image, boolean toGrey, String[] properties) {
        Set<Image.Type> availableTypes = image.getAvailableTypes();
        HybridImage res = null;
        if (availableTypes.contains(Image.Type.OPEN_CV)) {
            res = HybridImage.newInstance(image.getImageOpenCVImage());
        } else {
            BufferedImage bufImg = image.getImageBufferedImage(true);
            if (bufImg.getType() == BufferedImage.TYPE_CUSTOM || bufImg.getType() == BufferedImage.TYPE_4BYTE_ABGR || bufImg.getType() == BufferedImage.TYPE_4BYTE_ABGR_PRE || bufImg.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
                bufImg = convertColorspace(bufImg, BufferedImage.TYPE_3BYTE_BGR);
            }
            res = HybridImage.newInstance(bufImg);
        }

        if (toGrey) {
            res = ImageHelper.makeGrey(res);
        }
        if (PropertyUtil.hasProperty(properties, "mask")) {
            Polygon2DInt mask = PolygonUtil.string2Polygon2DInt(PropertyUtil.getProperty(properties, "mask"));
            res.setProperty(ImagePropertyIDs.MASK.toString(), Arrays.asList(mask));
        }
        return res;
    }

    public static Image getImage(HybridImage hi) {
        Mat openCVMatImage = hi.getOpenCVMatImage();
        if (openCVMatImage != null) {
            return new Image(openCVMatImage);
        }
        BufferedImage bi = hi.getBufferedImage();
        if (bi != null) {
            return new Image(bi);
        }
        return new Image(hi.getAsOpenCVMatImage());
    }

    public static BufferedImage copy(BufferedImage original) {
        BufferedImage newImage = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
        Graphics g = newImage.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();
        return newImage;
    }

    private static List<Color> colorMap2 = new ArrayList<>();
    private static List<Color> colorMap3 = new ArrayList<>();
    private static List<Color> colorMap4 = new ArrayList<>();
    private static List<Color> colorMap5 = new ArrayList<>();
    private static List<Color> colorMap7 = new ArrayList<>();
//    private static List<Color> colorMap5;

    static {
        int[] black = new int[]{0, 0, 0};//black
        int[] blue = new int[]{0, 0, 1};//blue
        int[] cyan = new int[]{0, 1, 1};//cyan
        int[] green = new int[]{0, 1, 0};//green
        int[] yellow = new int[]{1, 1, 0};//yellow
        int[] red = new int[]{1, 0, 0};//red
        int[] white = new int[]{1, 1, 1};//white
        colorMap7 = getMap(new int[][]{black, blue, cyan, green, yellow, red, white});
        colorMap4 = getMap(new int[][]{black, red, yellow, white});
        colorMap5 = getMap(new int[][]{blue, cyan, green, yellow, red});
        colorMap3 = getMap(new int[][]{blue, green, red});
        colorMap2 = getMap(new int[][]{black, white});
    }

    private static List<Color> getMap(int[][] colors) {
        List<Color> res = new ArrayList<>(256 * (colors.length - 1));
        for (int c = 0; c < colors.length - 1; c++) {
            int[] low = colors[c];
            int[] high = colors[c + 1];
            for (int i = 0; i < 256; i++) {
                res.add(new Color(
                        high[0] * i + low[0] * (255 - i),
                        high[1] * i + low[1] * (255 - i),
                        high[2] * i + low[2] * (255 - i)));
            }
        }
        return res;
    }

    public static void main(String[] args) throws InvalidParameterException, IOException {
        ImageUtil instance = new ImageUtil();
        double[][] mat = new double[200][200];
        for (int j = 0; j < mat.length; j++) {
            double[] ds = mat[j];
            for (int i = 0; i < ds.length; i++) {
                ds[i] = Math.sqrt(Math.abs((i - 40) * (j - 50))) / 50.0;
            }
        }
        Pair<Double, Double> minMax = MatrixUtil.getMinMax(mat);
        int[] colors = new int[]{2, 3, 4, 5, 7};
        for (int color : colors) {
            getHeatMap(mat, minMax.second, minMax.first, color).save("out" + color + ".png");
        }
    }

    public static HybridImage getHeatMap(double[][] matrix, int colors) {
        return getHeatMap(matrix, colors, false);
    }

    public static HybridImage getHeatMap(double[][] matrix, int colors, boolean invert) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double[] ds : matrix) {
            for (int i = 0; i < ds.length; i++) {
                min = Math.min(ds[i], min);
                max = Math.max(ds[i], max);
            }
        }
        return invert ? getHeatMap(matrix, max, min, colors) : getHeatMap(matrix, min, max, colors);
    }

    public static HybridImage getHeatMap(double[][] matrix, double low, double high, int colors) {
        List<Color> colorMap = null;
        switch (colors) {
            case 2:
                colorMap = colorMap2;
                break;
            case 3:
                colorMap = colorMap3;
                break;
            case 4:
                colorMap = colorMap4;
                break;
            case 5:
                colorMap = colorMap5;
                break;
            case 7:
                colorMap = colorMap7;
                break;
            default:
                throw new RuntimeException("unknown color count " + colors);
        }
        double factor = (colorMap.size() - 1) / (high - low);
        double offset = 0.5 - low * factor;
        BufferedImage bi = new BufferedImage(matrix[0].length, matrix.length, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < matrix.length; i++) {
            double[] vec = matrix[i];
            for (int j = 0; j < vec.length; j++) {
                bi.setRGB(j, i, colorMap.get((int) (vec[j] * factor + offset)).getRGB());
            }
        }
        return HybridImage.newInstance(bi);
    }

    public static BufferedImage getDebugImage(BufferedImage pageImg, PcGtsType pageResults, double fontHeight, boolean overlay, boolean drawRegion, boolean drawBaseline, boolean drawPolygon, boolean skipConfidences) {
        return getDebugImage(pageImg, pageResults, fontHeight, overlay, drawRegion, drawBaseline ? 0.0 : -1.0, drawPolygon, skipConfidences);
    }

    public static BufferedImage getDebugImage(BufferedImage pageImg, PcGtsType pageResults, double fontHeight, boolean overlay, boolean drawRegion, double drawBaseline, boolean drawPolygon, boolean skipConfidences) {
        BufferedImage bi_res = new BufferedImage(pageImg.getWidth() * (overlay || fontHeight <= 0 ? 1 : 2), pageImg.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics graphics = bi_res.getGraphics();
        graphics.drawImage(pageImg, 0, 0, Color.WHITE, null);
//        bi_res.flush();
        printPolygons(bi_res, pageResults, drawRegion, drawBaseline, drawPolygon);
        if (fontHeight > 0) {
            double sum = 0.0, factor = 0.0;
            List<TextLineType> textLines = PageXmlUtil.getTextLines(pageResults);
            for (TextLineType textLine : textLines) {
//                List<PropertyType> property = textLine.getProperty();
                if (textLine.getTextEquiv() != null) {
                    factor += textLine.getTextEquiv().getUnicode().length();
                    sum += PolygonUtil.getPolygon(textLine).getBounds().width;
                }
            }
            sum /= factor;
            int meanLineHeight = (int) Math.round(1.5 * sum * fontHeight);
            Font stringFont = new Font("SansSerif", Font.PLAIN, meanLineHeight);
            graphics.setFont(stringFont);
            graphics.setColor(Color.WHITE);
            int offset = overlay ? 0 : pageImg.getWidth();
            for (TextLineType textLine : textLines) {
                TextEquivType result = textLine.getTextEquiv();
                if (result == null) {
                    continue;
                }
                String unicode = result.getUnicode();
                String confidence = result.getConf() == null ? "?" : String.format("%.3f", result.getConf());
                Rectangle2D rect = PolygonUtil.getPolygon(textLine).getBounds();
//            result = result.replace("\u017f", "s");
                graphics.drawString(unicode + (skipConfidences ? "" : "(" + confidence + ")"),
                        (int) (offset + rect.getMinX()),
                        (int) (rect.getMinY() + Math.round(0.8 * rect.getHeight())));
            }
        }
        return bi_res;
    }

    public static void printPolygons(BufferedImage image, PcGtsType page, boolean drawRegion, boolean drawBaseline, boolean drawPolygon) {
        printPolygons(image, page, drawRegion, drawBaseline ? 0.0 : -1.0, drawPolygon);
    }

    public static void printPolygons(BufferedImage image, PcGtsType page, boolean drawRegion, double drawBaseline, boolean drawPolygon) {
        if (image == null) {
            throw new RuntimeException("image is null");
        }
        if (page == null) {
            throw new RuntimeException("page is null");
        }
//        BufferedImage imageBufferedImage = copy(image);
        Graphics graphics = image.getGraphics();
        Graphics2D g2 = (Graphics2D) graphics.create();
        List<TextRegionType> textRegions = PageXmlUtils.getTextRegions(page);
        for (TextRegionType reg : textRegions) {
            if (drawRegion) {
                g2.setStroke(new BasicStroke(5));
                g2.setColor(Color.yellow);
                g2.drawPolygon(PolygonUtil.string2Polygon(reg.getCoordinates()));
            }
            g2.setStroke(new BasicStroke(3));
            for (TextLineType line : reg.getTextLine()) {
                if (drawPolygon && line.getCoords() != null) {
                    g2.setColor(Color.blue);
                    try {
                        g2.drawPolygon(PolygonUtil.getPolygon(line));
                    } catch (RuntimeException ex) {
                        LOG.info("cannot draw polygon of line {} because coords are {}", line.getId(), line.getCoords().getPoints());
                    }
                }
                if (line.getBaseline() != null) {
                    g2.setColor(drawBaseline >= 0.0 && line.getTextEquiv() != null && line.getTextEquiv().getConf() != null && line.getTextEquiv().getConf() >= drawBaseline ? Color.GREEN : Color.RED);
//                    g2.setColor(Color.red);
                    Polygon baseline = PolygonUtil.getBaseline(line);
                    g2.drawPolyline(baseline.xpoints, baseline.ypoints, baseline.npoints);
                }
            }
        }
    }

    public static BufferedImage resize(BufferedImage img, Integer newW, Integer newH) {
        if (newW == null && newH == null) {
            return img;
        }
        if (newH == null) {
            newH = (int) (img.getHeight() / (double) img.getWidth() * newW);
        }
        if (newW == null) {
            newW = (int) (img.getWidth() / (double) img.getHeight() * newH);
        }
        BufferedImage dbi = null;
        if (img != null) {
            dbi = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = dbi.createGraphics();
            AffineTransform at = AffineTransform.getScaleInstance(newW / (double) img.getWidth(), newH / (double) img.getHeight());
            g.drawRenderedImage(img, at);
        }
        return dbi;
    }

    public static HybridImage rotate(HybridImage actCutInp, Polygon bound, double angle, int addX, int addYT, int addYB) {
        Polygon2DInt aPoly = new Polygon2DInt(bound.xpoints, bound.ypoints, bound.npoints);
        return rotate(actCutInp, aPoly, angle, addX, addYT, addYB);

    }

    public static HybridImage rotate(HybridImage actCutInp, Polygon2DInt bound, double angle, int addX, int addYT, int addYB) {
        if (angle == 0.0 || angle == Math.PI || angle == Math.PI / 2.0 || angle == 3.0 * Math.PI / 2.0) {
            Rectangle2DInt bb = bound.getBounds();
            int ox, addL, oy, addT;
            if (angle == 0.0 || angle == Math.PI) {
                ox = Math.max(0, bb.x - addX);
                addL = bb.x - ox;
                oy = Math.max(bb.y - addYT, 0);
                addT = bb.y - oy;
            } else {
                ox = Math.max(0, bb.x - addYT);
                addL = bb.x - ox;
                oy = Math.max(bb.y - addX, 0);
                addT = bb.y - oy;
            }
            ByteImage cut = SubImage.get(actCutInp.getAsByteImage(), ox, oy, bb.w + (addX + addL), bb.h + (addYB + addT), false);
            if (angle == 0.0) {
                return HybridImage.newInstance(cut);
            }
            if (angle == Math.PI) {
                return HybridImage.newInstance(Rot90.rot180(cut));
            }
            if (angle == Math.PI / 2.0) {
                return HybridImage.newInstance(Rot90.rot90(cut));
            }
            if (angle == 3.0 * Math.PI / 2.0) {
                return HybridImage.newInstance(Rot90.rot270(cut));
            }
        }

        //Calc Polygon in actual Size and Position
        Polygon2DInt modBound = PolygonHelper.copy(bound);
        TransformAffine rotIns = TrafoUtil.getRotateInstance(-angle);
        TransformAffine rotInsB = TrafoUtil.getRotateInstance(angle);
        Polygon2DInt rotatedBound = rotate(modBound, rotIns);
        Rectangle2DInt rotatedBB = rotatedBound.getBounds();
        rotatedBB.x -= addX;
        rotatedBB.w += 2 * addX;
        rotatedBB.y -= addYT;
        rotatedBB.h += addYT + addYB;
        double[] origBB = rotate(rotatedBB, rotInsB);
        GrabQuad grabObj = new GrabQuad();
        grabObj.setBorderMode(BorderMode.REPLICATE);
        grabObj.setBorderValue(255.0);
        grabObj.setSrcPoint(0, origBB[0], origBB[1]);
        grabObj.setSrcPoint(1, origBB[2], origBB[3]);
        grabObj.setSrcPoint(2, origBB[4], origBB[5]);
        grabObj.setInterpolation(Interpolation.CUBIC);
//            grabObj.setSrcPoint(3, origBB[6], origBB[7]); // not necessary for rect: affine, not perspective warp
//        HybridImage tmpPic = HybridImage.newInstance(actCutInp.getAsByteImage());
        HybridImage ret = grabObj.grab(actCutInp, null);
//        System.out.println("Rotat Time: " + (System.currentTimeMillis() - aT));
//        ByteImage resByte = ret.getAsByteImage();
//        tmpPic.clear();
//        ret.clear();
        return ret;
    }

    private static Polygon2DInt rotate(Polygon2DInt src, TransformAffine rotObj) {
        return TrafoUtil.transform(rotObj, src);
    }

    private static double[] rotate(Rectangle2DInt src, TransformAffine rotObj) {
        double[] dstPts = new double[8];
        Point2DInt[] vertices = src.getVertices();
        int idx = 0;
        for (Point2DInt pdi : vertices) {
            Point2DDouble transformed = TrafoUtil.transform(rotObj, Geom2DConversionUtil.point2DIntToPoint2DDouble(pdi));
            dstPts[idx++] = transformed.x;
            dstPts[idx++] = transformed.y;
        }
        return dstPts;
    }

    private static short[][] convertToShort(Mat inp) {
        if (inp == null) {
            return null;
        }
        if (inp.type() == CvType.CV_16S) {
            final int w = inp.cols();
            final int h = inp.rows();
            final short[][] pix = new short[h][w];
            for (int y = 0; y < h; ++y) {
                inp.get(y, 0, pix[y]);
            }
            return pix;
        } else {
            return null;
        }
    }

    public static short[][] getCombineSobelImg(HybridImage inp) {
        Mat inpMat = inp.getAsOpenCVMatImage();

        /// Generate grad_x and grad_y
        Mat sobel_x = new Mat(inpMat.rows(), inpMat.cols(), CvType.CV_16S);
        Mat sobel_y = new Mat(inpMat.rows(), inpMat.cols(), CvType.CV_16S);

        /// Gradient X
        Imgproc.Sobel(inpMat, sobel_x, CvType.CV_16S, 1, 0, 3, 1, 0, Core.BORDER_DEFAULT);

        /// Gradient Y
        Imgproc.Sobel(inpMat, sobel_y, CvType.CV_16S, 0, 1, 3, 1, 0, Core.BORDER_DEFAULT);

        short[][] sobel_x_s = convertToShort(sobel_x);
        short[][] sobel_y_s = convertToShort(sobel_y);

        short[][] sobel_s = new short[sobel_x_s.length][sobel_x_s[0].length];
        for (int i = 0; i < sobel_x_s.length; i++) {
            short[] sobel_x_s1 = sobel_x_s[i];
            short[] sobel_y_s1 = sobel_y_s[i];
            short[] sobel1 = sobel_s[i];
            for (int j = 0; j < sobel1.length; j++) {
                sobel1[j] = (short) (Math.abs(sobel_x_s1[j]) + Math.abs(sobel_y_s1[j]));
//                sobel1[j] = (short) Math.abs(sobel_x_s1[j]);
            }
        }
        sobel_x.release();
        sobel_y.release();
        return sobel_s;
    }

}
