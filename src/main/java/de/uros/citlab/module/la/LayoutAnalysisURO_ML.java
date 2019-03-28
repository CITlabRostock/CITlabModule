/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.la;

import com.achteck.misc.types.Pair;
import com.achteck.misc.util.ArrayUtil;
import de.planet.imaging.types.HybridImage;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.util.PolygonHelper;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.*;
import de.uros.citlab.segmentation.CITlab_LA_ML;
import de.uros.citlab.segmentation.interfaces.ILineClusterer;
import eu.transkribus.core.model.beans.pagecontent.*;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextRegionType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.ILayoutAnalysis;
import eu.transkribus.interfaces.types.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.List;

/**
 * @author tobi
 */
public class LayoutAnalysisURO_ML implements ILayoutAnalysis, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(LayoutAnalysisURO_ML.class.getName());
    private String name;

    private final String netPath1 = "/net_tf/LA73_249_0mod360.pb";
    private final String netPath2 = "/net_tf/LA76_249_0mod90.pb";
    private final String singleConfigPath = "/net_tf/singleThread.conf";

    private String configPath = "";
    private String netPath = null;

    //    private static final String DEL_DEFAULT = "default";
    public static final String DEL_REGIONS = "regions";
    public static final String DEL_LINES = "lines";

    //    private static final String SEP_DEFAULT = "default";
    public static final String SEP_NEVER = "never";
    public static final String SEP_ALWAYS = "always";

    private static final String ROT_DFT = "default";
    public static final String ROT_HOM = "hom";
    public static final String ROT_HETRO = "het";

    private ILineClusterer impl = null;
    private final String netPathPreset;

    private final String[] propsDft;

    public LayoutAnalysisURO_ML(String[] props) {
        this(null, props);
    }

    public LayoutAnalysisURO_ML(String path, String[] props) {
        this.propsDft = props;
        netPathPreset = path;
    }

    private void initLineCluster(String[] props) {
        String netPathNew;
        String property = PropertyUtil.getProperty(props, Key.LA_ROTSCHEME);
        if (netPathPreset == null || netPathPreset.isEmpty()) {
            if (property == null) {
                netPathNew = netPath1;
            } else {
                if (!ROT_HOM.equals(property) && !ROT_HETRO.equals(property)) {
                    throw new RuntimeException("cannot interprete key '" + Key.LA_ROTSCHEME + "' with value '" + property + "'.");
                }
                netPathNew = netPath2;
            }
        } else {
            netPathNew = netPathPreset;
        }
        if (!netPathNew.equals(netPath)) {
            LOG.info("network '{}' loaded with property {} = {}", netPathNew, Key.LA_ROTSCHEME, property);
            if (PropertyUtil.isPropertyTrue(props, Key.LA_SINGLECORE)) {
                configPath = singleConfigPath;
            }
            if (impl != null) {
                LOG.warn("network '{}' was already loaded, load new network '{}'.", netPath, netPathNew);
            }
            netPath = netPathNew;
            impl = new CITlab_LA_ML(netPath, configPath);
            name = netPath + File.pathSeparator + impl.getClass().getName();
        }

    }

    public void processLayout(Image image, String xmlInOut) {
        process(image, xmlInOut, null, null);
    }

    @Override
    public void process(Image image, String xmlInOut, String[] ids, String[] props) {
        PcGtsType loadXml = null;
        loadXml = PageXmlUtil.unmarshal(new File(xmlInOut));
        process(image, loadXml, ids, props);
        PageXmlUtil.marshal(loadXml, new File(xmlInOut));
    }

    public boolean process(Image image, PcGtsType xmlFile, String[] ids, String[] props) {

        if (PropertyUtil.hasProperty(props, Key.LA_SINGLECORE)) {
            LOG.warn("SingleThread prop is just taken into account in constructor, NOT in process method. ");
        }
        String[] propCur = PropertyUtil.merge(props, propsDft);
        initLineCluster(props);

        PageType page = xmlFile.getPage();
        List<TrpRegionType> globRegs = page.getTextRegionOrImageRegionOrLineDrawingRegion();
        List<TextRegionType> globTextRegs = PageXmlUtils.getTextRegions(xmlFile);

//        List<TextRegionType> globTextRegs = PageXmlUtils.getTextRegions(xmlFile);
        String deleteScheme = PropertyUtil.getProperty(propCur, Key.LA_DELETESCHEME);
        boolean doLA = globTextRegs.isEmpty();
        if (deleteScheme != null) {
            doLA = true;
            switch (deleteScheme) {
                case DEL_REGIONS:
                    if (!globRegs.isEmpty()) {
                        LOG.debug("delete {} text regions", globRegs.size());
                        globRegs.clear();
                        globTextRegs.clear();
                    }
                    break;
                case DEL_LINES:
                    //If Flag is set, delete all TextLines in given TextRegions
                    for (TextRegionType aTextReg : globTextRegs) {
                        List<TextLineType> textLine = aTextReg.getTextLine();
                        if (!textLine.isEmpty()) {
                            LOG.debug("delete " + textLine.size() + " text lines in TextRegion " + aTextReg.getId());
                            textLine.clear();
                        }
                    }
                    break;
                default:
                    throw new RuntimeException("cannot interprete key '" + Key.LA_DELETESCHEME + "' with value '" + deleteScheme + "'.");
            }
        }
        //All TextRegions NOT containing any TextLines are further processed.
        List<TextRegionType> reducedTextRegs = new ArrayList<>();
        List<Polygon2DInt> reducedTextRegsPoly = new ArrayList<>();
        for (TextRegionType aGlobTextRegion : globTextRegs) {
            if (ids == null || ArrayUtil.linearSearch(ids, aGlobTextRegion.getId()) >= 0) {
                List<TextLineType> textLines = aGlobTextRegion.getTextLine();
                if (textLines == null || textLines.isEmpty()) {
                    doLA = true;
                    reducedTextRegs.add(aGlobTextRegion);
                    try {
                        reducedTextRegsPoly.add(PolygonUtil.string2Polygon2DInt(aGlobTextRegion.getCoords().getPoints()));
                    } catch (RuntimeException ex) {
                        LOG.warn("cannot process region with id {} and polygon {}.", aGlobTextRegion.getId(), aGlobTextRegion.getCoords().getPoints(), ex);
                    }
                }
            }
        }
        if (!doLA) {
            MetadataUtil.addMetadata(xmlFile, this);
            return true;
        }
        HybridImage hi = ImageUtil.getHybridImage(image, true);
        List<Polygon2DInt> polysToRegard = null;
        if (!reducedTextRegsPoly.isEmpty()) {
            polysToRegard = reducedTextRegsPoly;
        }

        String seperatorScheme = PropertyUtil.getProperty(propCur, Key.LA_SEPSCHEME);
        boolean useSep;
        if (seperatorScheme == null) {
            useSep = polysToRegard == null;
        } else {
            switch (seperatorScheme) {
                case SEP_ALWAYS:
                    useSep = true;
                    break;
                case SEP_NEVER:
                    useSep = false;
                    break;
                default:
                    throw new RuntimeException("cannot interprete key '" + Key.LA_SEPSCHEME + "' with value '" + seperatorScheme + "'.");
            }
        }

        String rotationScheme = PropertyUtil.getProperty(props, Key.LA_ROTSCHEME, ROT_DFT);
        if (!rotationScheme.equals(ROT_DFT) && !rotationScheme.equals(ROT_HETRO) && !rotationScheme.equals(ROT_HOM)) {
            throw new RuntimeException("cannot interprete key '" + Key.LA_ROTSCHEME + "' with value '" + rotationScheme + "'.");
        }

        ILineClusterer.IRes res = impl.process(hi, polysToRegard, useSep);
        List<Polygon2DInt> bls = res.getBaseLines();
        if (bls != null && !bls.isEmpty()) {
            buildRegs(hi, page, globRegs, reducedTextRegs, bls, rotationScheme);
        }
        MetadataUtil.addMetadata(xmlFile, this);

        if (!image.hasType(Image.Type.OPEN_CV) && hi.getOpenCVMatImage() != null) {
            hi.getOpenCVMatImage().release();
        }

        return true;
    }

    @Override
    public String usage() {
        return "The method detects baselines in an image.\n"
                + "Therefore, it uses a neural network to detect baselines and seperators.\n"
                + "In general it tries to enrich a given document:\n"
                + "If no region is given, lines are found in the whole image.\n"
                + "If the lines are seperateable into columns, each column becomes a region, ordered left to right.\n"
                + "If regions are given, simple line layout will be applied in the region,\n"
                + "that means it is assumed that in one region there is only one column and without marginalia.\n"
                + "If property '" + Key.LA_SEPSCHEME + " is set to '" + SEP_ALWAYS + ", even in given regions the algorithm tries to seperate the structure into columns.\n"
                + "If property is set to '" + SEP_NEVER + "', the algorithm will assume simple line layout - even if no prior region is given.\n\n"
                + "The property '" + Key.LA_DELETESCHEME + "' can be used to delete regions or lines in advance:\n"
                + "If property is '" + DEL_REGIONS + "', all regions are deleted and complete LA is done for the whole image.\n"
                + "If property is '" + DEL_LINES + "', all lines in regions are deleted and line layout will be applied in the region.\n\n"
                + "The property '" + Key.LA_ROTSCHEME + "' can be set, if there is prior knowlege of the text layout.\n"
                + "If no property is set, the algorithm assumes that the text orientation is approx 0°.\n"
                + "If property is '" + ROT_HOM + "', the text orientation is approx 0,90,180 or 270° but constant over the whole page.\n"
                + "If property is '" + ROT_HETRO + "', the text orientation can be arbitrary and mixed on the whole page.\n";
    }

    @Override
    public String getToolName() {
        return name;
    }

    @Override
    public String getProvider() {
        return MetadataUtil.getProvider("Tobias Gruening", "tobias.gruening@uni-rostock.de");
    }

    @Override
    public String getVersion() {
        return MetadataUtil.getSoftwareVersion();
    }

    private PageType buildRegs(HybridImage hi, PageType page, List<TrpRegionType> globRegs, List<TextRegionType> reducedTextRegions, List<Polygon2DInt> bls, String thisRotateScheme) {

        if (reducedTextRegions == null || reducedTextRegions.isEmpty()) {
            List<Cluster> cluster = clusterBLs(hi, bls, thisRotateScheme, true, true);
            int cntRegion = 0;

            for (Cluster aCluster : cluster) {
                TrpTextRegionType rt = new TrpTextRegionType();
                rt.setOrientation((float) aCluster.getAngle());
                rt.setCoords(PolygonUtil.polyon2Coords(aCluster.getCoords()));
                rt.setParent(page);
                //set reading order explicitely
                rt.setReadingOrder(cntRegion, null);
                rt.setId("r" + (++cntRegion));
                globRegs.add(rt);
                int cntLine = 0;
                if (aCluster.bls != null) {

                    for (int i = 0; i < aCluster.bls.size(); i++) {
                        Polygon2DInt aBL = aCluster.bls.get(i);
                        Polygon2DInt aSur = aCluster.surr.get(i);
                        TrpTextLineType textLine = new TrpTextLineType();
                        rt.getTextLine().add(textLine);
                        textLine.setParent(rt);
                        //set reading order explicitely
                        textLine.setReadingOrder(cntLine, null);
                        textLine.setId(rt.getId() + "l" + (++cntLine));
                        textLine.setBaseline(PolygonUtil.polyon2TrpBaseline(aBL));
                        textLine.setCoords(PolygonUtil.polyon2Coords(aSur));
                    }
                }
            }
        } else {
            int assigned = 0;
            for (TextRegionType aReg : reducedTextRegions) {
                List<Polygon2DInt> regBLs = new ArrayList<>();
                CoordsType aCoords = aReg.getCoords();
                Polygon aRegPoly = PolygonUtil.string2Polygon(aCoords.getPoints());
                for (Polygon2DInt aBL : bls) {
                    if (aRegPoly.contains(aBL.xpoints[aBL.npoints / 2], aBL.ypoints[aBL.npoints / 2])) {
                        assigned++;
                        regBLs.add(aBL);
                    }
                }
                List<Cluster> regCluster;
                regCluster = clusterBLs(
                        hi,
                        regBLs,
                        thisRotateScheme.equals(ROT_HETRO) ? ROT_HOM : thisRotateScheme,
                        false,
                        false);
                if (regCluster == null || regCluster.isEmpty()) {
                    continue;
                }
                int cntLine = 0;
                Cluster aCluster = regCluster.get(0);
                aReg.setOrientation((float) aCluster.getAngle());
                for (int i = 0; i < aCluster.bls.size(); i++) {
                    Polygon2DInt aBL = aCluster.bls.get(i);
                    Polygon2DInt aCoord = aCluster.surr.get(i);
                    TrpTextLineType textLine = new TrpTextLineType();
                    aReg.getTextLine().add(textLine);
                    textLine.setParent(aReg);
                    //set reading order explicitely
                    textLine.setReadingOrder(cntLine, null);
                    textLine.setId(aReg.getId() + "l" + (++cntLine));
                    textLine.setBaseline(PolygonUtil.polyon2TrpBaseline(aBL));
                    textLine.setCoords(PolygonUtil.polyon2Coords(aCoord));

                }

            }
            if (assigned != bls.size()) {
                LOG.warn("Number of BLs which were assigned to regions is inconsistent!");
            }
        }
        return page;
    }

    private List<Cluster> clusterBLs(HybridImage hi, List<Polygon2DInt> bls, String thisRotateScheme, boolean rotateBaselines, boolean clustLeft2Right) {
        short[][] absSobelSum = null;
        if (!thisRotateScheme.equals(ROT_DFT)) {
            absSobelSum = ImageUtil.calcAbsSobelSum(hi.getAsOpenCVMatImage());
        }

        List<Cluster> cluster = new ArrayList<>();
        ArrayList<Pair<Double, Polygon2DInt>> wBLs = new ArrayList<>();
        for (Polygon2DInt aBL : bls) {
            Polygon2DInt blownUp = PolygonUtil.blowUp(aBL);
            Polygon2DInt thinOut = PolygonUtil.thinOut(blownUp, Math.max(blownUp.npoints / 20, 25));
            double angle = PolygonUtil.calcRegLineStats(blownUp)[0];
            if (angle > 7.0 * Math.PI / 4.0 || angle < Math.PI / 4.0 || (angle > 3.0 * Math.PI / 4.0 && angle < 5.0 * Math.PI / 4.0)) {
                angle = 0.0;
            } else {
                angle = Math.PI / 2.0;
            }
            if (!thisRotateScheme.equals(ROT_DFT)) {
                //            Check Modulo 180°   with simple hist approach 
                if (angle == 0.0) {
                    boolean isTop = checkTop(blownUp, absSobelSum);
                    if (!isTop) {
                        angle = Math.PI;
                    }
                } else {
                    boolean isLeft = checkLeft(blownUp, absSobelSum);
                    if (!isLeft) {
                        angle = 3.0 * Math.PI / 2.0;
                    }
                }
                wBLs.add(new Pair<>(angle, thinOut));
            } else if (angle == 0.0) {
                wBLs.add(new Pair<>(angle, thinOut));
            } else {
                LOG.debug("ignore baseline because angle is {} and {} = {}.", angle, Key.LA_ROTSCHEME, thisRotateScheme);
            }
        }
        if (thisRotateScheme.equals(ROT_HOM)) {
            int[] hist = new int[4];
            for (Pair<Double, Polygon2DInt> aWBL : wBLs) {
                if (aWBL.first == 0.0) {
                    hist[0]++;
                }
                if (aWBL.first == Math.PI / 2.0) {
                    hist[1]++;
                }
                if (aWBL.first == Math.PI) {
                    hist[2]++;
                }
                if (aWBL.first == 3.0 * Math.PI / 2.0) {
                    hist[3]++;
                }
            }
            double winner = 0.0;
            int winCnt = hist[0];
            if (hist[1] > winCnt) {
                winCnt = hist[1];
                winner = Math.PI / 2.0;
            }
            if (hist[2] > winCnt) {
                winCnt = hist[2];
                winner = Math.PI;
            }
            if (hist[3] > winCnt) {
                winCnt = hist[3];
                winner = 3.0 * Math.PI / 2.0;
            }
            Iterator<Pair<Double, Polygon2DInt>> iter = wBLs.iterator();
            while (iter.hasNext()) {
                Pair<Double, Polygon2DInt> aWBL = iter.next();
                double ang = aWBL.first;
                if (ang == winner || Math.abs(ang - winner) == Math.PI) {
                    aWBL.first = winner;
                } else {
                    iter.remove();
                }
            }
        }

        for (Pair<Double, Polygon2DInt> aWBL : wBLs) {
            flipBL(aWBL.second, aWBL.first);
        }

        while (!wBLs.isEmpty()) {
            Pair<Double, Polygon2DInt> init = wBLs.remove(0);
            double aAngle = init.first;
            List<Polygon2DInt> cBLs = new ArrayList<>();
            cBLs.add(init.second);

            Iterator<Pair<Double, Polygon2DInt>> iter = wBLs.iterator();
            while (iter.hasNext()) {
                Pair<Double, Polygon2DInt> aBL = iter.next();
                if (aBL.first == aAngle) {
                    cBLs.add(aBL.second);
                    iter.remove();
                }
            }
            List<List<Polygon2DInt>> clusters;
            if (clustLeft2Right) {
                List<Polygon2DInt> rotBLs = new ArrayList<>();
                for (Polygon2DInt cBL : cBLs) {
                    Polygon2DInt polygonRotated = PolygonUtil.rotate(cBL, -aAngle);
                    rotBLs.add(polygonRotated);
                }
                clusters = PolygonUtil.clusterLeft2Right(rotBLs, 0.1, 0);
            } else {
                clusters = new ArrayList<>();
                clusters.add(cBLs);
            }
            for (List<Polygon2DInt> aClust : clusters) {
                Cluster aC = new Cluster(aAngle, hi.getHeight(), hi.getWidth());
                for (Polygon2DInt aBL : aClust) {
                    if (rotateBaselines) {
                        Polygon2DInt polygonRotated = PolygonUtil.rotate(aBL, aAngle);
                        aC.addBL(polygonRotated);
                    } else {
                        aC.addBL(aBL);
                    }
                }
                aC.sort();
                aC.calcSurr();
                aC.calcCoords();
                cluster.add(aC);
            }
        }
        return cluster;
    }

    private boolean checkTop(Polygon2DInt blownUp, short[][] absSobelSum) {
        int rg = 25;
        double avgTop = 0.0;
        int cntTop = 0;
        double avgBot = 0.0;
        int cntBot = 0;
        for (int i = 0; i < blownUp.npoints; i++) {
            int aX = blownUp.xpoints[i];
            if (aX < 0 || aX >= absSobelSum[0].length) {
                continue;
            }
            int aY = blownUp.ypoints[i];
            for (int j = 1; j <= rg; j++) {
                int tY = aY - j;
                if (tY >= 0) {
                    avgTop += absSobelSum[tY][aX];
                    cntTop++;
                }
                int bY = aY + j;
                if (bY < absSobelSum.length) {
                    avgBot += absSobelSum[bY][aX];
                    cntBot++;
                }
            }
        }
        if (cntTop > 0) {
            avgTop /= cntTop;
        }
        if (cntBot > 0) {
            avgBot /= cntBot;
        }
        return avgTop >= avgBot;
    }

    private boolean checkLeft(Polygon2DInt blownUp, short[][] absSobelSum) {
        int rg = 25;
        double avgLeft = 0.0;
        int cntLeft = 0;
        double avgRight = 0.0;
        int cntRight = 0;
        for (int i = 0; i < blownUp.npoints; i++) {
            int aX = blownUp.xpoints[i];
            int aY = blownUp.ypoints[i];
            for (int j = 1; j <= rg; j++) {
                int lX = aX - j;
                if (lX >= 0) {
                    avgLeft += absSobelSum[aY][lX];
                    cntLeft++;
                }
                int rX = aX + j;
                if (rX < absSobelSum[0].length) {
                    avgRight += absSobelSum[aY][rX];
                    cntRight++;
                }
            }
        }
        avgLeft /= cntLeft;
        avgRight /= cntRight;
        return avgLeft >= avgRight;
    }

    private void flipBL(Polygon2DInt aBL, double angle) {
        int mode = 0;
        if (angle == Math.PI) {
            mode = 1;
        }
        if (angle == Math.PI / 2.0) {
            mode = 2;
        }
        if (angle == 3.0 * Math.PI / 2.0) {
            mode = 3;
        }

        switch (mode) {
            case 0:
                //Angle 0.0
                if (aBL.xpoints[0] > aBL.xpoints[aBL.npoints - 1]) {
                    flipPoly(aBL);
                }
                break;
            case 1:
                //Angle PI
                if (aBL.xpoints[0] < aBL.xpoints[aBL.npoints - 1]) {
                    flipPoly(aBL);
                }
                break;
            case 2:
                //Angle PI/2.0
                if (aBL.ypoints[0] < aBL.ypoints[aBL.npoints - 1]) {
                    flipPoly(aBL);
                }
                break;
            case 3:
                //Angle 3.0*PI/2.0
                if (aBL.ypoints[0] > aBL.ypoints[aBL.npoints - 1]) {
                    flipPoly(aBL);
                }
                break;
            default:
                break;
        }

    }

    private void flipPoly(Polygon2DInt aBL) {
        Polygon2DInt aCpy = PolygonHelper.copy(aBL);
        for (int i = 0; i < aBL.npoints; i++) {
            aBL.xpoints[i] = aCpy.xpoints[aBL.npoints - 1 - i];
            aBL.ypoints[i] = aCpy.ypoints[aBL.npoints - 1 - i];
        }
    }

    private class Cluster {

        double angle;
        List<Polygon2DInt> bls;
        List<Polygon2DInt> surr;
        Polygon2DInt coords;
        int imgH, imgW;

        private Cluster(double angle, int imgH, int imgW) {
            this.angle = angle;
            bls = new ArrayList<>();
            this.imgH = imgH;
            this.imgW = imgW;
            surr = new ArrayList<>();
        }

        private double getAngle() {
            return angle;
        }

        private List<Polygon2DInt> getBLs() {
            return bls;
        }

        private Polygon2DInt getCoords() {
            if (coords == null) {
                calcCoords();
            }
            return coords;
        }

        private void addBL(Polygon2DInt aBL) {
            bls.add(aBL);
        }

        private void calcCoords() {
            Polygon2DInt allPoly = new Polygon2DInt();
            for (Polygon2DInt aBL : surr) {
                PolygonHelper.addPoints(allPoly, aBL);
            }
            coords = allPoly.getBounds().getPolygon();
        }

        private int getAvg(int[] arr, int n) {
            int cnt = 0;
            for (int i = 0; i < n; i++) {
                cnt += arr[i];
            }
            return cnt / n;
        }

        private void sort() {
            Collections.sort(bls, new Comparator<Polygon2DInt>() {
                @Override
                public int compare(Polygon2DInt o1, Polygon2DInt o2) {
                    if (angle == 0.0) {
                        return Integer.compare(getAvg(o1.ypoints, o1.npoints), getAvg(o2.ypoints, o2.npoints));
                    }
                    if (angle == Math.PI / 2.0) {
                        return Integer.compare(getAvg(o1.xpoints, o1.npoints), getAvg(o2.xpoints, o2.npoints));
                    }
                    if (angle == Math.PI) {
                        return Integer.compare(getAvg(o2.ypoints, o2.npoints), getAvg(o1.ypoints, o1.npoints));
                    }
                    return Integer.compare(getAvg(o2.xpoints, o2.npoints), getAvg(o1.xpoints, o1.npoints));
                }
            });
//            for (Polygon2DInt bl : bls) {
//                System.out.println(getAvg(bl.ypoints, bl.npoints) + " " + PolygonHelper.getCenterOfMass(bl));
//            }
        }

        private void calcSurr() {
            double[] calcTols = PolygonUtil.calcTols(bls, 5, 200, 0.5);
            for (int i = 0; i < calcTols.length; i++) {
                Polygon2DInt aBL = bls.get(i);
                Polygon2DInt aP = new Polygon2DInt();
                double aTol = Math.max(calcTols[i], 20);
                for (int j = 0; j < aBL.npoints; j++) {
                    int aX = aBL.xpoints[j];
                    int aY = aBL.ypoints[j];

                    int nX = aX;
                    int nY = aY;
                    if (angle == 0.0) {
                        nY += (int) (aTol / 2);
                        nX = aX;
                    }
                    if (angle == Math.PI) {
                        nY -= (int) (aTol / 2);
                        nX = aX;
                    }
                    if (angle == Math.PI / 2.0) {
                        nX += (int) (aTol / 2);
                        nY = aY;
                    }
                    if (angle == 3.0 * Math.PI / 2.0) {
                        nX -= (int) (aTol / 2);
                        nY = aY;
                    }
                    nX = Math.max(nX, 0);
                    nX = Math.min(nX, imgW - 1);
                    nY = Math.max(nY, 0);
                    nY = Math.min(nY, imgH - 1);
                    aP.addPoint(nX, nY);
                }
                for (int j = aBL.npoints - 1; j >= 0; j--) {
                    int aX = aBL.xpoints[j];
                    int aY = aBL.ypoints[j];

                    int nX = aX;
                    int nY = aY;
                    if (angle == 0.0) {
                        nY -= (int) (aTol);
                        nX = aX;
                    }
                    if (angle == Math.PI) {
                        nY += (int) (aTol);
                        nX = aX;
                    }
                    if (angle == Math.PI / 2.0) {
                        nX -= (int) (aTol);
                        nY = aY;
                    }
                    if (angle == 3.0 * Math.PI / 2.0) {
                        nX += (int) (aTol);
                        nY = aY;
                    }
                    nX = Math.max(nX, 0);
                    nX = Math.min(nX, imgW - 1);
                    nY = Math.max(nY, 0);
                    nY = Math.min(nY, imgH - 1);
                    aP.addPoint(nX, nY);
                }
                surr.add(aP);
            }
        }
    }
}
