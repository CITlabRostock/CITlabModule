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
import de.uros.citlab.module.interfaces.IB2P;
import de.uros.citlab.module.la.B2PSimple;
import de.uros.citlab.module.util.ImageUtil;
import de.uros.citlab.module.util.MetadataUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.util.PolygonUtil;
import eu.transkribus.core.model.beans.pagecontent.BaselineType;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.interfaces.IBaseline2Polygon;
import eu.transkribus.interfaces.types.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
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

    private List<Polygon2DInt> tryApplyB2P(IB2P implModule, HybridImage hi, List<Polygon2DInt> linesPolygon, Double angle) {
        try {
            return implModule.process(hi, linesPolygon, angle);
        } catch (RuntimeException ex) {
            LOG.warn("Baseline2Polygon method {} throws exception - return null-result", implModule.getClass().getCanonicalName(), ex);
            return null;
        }
    }

    private Polygon2DInt tryGetPolygonWorstCase(Polygon2DInt baseline) {
        LOG.error("baseline {} used directly to generate polygon (grow-x=5, grow-y=20)", baseline);
        Rectangle2DInt bounds = baseline.getBounds();
        bounds.grow(5, 20);
        return bounds.getPolygon();
    }

    private boolean isValid(Polygon2DInt polygon) {
        return polygon != null && polygon.npoints > 3;
    }


    public void process(Image image, PcGtsType loadXml, String[] ids, String[] props) {
        HybridImage hi = ImageUtil.getHybridImage(image, true);
        List<TextRegionType> textRegions = PageXmlUtils.getTextRegions(loadXml);
        int cntlinesExecutions = 0;
        HashMap<Double, List<TextLineType>> mapTextLines = new HashMap<>();
        //collect baselines with same angle
        for (TextRegionType textRegion : textRegions) {
            List<TextLineType> textLines = new LinkedList<>();
            List<TextLineType> textLinesOfRegion = textRegion.getTextLine();
            for (TextLineType textLineType : textLinesOfRegion) {
                if (ids == null || ArrayUtil.linearSearch(ids, textLineType.getId()) >= 0) {
                    BaselineType bl = textLineType.getBaseline();
                    if (bl == null) {
                        LOG.warn("for textline '{}' no baseline polygon given", textLineType.getId());
                        continue;
                    }
                    textLines.add(textLineType);
                }
            }
            Double angle = textRegion.getOrientation() != null ? textRegion.getOrientation().doubleValue() : Double.NaN;
            List<TextLineType> textLinesToFill = mapTextLines.get(angle);
            if (textLinesToFill == null) {
                textLinesToFill = new LinkedList<>();
                mapTextLines.put(angle, textLinesToFill);
            }
            textLinesToFill.addAll(textLines);
        }
        //apply b2p on all baselines with same angle
        for (Double angle : mapTextLines.keySet()) {
            List<TextLineType> textLines = mapTextLines.get(angle);
            List<Polygon2DInt> baselines = new LinkedList<>();
            for (TextLineType textLine : textLines) {
                baselines.add(PolygonUtil.convert(PolygonUtil.getBaseline(textLine)));
            }
            if (Double.isNaN(angle)) {
                angle = 0.0;
            }
            if (textLines.isEmpty() && ids != null) {
                continue;
            }
            cntlinesExecutions += textLines.size();
            List<Polygon2DInt> process = tryApplyB2P(implModule, hi, baselines, angle);
            List<Polygon2DInt> processFallback = null;
            boolean fallbackCalculated = false;
            for (int j = 0; j < textLines.size(); j++) {
                TextLineType textLine = textLines.get(j);
                Polygon2DInt result = process != null ? PolygonUtil.reducePoints(process.get(j)) : null;
                if (!isValid(result)) {
                    if (!fallbackCalculated) {
                        processFallback = tryApplyB2P(implModuleFallback, hi, baselines, angle);
                        fallbackCalculated = true;
                    }
                    result = processFallback != null ? PolygonUtil.reducePoints(processFallback.get(j)) : null;
                    if (!isValid(result)) {
                        result = tryGetPolygonWorstCase(baselines.get(j));
                    }
                }
                if (result != null) {
                    textLine.setCoords(PolygonUtil.polyon2Coords(result));
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
        if (!image.hasType(Image.Type.OPEN_CV) && hi.getOpenCVMatImage() != null) {
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
