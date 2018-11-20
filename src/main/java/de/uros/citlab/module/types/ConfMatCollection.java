//////////////////////////////////////////////////
///// File:       ConfMatCollection.java
///// Created:    22.06.2015  22:27:26
///// Encoding:   UTF-8
//////////////////////////////////////////////////
//package de.uros.citlab.module.types;
//
//import com.achteck.misc.log.Logger;
//import com.achteck.misc.types.CharMap;
//import com.achteck.misc.types.ConfMat;
//import com.achteck.misc.types.Pair;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * Concatenate a list of ConfMats into one long ConfMat with a new channel for
// * newlines.
// *
// * Since 22.06.2015
// *
// * @author tobias
// */
//public class ConfMatCollection extends ConfMat {
//
//    private static final long serialVersionUID = 1L;
//    private static final Logger LOG = Logger.getLogger(ConfMatCollection.class.getName());
//    private final CharMap<Integer> charMapOrig;
//    private final Integer idxNaC;
//    private double confNac;
//    private double confRet;
//    private final double retProb;
//    private int[] indicesStart;
//    private final int numOfCM;
//    private final char returnSymb;
//    private final boolean appendFinalRet;
//    private final double offsetNaC;
//
//    public ConfMatCollection(double retProb, List<ConfMat> cmList, boolean appendFinalRet) {
//        this(retProb, cmList, appendFinalRet, 0.0);
//    }
//
//    public ConfMatCollection(double retProb, List<ConfMat> cmList, boolean appendFinalRet, double offsetNaC) {
//        this(ConfMat.Return, retProb, cmList, appendFinalRet, offsetNaC);
//    }
//
//    private ConfMatCollection(char retChar, double retProb, List<ConfMat> cmList, boolean appendFinalRet, double offsetNaC) {
//        this.appendFinalRet = appendFinalRet;
//        this.offsetNaC = offsetNaC;
//        this.retProb = retProb;
//        if (cmList.isEmpty()) {
//            throw new RuntimeException("empty ConfMat list");
//        }
//        int cmPos = 0;
//        charMapOrig = cmList.get(0).getCharMap();
//        if (charMapOrig.containsValue(retChar)) {
//            throw new IllegalArgumentException("symbol '" + retChar + "' already in charMap - please choose other retChar.");
//        }
//        for (ConfMat cm : cmList) {
//            if (cm.getLength() == 0) {
//                LOG.log(Logger.ERROR, "empty ConfMat");
//            }
//            cmPos += cm.getLength();
//            if (!charMapOrig.equals(cm.getCharMap())) {
//                throw new IllegalArgumentException("ConfMats have to have the same charmaps!");
//            }
//        }
//
//        this.returnSymb = retChar;
//        idxNaC = charMapOrig.getKey(ConfMat.NaC);
//        numOfCM = cmList.size();
//        double[][] bigMat = new double[cmPos + cmList.size() + (appendFinalRet ? 0 : -1)][charMapOrig.keySet().size() + 1];
//        int posIdx = 0;
//        indicesStart = new int[cmList.size()];
//        confNac = Math.log(1 - retProb);
//        confRet = Math.log(retProb);
//        int cmIdx = 0;
//        for (ConfMat cm : cmList) {
//            double[][] mat = cm.getDoubleMat();
//            indicesStart[cmIdx++] = posIdx;
//            for (int j = 0; j < mat.length; j++) {
//                double[] row = mat[j];
//                double[] bigVec = bigMat[posIdx++];
//                System.arraycopy(row, 0, bigVec, 0, row.length);
//                bigVec[idxNaC] += offsetNaC;
//                bigVec[row.length] = Double.NEGATIVE_INFINITY;
//            }
//            if (posIdx == bigMat.length) {
//                break;
//            }
//            double[] bigVec = bigMat[posIdx++];
//            Arrays.fill(bigVec, Double.NEGATIVE_INFINITY);
//            bigVec[idxNaC] = confNac;
//            bigVec[bigVec.length - 1] = confRet;
//        }
//        copyFrom(bigMat);
//        CharMap<Integer> charMapNew = new CharMap<>(charMapOrig);
//        charMapNew.put(charMapNew.keySet().size(), ConfMat.Return);
//        setCharMap(charMapNew);
//    }
//
//    /**
//     *
//     * @param idx
//     * @return index of ConfMat (first) and index in ConfMat (second)
//     */
//    public Pair<Integer, Integer> getOrigIdx(int idx) {
//        if (idx < 0 || idx > getLength()) {
//            return null;
//        }
//        int start = 0;
//        while (start + 1 < numOfCM && idx >= indicesStart[start + 1]) {
//            start++;
//        }
//        return start < indicesStart.length ? new Pair<>(start, idx - indicesStart[start]) : null;
//    }
//
//}
