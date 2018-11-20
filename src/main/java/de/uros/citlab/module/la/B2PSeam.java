////////////////////////////////////////////////
/// File:       LineGenerationSeam.java
/// Created:    29.01.2016  14:35:46
/// Encoding:   UTF-8
////////////////////////////////////////////////
package de.uros.citlab.module.la;

import com.achteck.misc.log.Logger;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamSetOrganizer;
import de.planet.imaging.types.HybridImage;
import de.planet.imaging.types.StdFrameAppender;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.geom2d.types.Rectangle2DInt;
import de.planet.math.trafo.ITransform;
import de.planet.math.util.MatrixUtil;
import de.planet.math.util.PolygonHelper;
import de.planet.math.util.VectorUtil;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import de.uros.citlab.module.interfaces.IB2P;
import de.uros.citlab.module.util.ImageUtil;
import java.awt.Polygon;
import java.util.LinkedList;
import static org.opencv.core.Core.BORDER_DEFAULT;

/**
 * Desciption of LineGenerationSeam
 *
 *
 * Since 29.01.2016
 *
 * @author Tobi <tobias.gruening@uni-rostock.de>
 */
public class B2PSeam extends ParamSetOrganizer implements IB2P {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(B2PSeam.class.getName());

    protected final int TOP_ORIENT = 1;
    protected final int BOT_ORIENT = 2;

    @ParamAnnotation(name = "rat", descr = "distance cost ratio")
    protected double dpRatio = 0.0005;

    public B2PSeam() {
        addReflection(this, B2PSeam.class);
    }

    public List<Polygon2DInt> process(HybridImage image, List<Polygon2DInt> polygons) {
        short[][] sobelImg = getSobelImg(image);
        ArrayList<int[]> medSeamPolys = getMedSeams(polygons, image.getWidth());

        ArrayList<Polygon2DInt> sepSeams = calcSeparatingSeams(sobelImg, medSeamPolys, 0.0, dpRatio);

        return getLinePolys(sepSeams);
    }

    @Override
    public List<Polygon2DInt> process(HybridImage image, List<Polygon2DInt> baselines, Double orientation) {
        HybridImage imageRotated = image;
        List<Polygon2DInt> baselinesRotated = baselines;
        ITransform trafo = null;
        if (orientation != null && orientation != 0.0) {
            Polygon p = new Polygon(new int[]{0, 0, image.getWidth(), image.getWidth()}, new int[]{0, image.getHeight(), image.getHeight(), 0}, 4);
            imageRotated = ImageUtil.rotate(image, p, -orientation, 0, 0, 0);
            trafo = imageRotated.getTrafo();
            baselinesRotated = new LinkedList<>();
            for (Polygon2DInt polygon : baselines) {
                Polygon2DInt polygonRotated = PolygonHelper.copy(polygon);
                PolygonHelper.transform(polygonRotated, trafo);
                baselinesRotated.add(polygonRotated);
            }
        }
        if (orientation == null) {
            LOG.log(Logger.INFO, "no orientation available for Baseline2Polygon. Assume orientation = 0.0");
        }
        List<Polygon2DInt> polysRotated = process(imageRotated, baselinesRotated);

        if (LOG.isTraceEnabled()) {
            LOG.log(Logger.TRACE, new StdFrameAppender.AppenderContent(imageRotated, "rotated image and baselines", PolygonHelper.copy(baselinesRotated)));
            LOG.log(Logger.TRACE, new StdFrameAppender.AppenderContent(imageRotated, "rotated image and polygons", PolygonHelper.copy(polysRotated)));
        }
        List<Polygon2DInt> polys = polysRotated;
        if (trafo != null) {
            polys = new LinkedList<>();
            for (int i = 0; i < polysRotated.size(); i++) {
                Polygon2DInt polyOrig = polysRotated.get(i);
                if (polyOrig == null) {
                    polys.add(null);
                    continue;
                }
                PolygonHelper.invert(polyOrig, trafo);
                polys.add(polyOrig);
            }
        }
        if (LOG.isTraceEnabled()) {
            LOG.log(Logger.TRACE, new StdFrameAppender.AppenderContent(image, "original image and baselines", PolygonHelper.copy(baselines)));
            LOG.log(Logger.TRACE, new StdFrameAppender.AppenderContent(image, "original image and polygons", PolygonHelper.copy(polys)));
        }
//        LOG.log(Logger.INFO, new StdFrameAppender.AppenderContent(image, "test2", linePolys, true));
        return polys;
    }

    protected ArrayList<Polygon2DInt> calcSeparatingSeams(short[][] sobelImg, ArrayList<int[]> medialSeams, double shiCfac, double distCostRatio) {
        ArrayList<Polygon2DInt> separatingSeams = new ArrayList<Polygon2DInt>();
        for (int i = 0; i < medialSeams.size(); i++) {
            int[] aMedSeam = medialSeams.get(i);
            int startIdx = 0;
            //including
            int endIdx = aMedSeam.length - 1;
            for (int j = 0; j < aMedSeam.length; j++) {
                if (aMedSeam[j] != -1) {
                    startIdx = j;
                    break;
                }
            }
            for (int j = aMedSeam.length - 1; j >= 0; j--) {
                if (aMedSeam[j] != -1) {
                    endIdx = j;
                    break;
                }
            }

            int[] newMed = new int[endIdx - startIdx + 1];
            int[] newTop = new int[endIdx - startIdx + 1];
            int[] newBot = new int[endIdx - startIdx + 1];

            int medMax = 0;
            int medMin = sobelImg.length - 1;
            int topMin = sobelImg.length - 1;
            int botMax = 0;

            int minDistT = Integer.MAX_VALUE;
            int minDistB = Integer.MAX_VALUE;
            for (int j = startIdx; j <= endIdx; j++) {
                int aMed = aMedSeam[j];
                medMax = Math.max(medMax, aMed);
                medMin = Math.min(medMin, aMed);
                int aTop = 0;
                int aBot = sobelImg.length - 1;
                newMed[j - startIdx] = aMed;
                for (int[] aCompSeam : medialSeams) {
                    int aVal = aCompSeam[j];
                    if (aVal != -1) {
                        if (aVal < aMed && aVal > aTop) {
                            aTop = aVal;
                            minDistT = Math.min(minDistT, aMed - aVal);
                        }
                        if (aVal > aMed && aVal < aBot) {
                            aBot = aVal;
                            minDistB = Math.min(minDistB, aVal - aMed);
                        }
                    }
                }
                newTop[j - startIdx] = aTop;
                newBot[j - startIdx] = aBot;
                topMin = Math.min(topMin, aTop);
                botMax = Math.max(botMax, aBot);
            }

            if (minDistT > 20 && minDistT < Integer.MAX_VALUE) {
                topMin = Math.max(topMin, medMin - 2 * minDistT);
            }
            if (minDistB > 20 && minDistB < Integer.MAX_VALUE) {
                botMax = Math.min(botMax, medMax + 2 * minDistB);
            }

            topMin = Math.max(topMin, 0);
            medMin = Math.max(medMin, 0);
            botMax = Math.min(botMax, sobelImg.length - 1);
            medMax = Math.min(medMax, sobelImg.length - 1);

            if (medMax - topMin + 1 < 5) {
                separatingSeams.add(null);
                separatingSeams.add(null);
                continue;
            }
            int[][] aCostMatA = new int[medMax - topMin + 1][endIdx - startIdx + 1];
            int sum = 0;
            int cnt = 0;
            for (int j = topMin; j < medMax + 1; j++) {
                short[] tmpS = sobelImg[j];
                int[] ds = aCostMatA[j - topMin];
                for (int k = startIdx; k < endIdx + 1; k++) {
                    int val = tmpS[k];
                    ds[k - startIdx] = val;
                    sum += val;
                    cnt++;
                }
            }
            int[] tmSeam = doItDynamic(aCostMatA, newTop, newMed, BOT_ORIENT, topMin, sum / cnt, shiCfac, distCostRatio);
            if (botMax - medMin + 1 < 5) {
                separatingSeams.add(null);
                separatingSeams.add(null);
                continue;
            }
            int[][] aCostMatB = new int[botMax - medMin + 1][endIdx - startIdx + 1];
            sum = 0;
            cnt = 0;
            for (int j = medMin; j < botMax + 1; j++) {
                short[] tmpS = sobelImg[j];
                int[] ds = aCostMatB[j - medMin];
                for (int k = startIdx; k < endIdx + 1; k++) {
                    int val = tmpS[k];
                    ds[k - startIdx] = val;
                    sum += val;
                    cnt++;
                }
            }
            int[] bmSeam = doItDynamic(aCostMatB, newMed, newBot, TOP_ORIENT, medMin, sum / cnt, shiCfac, distCostRatio);

            int[] xVal = new int[bmSeam.length];
            for (int j = 0; j < xVal.length; j++) {
                xVal[j] = j + startIdx;
            }
            separatingSeams.add(new Polygon2DInt(xVal, tmSeam, xVal.length));
            separatingSeams.add(new Polygon2DInt(xVal, bmSeam, xVal.length));
        }
        return separatingSeams;
    }

    /**
     *
     * @param aCostMat
     * @param topIndices
     * @param bottomIndices
     * @param twoPass were to orient
     * @param shift
     * @param avgVal
     * @param shiCfac
     * @return
     */
    private int[] doItDynamic(int[][] aCostMat, int[] topIndices, int[] bottomIndices, int twoPass, int shiftY, int avgVal, double shiCfac, double distCostRatio) {

        int[][] transCostMat = MatrixUtil.transpose(aCostMat);

        int defC = 50000;
        //passC times aCostMat value!
        int passC = 100;
        int shiC = (int) (shiCfac * passC * avgVal);
        double distC = distCostRatio * passC * avgVal;
        int[] aDistIndices;
        switch (twoPass) {
            case TOP_ORIENT:
                aDistIndices = topIndices;
                break;
            case BOT_ORIENT:
                aDistIndices = bottomIndices;
                break;
            default:
                distC = 0;
                aDistIndices = bottomIndices;
                break;
        }

        //        int shiC = 0;
        int[][] costs = new int[transCostMat.length][transCostMat[0].length];
        int[][] dir = new int[transCostMat.length][transCostMat[0].length];
        int[] lastC = null;

        for (int i = 0; i < costs.length; i++) {
            int aTop = topIndices[i] - shiftY;
            int aBot = bottomIndices[i] - shiftY;
            int aDist = aDistIndices[i] - shiftY;
            int[] actCost = costs[i];
            int[] actDir = dir[i];
            int[] actSob = transCostMat[i];
            for (int j = 0; j < actCost.length; j++) {
                int actC = 0;
                if (j <= aTop || j >= aBot) {
                    actC = defC;
                } else {
                    actC = passC * actSob[j] + (int) (distC * Math.abs(aDist - j));
                }
                if (lastC != null) {
                    int[] c = new int[3];
                    if (j > 0) {
                        c[0] = lastC[j - 1] + shiC;
                    } else {
                        c[0] = lastC[j] + lastC[j + 1] + 2 * defC;
                    }
                    c[1] = lastC[j];
                    if (j < lastC.length - 1) {
                        c[2] = lastC[j + 1] + shiC;
                    } else {
                        c[2] = lastC[j] + lastC[j - 1] + 2 * defC;
                    }
//                    int minIndex = VectorUtil.getMinIndex(c);
                    int minIndex = 0;

                    if (c[1] <= c[0] && c[1] <= c[2]) {
                        minIndex = 1;
                    } else if (c[0] <= c[2]) {
                        minIndex = 0;
                    } else {
                        minIndex = 2;
                    }

                    actC += c[minIndex];
                    if (minIndex == 0) {
                        actDir[j] = -1;
                    }
                    if (minIndex == 1) {
                        actDir[j] = 0;
                    }
                    if (minIndex == 2) {
                        actDir[j] = 1;
                    }
                }
                actCost[j] = actC;
            }
            lastC = actCost;
        }
        int lastI = VectorUtil.getMinIndex(lastC);
        int[] separatingSeam = new int[topIndices.length];
        separatingSeam[separatingSeam.length - 1] = lastI + shiftY;
        final int max = dir[0].length - 1;
        for (int i = separatingSeam.length - 1; i >= 1; i--) {
            lastI = Math.max(Math.min(lastI, max), 0);
            int toGo = dir[i][lastI];
            lastI = lastI + toGo;
            separatingSeam[i - 1] = lastI + shiftY;
        }
        return separatingSeam;
    }

    protected short[][] getSobelImg(HybridImage inp) {
        Mat inpMat = inp.getAsOpenCVMatImage();

        /// Generate grad_x and grad_y
        Mat sobel_x = new Mat(inpMat.rows(), inpMat.cols(), CvType.CV_16S);
        Mat sobel_y = new Mat(inpMat.rows(), inpMat.cols(), CvType.CV_16S);

        /// Gradient X
        Imgproc.Sobel(inpMat, sobel_x, CvType.CV_16S, 1, 0, 3, 1, 0, BORDER_DEFAULT);
        /// Gradient Y
        Imgproc.Sobel(inpMat, sobel_y, CvType.CV_16S, 0, 1, 3, 1, 0, BORDER_DEFAULT);

        short[][] sobel_x_s = convertToShort(sobel_x);
        short[][] sobel_y_s = convertToShort(sobel_y);

        short[][] sobel_s = new short[sobel_x_s.length][sobel_x_s[0].length];
        for (int i = 0; i < sobel_x_s.length; i++) {
            short[] sobel_x_s1 = sobel_x_s[i];
            short[] sobel_y_s1 = sobel_y_s[i];
            short[] sobel1 = sobel_s[i];
            for (int j = 0; j < sobel1.length; j++) {
                sobel1[j] = (short) (Math.abs(sobel_x_s1[j]) + Math.abs(sobel_y_s1[j]));
            }
        }
        sobel_x.release();
        sobel_y.release();
        return sobel_s;
    }

    public short[][] convertToShort(Mat inp) {
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

    public static ArrayList<int[]> getMedSeams(List<Polygon2DInt> poly, int width) {
        ArrayList<int[]> resPoly = new ArrayList<int[]>();

        for (Polygon2DInt aPoly : poly) {
            Rectangle2DInt polyBB = aPoly.getBounds();
            int[] nPoly = new int[width];
            for (int i = 0; i < polyBB.x; i++) {
                nPoly[i] = -1;
            }
            for (int i = polyBB.x; i < Math.min(polyBB.x + polyBB.w, width); i++) {
                nPoly[i] = getYval(i, aPoly);
            }
            for (int i = polyBB.x + polyBB.w; i < width; i++) {
                nPoly[i] = -1;
            }
            resPoly.add(nPoly);

        }

        return resPoly;
    }

    private static int getYval(int i, Polygon2DInt aPoly) {
        int xL = Integer.MIN_VALUE;
        int yL = 0;
        int xR = Integer.MAX_VALUE;
        int yR = 0;

        for (int j = 0; j < aPoly.npoints; j++) {
            int aX = aPoly.xpoints[j];
            if (aX > xL && aX <= i) {
                xL = aX;
                yL = aPoly.ypoints[j];
            }
            if (aX < xR && aX >= i) {
                xR = aX;
                yR = aPoly.ypoints[j];
            }
        }
        if (xL == xR) {
            return yL;
        }

        int nY = (int) (((double) yR - yL) / (xR - xL) * (i - xL) + yL);
        return nY;
    }

    protected ArrayList<Polygon2DInt> getLinePolys(ArrayList<Polygon2DInt> sepSeams) {
        ArrayList<Polygon2DInt> res = new ArrayList<>();
        for (int i = 0; i < sepSeams.size() - 1; i += 2) {
            if (sepSeams.get(i) == null) {
                res.add(null);
                continue;
            }
            Polygon2DInt nLine = new Polygon2DInt();
            PolygonHelper.addPoints(nLine, sepSeams.get(i));
            Polygon2DInt sPoly = sepSeams.get(i + 1);
            for (int j = sPoly.npoints - 1; j >= 0; j--) {
                nLine.addPoint(sPoly.xpoints[j], sPoly.ypoints[j]);
            }
            res.add(nLine);
        }
        return res;
    }

}
