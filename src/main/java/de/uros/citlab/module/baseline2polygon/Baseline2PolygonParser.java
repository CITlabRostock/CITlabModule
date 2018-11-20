/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.baseline2polygon;

import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamSetHandlerAbstract;
import com.achteck.misc.util.ArrayUtil;
import de.planet.imaging.types.HybridImage;
import de.planet.math.geom2d.types.Point2DInt;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.geom2d.types.Rectangle2DInt;
import de.uros.citlab.module.util.PolygonUtil;
import eu.transkribus.core.model.beans.pagecontent.BaselineType;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.IBaseline2Polygon;
import eu.transkribus.interfaces.types.Image;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import de.uros.citlab.module.interfaces.IB2P;
import de.uros.citlab.module.la.B2PSimple;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.MetadataUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gundram
 */
public class Baseline2PolygonParser implements IBaseline2Polygon {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(Baseline2PolygonParser.class.getName());

    private String impl = "";
    private IB2P implModule;
    private IB2P implModuleFallback;

    public Baseline2PolygonParser(String classname) {
        impl = classname;
        implModule = (IB2P) ParamSetHandlerAbstract.loadIParamSetHandler(classname);
        implModule.setParamSet(implModule.getDefaultParamSet(new ParamSet()));
        implModule.init();
        implModuleFallback = new B2PSimple();
        implModuleFallback.setParamSet(implModuleFallback.getDefaultParamSet(new ParamSet()));
        implModuleFallback.init();
    }

    @Override
    public void process(Image image, String xmlInOut, String[] ids, String[] props) {
        PcGtsType loadXml = PageXmlUtil.unmarshal(new File(xmlInOut));
        process(image, loadXml, ids, props);
        PageXmlUtil.marshal(loadXml, new File(xmlInOut));
    }

    public void process(Image image, PcGtsType loadXml, String[] ids, String[] props) {
        HybridImage hi = ImageUtil.getHybridImage(image, true);
        List<TextRegionType> textRegions = PageXmlUtils.getTextRegions(loadXml);
        int cntlinesExecutions = 0;
        for (TextRegionType textRegion : textRegions) {
            List<TextLineType> linesExecution = new LinkedList<>();
            List<Polygon2DInt> linesPolygon = new LinkedList<>();
            List<TextLineType> linesInRegion1 = textRegion.getTextLine();
            for (TextLineType textLineType : linesInRegion1) {
                if (ids == null || ArrayUtil.linearSearch(ids, textLineType.getId()) >= 0) {
                    BaselineType bl = textLineType.getBaseline();
                    if (bl == null) {
                        LOG.warn("for textline '{}' no baseline polygon given", textLineType.getId());
                        continue;
                    }
                    linesExecution.add(textLineType);
                    linesPolygon.add(PolygonUtil.convert(PolygonUtil.getBaseline(textLineType)));
                }
            }
            if (linesExecution.isEmpty() && ids != null) {
                continue;
            }
            cntlinesExecutions += linesExecution.size();
            Double angle = textRegion.getOrientation() != null ? textRegion.getOrientation().doubleValue() : null;
            List<Polygon2DInt> process = implModule.process(hi, linesPolygon, angle);
            List<Polygon2DInt> processFallback = null;
            for (int j = 0; j < linesExecution.size(); j++) {
                TextLineType textLine = linesExecution.get(j);
                Polygon2DInt p2d = PolygonUtil.reducePoints(process.get(j));
                if (p2d != null) {
                    if (p2d.npoints < 3) {
                        LOG.warn("coord polygon has coords {} - use fallback polygon calculation");
                        if (processFallback == null) {
                            processFallback = implModuleFallback.process(hi, process, angle);
                        }
                        Polygon2DInt get = processFallback.get(j);
                        Polygon2DInt p2dFallback = PolygonUtil.reducePoints(processFallback.get(j));
                        if (p2dFallback.npoints < 3) {
                            Rectangle2DInt bounds = linesPolygon.get(j).getBounds();
                            bounds.grow(5, 20);
                            p2d = bounds.getPolygon();
                            LOG.error("coord of line {}: polygon has coords {} and fallback {} - both are incorrect.", linesExecution.get(j).getId(), p2d, p2dFallback);
                            textLine.setCoords(PolygonUtil.polyon2Coords(p2d));
                        }
                        textLine.setCoords(PolygonUtil.polyon2Coords(p2dFallback));
                    }
                    textLine.setCoords(PolygonUtil.polyon2Coords(p2d));
                } else {
                    LOG.error("cannot calculate Polygon for Line-ID {} - no polygon is set.", textLine.getId());
                }
            }
        }
        if (ids != null && cntlinesExecutions != ids.length) {
            LOG.warn("can only find {} of {} given lines (ids were {})", cntlinesExecutions, ids.length, Arrays.toString(ids));
            if (cntlinesExecutions == 0) {
                throw new RuntimeException("regions with ids " + Arrays.deepToString(ids) + " not found.");
            }
        }

        MetadataUtil.addMetadata(loadXml, this);
        if(!image.hasType(Image.Type.OPEN_CV) && hi.getOpenCVMatImage()!=null){
            hi.getOpenCVMatImage().release();
        }
    }

    private static Point2DInt substract(Point2DInt first, Point2DInt second) {
        return new Point2DInt(first.x - second.x, first.y - second.y);
    }

    @Override
    public String usage() {
        return "adds/overwrites coords for given baselines. If baseline is not given, no coords is calculated";
    }

    @Override
    public String getToolName() {
        return impl;
    }

    @Override
    public String getVersion() {
        return "1.0.1";
    }

    @Override
    public String getProvider() {
        return MetadataUtil.getProvider("Tobias Gruening", "tobias.gruening@uni-rostock.de");
    }

}
