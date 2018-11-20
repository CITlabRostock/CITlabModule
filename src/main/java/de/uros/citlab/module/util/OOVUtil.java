/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import de.planet.itrtech.types.IDictOccurrence;
import java.util.HashSet;

/**
 *
 * @author tobias
 */
public class OOVUtil {

    public static double calcOOVRate(IDictOccurrence refDict, IDictOccurrence evalDict) {
        double s = 0.0;
//        System.out.println("Start eval");
        HashSet<String> set = new HashSet<String>(refDict.getDict());
        for (String word : evalDict.getDict()) {
            if (!set.contains(word)) {
                s += Math.exp(-evalDict.getCost(word));
            }
        }
//        System.out.println("eval done " + s);
        return s;
    }

}
