/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.io.File;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.CharMap;
import com.achteck.misc.types.ConfMat;
import com.achteck.misc.util.IO;
import com.achteck.misc.util.StringIO;
import de.planet.imaging.types.HybridImage;
import de.planet.itrtech.reco.ISNetwork;
import de.planet.itrtech.types.ImagePropertyIDs;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.math.util.PolygonHelper;
import de.uros.citlab.errorrate.costcalculator.CostCalculatorDft;
import de.uros.citlab.errorrate.htr.ErrorModuleDynProg;
import de.uros.citlab.errorrate.normalizer.StringNormalizerDft;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.module.util.IOUtil;
import de.uros.citlab.tokenizer.categorizer.CategorizerCharacterDft;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author tobias
 */
public class BestPathTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
//        ISNetwork net = (ISNetwork) IO.load("snet_barlach_lstm3.tf");
//        ISNetwork net = (ISNetwork) IO.load("snet_barlach.tf");
        ISNetwork net = (ISNetwork) IOUtil.load("/home/tobias/arbeit/Read/Barlach/java/barlach_cm_huge_A_ema_q.tf");
//        ISNetwork net = (ISNetwork) IO.load("/home/tobias/arbeit/Read/MegaNet/mega_vb_mvn_q.tf");
//        String toString = net.getCharMap().toString();
//        saveCharMap(net.getCharMap(), new File("abc.txt"));
//        StringIO.writeString("abc.txt", toString);
//        ISNetwork net = (ISNetwork) IO.load("meganet_usaddr_12_pp_crx.sprnn");
        net.setParamSet(net.getDefaultParamSet(new ParamSet()));
//        ISNetwork net = (ISNetwork) IO.load("snet_mega10.tf");
//        ISNetwork net = (ISNetwork) IO.load("snet_mega9.tf");
        net.init();
        System.out.println(net.getName());
        System.out.println(net.getUniqueId());
        ErrorModuleDynProg instance;
//        ArrayList<String> valList = IO.loadLineArrayList("./lists/mega_val_orig.lst");
//        ArrayList<String> valList = IO.loadLineArrayList("./lists/mega_val_22.lst");
        ArrayList<String> valList = IO.loadLineArrayList("/home/tobias/devel/projects/LicensePlate/lists/barlach_val_orig.lst");
//        ArrayList<String> valList = IO.loadLineArrayList("/home/tobias/devel/projects/Academic/lists/mega_val_orig.lst");
        long curT = System.currentTimeMillis();
        int Cnt_T = 0;
        int Cnt_Y = 0;

        int cnt = 0;
        for (String aPath : valList) {
//            System.out.println(aPath);
            HybridImage inImg = HybridImage.newInstance(aPath);
            addPoly(inImg, aPath);

            String trPath = aPath + ".txt";
            String tr = StringIO.readString(trPath);
            if (tr.equals("")) {
                continue;
            }
            tr = tr.replace("\n", "");
            instance = new ErrorModuleDynProg(new CostCalculatorDft(), new CategorizerCharacterDft(), new StringNormalizerDft(), Boolean.FALSE);
            net.setInput(inImg);
            net.update();
            ConfMat cm = new ConfMat(net.getConfMat());
            String bestPath = cm.getString(cm.getBestPath());

            instance.calculate(bestPath, tr);
            Map<Count, Long> map = instance.getCounter().getMap();
            long t = getValue(map, Count.GT);
            long y = getSumErrors(map);
            Cnt_T += t;
            Cnt_Y += y;
            double cer = ((double) Cnt_Y) / Cnt_T;
            System.out.println("CER: " + cer);
            double aCer = (double) y / t;
//            System.out.println(Cnt_T);
//            System.out.println(Cnt_Y);
            if (aCer > 0.25) {
                System.out.println(aPath);
                System.out.println(bestPath);
                System.out.println(tr);
                System.out.println(aCer);
            }
//            Logger.getLogger(BestPathTest.class.getName()).log(Logger.INFO, new StdFrameAppender.AppenderContent(inImg, bestPath));
//            Logger.getLogger(BestPathTest.class.getName()).log(Logger.INFO, new StdFrameAppender.AppenderContent(inImg, tr, true));
            cnt++;
        }

        long runT = System.currentTimeMillis() - curT;
        System.out.println(runT / valList.size());

    }

    private static long getValue(Map<Count, Long> map, Count val) {
        Long get = map.get(val);
        return get == null ? 0 : get;
    }

    private static long getSumErrors(Map<Count, Long> map) {
        return getValue(map, Count.INS) + getValue(map, Count.DEL) + getValue(map, Count.SUB);
    }

    private static void addPoly(HybridImage inImg, String aPath) {
        HashMap<String, Object> info = new HashMap<>();
        File infofile = new File(aPath + ".info");
        if (infofile.exists()) {
            try {
                List<String> list = StringIO.loadLineList(infofile);
                for (int i = 0; i < list.size() - 1; i += 2) {
                    String key = list.get(i);
                    String value = list.get(i + 1);
                    if (key.equals(ImagePropertyIDs.MASK.toString())) {
                        String[] split = value.split(":");
                        ArrayList<Polygon2DInt> mask = new ArrayList<>();
                        for (String stringPolygon : split) {
                            mask.add(PolygonHelper.fromString(stringPolygon));
                        }
                        inImg.setProperty(key, mask);
                        info.put(key, mask);
                    } else if (key.equals(ImagePropertyIDs.DEBUG_DESCR.toString())) {
                        inImg.setProperty(key, value);
                        info.put(key, value);
                    } else if (key.toUpperCase().equals(ImagePropertyIDs.TRAFO.toString())) {
                        info.put("trafo_orig", value);
                    } else {
                        info.put(key, value);
                    }
                }
            } catch (Throwable ex) {
            }
        }
    }

    public static void saveCharMap(CharMap<Integer> cm, java.io.File outChar) {
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
            throw new RuntimeException("cannot save file '" + outChar.getAbsolutePath() + "'.", ex);
        }
    }

}
