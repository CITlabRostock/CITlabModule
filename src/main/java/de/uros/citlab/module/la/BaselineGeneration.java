/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.la;

import de.uros.citlab.module.interfaces.IP2B;
import de.planet.imaging.types.HybridImage;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.util.PolygonHelper;
import de.planet.math.util.VectorUtil;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.PolygonUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.types.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gundram
 */
public class BaselineGeneration /*extends ParamTreeOrganizer*/ implements Serializable, IP2B {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(BaselineGeneration.class.getName());
//    @ParamAnnotation(descr = "default shift in y-dimension (positiv=lower)")
    private final int shifty;

    public BaselineGeneration() {
        this(15);
        //  addReflection(this, BaselineGeneration.class);
    }

    public BaselineGeneration(int shifty) {
        this.shifty = shifty;
    }

    public void processOld(HybridImage img, TrpTextLineType textLine) {
//        coords = new Polygon(new int[]{0, 10, 10, 0}, new int[]{0, 0, 20, 20}, 4);
        Polygon p = PageXmlUtils.buildPolygon(textLine.getCoords());
        Rectangle bounds = p.getBounds();
        int numPoints = Math.max(Math.min(10, bounds.width / 3), 2);
        int[] xVec = new int[numPoints];
        int[] yVec = new int[numPoints];
        LOG.info(Arrays.toString(p.xpoints));
        LOG.info(Arrays.toString(p.ypoints));
        LOG.info("--------------------");
        double stepsize = ((double) bounds.width) / (numPoints - 1);
        for (int i = 0; i < numPoints - 1; i++) {
            xVec[i] = (int) Math.round(bounds.x + stepsize * i);
            int upper = bounds.y;
            while (!p.contains(xVec[i], upper) && upper < bounds.y + bounds.height) {
                upper++;
            }
            int lower = bounds.y + bounds.height;
            while (!p.contains(xVec[i], lower) && lower > bounds.y) {
                lower--;
            }
            yVec[i] = (upper + lower) / 2 + shifty;
        }
        int yMin = -1;
        int yMax = Integer.MAX_VALUE;
        int max = -1;
        for (int i = 0; i < p.npoints; i++) {
            final int y = p.ypoints[i];
            final int x = p.xpoints[i];
            if (max < x) {
                max = x;
                yMin = y;
                yMax = y;
            }
            if (max == x) {
                yMin = Math.min(yMin, y);
                yMax = Math.max(yMax, y);
            }
        }
        xVec[numPoints - 1] = max;
        yVec[numPoints - 1] = (yMax + yMin) / 2;
        textLine.setBaseline(PolygonUtil.polyon2TrpBaseline(new Polygon(xVec, yVec, xVec.length)));
    }

    @Override
    public void processLine(HybridImage hi, TextLineType tlt, TextRegionType rt) {
        Polygon2DInt coords = PolygonUtil.convert(PolygonUtil.getPolygon(tlt));
        double orientation = rt != null && rt.getOrientation() != null ? rt.getOrientation().doubleValue() : 0.0;
        HybridImage rotatedSubI = ImageUtil.rotate(hi, coords, -orientation, 0, 0, 0);
        Polygon2DInt cpyP = PolygonHelper.copy(coords);
        PolygonHelper.transform(cpyP, rotatedSubI.getTrafo());
//        ByteImage bImg = hi.getAsByteImage();
//        Rectangle2DInt aBB = coords.getBounds();
//        aBB.x = Math.max(aBB.x,0);
//        aBB.w = Math.min(aBB.w, img.getWidth()-aBB.x);
//        aBB.y = Math.max(aBB.y,0);
//        aBB.h = Math.min(aBB.h, img.getHeight()-aBB.y);
//        System.out.println(aBB);
//        ByteImage aSubI = SubImage.get(bImg, aBB.x, aBB.y, aBB.w, aBB.h);
        Pair<int[], int[]> topBot = PolygonUtil.getTopBot(cpyP);
        short[][] aSobel = ImageUtil.getCombineSobelImg(rotatedSubI);
        int[] topV = topBot.getFirst();
        int[] botV = topBot.getSecond();
//        for (int i = 0; i < botV.length; i++) {
//            botV[i] -= aBB.y;
//            topV[i] -= aBB.y;
//        }

        double partMax = 0.25;
        int[] maxV = new int[topV.length];
        int[] lastIdxPartMax = new int[topV.length];
        for (int i = 0; i < aSobel.length; i++) {
            short[] aSobelA = aSobel[i];
            for (int j = 0; j < aSobelA.length; j++) {
                if (i >= topV[j] && i <= botV[j]) {
                    int aVal = aSobelA[j];
                    maxV[j] = Math.max(maxV[j], aVal);
                    if (aVal >= partMax * maxV[j]) {
                        lastIdxPartMax[j] = i;
                    }
                }
            }
        }
        int rg = 100;
        Polygon2DInt res = new Polygon2DInt();

        int aIdx = 0;

        while (aIdx + rg < maxV.length) {
            int rgMaxV = -1;
            int sumPos = 0;
            int cnt = 0;
            for (int i = 0; i < rg; i++) {
                if (aIdx + i < maxV.length) {
                    rgMaxV = Math.max(rgMaxV, maxV[aIdx + i]);
                }
            }
            for (int i = 0; i < rg; i++) {
                if (maxV[aIdx + i] > rgMaxV / 10) {
                    sumPos += lastIdxPartMax[aIdx + i];
                    cnt++;
                }
            }
            if (cnt > 0) {
                int aPos = sumPos / cnt;
                res.addPoint(aIdx, aPos);
            }
            aIdx += rg;
        }

        if (res.npoints == 0) {
            int mIdx = VectorUtil.getMaxIndex(maxV);
            res.addPoint(0, lastIdxPartMax[mIdx]);
        }
        res.addPoint(rotatedSubI.getWidth() - 1, res.ypoints[res.npoints - 1]);
        PolygonHelper.invert(res, rotatedSubI.getTrafo());
        PolygonUtil.setBaseline(tlt, PolygonUtil.convert(res));

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
    public void processRegion(HybridImage img, TextRegionType trt) {
        for (int i = 0; i < trt.getTextLine().size(); i++) {
            TextLineType line = trt.getTextLine().get(i);
            try {
                processLine(img, line, trt);
            } catch (Throwable e) {
                LOG.warn("cannot process line '" + line.getId() + "' - ignore baseline creation.", e);
            }
        }
    }

}
