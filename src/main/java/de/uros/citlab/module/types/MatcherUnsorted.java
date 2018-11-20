/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.types;

import de.planet.math.util.MatrixUtil;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gundram
 * @param <Type1>
 * @param <Type2>
 */
public class MatcherUnsorted<Type1, Type2> {

    private CostCalcer<Type1, Type2> cc;
    private double maxCost;
    Logger LOG = LoggerFactory.getLogger(MatcherUnsorted.class);

    public MatcherUnsorted(CostCalcer<Type1, Type2> cc, double maxCost) {
        this.cc = cc;
        this.maxCost = maxCost;
    }

    public static interface CostCalcer<Type1, Type2> {

        public double getCost(Type1 reco, Type2 ref);
    }

    public static class Match<Type1, Type2> {

        private Type1 reco;
        private Type2 ref;
        private double cost;

        public Match(Type1 reco, Type2 ref, double cost) {
            this.reco = reco;
            this.ref = ref;
            this.cost = cost;
        }

        public Type1 getReco() {
            return null;
        }

        public Type2 getRef() {
            return null;
        }

        public double getCost() {
            return cost;
        }
    }

    public List<Match<Type1, Type2>> getMatches(Collection<? extends Type1> recos, Collection<? extends Type2> refs) {
        List<Match<Type1, Type2>> res = new LinkedList<>();
        if (recos.isEmpty() || refs.isEmpty()) {
            return res;
        }
        double[][] costMat = new double[recos.size()][refs.size()];
        LinkedList<? extends Type1> recoDyn = new LinkedList<>(recos);
        LinkedList<? extends Type2> refDyn = new LinkedList<>(refs);
        for (int i = 0; i < recos.size(); i++) {
            Type1 cm = recoDyn.get(i);
            double[] costVec = costMat[i];
            for (int j = 0; j < refs.size(); j++) {
                Type2 string = refDyn.get(j);
                costVec[j] = cc.getCost(cm, string);
            }
        }
        if (!recoDyn.isEmpty() || !refDyn.isEmpty()) {
            com.achteck.misc.types.Pair<Double, int[]> minPoint = MatrixUtil.getMinValAndPosXY(costMat);
            while (minPoint.first < maxCost) {
                int i = minPoint.second[1];
                int j = minPoint.second[0];
                res.add(new Match<>(recoDyn.get(i), refDyn.get(j), minPoint.first));
                recoDyn.remove(i);
                refDyn.remove(j);
                if (recoDyn.isEmpty() || refDyn.isEmpty()) {
                    break;
                }
                costMat = deleteItems(costMat, i, j);
                minPoint = MatrixUtil.getMinValAndPosXY(costMat);
            }
        }
        LOG.info("from " + recos.size() + " recos and " + refs.size() + " refs " + res.size() + " matches found.");
        return res;
    }

    private double[][] deleteItems(double[][] matOld, int i, int j) {
        double[][] matNew = new double[matOld.length - 1][matOld[0].length - 1];
        for (int iNew = 0, iOld = 0; iNew < matNew.length; iNew++, iOld++) {
            if (iOld == i) {
                iOld++;
            }
            double[] vecNew = matNew[iNew];
            double[] vecOld = matOld[iOld];
            for (int jNew = 0, jOld = 0; jNew < vecNew.length; jNew++, jOld++) {
                if (jOld == j) {
                    jOld++;
                }
                vecNew[jNew] = vecOld[jOld];

            }
        }
        return matNew;
    }

}
