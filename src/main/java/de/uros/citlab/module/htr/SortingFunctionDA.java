/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.htr;

import com.achteck.misc.log.Logger;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.planet.itrtech.types.IDictOccurrence;
import de.planet.citech.types.IDecodingType;
import de.planet.citech.types.ISortingFunction;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author gundram
 */
public class SortingFunctionDA extends ParamTreeOrganizer implements ISortingFunction {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(SortingFunctionDA.class.getName());
    @ParamAnnotation(descr = "convex param in [0,1]")
    private double lambda;
    @ParamAnnotation(descr = "OOV offset costs on bestpath")
    private double oov;
    transient private IDictOccurrence dict;
    private ISrcPrior srcPrior;
    @ParamAnnotation(descr = "character prior impact scale")
    private final double alpha;
    @ParamAnnotation(descr = "character offset")
    private final double beta;
    @ParamAnnotation(descr = "Word length impact scale")
    private final double gamma;

    public SortingFunctionDA(double lambda, double oov, double alpha, double beta, double gamma) {
        this.lambda = lambda;
        this.oov = oov;
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
        addReflection(this, SortingFunctionDA.class);
    }

    public SortingFunctionDA() {
        this(0.5, 22.0, 0.25, 1.0, 0.5);
    }

    @Override
    public void init() {
        super.init();
        LOG.log(Logger.INFO, "parameters: lambda = " + lambda + " oov = " + oov);
    }

    @Override
    public double getSortingCost(IDecodingType idt) {
        double priorCostTest = dict != null ? dict.getCost(idt.getText()) : Double.MAX_VALUE;
        double priorCostTrain = srcPrior.getNegLogProb(idt.getText());
        double posteriorCost = idt.getCostAbs();
        if (priorCostTest >= Double.MAX_VALUE) {
            if (idt.getLabel().equals(IDecodingType.Label.RAW_ENTRY)) {
                priorCostTest = getOOVCost(idt);
            } else {
                priorCostTest = posteriorCost;
            }
        }
        return (lambda * posteriorCost + (1 - lambda) * (priorCostTest - priorCostTrain));
    }

    @Override
    public double getSortingCost(List<? extends IDecodingType> list) {
        double sum = 0;
        for (IDecodingType idt : list) {
            sum += getSortingCost(idt);
        }
        return sum;
    }

    @Override
    public double[] getSortingCosts(List<? extends IDecodingType> list) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void checkOovCosts() {
        if (Collections.max(dict.getProbMap().values()) > oov) {
            LOG.log(Logger.WARN, "smallest prior for dictionary item is greater than oov offset. Try to cut the dictionary or increase the oov value!");
        }
    }

    @Override
    public boolean setDict(IDictOccurrence ido) {
        if (dict == ido) {
            return false;
        }
        dict = ido;
        srcPrior = new SrcPriorFromDict(dict, alpha, beta, gamma);
        checkOovCosts();
        return true;
    }

    protected double getOOVCost(IDecodingType group) {
        return oov;
    }

    public static interface ISrcPrior {

        public double getNegLogProb(String word);
    }

    private static class SrcPriorFromDict implements ISrcPrior {

        private final IDictOccurrence dict;
        private double[] negLogWordLengthProbs;
        private HashMap<Character, Double> costs;
        private double dfltCharCost;
        private final double beta;
        private final double alpha;
        private final double gamma;

        private SrcPriorFromDict(IDictOccurrence dict, double alpha, double beta, double gamma) {
            this.dict = dict;
            this.alpha = alpha; // character prior impact scale
            this.beta = beta;   // character offset
            this.gamma = gamma; // Word length impact scale 
            init();

        }

        @Override
        public double getNegLogProb(String word) {
            double ret;
            if (word.length() < negLogWordLengthProbs.length) {
                ret = negLogWordLengthProbs[word.length()];
            } else {
                ret = negLogWordLengthProbs[0];
            }
            ret *= gamma;
            for (char c : word.toCharArray()) {
                Double neglogcharprob = costs.get(c);
                if (neglogcharprob == null) {
                    neglogcharprob = dfltCharCost;
                }
                ret += alpha * neglogcharprob + beta;
            }
            return ret;
        }

        private void init() {
            LOG.log(Logger.DEBUG, "Calculating the source priors.");
            HashMap<String, Double> negLogProbMap = dict.getProbMap();
            int maxWordLength = getMaxWordLength(negLogProbMap);
            negLogWordLengthProbs = calcWordLengthCosts(maxWordLength, negLogProbMap);

            costs = new HashMap<>();
            double sum = 0.0;
            /**
             * Summe der Wort-Wahrscheinlichkeiten für jeden Character des
             * Wortes aufsummiert
             */
            for (Map.Entry<String, Double> entry : negLogProbMap.entrySet()) {
                double prob = Math.exp(-entry.getValue());
                for (char c : entry.getKey().toCharArray()) {
                    add(costs, c, prob);
                }
                sum += prob * entry.getKey().length();
                negLogWordLengthProbs[entry.getKey().length()] += prob;
            }
            double minProb = getMinProb(costs);
            sum += minProb;
            /**
             * Ab hier wird mit Kosten gearbeitet + Normerug der WK auf 1
             */
            dfltCharCost = -Math.log(minProb / sum);
            double plschk = minProb / sum;
            for (Map.Entry<Character, Double> entry : costs.entrySet()) {
                plschk += entry.getValue() / sum;
                double newCost = -Math.log(entry.getValue() / sum);
                costs.put(entry.getKey(), newCost);
            }
            if (Math.abs(plschk - 1) > 10E-7) {
                throw new RuntimeException("Error in PlausiCheck. Summation of char probs");
            }
            LOG.log(Logger.DEBUG, "Finished calculation of the source priors.");
        }

        private void add(HashMap<Character, Double> probs, char c, Double value) {
            Double get = probs.get(c);
            if (get == null) {
                probs.put(c, value);
                return;
            }
            probs.put(c, get + value);
        }

        private int getMaxWordLength(HashMap<String, Double> negLogProbMap) {
            int maxLength = 0;
            for (String string : negLogProbMap.keySet()) {
                if (maxLength < string.length()) {
                    maxLength = string.length();
                }
            }
            return maxLength;
        }

        private double getMinProb(HashMap<Character, Double> costs) {
            double ret = Double.MAX_VALUE;
            for (Map.Entry<Character, Double> entry : costs.entrySet()) {
                double newProb = entry.getValue();
                if (newProb < ret) {
                    ret = newProb;
                }
            }
            return ret;
        }

        private double[] calcWordLengthCosts(int maxWordLength, HashMap<String, Double> negLogProbMap) {
            double[] ret = new double[maxWordLength + 1];
            for (Map.Entry<String, Double> entry : negLogProbMap.entrySet()) {
                double prob = Math.exp(-entry.getValue());
                ret[entry.getKey().length()] += prob;
            }
            double sum = 0.0;
            double minprob = Double.POSITIVE_INFINITY;
            for (double prob : ret) {
                sum += prob;
                if (prob > 0 && minprob > prob) {
                    minprob = prob;
                }
            }
            for (int i = 0; i < ret.length; i++) {
                double d = ret[i];
                if (d == 0.0) {
                    ret[i] = minprob;
                    sum += minprob;
                }
            }
            for (int i = 0; i < ret.length; i++) {
                ret[i] /= sum;
            }
            //PlausiCheck
            sum = 0;
            for (double d : ret) {
                sum += d;
            }
            if (Math.abs(sum - 1) > 10E-7) {
                throw new RuntimeException("Error in PlausiCheck. Summation of word length probs");
            }
            return ret;
        }

    }
}
