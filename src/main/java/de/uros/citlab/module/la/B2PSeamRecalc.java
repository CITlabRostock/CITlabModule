////////////////////////////////////////////////
/// File:       LineGenerationSeam.java
/// Created:    29.01.2016  14:35:46
/// Encoding:   UTF-8
////////////////////////////////////////////////
package de.uros.citlab.module.la;

import com.achteck.misc.log.Logger;
import com.achteck.misc.types.Pair;
import de.planet.imaging.algo.ChainCode;
import de.planet.imaging.algo.KamelZhaoLLT;
import de.planet.imaging.types.ByteImage;
import de.planet.imaging.types.HybridImage;
import de.planet.imaging.types.StdFrameAppender;
import de.planet.imaging.util.ByteImageHelper;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.geom2d.types.Rectangle2DInt;
import de.planet.math.types.IFPolygon;
import de.planet.math.util.PolygonHelper;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Desciption of LineGenerationSeam
 *
 *
 * Since 29.01.2016
 *
 * @author Tobi <tobias.gruening@uni-rostock.de>
 */
public class B2PSeamRecalc extends B2PSeam {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(B2PSeamRecalc.class.getName());

    @Override
    public List<Polygon2DInt> process(HybridImage image, List<Polygon2DInt> polygons) {

//        PolygonHelper.translate(listNewPoly, 0, -15);
        short[][] sobelImg = getSobelImg(image);
        ArrayList<int[]> medSeamPolys = getMedSeams(polygons, image.getWidth());

        ArrayList<Polygon2DInt> sepSeams = calcSeparatingSeams(sobelImg, medSeamPolys, 0.0, dpRatio);

        ArrayList<Pair<Polygon2DInt, Polygon2DInt>> seamPairs = getSeamPairs(sepSeams);

        return binPostProcessLines(image, seamPairs, medSeamPolys);
    }

    private ArrayList<Polygon2DInt> binPostProcessLines(HybridImage inImg, ArrayList<Pair<Polygon2DInt, Polygon2DInt>> sepSeamPolys, ArrayList<int[]> medSeamPolys) {

        ByteImage aByte = inImg.getAsByteImage();

        KamelZhaoLLT kz = new KamelZhaoLLT();
        kz.t = 50;
        ByteImage binImg = kz.binarise(aByte);
        ByteImageHelper.invert(binImg);
        ChainCode.prepareImage(binImg);
        ArrayList<ChainCode> cc = ChainCode.getChainCode(binImg);

        if (LOG.isTraceEnabled()) {
            ArrayList<IFPolygon> pPoly = new ArrayList<IFPolygon>();
            for (ChainCode aCC : cc) {
                pPoly.addAll(PolygonHelper.getIFPolygonList(aCC.getPolygon()));
            }
            LOG.log(Logger.TRACE, new StdFrameAppender.AppenderContent(binImg, "Bin + CCs", pPoly));
        }

        LinkedList<ContainerCC> contCC = new LinkedList<ContainerCC>();
        for (ChainCode aCC : cc) {
            int areaCC = aCC.getArea();
            Polygon2DInt polyCC = aCC.getPolygon();
            Rectangle2DInt bbCC = aCC.getBounds();
            if (areaCC > 16 && bbCC.h > 4) {
                contCC.add(new ContainerCC(polyCC, bbCC, areaCC));
            }
        }

        if (LOG.isTraceEnabled()) {
            ArrayList<IFPolygon> pPoly = new ArrayList<IFPolygon>();
            for (ContainerCC aCC : contCC) {
                pPoly.addAll(PolygonHelper.getIFPolygonList(aCC.polyCC));
            }
            LOG.log(Logger.TRACE, new StdFrameAppender.AppenderContent(binImg, "Bin + Cont - CCs", pPoly));
        }

        ArrayList<Polygon2DInt> seamPolyList = new ArrayList<Polygon2DInt>();
        for (Pair<Polygon2DInt, Polygon2DInt> aSepSeamPoly : sepSeamPolys) {
            seamPolyList.add(getLinePoly(aSepSeamPoly));
        }

        LinkedList<ContainerCC> freeContCC = new LinkedList<ContainerCC>();
        for (ContainerCC aContCC : contCC) {
            double ratio = aContCC.bbCC.w / Math.max(aContCC.bbCC.h, 1);
            int area = aContCC.area;
            if (ratio > 2.0) {
                continue;
            }
            if (isFree(aContCC, seamPolyList)) {
                freeContCC.add(aContCC);
            }
        }

        if (LOG.isTraceEnabled()) {
            ArrayList<IFPolygon> pPoly = new ArrayList<IFPolygon>();
            for (ContainerCC aCC : freeContCC) {
                pPoly.addAll(PolygonHelper.getIFPolygonList(aCC.polyCC));
            }
            LOG.log(Logger.TRACE, new StdFrameAppender.AppenderContent(binImg, "Bin + FREE - CCs", pPoly));
        }

        int[] avgH = new int[seamPolyList.size()];

        for (int i = 0; i < sepSeamPolys.size(); i++) {
            Pair<Polygon2DInt, Polygon2DInt> aSepSeamPoly = sepSeamPolys.get(i);
            if (aSepSeamPoly == null) {
                avgH[i] = -1;
                continue;
            }
            int sumS = 0;

            Polygon2DInt topPoly = aSepSeamPoly.first;
            Polygon2DInt botPoly = aSepSeamPoly.second;

            for (int j = 0; j < topPoly.npoints; j++) {
                sumS += botPoly.ypoints[j] - topPoly.ypoints[j];
            }

            avgH[i] = sumS / topPoly.npoints;
        }

        for (ContainerCC aFreeContCC : freeContCC) {
            int aIdxMAX = -1;
            int aDistMAX = Integer.MAX_VALUE;
            int aIdxMIN = -1;
            int aDistMIN = Integer.MAX_VALUE;

            int xPos = aFreeContCC.bbCC.x + aFreeContCC.bbCC.w / 2;
            int yPos = aFreeContCC.bbCC.y + aFreeContCC.bbCC.h / 2;
            for (int i = 0; i < medSeamPolys.size(); i++) {
                if (avgH[i] < 0) {
                    continue;
                }
                int[] aMedSeam = medSeamPolys.get(i);
                if (yPos < aMedSeam[xPos]) {
                    int dist = aMedSeam[xPos] - yPos;
                    if (dist < aDistMAX) {
                        aDistMAX = dist;
                        aIdxMAX = i;
                    }
                }
                if (yPos >= aMedSeam[xPos]) {
                    int dist = yPos - aMedSeam[xPos];
                    if (dist < aDistMIN) {
                        aDistMIN = dist;
                        aIdxMIN = i;
                    }
                }
            }

            if (aIdxMAX >= 0 && (aDistMAX < 3 * aDistMIN || aDistMIN == -1)) {
                if (aIdxMAX >= 0 && aDistMAX < avgH[aIdxMAX] && Math.max(aFreeContCC.bbCC.w, aFreeContCC.bbCC.h) < avgH[aIdxMAX] / 2.0) {
                    addItTop(aFreeContCC, sepSeamPolys.get(aIdxMAX).first);
                }
            } else if (aIdxMIN >= 0 && aDistMIN < avgH[aIdxMIN] / 2 && Math.max(aFreeContCC.bbCC.w, aFreeContCC.bbCC.h) < avgH[aIdxMIN] / 2.0) {
                addItBot(aFreeContCC, sepSeamPolys.get(aIdxMIN).second);
            }

        }

        ArrayList<Polygon2DInt> seamPolyListFin = new ArrayList<Polygon2DInt>();
        for (Pair<Polygon2DInt, Polygon2DInt> aSepSeamPoly : sepSeamPolys) {
            seamPolyListFin.add(getLinePoly(aSepSeamPoly));
        }

        return seamPolyListFin;
    }

    private class ContainerCC {

        private Polygon2DInt polyCC;
        private Rectangle2DInt bbCC;
        private int area;
        private Polygon2DInt polyConvHull = null;

        public ContainerCC(Polygon2DInt polyCC, Rectangle2DInt bbCC, int area) {
            this.polyCC = polyCC;
            this.bbCC = bbCC;
            this.area = area;
        }

        private Polygon2DInt getConvHull() {
            if (polyConvHull == null) {
                polyConvHull = PolygonHelper.getConvexHull(polyCC);
            }
            return polyConvHull;
        }

    }

    private boolean isFree(ContainerCC aContCC, ArrayList<Polygon2DInt> seamPolys) {
        if (aContCC.bbCC.x <= 3 || aContCC.bbCC.y <= 3) {
            return false;
        }

        boolean free = true;
        Polygon2DInt convHull = aContCC.getConvHull();

        for (int i = 0; i < convHull.npoints; i++) {
            for (Polygon2DInt aSeamP : seamPolys) {
                if (aSeamP != null) {
                    if (de.planet.roi_core.util.PolygonUtil.containsPoint(aSeamP, convHull.xpoints[i], convHull.ypoints[i])) {
                        free = false;
                        break;
                    }
                }
            }
            if (!free) {
                break;
            }
        }
        return free;
    }

    private ArrayList<Pair<Polygon2DInt, Polygon2DInt>> getSeamPairs(ArrayList<Polygon2DInt> sepSeams) {
        ArrayList<Pair<Polygon2DInt, Polygon2DInt>> res = new ArrayList<Pair<Polygon2DInt, Polygon2DInt>>();
        for (int i = 0; i < sepSeams.size() - 1; i += 2) {
            Polygon2DInt topPoly = sepSeams.get(i);
            Polygon2DInt botPoly = sepSeams.get(i + 1);
            if (topPoly == null || botPoly == null) {
                res.add(null);
            } else {
                res.add(new Pair<Polygon2DInt, Polygon2DInt>(topPoly, botPoly));
            }
        }
        return res;
    }

    protected Polygon2DInt getLinePoly(Pair<Polygon2DInt, Polygon2DInt> sepSeams) {
        if (sepSeams == null) {
            return null;
        }
        Polygon2DInt res = new Polygon2DInt();
        PolygonHelper.addPoints(res, sepSeams.first);
        Polygon2DInt botSeam = sepSeams.second;
        for (int i = botSeam.npoints - 1; i >= 0; i--) {
            res.addPoint(botSeam.xpoints[i], botSeam.ypoints[i]);
        }
        return res;
    }

    private void addItTop(ContainerCC aContCC, Polygon2DInt topSeam) {

        int bL = aContCC.bbCC.x;
        int bR = aContCC.bbCC.x + aContCC.bbCC.w;

        int yVal = Math.max(aContCC.bbCC.y, topSeam.getBounds().y);

        for (int i = 0; i < topSeam.npoints; i++) {
            int aX = topSeam.xpoints[i];

            if (aX >= bL && aX <= bR) {
                topSeam.ypoints[i] = Math.min(topSeam.ypoints[i], yVal);
            }
        }

    }

    private void addItBot(ContainerCC aContCC, Polygon2DInt botSeam) {

        int bL = aContCC.bbCC.x;
        int bR = aContCC.bbCC.x + aContCC.bbCC.w;

        int yVal = Math.min(aContCC.bbCC.y + aContCC.bbCC.h, botSeam.getBounds().y + botSeam.getBounds().h);

        for (int i = 0; i < botSeam.npoints; i++) {
            int aX = botSeam.xpoints[i];

            if (aX >= bL && aX <= bR) {
                botSeam.ypoints[i] = Math.max(botSeam.ypoints[i], yVal);
            }
        }

    }

}
