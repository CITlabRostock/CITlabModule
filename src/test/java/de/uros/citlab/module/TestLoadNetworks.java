/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module;

import com.achteck.misc.util.IO;
import de.planet.reco.types.SNetwork;
import de.uros.citlab.module.train.TrainHtr;
import org.junit.Test;

import java.io.IOException;

/**
 *
 * @author gundram
 */
public class TestLoadNetworks {

    @Test
    public void testLoadOldNetworks(){
        try {
            SNetwork net = (SNetwork) IO.load(TrainHtr.getNet(TestFiles.getHtrs().get(2)),"de.planet.tech", "de.planet");
            net.setParamSet(net.getDefaultParamSet(null));
            net.init();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
}
