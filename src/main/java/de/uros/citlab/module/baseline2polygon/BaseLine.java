////////////////////////////////////////////////
/// File:       BaseLineAugmented.java
/// Created:    29.08.2016  09:59:37
/// Encoding:   UTF-8
////////////////////////////////////////////////
package de.uros.citlab.module.baseline2polygon;

import com.achteck.misc.types.Pair;
import de.planet.imaging.algo.BinarizeTheshold;
import de.planet.imaging.algo.ThresOtsu;
import de.planet.imaging.types.ByteImage;
import de.planet.imaging.types.HybridImage;
import de.planet.imaging.util.ByteImageHelper;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.geom2d.types.Rectangle2DInt;
import de.planet.math.util.PolygonHelper;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.PolygonUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Desciption of BaseLineAugmented
 *
 *
 * Since 29.08.2016
 *
 * @author Tobi <tobias.gruening.hro@gmail.com>
 */
public class BaseLine {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(BaseLine.class.getName());

    private Polygon2DInt bL, bLmon, bLThin;
    private double orientation;
    private double minDist = Double.MAX_VALUE;

    private HybridImage subImg, subImgBin;
    private short[][] sobelI;
    private Polygon2DInt bLmonTrafo, bLmonTrafoBlown, bLmonTrafoThin;
    private Polygon2DInt bLmonTrafoShifted;

    private List<Polygon2DInt> obstacle;

    private Pair<Integer, Polygon2DInt> topM, botM;

    private Polygon2DInt surPolyTrafo;

    private boolean topBounded = false;

//    private double[] regLineStats;
    public BaseLine(Polygon2DInt bL) {
        this.bL = bL;
    }

    public Polygon2DInt getbL() {
        return bL;
    }

    public double getOrientation() {
        return orientation;
    }

    public Polygon2DInt getbLmon() {
        return bLmon;
    }

    public double getMinDist() {
        return minDist;
    }

    public HybridImage getSubImg() {
        return subImg;
    }

    public Pair<Integer, Polygon2DInt> getSCTop() {
        return topM;
    }

    public Pair<Integer, Polygon2DInt> getSCBot() {
        return botM;
    }

    public Polygon2DInt getSurPoly() {
        Polygon2DInt surPoly = PolygonHelper.copy(surPolyTrafo);
        PolygonHelper.invert(surPoly, subImg.getTrafo());
        return surPoly;
    }

    public Polygon2DInt getbLShifted() {
        Polygon2DInt bS = PolygonHelper.copy(bLmonTrafoShifted);
        PolygonHelper.invert(bS, subImg.getTrafo());
        return bS;
    }

    public void setOrientation(double orientation) {
        this.orientation = orientation;
    }

    public void setbLmon(Polygon2DInt bLmon) {
        Polygon2DInt blown = PolygonUtil.blowUp(bLmon);
        bLThin = PolygonUtil.thinOut(blown, 10);
        this.bLmon = bLmon;
    }

    public void setSubImg(HybridImage subImg) {
        this.subImg = subImg;
        obstacle = new ArrayList<Polygon2DInt>();
        int otsu = ThresOtsu.calcThresOtsu(subImg.getAsByteImage());
        ByteImage binImg = subImg.getAsByteImage().copy();
        BinarizeTheshold.binarise(binImg, otsu);
        ByteImageHelper.invert(binImg);
        subImgBin = HybridImage.newInstance(binImg);
        bLmonTrafo = PolygonHelper.copy(bLmon);
        PolygonHelper.transform(bLmonTrafo, subImg.getTrafo());
        bLmonTrafoBlown = PolygonUtil.blowUp(bLmonTrafo);
        bLmonTrafoThin = PolygonUtil.thinOut(bLmonTrafoBlown, 5);

//        if (LOG.isTraceEnabled()) {
//            Polygon2DInt blCpy = PolygonHelper.copy(bLmonTrafo);
//            int pts = blCpy.npoints;
//            for (int i = 0; i < pts; i++) {
//                blCpy.addPoint(blCpy.xpoints[pts - 1 - i], blCpy.ypoints[pts - 1 - i]);
//            }
//            List<Polygon2DInt> aL = new ArrayList<Polygon2DInt>();
//            aL.add(blCpy);
//            LOG.info(new StdFrameAppender.AppenderContent(subImg, "SubImg with monPoly", PolygonHelper.getIFPolygonList(aL), false));
//            LOG.log(Logger.INFO, new StdFrameAppender.AppenderContent(subImgBin, "SubImgBin with monPoly", PolygonHelper.getIFPolygonList(aL), false));
//        }
    }

    public void setMinDist(double minDist) {
        this.minDist = minDist;
    }

    public void updateMinDist(BaseLine cBL) {
        Rectangle2DInt aBB = bL.getBounds();
        Rectangle2DInt cBB = cBL.getbL().getBounds();
        Rectangle2DInt inter = aBB.intersection(cBB);
        if (!(Math.max(-inter.h, -inter.w) > minDist)) {
            int[] dists = new int[bLThin.npoints];
            Polygon2DInt thinC = cBL.bLThin;
            for (int i = 0; i < bLThin.npoints; i++) {
                int aMinDist = Integer.MAX_VALUE;
                int xA = bLThin.xpoints[i];
                int yA = bLThin.ypoints[i];
                for (int j = 0; j < thinC.npoints; j++) {
                    int xC = thinC.xpoints[j];
                    int yC = thinC.ypoints[j];
//                minDist = Math.min(Math.sqrt((xC - xA) * (xC - xA) + (yC - yA) * (yC - yA)), minDist);
                    aMinDist = Math.min(Math.abs(xA - xC) + Math.abs(yA - yC), aMinDist);
                }
                dists[i] = aMinDist;
            }
            Arrays.sort(dists);
            double avgDist = 0.0;
            int s = dists.length / 4;
            int e = 3 * dists.length / 4;
            for (int i = s; i <= e; i++) {
                avgDist += dists[i];
            }
            avgDist /= Math.max(1, e - s + 1);
            minDist = Math.min(minDist, avgDist);
            minDist = Math.max(minDist, 20);
        }
    }

    public void calcShiftedBL() {
        if (subImg == null) {
            return;
        }

        int maxShift = Integer.MAX_VALUE;

        int slices = 5;

        int lookAt = Math.min(subImg.getHeight() / 4, 30);
        long[][] sobelSum = new long[slices][lookAt];

        sobelI = ImageUtil.getCombineSobelImg(subImg);

        //Add PixelIntensities to sobelImg
        byte[][] bImg = subImg.getAsByteImage().pixels[0];
        byte[][] bImgBin = subImgBin.getAsByteImage().pixels[0];
        for (int i = 0; i < bImg.length; i++) {
            byte[] aB = bImg[i];
            byte[] aBbin = bImgBin[i];
            short[] aS = sobelI[i];
            for (int j = 0; j < aS.length; j++) {
                int aVal = aB[j] & 0xFF;
                int aBinVal = aBbin[j] & 0xFF;
                int addPen = 0;
                //Adjust the SobelImage.
                //Highlight the FOREGROUND
                if (aBinVal == 255) {
                    addPen += 4 * (255 - aVal);
                }
                aS[j] += (short) addPen;
            }
        }

        long[] sobelSumA = null;
        for (int i = 0; i < bLmonTrafoThin.npoints; i++) {
            for (int j = 0; j < slices; j++) {
                if (i <= (j + 1) * bLmonTrafoThin.npoints / slices) {
                    sobelSumA = sobelSum[j];
                    break;
                }
            }

            int xA = bLmonTrafoThin.xpoints[i];
            int yA = bLmonTrafoThin.ypoints[i];

            for (Polygon2DInt aObs : obstacle) {
                for (int j = 0; j < aObs.npoints; j++) {
                    if (aObs.xpoints[j] == xA) {
                        if (aObs.ypoints[j] < yA) {
                            maxShift = Math.min(maxShift, (yA - aObs.ypoints[j]) / 2);
                        }
                    }
                }
            }

            for (int j = 0; j < lookAt; j++) {
                if (yA - j >= 0) {
                    if (xA < sobelI[0].length) {
                        sobelSumA[j] += sobelI[yA - j][xA];
                    }
                } else {
                    sobelSumA[j] -= 10000000;
                }
            }
        }

        int[] shiftVals = new int[slices];

        for (int i = 0; i < slices; i++) {
            sobelSumA = sobelSum[i];
            long mVal = 0;
            for (int j = 0; j < sobelSumA.length; j++) {
                if (sobelSumA[j] > mVal) {
                    mVal = sobelSumA[j];
                    shiftVals[i] = j;
                }
            }
            shiftVals[i] = Math.min(shiftVals[i], maxShift);
        }

        bLmonTrafoShifted = PolygonHelper.copy(bLmonTrafoThin);
        for (int i = 0; i < bLmonTrafoShifted.npoints; i++) {
            for (int j = 0; j < slices; j++) {
                if (i <= (j + 1) * bLmonTrafoThin.npoints / slices) {
                    bLmonTrafoShifted.ypoints[i] -= shiftVals[j];
                    break;
                }
            }
        }
//        if (LOG.isTraceEnabled()) {
//            List<Polygon2DInt> aL = new ArrayList<Polygon2DInt>();
//            Polygon2DInt blCpy = PolygonHelper.copy(bLmonTrafoShifted);
//            int pts = blCpy.npoints;
//            for (int i = 0; i < pts; i++) {
//                blCpy.addPoint(blCpy.xpoints[pts - 1 - i], blCpy.ypoints[pts - 1 - i]);
//            }
//            aL.add(blCpy);
//            List<Polygon2DInt> aLB = new ArrayList<Polygon2DInt>();
//            for (Polygon2DInt aObs : obstacle) {
//                blCpy = PolygonHelper.copy(aObs);
//                pts = blCpy.npoints;
//                for (int i = 0; i < pts; i++) {
//                    blCpy.addPoint(blCpy.xpoints[pts - 1 - i], blCpy.ypoints[pts - 1 - i]);
//                }
//                aLB.add(blCpy);
//            }
//
//            LOG.log(Logger.INFO, new StdFrameAppender.AppenderContent(subImg, "SubImg with monPoly Shifted", PolygonHelper.getIFPolygonList(aL), false));
//            LOG.log(Logger.INFO, new StdFrameAppender.AppenderContent(subImg, "SubImg with obstacle", PolygonHelper.getIFPolygonList(aLB), false));
//        }
        if (maxShift != Integer.MAX_VALUE) {
            topBounded = true;
        }
        //Reset the obstacles
        obstacle = new ArrayList<Polygon2DInt>();
    }

    public void addObstacle(Polygon2DInt bl) {
        Rectangle2DInt iB = new Rectangle2DInt(0, 0, subImg.getWidth(), subImg.getHeight());
        Polygon2DInt potObs = PolygonHelper.copy(bl);
        PolygonHelper.transform(potObs, subImg.getTrafo());
        Rectangle2DInt bb = potObs.getBounds();
        if (iB.intersects(bb)) {
            Polygon2DInt aObs = new Polygon2DInt();
            Polygon2DInt blownUp = PolygonUtil.blowUp(potObs);
            for (int i = 0; i < blownUp.npoints; i++) {
                int xA = blownUp.xpoints[i];
                int yA = blownUp.ypoints[i];
                if (iB.contains(xA, yA)) {
                    aObs.addPoint(xA, yA);
                }
            }
            obstacle.add(aObs);
        }

    }

    public boolean calcSeparatingSeams() {
        //Adjust the sobelCostMatrix (if baselinearea is dark penalize bright areas...)
        byte[][] bImg = subImg.getAsByteImage().pixels[0];
        try {
            int maxIntensity = 0;
            for (int i = 0; i < bLmonTrafoBlown.npoints; i++) {
                int y = bLmonTrafoBlown.ypoints[i];
                int x = bLmonTrafoBlown.xpoints[i];
                if (y < bImg.length && x < bImg[0].length) {
                    maxIntensity = Math.max(maxIntensity, (bImg[y][x] & 0xFF));
                } else {
                    LOG.debug("dim = {}, {}, request to point {},{}", bImg.length, bImg[0].length, y, x, new ArrayIndexOutOfBoundsException());
                }
            }
            if (maxIntensity < 225) {
                for (int i = 0; i < bImg.length; i++) {
                    byte[] aB = bImg[i];
                    short[] aS = sobelI[i];
                    for (int j = 0; j < aS.length; j++) {
                        int aVal = aB[j] & 0xFF;
                        int addPen = 0;
                        //Adjust the SobelImage.
                        //Penalize white areas for the final seam calculation
                        if (aVal > 253) {
                            addPen += 2 * aVal;
                        }
                        aS[j] += (short) addPen;
                    }
                }
            }
            if (bLmonTrafoShifted.getBounds().w < 5) {
                return false;
            }
//        System.out.println(bLmonTrafoShifted.getBounds());
            List<Pair<Integer, Polygon2DInt>> sepSeams = SeamCarve.calcSeparatingSeams(sobelI, bLmonTrafoShifted, obstacle);
            if (sepSeams == null || sepSeams.size() < 2) {
                return false;
            }
            topM = sepSeams.get(0);
            botM = sepSeams.get(1);
        } catch (RuntimeException ex) {
            LOG.error("cannot apply seamcarving for image size [w,h]=[{},{}] and polygon {}", bImg[0].length, bImg.length, bLmonTrafoShifted, ex);
            return false;
        }
        return true;
    }

    public void buildSurPoly(int addX) {
        surPolyTrafo = new Polygon2DInt();

        Polygon2DInt tP = topM.second;
        Polygon2DInt bP = botM.second;

        for (int i = 0; i < addX; i++) {
            surPolyTrafo.addPoint(bP.xpoints[0] - addX + i, bP.ypoints[0]);
        }

        for (int i = 0; i < bP.npoints; i++) {
            surPolyTrafo.addPoint(bP.xpoints[i], bP.ypoints[i]);
        }

        for (int i = 0; i < addX; i++) {
            surPolyTrafo.addPoint(bP.xpoints[bP.npoints - 1] + 1 + i, bP.ypoints[bP.npoints - 1]);
        }

        for (int i = 0; i < addX; i++) {
            surPolyTrafo.addPoint(tP.xpoints[tP.npoints - 1] + addX - i, tP.ypoints[tP.npoints - 1]);
        }

        for (int i = tP.npoints - 1; i >= 0; i--) {
            surPolyTrafo.addPoint(tP.xpoints[i], tP.ypoints[i]);
        }

        for (int i = 0; i < addX; i++) {
            surPolyTrafo.addPoint(tP.xpoints[0] - 1 - i, tP.ypoints[0]);
        }
//        if (LOG.isTraceEnabled()) {
//            List<Polygon2DInt> aL = new ArrayList<Polygon2DInt>();
//            Polygon2DInt blCpy = PolygonHelper.copy(surPolyTrafo);
//            aL.add(blCpy);
//            LOG.log(Logger.INFO, new StdFrameAppender.AppenderContent(subImg, "SubImg with SurPoly", PolygonHelper.getIFPolygonList(aL), false));
//        }
    }

    public void setSCTop(Pair<Integer, Polygon2DInt> scTop) {
        this.topM = scTop;
    }

    public void setSCBot(Pair<Integer, Polygon2DInt> scBot) {
        this.botM = scBot;
    }

    public boolean isTopBounded() {
        return topBounded;
    }
    
    public void release(){
        subImg.clear();
        subImgBin.clear();
    }

}
