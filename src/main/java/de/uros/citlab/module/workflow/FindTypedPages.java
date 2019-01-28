package de.uros.citlab.module.workflow;

import de.planet.util.Gnuplot;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.util.PolygonUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class FindTypedPages {

    public static double getInDist(double[] aPt, double[] cPt, double orVecX, double orVecY) {
        double diffX = aPt[0] - cPt[0];
        double diffY = -aPt[1] + cPt[1];
        //Parallel component of (diffX, diffY) is lambda * (orVecX, orVecY) with
        double lambda = diffX * orVecX + orVecY * diffY;

        return lambda;
    }

    public static double getDistFast(double[] aPt, double[] bPt) {
        return Math.abs(aPt[0] - bPt[0]) + Math.abs(aPt[1] - bPt[1]);
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

    public static double getDistFast(double[] aPt, Rectangle bb) {
        double dist = 0.0;
        if (aPt[0] < bb.x) {
            dist += bb.x - aPt[0];
        }
        if (aPt[0] > bb.x + bb.width) {
            dist += aPt[0] - bb.x - bb.width;
        }
        if (aPt[1] < bb.y) {
            dist += bb.y - aPt[1];
        }
        if (aPt[1] > bb.y + bb.height) {
            dist += aPt[1] - bb.y - bb.height;
        }
        return dist;
    }

    public static Polygon[] normDesDist(Polygon[] polyIn, int desDist) {
        Polygon[] res = new Polygon[polyIn.length];
        for (int i = 0; i < res.length; i++) {
            Rectangle bb = polyIn[i].getBounds();
            if (bb.width > 100000 || bb.height > 100000) {
                Polygon nPoly = new Polygon();
                nPoly.addPoint(0, 0);
                polyIn[i] = nPoly;
            }
            res[i] = normDesDist(polyIn[i], desDist);
            res[i].getBounds();
        }
        return res;
    }

    public static Polygon normDesDist(Polygon polyIn, int desDist) {
        Polygon polyBlown = blowUp(polyIn);
        return thinOut(polyBlown, desDist);
    }

    private static Polygon blowUp(Polygon inPoly) {
        Polygon res = new Polygon();
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

    private static Polygon thinOut(Polygon polyBlown, int desDist) {
        Polygon res = new Polygon();
        if (polyBlown.npoints <= 20) {
            return polyBlown;
        }
        int dist = polyBlown.npoints - 1;
        int minPts = 20;
        int desPts = Math.max(minPts, dist / desDist + 1);
        double step = (double) dist / (desPts - 1);
        for (int i = 0; i < desPts - 1; i++) {
            int aIdx = (int) (i * step);
            res.addPoint(polyBlown.xpoints[aIdx], polyBlown.ypoints[aIdx]);
        }
        res.addPoint(polyBlown.xpoints[polyBlown.npoints - 1], polyBlown.ypoints[polyBlown.npoints - 1]);
        return res;
    }

    public static double[] calcTols(Polygon[] polyTruthNorm, int tickDist, int maxD, double relTol) {
        double[] tols = new double[polyTruthNorm.length];

        int lineCnt = 0;
        for (Polygon aPoly : polyTruthNorm) {
            double angle = 0.0;
            double orVecY = Math.sin(angle);
            double orVecX = Math.cos(angle);
            double aDist = maxD;
            double[] ptA1 = new double[]{aPoly.xpoints[0], aPoly.ypoints[0]};
            double[] ptA2 = new double[]{aPoly.xpoints[aPoly.npoints - 1], aPoly.ypoints[aPoly.npoints - 1]};
            for (int i = 0; i < aPoly.npoints; i++) {
                double[] pA = new double[]{aPoly.xpoints[i], aPoly.ypoints[i]};
                for (Polygon cPoly : polyTruthNorm) {
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


    public static void main(String[] args) {
        File folder = HomeDir.getFile("data/LA");
        File[] files = folder.listFiles();
        List<String> setName = new ArrayList<>();
        List<double[]> stat = new ArrayList<>();
        int br=0;
        for (File set : files) {
            if(!set.isDirectory()){
                continue;
            }
            if(br++>20){
                break;
            }
            List<File> xmls = FileUtil.listFiles(set, "xml", true);
            FileUtil.deleteMetadataAndMetsFiles(xmls);
            ObjectCounter<Integer> oc = new ObjectCounter<>();
            LinkedList<String> list = new LinkedList<>();
            int count = 0;
            for (File xml : xmls) {
                PcGtsType unmarshal = PageXmlUtil.unmarshal(xml);
                List<TextLineType> textLines = PageXmlUtil.getTextLines(unmarshal);
                Polygon[] polys = new Polygon[textLines.size()];
                for (int i = 0; i < textLines.size(); i++) {
                    polys[i] = PolygonUtil.getBaseline(textLines.get(i));
                }
                polys = normDesDist(polys, 5);
                double[] doubles = FindTypedPages.calcTols(polys, 5, 250, 1.0);
                Arrays.sort(doubles);
                int size = doubles.length == 0 ? 0 : (int) doubles[(doubles.length) / 2];
                size = Math.min(size, 250);
//            int size = PageXmlUtil.getTextLines(unmarshal).size();
                oc.add(size);
                String str = String.format("%03d %s", size, xml.getPath());
                list.add(str);
                System.out.println(count++ + " of " + xmls.size() + " ( " + size + " )");
                if (count == 3000) {
                    break;
                }
            }
            list.sort(String::compareTo);
            System.out.println(oc);
            List<Integer> result = oc.getResult();
            result.sort(Integer::compareTo);
            double[] y2 = new double[251];
            long n = 0;
            int idx = 0;
            for (int i = 0; i < 251; i++) {
                Long cnt = oc.get(i);
                if (cnt == null) {
                    cnt = 0L;
                }
                n += cnt;
//                y[idx] = (cnt * 100.0D) / count;
                y2[idx] = (n * 100.0D) / count;
                System.out.println(String.format("%3d %.2f%% %.2f%%", i, cnt * 100.0 / count, n * 100.0 / count));
                idx++;
            }
            setName.add(set.getName());
            stat.add(y2);
        }
        double[] x = new double[251];
        for (int i = 0; i < x.length; i++) {
            x[i]=i;
        }
        Gnuplot.plot(x, stat, "inter line distances", setName.toArray(new String[0]), 0, 100);
    }

}
