////////////////////////////////////////////////
/// File:       B2Tube.java
/// Created:    18.12.2018  13:44:12
/// Encoding:   UTF-8
///
/// Planet Artificial Intelligence GmbH CONFIDENTIAL
/// __________________
/// 
/// [2018] Planet Artificial Intelligence GmbH
/// All Rights Reserved.
/// 
/// NOTICE:  All information contained herein is, and remains
/// the property of Planet Artificial Intelligence GmbH and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to Planet Artificial Intelligence GmbH
/// and its suppliers and may be covered patents,
/// patents in process, and are protected by trade secret or copyright law.
/// Dissemination of this information or reproduction of this material
/// is strictly forbidden unless prior written permission is obtained
/// from Planet Artificial Intelligence GmbH.
////////////////////////////////////////////////

package de.uros.citlab.module.baseline2polygon;

import com.achteck.misc.types.ParamSetOrganizer;
import de.planet.imaging.types.HybridImage;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.roi_neural.baseline2polygon.Baseline2PolygonTubeParser;
import de.uros.citlab.module.interfaces.IB2P;
import java.util.List;


/**
*  Desciption of B2Tube
*
*
*  Since 18.12.2018
*
* @author Tobi G. <tobias.gruening@planet.de>
*/
public class B2PTube extends ParamSetOrganizer implements IB2P {
    private static final long serialVersionUID = 1L;

    @Override
    public List<Polygon2DInt> process(HybridImage image, List<Polygon2DInt> polygons, Double orientation) {
        List<Polygon2DInt> tubes = Baseline2PolygonTubeParser.process(image, polygons, orientation);
        return tubes;
    }

}
