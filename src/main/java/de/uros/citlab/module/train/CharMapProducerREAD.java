/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.train;

import com.achteck.misc.log.Logger;
import com.achteck.misc.types.CharMap;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.planet.citech.trainer.ISNetworkFactory;
import de.uros.citlab.module.util.CharMapUtil;
import java.io.File;

/**
 *
 * @author gundram
 */
public class CharMapProducerREAD extends ParamTreeOrganizer implements ISNetworkFactory.ICharMapFactory {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(CharMapProducerREAD.class.getName());

    @ParamAnnotation(descr = "path to character-file")
    private String i = "";

    public CharMapProducerREAD() {
        addReflection(this, CharMapProducerREAD.class);
    }

    @Override
    public CharMap<Integer> getCharMap() {
        if (i == null || i.isEmpty()) {
            throw new RuntimeException("no file given to create the CharMap.");
        }
        return CharMapUtil.loadCharMap(new File(i));
    }

}
