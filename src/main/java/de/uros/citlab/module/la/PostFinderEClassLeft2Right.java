/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.la;

import com.achteck.misc.geom.Rotation;
import com.achteck.misc.log.Logger;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.planet.imaging.types.HybridImage;
import de.planet.imaging.types.StdFrameAppender;
import de.planet.math.geom2d.types.Point2DDouble;
import de.planet.math.geom2d.types.Point2DInt;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.geom2d.types.Rectangle2DInt;
import de.planet.math.trafo.ITransform;
import de.planet.math.trafo.TrafoUtil;
import de.planet.math.trafo.TransformRotate;
import de.planet.math.util.Eclass;
import de.planet.math.util.PolygonHelper;
import de.planet.roi_core.interfaces.ILine;
import de.planet.roi_core.interfaces.IPostFinder;
import de.planet.roi_core.interfaces.IRegion;
import de.uros.citlab.module.util.PolygonUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * using the eclass-algorithm to cluster polygons. This algorithm can handle
 * angles of regions. Of each polygon the bounding box (BB) is calculated. Two
 * BBs are equivalent (for the eclass-algorithm), if they intersect in
 * x-direction. Parameter: grow: Two BBs are equivalent, if the distance in
 * x-direction is lower than grow That means: The higher grow is, the less
 * cluster are build. Grow can be negative. angle: Instead of having a straight
 * projection to x, one can allow an overlap between the both most narrow
 * verticies by a specific angle. For angles >0 there are more clusters.
 *
 * @author gundram
 */
public class PostFinderEClassLeft2Right extends ParamTreeOrganizer implements IPostFinder {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(PostFinderEClassLeft2Right.class.getName());

    @ParamAnnotation(descr = "grow polygons in x-dimension before checking if they overlap in x")
    private double grow = 0;

    @ParamAnnotation(descr = "maximal angle of overlapping (higer = seperate more)")
    private double angle = 0.1;

    public PostFinderEClassLeft2Right() {
        this(0, 0.1);
    }

    public PostFinderEClassLeft2Right(double grow, double angle) {
        this.grow = grow;
        this.angle = angle;
        addReflection(this, PostFinderEClassLeft2Right.class);
    }

    @Override
    public IRegion process(HybridImage hi, IRegion ir) {
        ArrayList<ILine> linesOld = ir.getLines();
        double angleReverse = -ir.getAngle();
//<editor-fold defaultstate="collapsed" desc="logging">
        if (LOG.isTraceEnabled()) {
            ArrayList<Polygon2DInt> linesOld2 = ir.getLineBounds();
            ArrayList<Polygon2DInt> polys = new ArrayList<>();
            for (int i = 0; i < linesOld2.size(); i++) {
                Polygon2DInt get = PolygonHelper.copy(linesOld2.get(i));
                polys.add(get);
                LOG.log(Logger.TRACE, new StdFrameAppender.AppenderContent(hi.copy(), i + "-th line before Postfinder resort", PolygonHelper.getIFPolygonList(get)));
            }
            LOG.log(Logger.TRACE, new StdFrameAppender.AppenderContent(hi.copy(), "all line before Postfinder resort", PolygonHelper.getIFPolygonList(polys)));
            LOG.log(Logger.TRACE, "has angle " + -angleReverse);
        }

//</editor-fold>
        //THE ILines of IRegions have to be sorted
        ArrayList<ILine> linesNew = new ArrayList<>(linesOld.size());
        ArrayList<Polygon2DInt> polygonsRotated = new ArrayList<>(linesOld.size());
        HashMap<Polygon2DInt, ILine> sortMap = new LinkedHashMap<>();
        for (ILine lineOld : linesOld) {
            Polygon2DInt polygonRotated = PolygonUtil.rotate(lineOld.getBound(), angleReverse);
            polygonsRotated.add(polygonRotated);
            sortMap.put(polygonRotated, lineOld);
        }
        List<List<Polygon2DInt>> clusterLeft2Right = PolygonUtil.clusterLeft2Right(polygonsRotated, angle, grow);
        if (LOG.isDebugEnabled() && clusterLeft2Right.size() > 1) {
            LOG.log(Logger.DEBUG, "found " + clusterLeft2Right.size() + " left-to-right-cluster in post process");
        }
        //sort clusters by the first polygon of each list - this should give the right Left2Right-Order
        Collections.sort(clusterLeft2Right, new Comparator<List<Polygon2DInt>>() {
            @Override
            public int compare(List<Polygon2DInt> o1, List<Polygon2DInt> o2) {
                return Double.compare(o1.get(0).getBounds().getCenterX(), o2.get(0).getBounds().getCenterX());
            }
        });
        //sort lines by Top2Bottom-Order
        for (List<Polygon2DInt> list : clusterLeft2Right) {
            Collections.sort(list, new Comparator<Polygon2DInt>() {
                @Override
                public int compare(Polygon2DInt o1, Polygon2DInt o2) {
                    return Double.compare(o1.getBounds().getCenterY(), o2.getBounds().getCenterY());
                }
            });
            for (Polygon2DInt polygon2DInt : list) {
                ILine nextLine = sortMap.get(polygon2DInt);
                linesNew.add(nextLine);
            }
        }
        linesOld.clear();
        linesOld.addAll(linesNew);
        if (LOG.isTraceEnabled()) {
            for (int i = 0; i < linesNew.size(); i++) {
                ILine get = linesNew.get(i);
                LOG.log(Logger.TRACE, new StdFrameAppender.AppenderContent(hi, i + "-th line after Postfinder resort", PolygonHelper.getIFPolygonList(PolygonHelper.copy(get.getBound()))));
            }
        }
        return ir;
    }

}
