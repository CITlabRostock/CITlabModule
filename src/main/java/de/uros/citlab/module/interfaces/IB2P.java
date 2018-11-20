/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.interfaces;

import com.achteck.misc.types.IParamSetHandler;
import de.planet.imaging.types.HybridImage;
import de.planet.math.geom2d.types.Polygon2DInt;
import java.util.List;

/**
 *
 * @author gundram
 */
public interface IB2P extends IParamSetHandler {

    public List<Polygon2DInt> process(HybridImage image, List<Polygon2DInt> polygons, Double orientation);
}
