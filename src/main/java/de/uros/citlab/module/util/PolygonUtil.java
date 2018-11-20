/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import com.achteck.misc.geom.Rotation;
import de.planet.math.geom2d.types.Point2DDouble;
import de.planet.math.geom2d.types.Point2DInt;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.geom2d.types.Rectangle2DInt;
import de.planet.math.trafo.ITransform;
import de.planet.math.trafo.TrafoUtil;
import de.planet.math.trafo.TransformRotate;
import de.planet.math.util.Eclass;
import eu.transkribus.core.model.beans.pagecontent.BaselineType;
import eu.transkribus.core.model.beans.pagecontent.CoordsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpBaselineType;
import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gundram
 */
public class PolygonUtil {

    private static Logger LOG = LoggerFactory.getLogger(PolygonUtil.class);

    public static CoordsType polyon2Coords(Polygon polygon) {
        CoordsType res = new CoordsType();
        res.setPoints(array2String(polygon.xpoints, polygon.ypoints, polygon.npoints));
        return res;
    }

    public static Polygon string2Polygon(String string) {
        String[] split = string.split(" ");
        int size = split.length;
        int[] x = new int[size];
        int[] y = new int[size];
        for (int i = 0; i < size; i++) {
            String[] point = split[i].split(",");
            x[i] = Integer.parseInt(point[0]);
            y[i] = Integer.parseInt(point[1]);
        }
        return new Polygon(x, y, size);
    }

    public static Polygon2DInt string2Polygon2DInt(String string) {
        String[] split = string.split(" ");
        int size = split.length;
        int[] x = new int[size];
        int[] y = new int[size];
        for (int i = 0; i < size; i++) {
            String[] point = split[i].split(",");
            x[i] = Integer.parseInt(point[0]);
            y[i] = Integer.parseInt(point[1]);
        }
        return new Polygon2DInt(x, y, size);
    }

    public static String polygon2String(Polygon polygon) {
        return array2String(polygon.xpoints, polygon.ypoints, polygon.npoints);
    }

    public static String polygon2String(Polygon2DInt polygon) {
        return array2String(polygon.xpoints, polygon.ypoints, polygon.npoints);
    }

    public static CoordsType polyon2Coords(Polygon2DInt polygon) {
        CoordsType res = new CoordsType();
        res.setPoints(array2String(polygon.xpoints, polygon.ypoints, polygon.npoints));
        return res;
    }

    public static BaselineType polyon2Baseline(Polygon polygon) {
        BaselineType res = new BaselineType();
        res.setPoints(array2String(polygon.xpoints, polygon.ypoints, polygon.npoints));
        return res;
    }

    public static BaselineType polyon2Baseline(Polygon2DInt polygon) {
        BaselineType res = new BaselineType();
        res.setPoints(array2String(polygon.xpoints, polygon.ypoints, polygon.npoints));
        return res;
    }

    public static TrpBaselineType polyon2TrpBaseline(Polygon polygon) {
        TrpBaselineType res = new TrpBaselineType();
        res.setPoints(array2String(polygon.xpoints, polygon.ypoints, polygon.npoints));
        return res;
    }

    public static TrpBaselineType polyon2TrpBaseline(Polygon2DInt polygon) {
        TrpBaselineType res = new TrpBaselineType();
        res.setPoints(array2String(polygon.xpoints, polygon.ypoints, polygon.npoints));
        return res;
    }

    private static String array2String(int[] x, int[] y, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(x[i]).append(',').append(y[i]).append(' ');
        }
        return sb.toString().trim();
    }

    public static Polygon2DInt convert(Polygon p) {
        return new Polygon2DInt(p.xpoints, p.ypoints, p.npoints);
    }

    public static Polygon convert(Polygon2DInt p) {
        return new Polygon(p.xpoints, p.ypoints, p.npoints);
    }

    public static Polygon reducePoints(Polygon polygon) {
        if (polygon.npoints < 3) {
            return polygon;
        }
        Polygon res = new Polygon();
        Point startPoint = new Point(polygon.xpoints[0], polygon.ypoints[0]);
        Point lastPoint = new Point(polygon.xpoints[1], polygon.ypoints[1]);
        res.addPoint(startPoint.x, startPoint.y);
        Point direction = substract(startPoint, lastPoint);
        for (int i = 2; i < polygon.npoints; i++) {
            Point currentPoint = new Point(polygon.xpoints[i], polygon.ypoints[i]);
            Point directionNew = substract(startPoint, currentPoint);
            if (directionNew.x * direction.y == direction.x * directionNew.y) {//same direction
                lastPoint = currentPoint;
            } else {
                res.addPoint(lastPoint.x, lastPoint.y);
                startPoint = lastPoint;
                lastPoint = currentPoint;
                direction = substract(startPoint, lastPoint);
            }
        }
        res.addPoint(lastPoint.x, lastPoint.y);
        return res;
    }

    private static Polygon2DInt deleteRepeatPoints(Polygon2DInt polygon) {
//        Point2DInt[] vertices = polygon.getVertices();
        Polygon2DInt res = new Polygon2DInt();
        res.addPoint(polygon.xpoints[0], polygon.ypoints[0]);
        for (int i = 0; i < polygon.npoints - 1; i++) {
            if (polygon.xpoints[i] != polygon.xpoints[i + 1] || polygon.ypoints[i] != polygon.ypoints[i + 1]) {
                res.addPoint(polygon.xpoints[i + 1], polygon.ypoints[i + 1]);
            }
        }
        return res;
    }

    public static Polygon2DInt reducePoints(Polygon2DInt polygon) {
        if (polygon == null || polygon.npoints < 3) {
            return polygon;
        }
        polygon = deleteRepeatPoints(polygon);
        Polygon2DInt res = new Polygon2DInt();
        Point startPoint = new Point(polygon.xpoints[0], polygon.ypoints[0]);
        Point lastPoint = new Point(polygon.xpoints[1], polygon.ypoints[1]);
        res.addPoint(startPoint.x, startPoint.y);
        Point direction = substract(startPoint, lastPoint);
        for (int i = 2; i < polygon.npoints; i++) {
            Point currentPoint = new Point(polygon.xpoints[i], polygon.ypoints[i]);
            Point directionNew = substract(startPoint, currentPoint);
            if (directionNew.x * direction.y == direction.x * directionNew.y) {//same direction
                lastPoint = currentPoint;
            } else {
                res.addPoint(lastPoint.x, lastPoint.y);
                startPoint = lastPoint;
                lastPoint = currentPoint;
                direction = substract(startPoint, lastPoint);
            }
        }
        res.addPoint(lastPoint.x, lastPoint.y);
        return res;
    }

    private static Point substract(Point first, Point second) {
        return new Point(first.x - second.x, first.y - second.y);
    }

    public static Polygon copy(Polygon p) {
        final int n = p.npoints;
        final int[] x = new int[n];
        final int[] y = new int[n];
        System.arraycopy(p.xpoints, 0, x, 0, n);
        System.arraycopy(p.ypoints, 0, y, 0, n);
        return new Polygon(x, y, n);
    }

    public static Polygon getPolygon(TextLineType tlt) {
        CoordsType coords = tlt.getCoords();
        if (coords == null) {
            throw new RuntimeException("textline with id " + tlt.getId() + " has no structure Coords.");
        }
        Polygon string2Polygon = PolygonUtil.string2Polygon(coords.getPoints());
        if (string2Polygon.npoints < 3) {
            LOG.warn("textline with id " + tlt.getId() + " has insufficient coords '" + coords.getPoints() + "'.");
            throw new RuntimeException("textline with id " + tlt.getId() + " has insufficient coords '" + coords.getPoints() + "'.");

//            return null;
        }
        return string2Polygon;
    }

    public static Polygon getBaseline(TextLineType tlt) {
        BaselineType bl = tlt.getBaseline();
        if (bl == null) {
            throw new RuntimeException("textline with id " + tlt.getId() + " has no structure Baseline.");
        }
        Polygon string2Polygon = PolygonUtil.string2Polygon(bl.getPoints());
        if (string2Polygon.npoints < 2) {
            throw new RuntimeException("textline with id " + tlt.getId() + " has insufficient Baseline '" + bl.getPoints() + "'.");
        }
        return string2Polygon;
    }

    public static void setBaseline(TextLineType tlt, Polygon baseline) {
        if (baseline.npoints < 2) {
            throw new RuntimeException("textline with id " + tlt.getId() + " has insufficient Baseline '" + PolygonUtil.polygon2String(baseline) + "'.");
        }
        BaselineType blt = new BaselineType();
        blt.setPoints(PolygonUtil.polygon2String(baseline));
        tlt.setBaseline(blt);
    }

    public static void setCoords(TextLineType tlt, Polygon coords) {
        if (coords.npoints < 3) {
            throw new RuntimeException("textline with id " + tlt.getId() + " has insufficient Baseline '" + PolygonUtil.polygon2String(coords) + "'.");
        }
        CoordsType ct = new CoordsType();
        ct.setPoints(PolygonUtil.polygon2String(coords));
        tlt.setCoords(ct);
    }

    /**
     * Compute convex hull of polygon.
     *
     * @param p
     * @return
     */
    public static Polygon getConvexHull(Polygon p) {
        class Helper extends Point implements Comparable<Helper> {

            private static final long serialVersionUID = 1L;

            double angle;

            Helper(int x, int y, double angle) {
                this.x = x;
                this.y = y;
                this.angle = angle;
            }

            @Override
            public int compareTo(Helper p) {
                if (angle < p.angle) {
                    return -1;
                }
                if (angle > p.angle) {
                    return 1;
                }
                if (x < p.x) {
                    return -1;
                }
                if (x > p.x) {
                    return 1;
                }
                if (y < p.y) {
                    return -1;
                }
                if (y > p.y) {
                    return 1;
                }
                return 0;
            }
        }

        if (p.npoints < 3) {
            return copy(p);
        }

        // get lowest left point
        int ox = Integer.MAX_VALUE, oy = Integer.MAX_VALUE;
        for (int i = 0; i < p.npoints; i++) {
            final int x = p.xpoints[i];
            final int y = p.ypoints[i];
            if (y <= oy) {
                if (y < oy || x < ox) {
                    ox = x;
                    oy = y;
                }
            }
        }

        Helper tmp[] = new Helper[p.npoints];
        for (int i = 0; i < p.npoints; i++) {
            tmp[i] = new Helper(p.xpoints[i], p.ypoints[i], quasiAngle(ox, oy, p.xpoints[i], p.ypoints[i]));
        }

        Arrays.sort(tmp);

        // Konvexe Hülle bestimmen
        int i, m;
        for (i = 3, m = 2; i < tmp.length; ++i) {
            while (m > 0 && orbitDir(tmp[m].x, tmp[m].y, tmp[m - 1].x, tmp[m - 1].y, tmp[i].x, tmp[i].y) >= 0) {
                --m;
            }

            if (++m != i) {
                Helper t = tmp[m];
                tmp[m] = tmp[i];
                tmp[i] = t;
            }
        }

        // konvexe H�lle kopieren
        final int s = m + 1;

        int[] xpoints = new int[s], ypoints = new int[s];

        for (i = 0; i <= m; ++i) {
            xpoints[i] = tmp[i].x;
            ypoints[i] = tmp[i].y;
        }
        Polygon ret = new Polygon(xpoints, ypoints, s);

        return ret;
    }

    /**
     * value similar to angle but faster to compute
     *
     * @return
     */
    private static double quasiAngle(int x1, int y1, int x2, int y2) {
        final int dx = x2 - x1;
        final int dy = y2 - y1;

        if (dy == 0) {
            return dx == 0 ? -1 : 0;
        }

        final double fdx = dx;
        final double fdy = dy;
        final double t = fdy / (Math.abs(fdx) + Math.abs(fdy));
        if (dx < 0) {
            return 2. - t;
        }
        if (dy < 0) {
            return 4. + t;
        }
        return t;
    }

    private static int orbitDir(int x0, int y0, int x1, int y1, int x2, int y2) {
        final int dx1 = x1 - x0;
        final int dy1 = y1 - y0;
        final int dx2 = x2 - x0;
        final int dy2 = y2 - y0;
        final int cross = dx1 * dy2 - dy1 * dx2;
        if (cross > 0) {
            return +1;
        }
        if (cross < 0) {
            return -1;
        }
        if (dx1 * dx2 < 0 || dy1 * dy2 < 0) {
            return -1;
        }
        if (dx1 * dx1 + dy1 * dy1 < dx2 * dx2 + dy2 * dy2) {
            return +1;
        }
        return 0;
    }

    public static Polygon2DInt blowUp(Polygon2DInt inPoly) {
        Polygon2DInt res = new Polygon2DInt();
        for (int i = 1; i < inPoly.npoints; i++) {
            int x1 = inPoly.xpoints[i - 1];
            int y1 = inPoly.ypoints[i - 1];
            int x2 = inPoly.xpoints[i];
            int y2 = inPoly.ypoints[i];
            int diffX = Math.abs(x2 - x1);
            int diffY = Math.abs(y2 - y1);
            if (Math.max(diffX, diffY) < 1) {
                if (i == inPoly.npoints - 1) {
                    res.addPoint(x2, y2);
                }
                continue;
            }
            res.addPoint(x1, y1);
            if (diffX >= diffY) {
                for (int j = 1; j < diffX; j++) {
                    int xN;
                    if (x1 < x2) {
                        xN = x1 + j;
                    } else {
                        xN = x1 - j;
                    }
                    int yN = (int) (Math.round(y1 + (double) (xN - x1) * (y2 - y1) / (x2 - x1)));
                    res.addPoint(xN, yN);
                }
            } else {
                for (int j = 1; j < diffY; j++) {
                    int yN;
                    if (y1 < y2) {
                        yN = y1 + j;
                    } else {
                        yN = y1 - j;
                    }
                    int xN = (int) (Math.round(x1 + (double) (yN - y1) * (x2 - x1) / (y2 - y1)));
                    res.addPoint(xN, yN);
                }
            }
            if (i == inPoly.npoints - 1) {
                res.addPoint(x2, y2);
            }
        }
        return res;
    }

    /**
     *
     * @param p
     * @return #0 - angle #1 - absVal
     */
    public static double[] calcRegLineStats(Polygon2DInt p) {
        if (p.npoints <= 1) {
            return new double[]{0.0, 0.0};
        }
        double m = 0.0;
        double n = Double.POSITIVE_INFINITY;
        if (p.npoints > 2) {
            int xMax = 0;
            int xMin = Integer.MAX_VALUE;
//            SimpleRegression sR = new SimpleRegression();
            for (int i = 0; i < p.npoints; i++) {
                int xVal = p.xpoints[i];
//                sR.addData(xVal, -p.ypoints[i]);
                xMax = Math.max(xMax, xVal);
                xMin = Math.min(xMin, xVal);
            }
            if (Math.abs(xMax - xMin) < 5) {
                m = Double.POSITIVE_INFINITY;
            } else {
//                RegressionResults reg = sR.regress();
//                double[] parameterEstimates = reg.getParameterEstimates();
//                m = reg.getParameterEstimates()[1];
                int[] xPs = new int[p.npoints];
                int[] yPs = new int[p.npoints];
                for (int i = 0; i < p.npoints; i++) {
                    xPs[i] = p.xpoints[i];
                    yPs[i] = -p.ypoints[i];
                }

                double[] calcLine = LinRegression.calcLine(xPs, yPs);
                m = calcLine[1];
                n = calcLine[0];
            }
        } else {
            int x1 = p.xpoints[0];
            int x2 = p.xpoints[1];
            int y1 = -p.ypoints[0];
            int y2 = -p.ypoints[1];
            if (x1 == x2) {
                m = Double.POSITIVE_INFINITY;
            } else {
                m = (double) (y2 - y1) / (x2 - x1);
                n = y2 - m * x2;
            }
        }
        double angle = 0.0;
        if (Double.isInfinite(m)) {
            angle = Math.PI / 2.0;
        } else {
            angle = Math.atan(m);
        }

        int fP = 0;
        int lP = p.npoints - 1;

        if (angle > -Math.PI / 2.0 && angle <= -Math.PI / 4.0) {
            if (p.ypoints[fP] > p.ypoints[lP]) {
                angle += Math.PI;
            }
        }
        if (angle > -Math.PI / 4.0 && angle <= Math.PI / 4.0) {
            if (p.xpoints[fP] > p.xpoints[lP]) {
                angle += Math.PI;
            }
        }
        if (angle > Math.PI / 4.0 && angle <= Math.PI / 2.0) {
            if (p.ypoints[fP] < p.ypoints[lP]) {
                angle += Math.PI;
            }
        }

        if (angle < 0) {
            angle += 2 * Math.PI;
        }
        return new double[]{angle, n};
    }

    public static Polygon2DInt thinOut(Polygon2DInt polyBlown, int desDist) {
        Polygon2DInt res = new Polygon2DInt();
        if (polyBlown.npoints <= 5) {
            return polyBlown;
        }
        int dist = polyBlown.npoints - 1;
        int minPts = 2;
        int desPts = Math.max(minPts, dist / desDist + 1);
        double step = (double) dist / (desPts - 1);
        for (int i = 0; i < desPts - 1; i++) {
            int aIdx = (int) (i * step);
            res.addPoint(polyBlown.xpoints[aIdx], polyBlown.ypoints[aIdx]);
        }
        res.addPoint(polyBlown.xpoints[polyBlown.npoints - 1], polyBlown.ypoints[polyBlown.npoints - 1]);
        return res;
    }

    public static Polygon2DInt monotonPoly(Polygon2DInt inPoly, double angle) {
        Polygon2DInt res = new Polygon2DInt();
        int lX = inPoly.xpoints[0];
        int lY = inPoly.ypoints[0];
        res.addPoint(lX, lY);

        for (int i = 1; i < inPoly.npoints; i++) {
            int aX = inPoly.xpoints[i];
            int aY = inPoly.ypoints[i];
            Polygon2DInt polyH = new Polygon2DInt();
            polyH.addPoint(lX, lY);
            polyH.addPoint(aX, aY);
            double aAngle = calcRegLineStats(polyH)[0];
            double dist = Math.abs(angle - aAngle);
            if (dist < Math.PI / 2.0 || dist > 3.0 / 2.0 * Math.PI) {
                res.addPoint(aX, aY);
                lX = aX;
                lY = aY;
            }
        }
        return res;
    }

    public static void downScaleInPlace(Polygon2DInt poly, double scaleFac) {
        if (poly != null) {
            for (int i = 0; i < poly.npoints; i++) {
                poly.xpoints[i] = (int) (poly.xpoints[i] / scaleFac);
                poly.ypoints[i] = (int) (poly.ypoints[i] / scaleFac);
            }

        }
    }

    public static Pair<int[], int[]> getTopBot(Polygon2DInt poly) {
        Rectangle2DInt bb = poly.getBounds();
        int offX = bb.x;
        int[] topV = new int[bb.w];
        Arrays.fill(topV, Integer.MAX_VALUE);
        int[] botV = new int[bb.w];
        Arrays.fill(botV, -1);

        for (int i = 0; i < poly.npoints; i++) {
            int startX, startY, endX, endY;
            if (i == 0) {
                startX = poly.xpoints[0];
                startY = poly.ypoints[0];
                endX = poly.xpoints[poly.npoints - 1];
                endY = poly.ypoints[poly.npoints - 1];
            } else {
                startX = poly.xpoints[i - 1];
                startY = poly.ypoints[i - 1];
                endX = poly.xpoints[i];
                endY = poly.ypoints[i];
            }
            if (startX == endX) {
                topV[startX - offX] = Math.min(topV[startX - offX], Math.min(startY, endY));
                botV[startX - offX] = Math.max(botV[startX - offX], Math.max(startY, endY));
            } else {
                int x1, x2, y1, y2;
                if (startX < endX) {
                    x1 = startX;
                    y1 = startY;
                    x2 = endX;
                    y2 = endY;
                } else {
                    x1 = endX;
                    y1 = endY;
                    x2 = startX;
                    y2 = startY;
                }
                for (int j = x1; j <= x2; j++) {
                    int aVal = (int) Math.round((y2 - y1) / (double) (x2 - x1) * (j - x1) + y1);
                    topV[j - offX] = Math.min(topV[j - offX], aVal);
                    botV[j - offX] = Math.max(botV[j - offX], aVal);
                }
            }
        }
        return new Pair<>(topV, botV);
    }

    public static Polygon2DInt rotate(Polygon2DInt polygon, double angle) {
        return TrafoUtil.transform(TrafoUtil.getRotateInstance(angle), polygon);
    }

    public static List<Polygon2DInt> rotate(List<Polygon2DInt> polygons, double angle) {
        ArrayList<Polygon2DInt> res = new ArrayList<>(polygons.size());
        for (Polygon2DInt polygon : polygons) {
            res.add(rotate(polygon, angle));
        }
        return res;
    }

    public static double[] calcTols(List<Polygon2DInt> polyTruthNorm, int tickDist, int maxD, double relTol) {
        double[] tols = new double[polyTruthNorm.size()];

        int lineCnt = 0;
        for (Polygon2DInt aPoly : polyTruthNorm) {
            double angle = calcRegLineStats(aPoly)[0];
            double orVecY = Math.sin(angle);
            double orVecX = Math.cos(angle);
            double aDist = maxD;
            double[] ptA1 = new double[]{aPoly.xpoints[0], aPoly.ypoints[0]};
            double[] ptA2 = new double[]{aPoly.xpoints[aPoly.npoints - 1], aPoly.ypoints[aPoly.npoints - 1]};
            for (int i = 0; i < aPoly.npoints; i++) {
                double[] pA = new double[]{aPoly.xpoints[i], aPoly.ypoints[i]};
                for (Polygon2DInt cPoly : polyTruthNorm) {
                    if (cPoly != aPoly) {
                        if (getDistFast(pA, cPoly.getBounds()) > aDist) {
                            continue;
                        }
                        double[] ptC1 = new double[]{cPoly.xpoints[0], cPoly.ypoints[0]};
                        double[] ptC2 = new double[]{cPoly.xpoints[cPoly.npoints - 1], cPoly.ypoints[cPoly.npoints - 1]};
                        double inD1 = getInDist(ptA1, ptC1, orVecX, orVecY);
                        double inD2 = getInDist(ptA1, ptC2, orVecX, orVecY);
                        double inD3 = getInDist(ptA2, ptC1, orVecX, orVecY);
                        double inD4 = getInDist(ptA2, ptC2, orVecX, orVecY);
                        if ((inD1 < 0 && inD2 < 0 && inD3 < 0 && inD4 < 0) || (inD1 > 0 && inD2 > 0 && inD3 > 0 && inD4 > 0)) {
                            continue;
                        }

                        for (int j = 0; j < cPoly.npoints; j++) {
                            double[] pC = new double[]{cPoly.xpoints[j], cPoly.ypoints[j]};
                            if (Math.abs(getInDist(pA, pC, orVecX, orVecY)) <= 2 * tickDist) {
                                aDist = Math.min(aDist, Math.abs(getOffDist(pA, pC, orVecX, orVecY)));
                            }
                        }
                    }
                }
            }
//            System.out.println("Line " + lineCnt + " has min dist of: " + aDist);
//            System.out.println("Line " + lineCnt + " has startX: " + aPoly.xpoints[0] + " and startY: " + aPoly.ypoints[0]);
            if (aDist < maxD) {
                tols[lineCnt] = aDist;
            }
            lineCnt++;
        }
        double sumVal = 0.0;
        int cnt = 0;
        for (int i = 0; i < tols.length; i++) {
            double aTol = tols[i];
            if (aTol != 0) {
                sumVal += aTol;
                cnt++;
            }
        }
        double meanVal = maxD;
        if (cnt != 0) {
            meanVal = sumVal / cnt;
        }

        for (int i = 0; i < tols.length; i++) {
            if (tols[i] == 0) {
                tols[i] = meanVal;
            }
            tols[i] = Math.min(tols[i], meanVal);
            tols[i] *= relTol;
        }

        return tols;
    }

    private static double getDistFast(double[] aPt, Rectangle2DInt bb) {
        double dist = 0.0;
        if (aPt[0] < bb.x) {
            dist += bb.x - aPt[0];
        }
        if (aPt[0] > bb.x + bb.w) {
            dist += aPt[0] - bb.x - bb.w;
        }
        if (aPt[1] < bb.y) {
            dist += bb.y - aPt[1];
        }
        if (aPt[1] > bb.y + bb.h) {
            dist += aPt[1] - bb.y - bb.h;
        }
        return dist;
    }

    private static double getOffDist(double[] aPt, double[] cPt, double orVecX, double orVecY) {
        double diffX = aPt[0] - cPt[0];
        double diffY = -aPt[1] + cPt[1];
        //Since orVec has length 1 calculate the cross product, which is 
        //the orthogonal distance from diff to orVec, take into account 
        //the z-Value to decide whether its a positive or negative distance!
        //double dotProdX = 0;
        //double dotProdY = 0;
        return diffX * orVecY - diffY * orVecX;
    }

    private static double getInDist(double[] aPt, double[] cPt, double orVecX, double orVecY) {
        double diffX = aPt[0] - cPt[0];
        double diffY = -aPt[1] + cPt[1];
        //Parallel component of (diffX, diffY) is lambda * (orVecX, orVecY) with
        double lambda = diffX * orVecX + orVecY * diffY;

        return lambda;
    }

    public static List<List<Polygon2DInt>> clusterLeft2Right(List<Polygon2DInt> ps, double angle, double grow) {
        final boolean[][] cmp = new boolean[ps.size()][ps.size()];
        for (int i = 0; i < ps.size(); i++) {
            Rectangle2DInt p1 = ps.get(i).getBounds();
            for (int j = i; j < ps.size(); j++) {
                Rectangle2DInt p2 = ps.get(j).getBounds();
                if (hasIntersact(p1, p2, angle, grow)) {
                    cmp[i][j] = true;
                    cmp[j][i] = true;
                }
            }
        }
        List<List<Polygon2DInt>> eclass = Eclass.eclass(ps, new Eclass.IEquivalence() {
            @Override
            public boolean equivalent(int i, int i1) {
                return cmp[i][i1];
            }
        });
        Collections.sort(eclass, new Comparator<List<Polygon2DInt>>() {
            @Override
            public int compare(List<Polygon2DInt> o1, List<Polygon2DInt> o2) {
                return Double.compare(o1.get(0).getBounds().getCenterX(), o2.get(0).getBounds().getCenterX());
            }
        });
        return eclass;

    }

    private static boolean hasIntersact(Rectangle2DInt r1, Rectangle2DInt r2, double angle, double growX) {
        Point2DInt[] vs1 = r1.getVertices();
        Point2DInt[] vs2 = r2.getVertices();
        //If angle >=0, one has to check if a 0-angle leeds to intersection.
        //For the small triangle between Top-Right and Bottom-Right one also has to check for angle>0 the angle =0.
        if (angle >= 0) {
            if (!hasIntersect(vs1[2], vs2[0], null, growX)) {
                return false;
            }
            if (!hasIntersect(vs2[2], vs1[0], null, growX)) {
                return false;
            }
            //If there already is no intersection for angle = 0, one has finished.
            if (angle == 0.0) {
                return true;
            }
        }
        ITransform trafo = new TransformRotate(new Rotation(angle));
        ITransform trafo_ = new TransformRotate(new Rotation(-angle));

        //otherwise check with angles
        //if there is one false intersection the global intersection is false
        if (!hasIntersect(vs1[2], vs2[0], trafo, growX)) {
            return false;
        }
        if (!hasIntersect(vs2[2], vs1[0], trafo, growX)) {
            return false;
        }
        if (!hasIntersect(vs1[3], vs2[1], trafo_, growX)) {
            return false;
        }
        if (!hasIntersect(vs2[3], vs1[1], trafo_, growX)) {
            return false;
        }
        return true;
    }

    private static boolean hasIntersect(Point2DInt pointBottomRight, Point2DInt pointTopLeft, ITransform trafo, double growX) {
        Point2DDouble pBR = new Point2DDouble(pointBottomRight.x, pointBottomRight.y);
        Point2DDouble pTL = new Point2DDouble(pointTopLeft.x, pointTopLeft.y);
        if (trafo != null) {
            pBR = TrafoUtil.transform(trafo, pBR);
            pTL = TrafoUtil.transform(trafo, pTL);
        }
        return pTL.x - pBR.x <= growX;
    }

}
