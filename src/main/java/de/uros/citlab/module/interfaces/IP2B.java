/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.interfaces;

import de.planet.imaging.types.HybridImage;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.interfaces.types.Image;
import java.util.List;

/**
 *
 * @author gundram
 */
public interface IP2B {

    void processImage(Image image, PcGtsType page);

    void processRegion(HybridImage img, TextRegionType trt);

    void processLine(HybridImage image, TextLineType tlt, TextRegionType rt);
    
}
