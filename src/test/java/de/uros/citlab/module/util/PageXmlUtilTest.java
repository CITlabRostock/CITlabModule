/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import de.uros.citlab.module.TestFiles;
import de.uros.citlab.module.types.PageStruct;
import eu.transkribus.core.model.beans.customtags.CustomTag;
import eu.transkribus.core.model.beans.customtags.CustomTagUtil;
import eu.transkribus.core.model.beans.customtags.ReadingOrderTag;
import eu.transkribus.core.model.beans.customtags.TextStyleTag;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import java.io.File;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gundram
 */
public class PageXmlUtilTest {

    public PageXmlUtilTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of deleteCustomTags method, of class PageXmlUtil.
     */
    @Test
    public void testCleanUpCustomTags() {
        System.out.println("cleanUpCustomTags");
        List<File> testFiles = TestFiles.getTestFiles();
        boolean checked = false;
        for (File testFile : testFiles) {
            File xmlPath = PageXmlUtil.getXmlPath(testFile);
            PcGtsType unmarshal = PageXmlUtil.unmarshal(xmlPath);
            List<TextLineType> textLines = PageXmlUtil.getTextLines(unmarshal);
            for (TextLineType tlt : textLines) {
                String customBefore = tlt.getCustom();
                if (customBefore != null && !customBefore.isEmpty()) {
                    List<CustomTag> listBefore = CustomTagUtil.getCustomTags(customBefore);
                    PageXmlUtil.deleteCustomTags(tlt, ReadingOrderTag.TAG_NAME);
                    String customAfter = tlt.getCustom();
                    List<CustomTag> listAfter = CustomTagUtil.getCustomTags(customAfter);
                    assertEquals("should only contain one Tag", 1, listAfter.size());
                    assertTrue("cleanUpCustomTags should keep tagName '" + ReadingOrderTag.TAG_NAME + "'", customBefore.contains(ReadingOrderTag.TAG_NAME));
                    checked = true;
                }
            }
        }
        assertTrue("Testcase should be found in documents " + testFiles + ".", checked);
    }

}
