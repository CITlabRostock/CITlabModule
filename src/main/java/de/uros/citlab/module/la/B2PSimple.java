////////////////////////////////////////////////
/// File:       LineGenerationSeam.java
/// Created:    29.01.2016  14:35:46
/// Encoding:   UTF-8
////////////////////////////////////////////////
package de.uros.citlab.module.la;

import com.achteck.misc.log.Logger;
import de.planet.imaging.types.HybridImage;
import de.planet.math.geom2d.types.Polygon2DInt;
import java.util.ArrayList;
import java.util.List;
import de.uros.citlab.module.interfaces.IB2P;

/**
 * Desciption of LineGenerationSeam
 *
 *
 * Since 29.01.2016
 *
 * @author Tobi <tobias.gruening@uni-rostock.de>
 */
public class B2PSimple extends B2PSeamRecalc implements IB2P {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(B2PSimple.class.getName());

    public B2PSimple() {
        addReflection(this, B2PSimple.class);
    }

    @Override
    public List<Polygon2DInt> process(HybridImage image, List<Polygon2DInt> polygons) {

        short[][] sobelImg = getSobelImg(image);
        ArrayList<int[]> medSeamPolys = getMedSeams(polygons, image.getWidth());

        ArrayList<Polygon2DInt> sepSeams = calcSeparatingSeams(sobelImg, medSeamPolys, 0.0, dpRatio);

        ArrayList<Polygon2DInt> linePolys = getLinePolys(sepSeams, polygons);
        return linePolys;
    }

    private double avgY(Polygon2DInt p) {
        long val = p.ypoints[0] + p.ypoints[p.npoints - 1];
        for (int i = 1; i < p.npoints - 1; i++) {
            val += p.ypoints[i] * 2;
        }
        return ((double) val) / (2 * p.npoints - 2);
    }

    private ArrayList<Polygon2DInt> getLinePolys(ArrayList<Polygon2DInt> sepSeams, List<Polygon2DInt> bls) {
        ArrayList<Polygon2DInt> res = new ArrayList<>();
        for (int i = 0; i < bls.size(); i++) {
            Polygon2DInt polyT = sepSeams.get(2 * i);
            Polygon2DInt polyB = sepSeams.get(2 * i + 1);
            if (polyT == null || polyB == null) {
                res.add(null);
                continue;
            }
            Polygon2DInt pBL = bls.get(i);
            double avgBL = avgY(pBL);
            int offsetT = (int) Math.round(avgY(polyT) - avgBL);
            int offsetB = (int) Math.round(avgY(polyB) - avgBL);
            Polygon2DInt p = new Polygon2DInt();
            int[] yBL = pBL.ypoints;
            int[] xBL = pBL.xpoints;
            int length = pBL.npoints;
            for (int j = 0; j < length; j++) {
                p.addPoint(xBL[j], yBL[j] + offsetT);
            }
            for (int j = length - 1; j >= 0; j--) {
                p.addPoint(xBL[j], yBL[j] + offsetB);
            }
            res.add(p);
        }
        return res;
    }

}
