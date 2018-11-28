/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.planet.imaging.types.HybridImage;
import de.planet.imaging.types.StdFrameAppender;
import de.planet.imaging.util.GraphicalMonPanelFrame;
import de.planet.imaging.util.StdGraphicalMonPanelFrame;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.util.PolygonUtil;
import de.uros.citlab.errorrate.types.KWS;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author gundram
 */
public class ShowKwsResult extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ShowKwsResult.class.getName());

    @ParamAnnotation(descr = "path to result file")
    private String i = "";
    @ParamAnnotation(descr = "group by keyword-page (k), page-keyword (patternChoose) or no grouping (n) ")
    private String group = "n";

    public ShowKwsResult() {
        addReflection(this, ShowKwsResult.class);
    }

    public void run() throws InterruptedException {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
        KWS.Result fromJson = null;
        StdGraphicalMonPanelFrame fr = new StdGraphicalMonPanelFrame(GraphicalMonPanelFrame.PANEL_MODE_RESULT);
        fr.enable(true);
        try {
//            List<KeyWord> fromJson = gson.fromJson(new FileReader(i), (new LinkedList<KeyWord>()).getClass());
            fromJson = gson.fromJson(new FileReader(i), KWS.Result.class);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("cannot load file '" + i + "'", ex);
        }
        switch (group) {
            case "n":
                for (KWS.Word keyWord : fromJson.getKeywords()) {
                    System.out.println("keyword: " + keyWord.getKeyWord());
                    List<KWS.Entry> pos = keyWord.getPos();
                    pos.sort(new Comparator<KWS.Entry>() {
                        @Override
                        public int compare(KWS.Entry o1, KWS.Entry o2) {
                            return Double.compare(o2.getConf(), o1.getConf());
                        }
                    });
                    int cnt = 0;
                    boolean imageAvail = false;
                    for (KWS.Entry entry : pos) {
                        File image = new File(entry.getImage());
                        Polygon2DInt baseline = PolygonUtil.string2Polygon2DInt(entry.getBl());
//                        LOG.log(Logger.INFO, new StdFrameAppender.AppenderContent(HybridImage.newInstance(image), String.format("%s:\"%s\":%.2f", image.getName(), keyWord.getKeyWord(), -Math.log(entry.getConf())), Arrays.asList(baseline)));
                        fr.addImage(HybridImage.newInstance(image), String.format("%s:\"%s\":%.2f", image.getName(), keyWord.getKeyWord(), -Math.log(entry.getConf())), Arrays.asList(baseline), String.format("%s:\"%s\":%.3f", image.getName(), keyWord.getKeyWord(), entry.getConf()));
                        imageAvail = true;
                        if (++cnt % 10 == 0) {
                            fr.next();
                            imageAvail = false;
//                            LOG.log(Logger.INFO, new StdFrameAppender.AppenderContent(true));
                        }
                    }
                    if (imageAvail) {
                        fr.next();
                    }
                }
                break;
            case "k":
                for (KWS.Word keyWord : fromJson.getKeywords()) {
                    System.out.println("keyword: " + keyWord.getKeyWord());
                    List<KWS.Entry> pos = keyWord.getPos();
                    LOG.log(Logger.INFO, "found " + pos.size() + " keywords");
                    if (pos.isEmpty()) {
                        continue;
                    }
                    Collections.sort(pos, new Comparator<KWS.Entry>() {
                        @Override
                        public int compare(KWS.Entry o1, KWS.Entry o2) {
                            int res = o1.getImage().compareTo(o2.getImage());
                            if (res != 0) {
                                return res;
                            }
                            return Double.compare(o1.getBaseLineKeyword().getBounds().getCenterX(), o2.getBaseLineKeyword().getBounds().getCenterX());
                        }
                    });
                    pos.add(null);
                    String image = null;
                    List<Polygon2DInt> polygons = new LinkedList<>();
                    String confidences = "";
                    for (KWS.Entry entry : pos) {
                        if (entry == null || !entry.getImage().equals(image)) {
                            if (image != null) {
                                fr.addImage(HybridImage.newInstance(image), String.format("%s:\"%s\":%s", new File(image).getName(), keyWord.getKeyWord(), confidences.trim()), polygons);
                                polygons = new LinkedList<>();
                                confidences = "";
                            }
                            if (entry == null) {
                                break;
                            }
                            image = entry.getImage();
                        }
                        polygons.add(PolygonUtil.string2Polygon2DInt(entry.getBl()));
                        confidences += " " + String.format("%.3f", entry.getConf());
                    }
                    fr.next();
                }
                break;
            case "patternChoose":
                List<Entry2> kws = new LinkedList<>();
                for (KWS.Word keyWord : fromJson.getKeywords()) {
                    for (KWS.Entry po : keyWord.getPos()) {
                        kws.add(new Entry2(keyWord.getKeyWord(), po));
                    }
                }
                Collections.sort(kws, new Comparator<Entry2>() {
                    @Override
                    public int compare(Entry2 o1, Entry2 o2) {
                        int res = o1.entry.getImage().compareTo(o2.entry.getImage());
                        if (res != 0) {
                            return res;
                        }
                        int res2 = Double.compare(o1.entry.getBaseLineKeyword().getBounds().getCenterX(), o2.entry.getBaseLineKeyword().getBounds().getCenterX());
                        if (res2 != 0) {
                            return res2;
                        }
                        return o1.keyword.compareTo(o2.keyword);
                    }
                });
                HashMap<String, List<Entry2>> map = new HashMap<>();
                for (Entry2 entry : kws) {
                    List<Entry2> get = map.get(entry.entry.getImage());
                    if (get == null) {
                        get = new LinkedList<>();
                        map.put(entry.entry.getImage(), get);
                    }
                    get.add(entry);
                }
                for (String key : map.keySet()) {
                    List<Entry2> matches = map.get(key);
//                    matches.add(null);
                    List<Polygon2DInt> polygons = new LinkedList<>();
//                    String confidences = "";
                    String lastImage = null;
//                    ObjectCounter<String> keywords = new ObjectCounter<>();
                    StringBuilder sb = new StringBuilder();
                    HybridImage img = null;
                    for (Entry2 entry : matches) {
                        Polygon2DInt string2Polygon2DInt = PolygonUtil.string2Polygon2DInt(entry.entry.getBl());
                        polygons.add(PolygonUtil.string2Polygon2DInt(entry.entry.getBl()));
                        if (!entry.entry.getImage().equals(lastImage)) {
                            lastImage = entry.entry.getImage();
                            img = HybridImage.newInstance(lastImage);
                        }
                        sb.append("'").append(entry.keyword).append("' ");
                        fr.addImage(img, String.format("'%s': %.3f", entry.keyword, entry.entry.getConf()), Arrays.asList(string2Polygon2DInt));
                    }
                    fr.addImage(img, sb.toString(), polygons);
                    fr.next();
                }
                break;
            default:
                throw new RuntimeException("unknown group-mode '" + group + "'");

        }
        fr.dispose();

    }

    private static class Entry2 {

        public String keyword;
        public KWS.Entry entry;

        public Entry2(String keyword, KWS.Entry entry) {
            this.keyword = keyword;
            this.entry = entry;
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidParameterException, InterruptedException {
        ArgumentLine al = new ArgumentLine();
        al.addArgument("i", HomeDir.getFile(String.format("out_%02d.json", 20)));
//        al.addArgument("group", "k");
//        al.setHelp();
        args = al.getArgs();
        ShowKwsResult instance = new ShowKwsResult();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
