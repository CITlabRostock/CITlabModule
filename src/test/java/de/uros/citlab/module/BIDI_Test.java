/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module;

import com.achteck.misc.log.Logger;
import de.uros.citlab.module.types.PageStruct;
import de.uros.citlab.module.util.BidiUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import java.io.File;
import java.util.List;
import java.util.regex.Pattern;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author gundram
 */
public class BIDI_Test {

    static PageStruct page = null;
    public static Logger LOG = Logger.getLogger(BIDI_Test.class.getName());

    public BIDI_Test() {
    }

    @Before
    public void setUp() {
        page = new PageStruct(new File(TestFiles.getPrefix(), "test_bidi/ms50-128r-large.jpg"));
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testReadText() {
        PcGtsType xml = page.getXml();
        List<String> text = PageXmlUtil.getText(xml);
        int[] directions = new int[]{BidiUtil.FORCE_LTR, BidiUtil.FORCE_RTL, BidiUtil.SOFT_LTR, BidiUtil.SOFT_RTL};
        for (String ref : text) {
            String visual_ltr = null;
            String visual_rtl = null;
            for (int direction : directions) {

//            String visualRef = BidiUtil.logical2visual(ref);
                String visualRef = BidiUtil.logical2visual(ref, direction);
                if (direction == BidiUtil.FORCE_LTR) {
                    visual_ltr = visualRef;
                }
                if (direction == BidiUtil.FORCE_RTL) {
                    visual_rtl = visualRef;
                }
                String newRef = BidiUtil.visual2logical(visualRef, direction);
                Assert.assertEquals("not the same reference after double transform", ref, newRef);
                if (LOG.isDebugEnabled()) {
                    switch (direction) {
                        case BidiUtil.FORCE_LTR:
                            LOG.log(Logger.DEBUG, "FORCE_LTR");
                            break;
                        case BidiUtil.FORCE_RTL:
                            LOG.log(Logger.DEBUG, "FORCE_RTL");
                            break;
                        case BidiUtil.SOFT_LTR:
                            LOG.log(Logger.DEBUG, "SOFT_LTR");
                            break;
                        case BidiUtil.SOFT_RTL:
                            LOG.log(Logger.DEBUG, "SOFT_RTL");
                            break;
                        default:
                            throw new RuntimeException("unexpected direction " + direction);
                    }
                    LOG.log(Logger.DEBUG, "logical:    " + ref);
                    LOG.log(Logger.DEBUG, "visual:     " + visualRef);
                    LOG.log(Logger.DEBUG, "reconsturct:" + newRef);
                    LOG.log(Logger.DEBUG, "");
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.log(Logger.DEBUG, visual_ltr);
                LOG.log(Logger.DEBUG, visual_rtl);
                LOG.log(Logger.DEBUG, "");
            }
            boolean equals = visual_ltr.equals(visual_rtl);
//            Assert.assertFalse("forced LTR and forced RTL should differ ('" + visual_ltr + "')", equals);
            LOG.log(Logger.ERROR, "forced LTR and forced RTL should differ ('" + visual_ltr + "')");

        }
    }

}
