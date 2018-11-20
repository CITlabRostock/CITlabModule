/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.types;

import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import com.achteck.misc.util.StringIO;
import de.planet.itrtech.types.IDictOccurrence;
import de.planet.util.types.DictOccurrence;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tobias
 */
public class DictOccurenceFromText extends ParamTreeOrganizer implements IDictOccurrence {

    @ParamAnnotation
    String l;
    @ParamAnnotation
    String sep = "§";
    private DictOccurrence dict;

    public DictOccurenceFromText() {
        addReflection(this, DictOccurenceFromText.class);
    }

    @Override
    public void init() {
        super.init();
        String tmpfile = "tmp.dict";
        CreateDictSimple cds = new CreateDictSimple(sep);
        try {
            List<String> files;
            files = StringIO.loadLineList(l);
            cds.processLines(files);
            cds.savePDict(tmpfile);
            dict = new DictOccurrence(tmpfile, sep, 1, 0, true);
            dict.setParamSet(dict.getDefaultParamSet(null));
            dict.init();
            File f = new File(tmpfile);
            f.delete();
        } catch (IOException ex) {
            Logger.getLogger(DictOccurenceFromText.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public double getCost(String string) {
        return dict.getCost(string);
    }

    @Override
    public ArrayList<String> getDict() {
        return dict.getDict();
    }

    @Override
    public ArrayList<String> getPDict() {
        return dict.getPDict();
    }

    @Override
    public HashMap<String, Double> getProbMap() {
        return dict.getProbMap();
    }

    @Override
    public long getTotalCount() {
        return dict.getTotalCount();
    }

    @Override
    public String getNext(Random random) {
        return dict.getNext(random);
    }

}
