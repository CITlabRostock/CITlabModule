/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.feat;

import com.achteck.misc.log.Logger;
import com.achteck.misc.util.IO;
import de.planet.math.util.MatrixUtil;
import de.planet.reco.types.SNetwork;
import de.planet.sprnn.SNet;
import de.uros.citlab.module.TestFiles;
import de.uros.citlab.module.interfaces.IFeatureGeneratorStreamable;
import de.uros.citlab.module.train.TrainHtr;
import de.uros.citlab.module.util.IModuleTest;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 *
 * @author gundram
 */
public class FeatNetworkTest {

    public static Logger LOG = Logger.getLogger(FeatNetworkTest.class.getName());
    private static FeatNetwork instanceCM;
    private static FeatNetwork instanceH;
    private static FeatNetwork instanceBoth;

    @BeforeClass
    public static void setUp() {
        System.out.println("setUp ( load networks)");
        try {
            SNetwork load = (SNetwork) IO.load(TrainHtr.getNetBestOrLast(TestFiles.getHtrDft()));
            SNet net = (SNet) load.getNet();
            instanceBoth = new FeatNetwork(net, true, true);
            instanceH = new FeatNetwork(net, false, true);
            instanceCM = new FeatNetwork(net, true, false);

        } catch (IOException ex) {
            LOG.log(Logger.ERROR, ex);
            fail(ex.getMessage());
        } catch (ClassNotFoundException ex) {
            LOG.log(Logger.ERROR, ex);
            fail(ex.getMessage());
        }

    }

    @After
    public void tearDown() {
    }

    /**
     * Test of usage method, of class TrainHtrSGD.
     */
    @Test
    public void testUsage() {
        IModuleTest.testUsage(instanceBoth);
    }

    /**
     * Test of getToolName method, of class TrainHtrSGD.
     */
    @Test
    public void testGetToolName() {
        IModuleTest.testGetToolName(instanceH);
    }

    /**
     * Test of getVersion method, of class TrainHtrSGD.
     */
    @Test
    public void testGetVersion() {
        IModuleTest.testGetVersion(instanceCM);
    }

    /**
     * Test of getProvider method, of class TrainHtrSGD.
     */
    @Test
    public void testGetProvider() {
        IModuleTest.testGetProvider(instanceH);
    }

    public void testGetSaveMode() {
        System.out.println("getSaveMode");
        assertEquals(IFeatureGeneratorStreamable.Savemode.FLOAT, instanceBoth.getSaveMode());
        assertEquals(IFeatureGeneratorStreamable.Savemode.LLH, instanceCM.getSaveMode());
        assertEquals(IFeatureGeneratorStreamable.Savemode.FLOAT, instanceH.getSaveMode());
    }
//

    /**
     * Test of process method, of class FeatNetwork.
     */
    @Test
    public void testProcess_doubleArrArr_StringArr() {
        System.out.println("process");
        double[][] in = MatrixUtil.createRandomMatrixDbl(50, 30);
        String[] props = null;
        double[][] result = instanceBoth.process(in, props);
        assertNotNull("result should not be null", result);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

}
