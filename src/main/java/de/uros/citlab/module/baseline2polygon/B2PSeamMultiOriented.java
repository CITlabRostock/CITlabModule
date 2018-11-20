////////////////////////////////////////////////
/// File:       BaseLine2Polygon.java
/// Created:    14.07.2016  11:17:02
/// Encoding:   UTF-8
////////////////////////////////////////////////
package de.uros.citlab.module.baseline2polygon;

import com.achteck.misc.log.Logger;
import com.achteck.misc.types.Pair;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamSetOrganizer;
import de.planet.imaging.types.HybridImage;
import de.planet.imaging.types.StdFrameAppender;
import de.planet.imaging.util.ImageHelper;
import de.planet.imaging.util.OpenCVHelper;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.util.PolygonHelper;
import de.uros.citlab.module.interfaces.IB2P;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.PolygonUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Desciption of BaseLine2Polygon
 *
 *
 * Since 14.07.2016
 *
 * @author Tobi <tobias.gruening.hro@gmail.com>
 */
public class B2PSeamMultiOriented extends ParamSetOrganizer implements IB2P {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(B2PSeamMultiOriented.class.getName());

    @ParamAnnotation(descr = "desired textline width")
    private int desLineWidth = 800;

//    @Override
//    public List<Polygon2DInt> process(HybridImage image, List<Polygon2DInt> polygons, String[] props) {
//        return process(image, polygons);
//    }
    /**
     *
     * @param aImg image
     * @param polys list of baselines in arbitrary orientation
     * @param angle
     * @return
     */
    @Override
    public List<Polygon2DInt> process(HybridImage aImg, List<Polygon2DInt> polys, Double angle) {
        if (polys == null) {
            return null;
        }
        if (polys.isEmpty()) {
            return new LinkedList<>();
        }
        List<Polygon2DInt> res = new ArrayList<Polygon2DInt>();

        List<BaseLine> bLs = new ArrayList<BaseLine>();
        for (Polygon2DInt aBL : polys) {
            bLs.add(new BaseLine(aBL));
        }

        //Plot It
        if (LOG.isTraceEnabled()) {
            ArrayList<Polygon2DInt> pL = new ArrayList<Polygon2DInt>();
            for (BaseLine aBL : bLs) {
                Polygon2DInt pCpy = PolygonHelper.copy(aBL.getbL());
                int pts = pCpy.npoints;
                for (int i = 0; i < pts; i++) {
                    pCpy.addPoint(pCpy.xpoints[pts - 1 - i], pCpy.ypoints[pts - 1 - i]);
                }
                pL.add(pCpy);
            }
            LOG.log(Logger.TRACE, new StdFrameAppender.AppenderContent(aImg, "Input Img with baseLines", PolygonHelper.getIFPolygonList(pL), false));
        }

        //BlowUp BaseLines + Calculation of Angles + Calculation of rescale factor
        int maxBlownOfMon = 0;
        double aAngle = 0.0;
        for (BaseLine aBL : bLs) {
            Polygon2DInt blownUp = PolygonUtil.blowUp(aBL.getbL());
            aBL.setMinDist(blownUp.npoints / 2);
//            aBL.setOrientation(angle != null ? angle : PolygonUtil.calcRegLineStats(blownUp)[0]);
            aBL.setOrientation(PolygonUtil.calcRegLineStats(blownUp)[0]);
            if (blownUp.npoints > maxBlownOfMon) {
                maxBlownOfMon = blownUp.npoints;
                aAngle = aBL.getOrientation();
            }
        }
        double ratOfImg;
        int imgSpan;
        if (Math.min(Math.min(Math.abs(aAngle - Math.PI), aAngle), Math.abs(aAngle - 2.0 * Math.PI)) < Math.PI / 4.0) {
            ratOfImg = (double) aImg.getWidth() / maxBlownOfMon;
            imgSpan = aImg.getWidth();
        } else {
            ratOfImg = (double) aImg.getHeight() / maxBlownOfMon;
            imgSpan = aImg.getHeight();
        }
        //Dependend of linelength and fraction of page calculate a downsampling factor
        double downScale = Math.max(1.0, (double) maxBlownOfMon / desLineWidth);
        if (ratOfImg > 5.0) {
            double maxDSC = 1.0;
            if (imgSpan > 2400) {
                maxDSC = 2.0;
            }
            if (imgSpan > 3600) {
                maxDSC = 3.0;
            }
            if (imgSpan > 4800) {
                maxDSC = 4.0;
            }
            downScale *= Math.min(ratOfImg / 3.0, maxDSC);
            downScale = Math.min(downScale, maxDSC);
        }

        //Normalization of BaseLines
        for (int i = 0; i < bLs.size(); i++) {
            BaseLine aBL = bLs.get(i);
            Polygon2DInt monotonPoly = PolygonUtil.monotonPoly(aBL.getbL(), aBL.getOrientation());
            if (monotonPoly.npoints < 2) {
                LOG.log(Logger.ERROR, "BL orientation doesn't fit region angle. Using default monoton poly instead.");
                monotonPoly = new Polygon2DInt();
                monotonPoly.addPoint(aBL.getbL().xpoints[0], aBL.getbL().ypoints[0]);
                monotonPoly.addPoint(aBL.getbL().xpoints[aBL.getbL().npoints - 1], aBL.getbL().ypoints[aBL.getbL().npoints - 1]);
            }
            aBL.setbLmon(monotonPoly);
        }

        //Calculate the size of the interesting window
        for (BaseLine aBl : bLs) {
            for (BaseLine cBl : bLs) {
                if (aBl != cBl) {
                    aBl.updateMinDist(cBl);
                }
            }
        }

        //Calculate the same window height for all lines, it is adapted later if necessary
        double[] hs = new double[bLs.size()];
        for (int i = 0; i < hs.length; i++) {
            hs[i] = bLs.get(i).getMinDist();
        }
        Arrays.sort(hs);
        int s = hs.length / 4;
        int e = 3 * hs.length / 4;
        double meanD = 0.0;
        for (int i = s; i <= e; i++) {
            meanD += hs[i];
        }
        meanD /= e - s + 1;
        meanD *= 1.5;
        meanD /= downScale;

        HybridImage tmp = ImageHelper.scale(1.0 / downScale, aImg);
        HybridImage scaled = OpenCVHelper.blur(tmp, null, 3, 3);
        tmp.clear();

        for (BaseLine aBL : bLs) {
            Polygon2DInt bl = aBL.getbLmon();
            Polygon2DInt blScaled = PolygonHelper.copy(bl);
            PolygonHelper.transform(blScaled, scaled.getTrafo());
            HybridImage subImg = ImageUtil.rotate(scaled, blScaled, -aBL.getOrientation(), 2, (int) meanD, (int) (meanD / 2));
            aBL.setSubImg(subImg);
            for (BaseLine cBL : bLs) {
                if (cBL != aBL) {
                    aBL.addObstacle(cBL.getbL());
                }
            }
            aBL.calcShiftedBL();
        }

        for (BaseLine aBL : bLs) {
            for (BaseLine cBL : bLs) {
                if (cBL != aBL) {
                    aBL.addObstacle(cBL.getbLShifted());
                }
            }

            boolean valid = aBL.calcSeparatingSeams();
            if (valid) {
                Pair<Integer, Polygon2DInt> scTop = aBL.getSCTop();
                Pair<Integer, Polygon2DInt> scBot = aBL.getSCBot();
                if (!aBL.isTopBounded()) {
                    if (scTop.first > 2.0 * scBot.first) {
                        HybridImage subImg = aBL.getSubImg();
                        Polygon2DInt bl = aBL.getbLmon();
                        Polygon2DInt blScaled = PolygonHelper.copy(bl);
                        PolygonHelper.transform(blScaled, scaled.getTrafo());
                        HybridImage subImgB = ImageUtil.rotate(scaled, blScaled, -aBL.getOrientation(), 1, (int) (3 * meanD), (int) (meanD / 2));
                        aBL.setSubImg(subImgB);
                        for (BaseLine cBL : bLs) {
                            if (cBL != aBL) {
                                aBL.addObstacle(cBL.getbL());
                            }
                        }
                        aBL.calcShiftedBL();
                        aBL.calcSeparatingSeams();
                        Pair<Integer, Polygon2DInt> scTopB = aBL.getSCTop();
                        Pair<Integer, Polygon2DInt> scBotB = aBL.getSCBot();

                        if (scTopB.first > 1.5 * scBotB.first) {
                            aBL.setSubImg(subImg);
                            for (BaseLine cBL : bLs) {
                                if (cBL != aBL) {
                                    aBL.addObstacle(cBL.getbL());
                                }
                            }
                            aBL.calcShiftedBL();
                            aBL.setSCTop(scTop);
                            aBL.setSCBot(scBot);
                        }
                    }
                }
                aBL.buildSurPoly(5);
                Polygon2DInt surPoly = aBL.getSurPoly();
                res.add(surPoly);
            } else {
                res.add(null);
            }
        }

        //Plot It
        if (LOG.isTraceEnabled()) {
            LOG.log(Logger.TRACE, new StdFrameAppender.AppenderContent(aImg, "Input Img with sur", PolygonHelper.getIFPolygonList(res), false));
        }
        LOG.log(Logger.TRACE, new StdFrameAppender.AppenderContent(true));
        for (BaseLine aBL : bLs) {
            aBL.release();
        }
        scaled.clear();
        return res;
    }

}
