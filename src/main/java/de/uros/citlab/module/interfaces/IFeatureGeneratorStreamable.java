/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.interfaces;

import eu.transkribus.interfaces.IFeatureGenerator;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author gundram
 */
public interface IFeatureGeneratorStreamable extends IFeatureGenerator {

    public enum Savemode {
        LLH, BYTE, FLOAT;
    }

    public void process(InputStream is, OutputStream os);

    public double[][] process(double[][] in, String[] props);

    public Savemode getSaveMode();

    public int getLLHAccuracy();
}
