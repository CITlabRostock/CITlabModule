/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.htr;

import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamSetOrganizer;
import de.planet.itrtech.types.IDictOccurrence;
import edu.berkeley.nlp.lm.ContextEncodedProbBackoffLm;
import edu.berkeley.nlp.lm.WordIndexer;
import edu.berkeley.nlp.lm.io.LmReaders;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import org.apache.commons.math3.util.Pair;

/**
 *
 * @author gundram
 */
public class DictOccurenceArpa extends ParamSetOrganizer implements IDictOccurrence {

    @ParamAnnotation(descr = "path to arpa-format file")
    private String f;
    @ParamAnnotation(descr = "maximal number of tokens (-1 for all tokens)")
    private int maxanz;
    private HashMap<String, Double> costMap;
    private ArrayList<String> lex;

    public DictOccurenceArpa() {
        this("", -1);
    }

    public DictOccurenceArpa(String pathToFile, int maxAnz) {
        f = pathToFile;
        maxanz = maxAnz;
        addReflection(this, DictOccurenceArpa.class);
    }

    @Override
    public void init() {
        super.init(); //To change body of generated methods, choose Tools | Templates.
        ContextEncodedProbBackoffLm<String> lm = LmReaders.readContextEncodedLmFromArpa(f);
        WordIndexer<String> wordIndexer = lm.getWordIndexer();
        costMap = new HashMap<>();
        double correctionfactor = 1 / Math.log(10);
        for (int i = 0; i < wordIndexer.numWords(); i++) {
            String token = wordIndexer.getWord(i);
            float logProb = lm.getLogProb(Arrays.asList(token));
            costMap.put(token, -logProb * correctionfactor);
        }
        if (maxanz > 0 && maxanz < costMap.size()) {
            List<Pair<Double, String>> cand = new ArrayList<>(costMap.size());
            for (String object : costMap.keySet()) {
                cand.add(new Pair<>(costMap.get(object), object));
            }
            Collections.sort(cand, new Comparator<Pair<Double, String>>() {
                @Override
                public int compare(Pair<Double, String> o1, Pair<Double, String> o2) {
                    return Double.compare(o1.getFirst(), o2.getFirst());
                }
            });
            for (int i = maxanz; i < cand.size(); i++) {
                costMap.remove(cand.get(maxanz).getValue());
            }
        }
        lex = new ArrayList<>(costMap.keySet());
    }

    @Override
    public double getCost(String string) {
        Double get = costMap.get(string);
        if (get == null || Double.isInfinite(get)) {
            return Double.MAX_VALUE;
        }
        return get;
    }

    @Override
    public ArrayList<String> getDict() {
        return lex;
    }

    @Override
    public ArrayList<String> getPDict() {
        throw new UnsupportedOperationException("Not supported for this class."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public HashMap<String, Double> getProbMap() {
        return costMap;
    }

    @Override
    public long getTotalCount() {
        throw new UnsupportedOperationException("Not supported for this class."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getNext(Random random) {
        return lex.get(random.nextInt(lex.size()));
    }

}
