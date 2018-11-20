/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.CharMap;
import com.achteck.misc.types.ConfMat;
import com.achteck.misc.util.IO;
import de.planet.reco.types.SNetwork;
import de.planet.sprnn.SLayer;
import de.planet.sprnn.SNet;
import de.planet.sprnn.SUnitAbstract;
import de.planet.sprnn.SWeight;
import de.planet.sprnn.cell.CellDft;
import de.planet.sprnn.unit.SUnitAbstractWithBias;
import de.planet.sprnn.unit.SUnitDft;
import de.planet.sprnn.util.SNetUtils;
import de.uros.citlab.errorrate.util.ObjectCounter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.util.Pair;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.*;

/**
 * @author gundram
 */
public class CharMapUtil {

    public static Logger LOG = Logger.getLogger(CharMapUtil.class.getName());

    private static class OtherFormatException extends RuntimeException {

        final RuntimeException re;

        public OtherFormatException(String s) {
            super(s);
            re = new RuntimeException(s);
        }

        public OtherFormatException(String message, Throwable cause) {
            super(message, cause);
            re = new RuntimeException(message, cause);
        }

        public RuntimeException getException() {
            return re;
        }

    }

    public static CharMap<Integer> getCharMap(Collection<Character> chars, boolean mergeNFKC) {
        List<Character> result = new LinkedList<>(chars);
        Collections.sort(result);
        CharMap<Integer> charMap = new CharMap<>();
        charMap.put(0, ConfMat.NaC);
        for (Character character : result) {
            if (mergeNFKC) {
                char c = Normalizer.normalize("" + character, Normalizer.Form.NFKD).charAt(0);
                Integer key = charMap.getKey(c);
                if (c == character) {
                    if (key != null) {
                        continue;
                    }
                    charMap.put(charMap.keySet().size(), character);
                    continue;
                }
                if (key != null) {
                    String alter = charMap.get(key);
                    charMap.put(key, c + alter.substring(1) + character);
                } else {
                    charMap.put(charMap.keySet().size(), "" + c + character);
                }
            } else {
                charMap.put(charMap.keySet().size(), character);
            }
        }
        return charMap;
    }

    public static CharMap<Integer> loadCharMap(File i) {
        try {
            if (i == null) {
                throw new NullPointerException("input file is not set");
            }
            List<String> readLines = IOUtils.readLines(new FileReader(i));
            List<Pair<Integer, Character>> pairs = new ArrayList<>();
            for (int j = 0; j < readLines.size(); j++) {
                String readLine = readLines.get(j);
                int indexOf = readLine.lastIndexOf("=");
                if (indexOf < 0) {
                    throw new OtherFormatException("no '=' found in line " + (j + 1) + ": " + readLine);
                }
                int channelNo;
                try {
                    channelNo = Integer.parseInt(readLine.substring(indexOf + 1));
                } catch (Throwable ex) {
                    throw new OtherFormatException("cannot parse part behind '=' of line '" + readLine + "' to an integer", ex);
                }
                if (channelNo < 0) {
                    throw new RuntimeException("channel number of line '" + readLine + "' must not be negativ");
                }
                String channelValue = readLine.substring(0, indexOf);
                if (channelValue.isEmpty()) {
                    throw new RuntimeException("cannot interprete line '" + readLine + "': begins with an '='");
                }
                if (channelValue.charAt(0) == '\\') {
                    if (channelValue.charAt(1) == 'u') {
                        channelValue = "" + ((char) Integer.parseInt(channelValue.substring(2), 16));
                    } else {
                        if (channelValue.length() != 2) {
                            throw new RuntimeException("cannot interprete line '" + readLine + "': begins with '\\' and " + channelValue.length() + " sign(s) until '='.");
                        }
                        channelValue = channelValue.substring(1);
                    }
                }
                if (channelValue.length() > 1) {
                    throw new RuntimeException("cannot interprete line '" + readLine + "': More than one character in front of '='");
                }
                pairs.add(new Pair<>(channelNo, channelValue.charAt(0)));
            }
            Collections.sort(pairs, new Comparator<Pair<Integer, Character>>() {
                @Override
                public int compare(Pair<Integer, Character> o1, Pair<Integer, Character> o2) {
                    return Integer.compare(o1.getFirst(), o2.getFirst());
                }
            });
            CharMap<Integer> cm = new CharMap<>();
            cm.put(0, ConfMat.NaC);
            int idx = 0;
            String chars = "";
            for (int j = 0; j < pairs.size(); j++) {
                Pair<Integer, Character> get = pairs.get(j);
                int first = get.getFirst();
                if (first == idx + 1) {
                    cm.put(cm.keySet().size(), chars);
                    idx++;
                    chars = "" + get.getSecond();
                } else if (first == idx) {
                    chars += get.getSecond();
                } else {
                    throw new RuntimeException("no characters found for channel number " + (idx + 1) + ".");
                }
            }
            if (!chars.isEmpty()) {
                cm.put(cm.keySet().size(), chars);
            }
            return cm;
        } catch (OtherFormatException ex) {
            List<String> readLines;
            try {
                readLines = IOUtils.readLines(new FileReader(i));
            } catch (IOException ex1) {
                LOG.getLogger(CharMapUtil.class.getName()).log(Logger.WARN, "fallback without '=' and numbers does not work", ex1);
                throw ex.getException();
            }
            List<String> pairs = new ArrayList<>();
            Set<Character> chars = new HashSet<>();
            for (int j = 0; j < readLines.size(); j++) {
                String readLine = readLines.get(j);
                if (readLine.isEmpty()) {
                    if (j < readLines.size() - 1) {
                        LOG.log(Logger.WARN, "empty line in Charmap - ignore empty line");
                    }
                    continue;
                }
                for (char c : readLine.toCharArray()) {
                    if (chars.contains(c)) {
                        RuntimeException ex1 = new RuntimeException("character '" + c + "' is twice in CharMap");
                        LOG.getLogger(CharMapUtil.class.getName()).log(Logger.WARN, "fallback without '=' and numbers does not work", ex1);
                        throw ex.getException();
                    }
                    chars.add(c);
                }
                pairs.add(readLine);
            }
            CharMap<Integer> cm = new CharMap<>();
            cm.put(0, ConfMat.NaC);
            int idx = 1;
            for (int j = 0; j < pairs.size(); j++) {
                String get = pairs.get(j);
                cm.put(idx++, get);
            }
            return cm;
        } catch (IOException ex) {
            throw new RuntimeException("cannot load file " + i + ".", ex);
        }
    }

    public static void saveCharMap(ObjectCounter<Character> oc, File outChar) {
        if (outChar == null) {
            throw new NullPointerException("no output file given");
        }
        List<Pair<Character, Long>> resultOccurrence = oc.getResultOccurrence();
        Collections.sort(resultOccurrence, new Comparator<Pair<Character, Long>>() {
            @Override
            public int compare(Pair<Character, Long> o1, Pair<Character, Long> o2) {
                return Long.compare(o1.getFirst(), o2.getFirst());
            }
        });
        try (FileWriter fw = new FileWriter(outChar)) {
            for (int i = 0; i < resultOccurrence.size(); i++) {
                Pair<Character, Long> get = resultOccurrence.get(i);
                char c = get.getFirst();
                final String out = ((c == '=' || c == '\\') ? "\\" : "") + c + "=" + (i + 1);
                fw.write(out + (i < resultOccurrence.size() - 1 ? "\n" : ""));
            }
            fw.flush();
        } catch (IOException ex) {
            LOG.log(Logger.WARN, "cannot save file '" + outChar.getAbsolutePath() + "'.", ex);
        }
    }

    public static void saveCharMap(File htr, File outChar) {
        SNetwork net = (SNetwork) IOUtil.load(htr);
        net.setParamSet(net.getDefaultParamSet(new ParamSet()));
        net.init();
        saveCharMap(net.getCharMap(), outChar);
    }

    public static void saveCharMap(CharMap<Integer> cm, File outChar) {
        if (outChar == null) {
            throw new NullPointerException("no output file given");
        }
        List<Character> values = new ArrayList<>(cm.getValues());
        Collections.sort(values);
        try (FileWriter fw = new FileWriter(outChar)) {
            for (int i = 0; i < values.size(); i++) {
                char c = values.get(i);
                if (c == ConfMat.NaC) {
                    if (cm.getKey(c) != 0) {
                        throw new RuntimeException("NaC have to be channel 0");
                    }
                    continue;
                }
                int index = cm.getKey(c);
                final String out = ((c == '=' || c == '\\') ? "\\" : "") + c + "=" + (index);
                fw.write(out + (i < values.size() - 1 ? "\n" : ""));
            }
            fw.flush();
        } catch (IOException ex) {
            LOG.log(Logger.WARN, "cannot save file '" + outChar.getAbsolutePath() + "'.", ex);
            throw new RuntimeException("cannot save file '" + outChar.getAbsolutePath() + "'.", ex);
        }
    }

    private static double getBias(SUnitAbstract unit) {
        if (unit instanceof SUnitAbstractWithBias) {
            return ((SUnitAbstractWithBias) unit).getBias().value;
        }
        return 0;
    }

    private static List<SWeight> copy(List<SWeight> toCopy) {
        List<SWeight> res = new LinkedList<>();
        for (SWeight sWeight : toCopy) {
            SWeight sWeight1 = new SWeight(sWeight.srcUnit, sWeight.weightType.shiftY, sWeight.weightType.shiftX);
            sWeight1.value = sWeight.value;
            res.add(sWeight1);
        }
        return res;
    }

    private static Pair<Double, List<SWeight>> copy(Pair<Double, List<SWeight>> toCopy) {
        return new Pair<>(toCopy.getFirst(), copy(toCopy.getSecond()));
    }

    private static Pair<Double, List<SWeight>> getWeights(Pair<Double, List<SWeight>> nac, List<SWeight> weightsSrc, List<Pair<Double, List<SWeight>>> weights, double offset) {
        if (weights == null || weights.isEmpty()) {
            List<SWeight> weightsNaC = copy(nac.getSecond());
            HashMap<Integer, MultiWeight> mw = new HashMap<>();
            int cntOld = weightsNaC.size();
            int cntNew = 0;
            for (SWeight sWeight : weightsNaC) {
                MultiWeight multiWeight = new MultiWeight(sWeight);
                mw.put(multiWeight.hashCode(), multiWeight);
            }
            for (SWeight sWeight : weightsSrc) {
                MultiWeight multiWeight = new MultiWeight(sWeight);
                if (!mw.containsKey(multiWeight.hashCode())) {
                    mw.put(multiWeight.hashCode(), multiWeight);
                    cntNew++;
                }
            }
            List<SWeight> weightsMerged = new LinkedList<>();
            for (MultiWeight value : mw.values()) {
                weightsMerged.add(value.getWeight());
            }
            double bias = nac.getFirst() + offset;
            LOG.log(Logger.TRACE, "no weights found for this characters, copy " + cntOld + " NaC-Connections and " + cntNew + " new 0-connections with offset " + offset + " on bias.");
            return new Pair<>(bias, weightsMerged);
        }
        if (weights.size() == 1) {
            LOG.log(Logger.TRACE, "found 1 weight for this characters, return this weight.");
            return copy(weights.get(0));
        }
        double bias = 0;
        HashMap<Integer, MultiWeight> mw = new HashMap<>();
        for (Pair<Double, List<SWeight>> weight : weights) {
            for (SWeight sWeight : weight.getSecond()) {
                MultiWeight multiWeight = new MultiWeight(sWeight);
                if (mw.containsKey(multiWeight.hashCode())) {
                    mw.get(multiWeight.hashCode()).addWeight(sWeight);
                } else {
                    mw.put(multiWeight.hashCode(), multiWeight);
                }
            }
            bias += weight.getFirst();
        }
        List<SWeight> weightsMerged = new LinkedList<>();
        for (MultiWeight value : mw.values()) {
            weightsMerged.add(value.getWeight(weights.size()));
        }
        bias /= weights.size();
        LOG.log(Logger.TRACE, "found " + weights.size() + " weights for this characters, return this weight.");
        Collections.sort(weightsMerged, new Comparator<SWeight>() {
            @Override
            public int compare(SWeight o1, SWeight o2) {
                return Integer.compare(o1.srcUnit.indexOfNet, o1.srcUnit.indexOfNet);
            }
        });
        return new Pair<>(bias, weightsMerged);
    }

    private static class MultiWeight {

        private double sum;
        private int cnt;
        //        private List<SWeight> weights = new LinkedList<>();
        private int shiftY;
        private int shiftX;
        private SUnitAbstract srcUnit;

        public MultiWeight(SWeight weight) {
            addWeight(weight);
            srcUnit = weight.srcUnit;
            shiftY = weight.weightType.shiftY;
            shiftX = weight.weightType.shiftX;
        }

        public SWeight getWeight(int count) {
            SWeight sWeight = new SWeight(srcUnit, shiftY, shiftX);
            if (cnt == 0) {
                throw new RuntimeException("should be greater than zero");
            }
            sWeight.value = sum / count;
            return sWeight;
        }

        public SWeight getWeight() {
            return getWeight(cnt);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 89 * hash + this.shiftY;
            hash = 89 * hash + this.shiftX;
            hash = 89 * hash + Objects.hashCode(this.srcUnit);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                if (obj.getClass() == SWeight.class) {
                    final SWeight other = (SWeight) obj;
                    if (!Objects.equals(this.shiftY, other.weightType.shiftY)) {
                        return false;
                    }
                    if (!Objects.equals(this.shiftX, other.weightType.shiftX)) {
                        return false;
                    }
                    return Objects.equals(this.srcUnit, other.srcUnit);

                }
                return false;
            }
            final MultiWeight other = (MultiWeight) obj;
            if (!Objects.equals(this.shiftY, other.shiftY)) {
                return false;
            }
            if (!Objects.equals(this.shiftX, other.shiftX)) {
                return false;
            }
            return Objects.equals(this.srcUnit, other.srcUnit);
        }

        private void addWeight(SWeight weight) {
//            weights.add(weight);
            sum += weight.value;
            cnt++;
        }
    }

    public static void setCharMap(String pathToHtrIn, String pathToHtrOut, String pathToCharMap, double offset) {
        setCharMap(pathToHtrIn, pathToHtrOut, loadCharMap(new File(pathToCharMap)), offset);
    }

    public static void setCharMap(String pathToHtrIn, String pathToHtrOut, CharMap<Integer> cmNew, double offset) {
        try {
            SNetwork network = (SNetwork) IOUtil.load(pathToHtrIn);
            network.setParamSet(network.getDefaultParamSet(new ParamSet()));
            network.init();
            setCharMap(network, cmNew, offset);
            IO.save(network, new File(pathToHtrOut));
        } catch (IOException ex) {
            throw new RuntimeException("cannot load or save network.", ex);
        }

    }

    public static void setCharMap(SNetwork network, CharMap<Integer> cmNew, double offset) {
        SNet net = (SNet) network.getNet();
        CharMap<Integer> cmOrig = net.getCharmap();
        net.setCharmap(cmNew);
        network.init();
        HashMap<Character, Pair<Double, List<SWeight>>> unitMap = new LinkedHashMap<>();
        SLayer outLayer = net.outLayer;
        List<SWeight> weightsDefault = null;
        List<Pair<Double, List<SWeight>>> connections = new LinkedList<>();
        ArrayList<SUnitAbstract> unitsVisible = SNetUtils.getUnitsVisible(outLayer);
        int idxNaC = cmOrig.getKey(ConfMat.NaC);
        SUnitAbstract unitNaC = unitsVisible.get(idxNaC);
        Pair<Double, List<SWeight>> connectionsNaC = new Pair<>(getBias(unitNaC), copy(unitNaC.getWeights()));
        if (unitsVisible.size() != outLayer.getCells().size()) {
            throw new RuntimeException("only default cells with one (visible) unit are allowed into last layer for this method.");
        }
        //collect weight with corresponding index and delete old entries. 
        for (SUnitAbstract sUnitAbstract : unitsVisible) {
            List<SWeight> weights = new LinkedList<>(sUnitAbstract.getWeights());
            Collections.sort(weights, new Comparator<SWeight>() {
                @Override
                public int compare(SWeight o1, SWeight o2) {
                    return Integer.compare(o1.srcUnit.indexOfNet, o2.srcUnit.indexOfNet);
                }
            });
            connections.add(new Pair<>(getBias(sUnitAbstract), (List<SWeight>) new ArrayList<>(weights)));
            weights.clear();
        }
        //find all units that are connected to output units - except recurrent connections
        {
            HashMap<Integer, Pair<SUnitAbstract, int[]>> srcUnitMap = new HashMap<>();
            for (Pair<Double, List<SWeight>> connection : connections) {
                for (SWeight sWeight : connection.getSecond()) {
                    if (!unitsVisible.contains(sWeight.srcUnit)) {
                        int hash = (69 * sWeight.srcUnit.hashCode() + sWeight.weightType.shiftY) * 69 + sWeight.weightType.shiftX;
                        if (!srcUnitMap.containsKey(hash)) {
                            srcUnitMap.put(hash, new Pair<>(sWeight.srcUnit, new int[]{sWeight.weightType.shiftY, sWeight.weightType.shiftX}));
                        }
                    }
                }
            }
            LOG.log(Logger.DEBUG, "found " + srcUnitMap.size() + " src-units for new charmap-candidates");
            weightsDefault = new LinkedList<>();
            for (Pair<SUnitAbstract, int[]> value : srcUnitMap.values()) {
                weightsDefault.add(new SWeight(value.getFirst(), value.getSecond()[0], value.getSecond()[1]));
            }
        }
        //delete all units in upper layer
        outLayer.getCells().clear();
        //get weight and Bias 
        for (Character character : cmOrig.getValues()) {
            Integer idx = cmOrig.getKey((char) character);
            if (idx >= 0) {
                unitMap.put(character, connections.get(idx));
                if (LOG.isTraceEnabled()) {
                    LOG.log(Logger.TRACE, "for original character: " + (Objects.equals(character, ConfMat.NaC) ? "NaC" : character) + " with index " + idx + ".");
                }
            } else {
                throw new RuntimeException("each character should have an index in its charmap");
            }
        }
        for (int i = 0; i < cmNew.keySet().size(); i++) {
            char[] values = cmNew.get(i).toCharArray();
            if (values == null || values.length == 0) {
                throw new RuntimeException("values of key " + i + " of new charMap is null or empty.");
            }
            List<Pair<Double, List<SWeight>>> weightsAndBiasEs = new LinkedList<>();
            for (char value : values) {
                if (unitMap.containsKey(value)) {
                    weightsAndBiasEs.add(unitMap.get(value));
                }
            }
            LOG.log(Logger.TRACE, "for new character: '" + String.valueOf(values).replace("" + ConfMat.NaC, "NaC") + "' with index " + i + " we have " + weightsAndBiasEs.size() + " candidates to combine");
            Pair<Double, List<SWeight>> weights = getWeights(connectionsNaC, weightsDefault, weightsAndBiasEs, offset);
            SUnitDft unit = new SUnitDft(true, null);
            unit.setBias(true);
            unit.getBias().value = weights.getFirst();
            unit.getWeights().addAll(weights.getSecond());
            LOG.log(Logger.TRACE, "add cell with " + weights.getSecond().size() + " weights");
            outLayer.addCell(new CellDft(unit));
        }
        net.initPhysStruct();
        network.init();
    }

    public static boolean equals(CharMap<Integer> cm1, CharMap<Integer> cm2) {
        List<Integer> keys1 = new ArrayList<>(cm1.keySet());
        List<Integer> keys2 = new ArrayList<>(cm2.keySet());
        if (keys1.size() != keys2.size()) {
            return false;
        }
        Collections.sort(keys1);
        Collections.sort(keys2);
        for (int i = 0; i < keys1.size(); i++) {
            if (0 == keys1.get(i).compareTo(keys2.get(i))) {
                char[] chars1 = cm1.get(i).toCharArray();
                Arrays.sort(chars1);
                String sorted1 = new String(chars1);
                char[] chars2 = cm2.get(i).toCharArray();
                Arrays.sort(chars2);
                String sorted2 = new String(chars2);
                if (!sorted1.equals(sorted2)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public static void copyIntoCharmap(CharMap<Integer> src, CharMap<Integer> tgt) {
        //check if the both CharMaps fit together - according their index
        List<Integer> arrTgt = new ArrayList<>(tgt.keySet());
        List<Integer> arrSrc = new ArrayList<>(src.keySet());
        if (arrSrc.size() != arrTgt.size()) {
            throw new RuntimeException("source CharMap has size" + arrSrc.size() + " but target CharMap has size " + arrTgt.size() + ".");
        }
        Collections.sort(arrTgt);
        Collections.sort(arrSrc);
        int length = Math.min(arrSrc.size(), arrTgt.size());
        for (int i = 0; i < length; i++) {
            if (0 != arrSrc.get(i).compareTo(arrTgt.get(i))) {
                int srcIdx = arrSrc.get(i);
                int tgtIdx = arrTgt.get(i);
                if (srcIdx < tgtIdx) {
                    throw new RuntimeException("in source CharMap index " + srcIdx + " exists but not in target CharMap.");
                } else {
                    throw new RuntimeException("in target CharMap index " + tgtIdx + " exists but not in source CharMap.");
                }
            }
        }
        //do merge
        for (Integer integer : arrTgt) {
            String before = tgt.get(integer);
            String after = src.get(integer);
            if (!before.equals(after)) {
                LOG.log(Logger.WARN, "substitute characters [" + before + "] by characters [" + after + "]");
            }
            tgt.put(integer, after);
        }

    }
}
