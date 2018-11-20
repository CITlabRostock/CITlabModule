/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.kws;

import com.achteck.misc.log.Logger;
import com.achteck.misc.types.ConfMat;
import com.achteck.misc.util.StopWatch;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.geom2d.types.Rectangle2DInt;
import de.uros.citlab.module.TestFiles;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author gundram
 */
public class ConfMatContainerTest {

    public static Logger LOG = Logger.getLogger(ConfMatContainerTest.class.getName());
    File dirStorage = new File(TestFiles.getPrefix(), "test_container/");
    File dirStorageTmp = new File(TestFiles.getPrefix(), "test_container_tmp/");

    public ConfMatContainerTest() {
    }

    @Before
    public void setUp() {
        try {
            FileUtils.copyDirectory(dirStorage, dirStorageTmp);
        } catch (IOException ex) {
            throw new RuntimeException("cannot copy " + dirStorage + " to " + dirStorageTmp, ex);
        }
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(dirStorageTmp);
    }

    /**
     * Test of newInstance method, of class ConfMatContainer.
     */
    @Test
    public void testNewInstance() {
        System.out.println("newInstance");
        File image = new File(dirStorageTmp, "example.jpg");
        Assert.assertTrue("file is not available '" + image.getAbsolutePath() + "'", image.exists() && image.canRead());
        File folderStorage = new File(dirStorageTmp, "storage_old");
        StopWatch sw1 = new StopWatch();
        StopWatch sw2 = new StopWatch();
        sw1.start();
        ConfMatContainer container1 = ConfMatContainer.newInstance(folderStorage, image);
        sw1.stop();
        sw2.start();
        ConfMatContainer container2 = ConfMatContainer.newInstance(folderStorage);
        sw2.stop();
        LOG.log(Logger.INFO, "time: old: " + sw1.getCurrentMillis() + " new: " + sw2.getCurrentMillis());
        Assert.assertTrue("old loading is faster", sw1.getCurrentMillis() >= sw2.getCurrentMillis());
        Assert.assertEquals("not same number of files", container1.getConfmats().size(), container2.getConfmats().size());
        List<ConfMat> confmats1 = container1.getConfmats();
        List<ConfMat> confmats2 = container2.getConfmats();
        Collections.sort(confmats1, new Comparator<ConfMat>() {
            @Override
            public int compare(ConfMat o1, ConfMat o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
        Collections.sort(confmats2, new Comparator<ConfMat>() {
            @Override
            public int compare(ConfMat o1, ConfMat o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
        for (int i = 0; i < confmats1.size(); i++) {
            ConfMat cm1 = confmats1.get(i);
            ConfMat cm2 = confmats2.get(i);
            Assert.assertEquals("output of cm have to be the same", cm1.toString(), cm2.toString());
            Polygon2DInt bl1 = container1.getBaseline(cm1);
            Polygon2DInt bl2 = container2.getBaseline(cm2);
            Rectangle2DInt bounds1 = bl1.getBounds();
            Rectangle2DInt bounds2 = bl2.getBounds();
            Assert.assertTrue("both bounding boxes have to be the same", bounds1.contains(bounds2));
            Assert.assertTrue("both bounding boxes have to be the same", bounds2.contains(bounds1));

        }

        ConfMatContainer result = ConfMatContainer.newInstance(folderStorage);
        Assert.assertEquals("old and new structure differ in number of confmats", result.getConfmats().size(), confmats1.size());
//        assertEquals(expResult, result);
    }

}
