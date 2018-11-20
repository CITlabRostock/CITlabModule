////////////////////////////////////////////////
/// File:       SeamCarve.java
/// Created:    30.08.2016  12:50:53
/// Encoding:   UTF-8
////////////////////////////////////////////////
package de.uros.citlab.module.baseline2polygon;

import com.achteck.misc.log.Logger;
import com.achteck.misc.types.Pair;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.geom2d.types.Rectangle2DInt;
import de.planet.math.util.VectorUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * Desciption of SeamCarve
 *
 *
 * Since 30.08.2016
 *
 * @author Tobi <tobias.gruening.hro@gmail.com>
 */
public class SeamCarve {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(SeamCarve.class.getName());

    public static final int TOP_ORIENT = 1;
    public static final int BOT_ORIENT = 2;

    public static List<Pair<Integer, Polygon2DInt>> calcSeparatingSeams(short[][] sobelImg, Polygon2DInt medSeam, List<Polygon2DInt> obstacles) {
//        return calcSeparatingSeams(sobelImg, medSeam, obstacles, 0.0, 0.00025);
        return calcSeparatingSeams(sobelImg, medSeam, obstacles, 0.0, 0.001);
//        return calcSeparatingSeams(sobelImg, medSeam, obstacles, 0.0, 0.0);
    }

    public static List<Pair<Integer, Polygon2DInt>> calcSeparatingSeams(short[][] sobelImg, Polygon2DInt medSeam, List<Polygon2DInt> obstacles, double shiCfac, double distCostRatio) {
        List<Pair<Integer, Polygon2DInt>> separatingSeams = new ArrayList<Pair<Integer, Polygon2DInt>>();
        int width = sobelImg[0].length;
        int[] aMedSeam = getSeam(medSeam, width);
        
        List<int[]> obsSeams = new ArrayList<int[]>();
        for (Polygon2DInt aObs : obstacles) {
            obsSeams.add(getSeam(aObs, width));
        }

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
            for (int[] aCompSeam : obsSeams) {
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

        if (medMax - topMin + 1 < 5) {
            return null;
        }
        //            short[][] aCostMatA = new short[medMax - topMin + 1][endIdx - startIdx + 1];
        short[][] aCostMatAtrans = new short[endIdx - startIdx + 1][medMax - topMin + 1];
        int sum = 0;
        int cnt = 0;
        for (int j = topMin; j < medMax + 1; j++) {
            short[] tmpS = sobelImg[j];
//                short[] ds = aCostMatA[j - topMin];
            for (int k = startIdx; k < endIdx + 1; k++) {
                short val = tmpS[k];
//                    ds[k - startIdx] = (short)val;
                aCostMatAtrans[k - startIdx][j - topMin] = (short) val;
                sum += val;
                cnt++;
            }
        }
        
        int[] sMedTop = new int[newMed.length];
        for (int i = 0; i < sMedTop.length; i++) {
            sMedTop[i] = newMed[i] -5;
        }
        
        
        Pair<Integer, int[]> sepTop = doItDynamic(aCostMatAtrans, newTop, sMedTop, BOT_ORIENT, topMin, sum / cnt, distCostRatio);
        if (botMax - medMin + 1 < 5) {
            return null;
        }
        //            short[][] aCostMatB = new short[botMax - medMin + 1][endIdx - startIdx + 1];
        short[][] aCostMatBtrans = new short[endIdx - startIdx + 1][botMax - medMin + 1];
        sum = 0;
        cnt = 0;
        for (int j = medMin; j < botMax + 1; j++) {
            short[] tmpS = sobelImg[j];
//                int[] ds = aCostMatB[j - medMin];
            for (int k = startIdx; k < endIdx + 1; k++) {
                int val = tmpS[k];
                aCostMatBtrans[k - startIdx][j - medMin] = (short) val;
                sum += val;
                cnt++;
            }
        }
        Pair<Integer, int[]> sepBot = doItDynamic(aCostMatBtrans, newMed, newBot, TOP_ORIENT, medMin, sum / cnt, distCostRatio);

        
        
        int[] xVal = new int[newTop.length];
            for (int j = 0; j < xVal.length; j++) {
                xVal[j] = j + startIdx;
            }
        Polygon2DInt polyTop = new Polygon2DInt(xVal, sepTop.second, xVal.length);
        separatingSeams.add(new Pair<Integer, Polygon2DInt>(sepTop.first, polyTop));
        Polygon2DInt polyBot= new Polygon2DInt(xVal, sepBot.second, xVal.length);
        separatingSeams.add(new Pair<Integer, Polygon2DInt>(sepBot.first, polyBot));
        
        return separatingSeams;
    }

    /**
     *
     * @param transCostMat Cost Matrx Transposed
     * @param topIndices
     * @param bottomIndices
     * @param twoPass were to orient
     * @param shiftY
     * @param avgVal avg value of transCostMat
     * @param shiCfac
     * @param distCostRatio
     * @return
     */
    private static Pair<Integer,int[]> doItDynamic(short[][] transCostMat, int[] topIndices, int[] bottomIndices, int twoPass, int shiftY, int avgVal, double distCostRatio) {

        int defC = 50000;
        //passC times aCostMat value!
        double distC = (distCostRatio * avgVal);
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
            short[] actSob = transCostMat[i];
            for (int j = 0; j < actCost.length; j++) {
                int actC = 0;
                if (j <= aTop || j >= aBot) {
                    actC = defC;
                } else {
                    actC = (int)(actSob[j] + distC * Math.abs(aDist - j));
                }
                if (lastC != null) {
                    int[] c = new int[3];
                    if (j > 0) {
                        c[0] = lastC[j - 1];
                    } else {
                        c[0] = lastC[j] + lastC[j + 1] + 2 * defC;
                    }
                    c[1] = lastC[j];
                    if (j < lastC.length - 1) {
                        c[2] = lastC[j + 1];
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
        int cost = lastC[lastI];
        int[] separatingSeam = new int[topIndices.length];
        separatingSeam[separatingSeam.length - 1] = lastI + shiftY;
        for (int i = separatingSeam.length - 1; i >= 1; i--) {
            int toGo = dir[i][lastI];
            lastI = Math.min(Math.max(lastI + toGo, 0), dir[0].length-1);
            separatingSeam[i - 1] = lastI + shiftY;
        }
        return new Pair<Integer,int[]>(cost,separatingSeam);
    }

    private static int[] getSeam(Polygon2DInt poly, int width) {
        Rectangle2DInt polyBB = poly.getBounds();
        int[] nSeam = new int[width];
        for (int i = 0; i < polyBB.x; i++) {
            nSeam[i] = -1;
        }
        for (int i = polyBB.x; i < Math.min(polyBB.x + polyBB.w, width); i++) {
            nSeam[i] = getYval(i, poly);
        }
        for (int i = polyBB.x + polyBB.w; i < width; i++) {
            nSeam[i] = -1;
        }
        return nSeam;
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

}
