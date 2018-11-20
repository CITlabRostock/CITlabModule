/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.la;

import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamSetOrganizer;
import com.achteck.misc.util.ArrayUtil;
import com.achteck.misc.util.Resources;
import de.planet.imaging.types.HybridImage;
import de.planet.itrtech.roifinder.IROIFinder;
import de.planet.itrtech.textfinder.ITextFinder;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.trafo.ITransform;
import de.planet.math.util.PolygonHelper;
import de.uros.citlab.module.interfaces.IB2P;
import de.uros.citlab.module.interfaces.IP2B;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.*;
import eu.transkribus.core.model.beans.pagecontent.PageType;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author gundram
 */
public class LayoutAnalysisParser /*extends ParamTreeOrganizer*/ implements ILayoutAnalysis, ITextFinder, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(LayoutAnalysisParser.class.getName());
    private transient ITextFinder impl;
    private transient IP2B p2b;
    private transient IB2P b2p = new B2PSimple();
    private final String name;
    private final String version = "0.1";
    ParamSet ps;
    private String[] props;

    static {
        Resources.registerResourcePath("/planet/");
    }

    public LayoutAnalysisParser(String path, String[] props) {
        impl = (ITextFinder) ParamSetOrganizer.loadIParamSetHandler(path);
        ParamSet defaultParamSet = impl.getDefaultParamSet(new ParamSet());
        impl.setParamSet(defaultParamSet);
        impl.init();
        ps = defaultParamSet;
        p2b = new BaselineGenerationHist();
        name = path + File.pathSeparator + p2b.getClass().getName();
        this.props = props;
    }

    protected static Polygon2DInt c(Polygon p) {
        return new Polygon2DInt(p.xpoints, p.ypoints, p.npoints);
    }

    protected static Polygon c(Polygon2DInt p) {
        return new Polygon(p.xpoints, p.ypoints, p.npoints);
    }

    public void processLayout(Image image, String xmlInOut) {
        process(image, xmlInOut, null, null);
    }

    @Override
    public void process(Image image, String xmlInOut, String[] ids, String[] props) {
        PcGtsType loadXml = null;
        String[] propsProcess = PropertyUtil.merge(props, this.props);
        loadXml = PageXmlUtil.unmarshal(new File(xmlInOut));
        process(image, loadXml, ids, propsProcess);
        PageXmlUtil.marshal(loadXml, new File(xmlInOut));
    }

    private synchronized TrpTextLineType getTextLine(Polygon2DInt polygon) {
        TrpTextLineType lt = new TrpTextLineType();
        lt.setCoords(PolygonUtil.polyon2Coords(PolygonUtil.reducePoints(polygon)));
        return lt;
    }

    private void createBaselineAndPolygon(HybridImage hi, TextRegionType trt) {
        p2b.processRegion(hi, trt);
        double angle = trt.getOrientation() != null ? trt.getOrientation().doubleValue() : 0.0;

        List<Polygon2DInt> polysIn = new LinkedList<>();
        for (TextLineType tlt : trt.getTextLine()) {
            Polygon2DInt bl = PolygonUtil.convert(PolygonUtil.getBaseline(tlt));
            polysIn.add(bl);
        }
        List<Polygon2DInt> polysOut = b2p.process(hi, polysIn, angle);
        for (int i = 0; i < polysOut.size(); i++) {
            Polygon2DInt p2d = polysOut.get(i);
            TextLineType textLine = trt.getTextLine().get(i);
            if (p2d != null) {
                PolygonUtil.setCoords(trt.getTextLine().get(i), PolygonUtil.convert(PolygonUtil.reducePoints(p2d)));
//                PolygonUtil.setCoords(trt.getTextLine().get(i), PolygonUtil.convert(polysOut.get(i)));
            } else {
                LOG.error("cannot calculate Polygon for Line-ID {} - no polygon is set.", textLine.getId());
            }
        }
    }

    public boolean process(Image image, PcGtsType xmlFile, String[] ids, String[] props) {
        PageType page = xmlFile.getPage();
        List<TrpRegionType> regions = page.getTextRegionOrImageRegionOrLineDrawingRegion();
        HybridImage hi = ImageUtil.getHybridImage(image, true);
        String value = PropertyUtil.getProperty(props, Key.METHOD_LA, "all");
        switch (value) {
            case "all":
                if (PropertyUtil.isPropertyTrue(props, Key.DELETE)) {
                    if (!regions.isEmpty()) {
                        LOG.debug("delete {} text regions", regions.size());
                        regions.clear();
                    }
                }
                if (ids != null && ids.length > 0) {
                    throw new RuntimeException("ids given, but layout analysis have to be done.");
                }
                if (!regions.isEmpty()) {
                    throw new RuntimeException("regions found, but layout analysis have to be done. Maybe add property " + Key.DELETE + "=true.");
                }
                List<IROIFinderResult> findROIs = impl.findROIs(hi);
                if (findROIs == null || findROIs.isEmpty()) {
                    break;
                }
                int cntRegion = 0;
                for (IROIFinderResult ROI : findROIs) {
                    //Return value is the correction-angle, not the orientation.
                    double angle = -ROI.getAngle();
                    if (angle < 0) {
                        angle += Math.PI * 2;
                    }
                    List<Polygon2DInt> detailedPolygons = ROI.getDetailedPolygons();
                    List<TrpTextLineType> tlts = new LinkedList<>();
                    TrpTextRegionType rt = new TrpTextRegionType();
                    rt.setOrientation((float) angle);
                    rt.setCoords(PolygonUtil.polyon2Coords(ROI.getPolygon()));
                    rt.setParent(page);
                    //set reading order explicitely
                    rt.setReadingOrder(cntRegion, null);
                    rt.setId("r" + (++cntRegion));
                    int cntLine = 0;
                    for (Polygon2DInt detailedPolygon : detailedPolygons) {
                        TrpTextLineType textLine = getTextLine(detailedPolygon);
                        rt.getTextLine().add(textLine);
                        textLine.setParent(rt);
                        //set reading order explicitely
                        textLine.setReadingOrder(cntLine, null);
                        textLine.setId("r" + cntRegion + "l" + (++cntLine));
                        tlts.add(textLine);
                    }
                    regions.add(rt);
                    createBaselineAndPolygon(hi, rt);
                }
                break;
            case "line":
                List<TextRegionType> textRegions = PageXmlUtils.getTextRegions(xmlFile);
                for (TextRegionType textRegion : textRegions) {
                    String textRegionID = textRegion.getId();
                    int cntLine = 0;
//                    List<TrpTextLineType> tlts = new LinkedList<>();
                    if (PropertyUtil.isPropertyTrue(props, Key.DELETE)) {
                        List<TextLineType> textLine = textRegion.getTextLine();
                        textRegion.setTextEquiv(null);
                        if (!textLine.isEmpty()) {
                            LOG.debug("delete {} text lines", regions.size());
                            textLine.clear();
                        }
                    }
                    if (ids == null || ArrayUtil.linearSearch(ids, textRegion.getId()) >= 0) {
                        Polygon2DInt mask = PolygonUtil.string2Polygon2DInt(textRegion.getCoords().getPoints());
                        double orientation = textRegion.getOrientation() == null ? 0.0 : textRegion.getOrientation().doubleValue();
                        HybridImage imageRotated = hi;
                        Polygon2DInt maskRotated = mask;
                        ITransform trafoRotated = null;
                        if (orientation != 0.0) {
                            imageRotated = ImageUtil.rotate(hi, mask, -orientation, 0, 0, 0);
                            trafoRotated = imageRotated.getTrafo();
                            maskRotated = PolygonHelper.copy(mask);
                            PolygonHelper.transform(maskRotated, trafoRotated);
                        }
                        List<IROIFinderResult> findROIs1 = impl.findROIs(imageRotated, Arrays.asList((IROIFinder.IROIMask) new RoiMask(maskRotated)), null);
                        List<Polygon2DInt> result = new LinkedList<>();
                        if (findROIs1 != null) {
                            for (IROIFinderResult iROIFinderResult : findROIs1) {
                                if (iROIFinderResult.getDetailedPolygons() != null) {
                                    List<Polygon2DInt> pRotated = iROIFinderResult.getDetailedPolygons();
                                    for (Polygon2DInt polygon2DInt : pRotated) {
                                        if (trafoRotated != null) {
                                            PolygonHelper.invert(polygon2DInt, trafoRotated);
                                        }
                                        result.add(polygon2DInt);
                                    }
                                }
                            }
                        }
                        if (result.isEmpty()) {
                            LOG.warn("cannot find lines in region " + textRegion.getId() + " with polygon " + textRegion.getCoords().getPoints() + ".");
                        } else {
                            List<TextLineType> textLines = textRegion.getTextLine();
                            for (Polygon2DInt findLine : result) {
                                TrpTextLineType textLine = getTextLine(findLine);
                                textLine.setId(textRegionID + "l" + (++cntLine));
                                textLines.add(textLine);
                            }
//                            if (LOG.isDebugEnabled()) {
//                                result.add(PolygonUtil.string2Polygon2DInt(textRegion.getCoords().getPoints()));
//                                LOG.log(Logger.DEBUG, new StdFrameAppender.AppenderContent(hi, "Result: Region and Line Polygons", result));
//                            }
                            createBaselineAndPolygon(hi, textRegion);
                        }
                    }
                }
                break;
            default:
                throw new RuntimeException("cannot interprete method '" + value + "'");
        }
//        if (PropertyUtil.isPropertyFalse(props, "calcCoords")) {
//            CoordsType dummy = PolygonUtil.polyon2Coords(new Polygon(new int[]{0}, new int[]{0}, 1));
//            for (TrpTextLineType tlt : tlts) {
//                tlt.setCoords(dummy);
//            }
//        }
        MetadataUtil.addMetadata(xmlFile, this);
        return true;
    }

    private static class RoiMask implements IROIFinder.IROIMask {

        private final Polygon2DInt p;

        public RoiMask(Polygon2DInt p) {
            this.p = p;
        }

        @Override
        public String getLabel() {
            return "";
        }

        @Override
        public Polygon2DInt getPolygon() {
            return p;
        }

        @Override
        public byte[][] getMask() {
            return null;
        }

    }

    //<editor-fold defaultstate="collapsed" desc="deligated methods to ITextFinder impl">
    @Override
    public List<ILine> findLines(HybridImage hi, Polygon2DInt pdi, List<Polygon2DInt> list) {
        return impl.findLines(hi, pdi, list);
    }

    @Override
    public List<IROIFinderResult> findLinesROI(HybridImage hybridImage, Polygon2DInt polygon2DInt, List<Polygon2DInt> list) {
        throw new UnsupportedOperationException("Not implemented so far");
    }

    @Override
    public List<ILine> findLines(HybridImage hi, IROIFinderResult iroifr) {
        return impl.findLines(hi, iroifr);
    }

    @Override
    public List<IROIFinderResult> findROIs(HybridImage hi) {
        return impl.findROIs(hi);
    }

    @Override
    public List<IROIFinderResult> findROIs(HybridImage hi, List<IROIMask> list, IAdditionalInformation iai) {
        return impl.findROIs(hi, list, iai);
    }

    @Override
    public List<IROIFinderResult> findROIs(Map<String, HybridImage> map) {
        return impl.findROIs(map);
    }

    @Override
    public List<IROIFinderResult> findROIs(Map<String, HybridImage> map, Map<String, List<IROIMask>> map1, IAdditionalInformation iai) {
        return impl.findROIs(map, map1, iai);
    }

    @Override
    public ParamSet getDefaultParamSet(ParamSet ps) {
        return impl.getDefaultParamSet(ps);
    }

    @Override
    public void setParamPrefix(String prefix) {
        impl.setParamPrefix(prefix);
    }

    @Override
    public String getParamPrefix() {
        return impl.getParamPrefix();
    }

    @Override
    public void setParamSet(ParamSet ps) {
        impl.setParamSet(ps);
    }

    @Override
    public ParamSet getParamSet() {
        return impl.getParamSet();
    }

    @Override
    public void init() {
        impl.init();
    }

    @Override
    public boolean isTerminated() {
        return impl.isTerminated();
    }

    @Override
    public void setTerminate(boolean terminate) {
        impl.setTerminate(terminate);
    }
//</editor-fold>

    @Override
    public String usage() {
        return "the method has two modes which can be chosen using the properties.\n"
                + "The property '" + Key.METHOD_LA + "' can be 'line', 'all' or the property cannot exist.\n"
                + "If the method is 'line', the algorithm iterates through the regions and "
                + "tries to find lines in these regions, without checking, if the block can be seperated in substructure or columns.\n"
                + "In 'all' case or when the property is not set, there will be TextRegion finding and Line finding afterwards.\n"
                + "The algorithm calculates a (rough) baseline. An detailed sourrunding polygon would be possible but is not set, because of convention.\n"
                + "If ids==null, the algorithm will be applied to all regions, otherwise only to the given regions.\n"
                + "If the property '" + Key.DELETE + "' is 'true' previous existing regions (case 'all') or lines (case 'line') will be deleted.";
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
        return version;
    }
}
