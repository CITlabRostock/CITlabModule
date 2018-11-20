/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.types;

import com.achteck.misc.log.Logger;
import de.planet.imaging.types.HybridImage;
import de.planet.imaging.types.StdFrameAppender;
import de.planet.itrtech.types.ImagePropertyIDs;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.util.PolygonHelper;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.util.PolygonUtil;
import eu.transkribus.core.model.beans.customtags.ReadingOrderTag;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import java.awt.Polygon;
import java.util.Arrays;

/**
 *
 * @author gundram
 */
public class LineImage {

    public static Logger LOG = Logger.getLogger(LineImage.class.getName());
    private TextLineType tlt;
    private TextRegionType rt;
    private Polygon2DInt bl;
    private Polygon2DInt poly;
    private HybridImage pageImg;

    public LineImage(HybridImage pageImg, TextLineType tlt, TextRegionType rt) {
        this.pageImg = pageImg;
        this.tlt = tlt;
        this.rt = rt;
        bl = tlt.getBaseline() != null ? PolygonUtil.convert(PolygonUtil.getBaseline(tlt)) : null;
        poly = tlt.getCoords() != null ? PolygonUtil.convert(PolygonUtil.getPolygon(tlt)) : null;
    }

    public void deleteTextEquiv() {
        tlt.setTextEquiv(null);
        PageXmlUtil.deleteCustomTags(tlt, ReadingOrderTag.TAG_NAME);
    }

    public HybridImage getPageImg() {
        return pageImg;
    }

    public HybridImage getSubImage() {
        //try to find an angle either in region or calculate angle from baseline
        double orientation = 0;
        if (rt != null && rt.getOrientation() != null) {
            orientation = rt.getOrientation();
            //make sure that even values (0,90,180,270°) are right after float quantisation 
            if (Math.abs(orientation - Math.PI) < 0.02) {
                orientation = Math.PI;
            } else if (Math.abs(orientation - Math.PI / 2.0) < 0.02) {
                orientation = Math.PI / 2.0;
            } else if (Math.abs(orientation - 3.0 * Math.PI / 2.0) < 0.02) {
                orientation = 3.0 * Math.PI / 2.0;
            }
        } else if (bl != null) {
            Polygon2DInt blownUp = PolygonUtil.blowUp(bl);
            orientation = PolygonUtil.calcRegLineStats(blownUp)[0];
        }
        if (Math.abs(orientation) < 0.02) {
            orientation = 0.0;
        }
        HybridImage subImg = ImageUtil.rotate(pageImg, poly, -orientation, 0, 0, 0);
        Polygon2DInt polyCpy = PolygonHelper.copy(poly);
        PolygonHelper.transform(polyCpy, subImg.getTrafo());
        subImg.setProperty(ImagePropertyIDs.MASK.toString(), Arrays.asList(polyCpy));
        if (LOG.isTraceEnabled()) {
            LOG.log(Logger.TRACE, new StdFrameAppender.AppenderContent(subImg.copy(), "Subimage: from line id '" + this.tlt.getId() + "'", Arrays.asList(PolygonHelper.copy(polyCpy))));
        }
        return subImg;
    }

    public Polygon2DInt getBaseLine() {
        return bl;
    }

    public Polygon2DInt getPolygon() {
        return poly;
    }

    public TextLineType getTextLine() {
        return tlt;
    }

    public TextRegionType getTextRegion() {
        return rt;
    }

}
