package de.uros.citlab.module.workflow;

import de.planet.imaging.types.HybridImage;
import de.planet.util.Gnuplot;
import de.planet.util.gui.Display;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.util.PolygonUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
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

    public static double[][] calcTolsUpperLower(Polygon[] polyTruthNorm, int tickDist, int maxD, double relTol) {
        double[] tols = new double[polyTruthNorm.length];
        double[] tols2 = new double[polyTruthNorm.length];

        int lineCnt = 0;
        for (Polygon aPoly : polyTruthNorm) {
            double angle = 0.0;
            double orVecY = Math.sin(angle);
            double orVecX = Math.cos(angle);
            double aDist1 = maxD;
            double aDist2 = maxD;
            double[] ptA1 = new double[]{aPoly.xpoints[0], aPoly.ypoints[0]};
            double[] ptA2 = new double[]{aPoly.xpoints[aPoly.npoints - 1], aPoly.ypoints[aPoly.npoints - 1]};
            for (int i = 0; i < aPoly.npoints; i++) {
                double[] pA = new double[]{aPoly.xpoints[i], aPoly.ypoints[i]};
                for (Polygon cPoly : polyTruthNorm) {
                    if (cPoly != aPoly) {
                        if (getDistFast(pA, cPoly.getBounds()) > Math.max(aDist1, aDist2)) {
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
                                double offDist = getOffDist(pA, pC, orVecX, orVecY);
                                if (offDist > 0) {
                                    aDist1 = Math.min(aDist1, Math.abs(offDist));
                                } else {
                                    aDist2 = Math.min(aDist2, Math.abs(offDist));
                                }
                            }
                        }
                    }
                }
            }
//            System.out.println("Line " + lineCnt + " has min dist of: " + aDist);
//            System.out.println("Line " + lineCnt + " has startX: " + aPoly.xpoints[0] + " and startY: " + aPoly.ypoints[0]);
            if (aDist1 < maxD) {
                tols[lineCnt] = aDist1;
            }
            if (aDist2 < maxD) {
                tols2[lineCnt] = aDist2;
            }
            lineCnt++;
        }
//        double sumVal = 0.0;
//        int cnt = 0;
//        for (int i = 0; i < tols.length; i++) {
//            double aTol = tols[i];
//            if (aTol != 0) {
//                sumVal += aTol;
//                cnt++;
//            }
//        }
//        double meanVal = maxD;
//        if (cnt != 0) {
//            meanVal = sumVal / cnt;
//        }
//
//        for (int i = 0; i < tols.length; i++) {
//            if (tols[i] == 0) {
//                tols[i] = meanVal;
//            }
//            tols[i] = Math.min(tols[i], meanVal);
//            tols[i] *= relTol;
//        }

        return new double[][]{tols, tols2};
    }


    public static void main(String[] args) {
//        File folder = HomeDir.getFile("data/LA/");
        File folderPos = HomeDir.getFile("data/UCL_URO/000_typed/");
        File folderAll = HomeDir.getFile("data/LA/");
        List<File> xml1 = FileUtil.listFiles(folderAll, "xml", true);
        List<String> out = new LinkedList<>();
        List<String> out2 = new LinkedList<>();
        List<String> blacklist = FileUtil.readLines(HomeDir.getFile("lists/blacklist.txt"));
        for (File f : xml1) {
            if (blacklist.contains(f.getName().substring(0, 3))) {
                out2.add(f.getAbsolutePath());
                continue;
            }
            int size = PageXmlUtil.getTextLines(PageXmlUtil.unmarshal(f)).size();
            if (size > 5 && size < 200) {
                out.add(f.getAbsolutePath());
            }else{
                out2.add(f.getAbsolutePath());
            }
        }
        FileUtil.writeLines(HomeDir.getFile("image_6-199-written.txt"), out);
        FileUtil.writeLines(HomeDir.getFile("image_6-199-written-blacklist.txt"), out2);
        System.exit(-1);
        ObjectCounter<Integer> ocPos = new ObjectCounter<>();
        ObjectCounter<Integer> ocAll = new ObjectCounter<>();
        ObjectCounter<String> map = new ObjectCounter<>();
        int cntPositive = 0;
        int cntNegative = 0;
        for (File folder : new File[]{folderAll}) {
            map.reset();
            ObjectCounter<Integer> oc = folder == folderPos ? ocPos : ocAll;
//        File folder = HomeDir.getFile("data/UCL_URO/000_typed");
//        File folder = HomeDir.getFile("data/UCL_URO/000_typed/004_028_002");
            File[] files = folder.listFiles();
//            Arrays.sort(files, new ReverseComparator(new DefaultFileComparator()));
            Arrays.sort(files, File::compareTo);
            List<String> setName = new ArrayList<>();
            List<double[]> stat = new ArrayList<>();
//            int br = 0;
//            List<Pair<Integer, File>> examples = new LinkedList<>();
            int count = 0;
            for (File set : files) {
                if (!set.isDirectory()) {
                    continue;
                }
//                if (br >= 10) {
//                    break;
//                }
                List<File> xmls = FileUtil.listFiles(set, "xml", true);
                FileUtil.deleteMetadataAndMetsFiles(xmls);
                if (blacklist.contains(set.getName())) {
                    System.out.println("skip " + set);
                    cntNegative += xmls.size();
                    continue;
                }
                LinkedList<String> list = new LinkedList<>();
                for (File xml : xmls) {
                    int cnt = 0;
//                    if (count > 1000) {
//                        break;
//                    }

                    PcGtsType unmarshal = PageXmlUtil.unmarshal(xml);
                    List<TextLineType> textLines = PageXmlUtil.getTextLines(unmarshal);
                    boolean useBaseLines = false;
                    if (useBaseLines) {
                        Polygon[] polys = new Polygon[textLines.size()];
                        for (int i = 0; i < textLines.size(); i++) {
                            polys[i] = PolygonUtil.getBaseline(textLines.get(i));
                        }
                        polys = normDesDist(polys, 5);
                        int maxd = 250;
                        double[][] doubles = FindTypedPages.calcTolsUpperLower(polys, 5, maxd, 1.0);
                        if (doubles[0].length < 20) {
                            continue;
                        }
//                int cntFalse = 0;
                        int cntCalcs = 0;
                        for (int i = 0; i < doubles[0].length - 1; i++) {
                            double up = doubles[0][i];
                            double down = doubles[1][i];
                            if (up == 0D || down == 0D || Double.isNaN(up) || Double.isNaN(down) || up >= maxd || down >= maxd) {
                                continue;
                            }
                            cntCalcs++;
                            if (up < 60 && down < 60 && up < down * 1.1 && down < up * 1.1) {
                                cnt++;
                            }
//                    if (up < down * 1.5 && down < up * 1.5) {
//                        cntFalse++;
//                    }
                        }
//                cnt-=cntFalse;
                        cnt *= 100;
                        if (cntCalcs > 0) {
                            cnt /= cntCalcs;
                        }
                        double[] ds = doubles[0];
                        Arrays.sort(ds);
                        int size2 = ds.length == 0 ? 0 : (int) ds[(ds.length) / 2];
//                size2 = Math.min(size2, 250);
//                    if (size2 > 80) {
//                        cnt += 10;
//                    }
//            int size = PageXmlUtil.getTextLines(unmarshal).size();
                    } else {
                        if (textLines.size() > 200) {
                            cnt = textLines.size();
                            System.out.println(cntPositive + " vs. " + cntNegative);
                        } else if (textLines.size() < 5) {
                            cnt = 10 + textLines.size();
                            System.out.println(cntPositive + " vs. " + cntNegative);
                        }
                    }
                    oc.add(cnt);
                    if (cnt > 10) {
                        cntNegative++;
                        long before = map.get(xml.getName().substring(0, 3));
                        if (before == 0) {
                            Display.show(false, true);
                        }
                        map.add(xml.getName().substring(0, 3));
                        System.out.println(xml.getName());
                        System.out.println(map);
//                        if (examples.size() % 1000 == 999) {
//                            examples.sort((o1, o2) -> Integer.compare(o2.getFirst(), o1.getFirst()));
//                            for (int i = 0; i < Math.min(100, examples.size()); i++) {
                        HybridImage img = HybridImage.newInstance(PageXmlUtil.getImagePath(xml, true));
                        BufferedImage resize = ImageUtil.resize(img.getAsBufferedImage(), img.getWidth() / 4, img.getHeight() / 4);
//                        Display.addPanel(new DisplayPlanet(HybridImage.newInstance(resize)), cnt + " " + xml.getName());

//                            }
//                    examples.clear();
//                        }
                    } else {
                        cntPositive++;
                    }
                    count++;
//                    examples.add(new Pair<>(cnt, PageXmlUtil.getImagePath(xml, true)));
                    String str = String.format("%03d %s", cnt, xml.getPath());
                    list.add(str);
//                    System.out.println(count++ + " of " + xmls.size() + " ( " + cnt + " )");
//                if (count == 3000) {
//                    break;
//                }
                }
//            list.sort(String::compareTo);
//            System.out.println(oc);
//            List<Integer> result = oc.getResult();
//            result.sort(Integer::compareTo);
            }
        }
        System.out.println("positiv: " + cntPositive);
        System.out.println("negativ: " + cntNegative);
        long n1 = 0;
        long n2 = 0;
        List<Integer> resultPos = ocPos.getResult();
        List<Integer> resultAll = ocAll.getResult();
        System.out.println("ocPos: " + ocPos);
        System.out.println("ocAll: " + ocAll);
        Integer max = Math.max(Collections.max(resultAll), Collections.max(resultPos));
        double[] y1 = new double[max + 1];
        double[] y2 = new double[max + 1];
        for (int i = 0; i < y2.length; i++) {
            Long cnt = ocAll.get(i);
            if (cnt != null) {
                n1 += cnt;
            }
            y1[i] = (n1 * 100.0D);
            Long cnt2 = ocPos.get(i);
            if (cnt2 != null) {
                n2 += cnt2;
            }
            y2[i] = (n2 * 100.0D);
//                System.out.println(String.format("%3d %.2f%% %.2f%%", i, cnt * 100.0 / count, n * 100.0 / count));
        }
        for (int i = 0; i < y1.length; i++) {
            y1[i] /= n1;
            y2[i] /= n2;
        }
        List<String> setName = new ArrayList<>();
        setName.add("All");
        setName.add("Positiv");
        List<double[]> stat = new ArrayList<>();
        stat.add(y1);
        stat.add(y2);
//            if (br % 8 == 0) {
        double[] x = new double[y2.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
        }
        Gnuplot.plot(x, stat, "inter line distances", setName.toArray(new String[0]), 0, 100);
        setName.clear();
        stat.clear();
        System.out.println(map);
    }

}
