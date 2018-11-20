/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.planet.imaging.types.HybridImage;
import de.planet.imaging.util.GraphicalMonPanelFrame;
import de.planet.imaging.util.StdGraphicalMonPanelFrame;
import de.uros.citlab.module.util.GroupUtil;
import de.uros.citlab.errorrate.KwsError;
import de.uros.citlab.errorrate.kws.KWSEvaluationMeasure;
import de.uros.citlab.errorrate.kws.measures.IRankingMeasure;
import de.uros.citlab.errorrate.types.KWS;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.util.Pair;

/**
 *
 * @author gundram
 */
public class EvalKws extends ParamTreeOrganizer {

    @ParamAnnotation(descr = "display PR-Curve")
    private boolean d;

    @ParamAnnotation(descr = "path to groundtruth file")
    private String gt;

    @ParamAnnotation(descr = "path to result file")
    private String hyp;

    @ParamAnnotation(descr = "display result and errors (P=page-results, FP, FN)")
    private String show;

    @ParamAnnotation(descr = "save pr-curve to given path")
    private String save;

    public EvalKws(boolean d, String gt, String hyp, String show, String save) {
        this.d = d;
        this.gt = gt;
        this.hyp = hyp;
        this.show = show;
        this.save = save;
        addReflection(this, EvalKws.class);
    }

    public EvalKws() {
        this(false, "", "", "", "");
    }

    @Override
    public void init() {
        super.init();
        show = show.toUpperCase();
    }

    public Map<IRankingMeasure.Measure, Double> run() throws InvalidParameterException, IOException, ClassNotFoundException {
        final List<KWS.MatchList> matchLists = new LinkedList<>();

        KwsError erp = new KwsError();
        KWSEvaluationMeasure evaluationMeasure = erp.getEvaluationMeasure();
        evaluationMeasure.setMatchObserver(new KWSEvaluationMeasure.MatchObserver() {

            @Override
            public void evalMatch(KWS.MatchList list) {
                matchLists.add(list);
            }
        });
        StringBuilder sb = new StringBuilder();
        if (hyp.isEmpty()) {
            throw new RuntimeException("no parameter hyp given.");
        }
        if (gt.isEmpty() && !show.toUpperCase().equals("P")) {
            throw new RuntimeException("no parameter gt given.");
        }
        if (d) {
            sb.append("-d ");
        }
        if (!save.isEmpty()) {
            sb.append("-s ").append(save).append(" ");
        }
        sb.append(this.hyp).append(" ");
        sb.append(this.gt).append(" ");
        Map<IRankingMeasure.Measure, Double> res = erp.run(sb.toString().trim().split(" "));
        for (IRankingMeasure.Measure measure : res.keySet()) {
            System.out.println(measure + " = " + res.get(measure));
        }

        List<KWS.Match> matches = new LinkedList<>();
        for (KWS.MatchList matchList : matchLists) {
            matches.addAll(matchList.matches);
        }
        if (show.isEmpty()) {
            return res;
        }
        final StdGraphicalMonPanelFrame fr = new StdGraphicalMonPanelFrame(GraphicalMonPanelFrame.PANEL_MODE_RESULT);
        fr.enable(true);

        switch (show) {
            case "P":
                matches.sort(new Comparator<KWS.Match>() {
                    @Override
                    public int compare(KWS.Match o1, KWS.Match o2) {
                        return o1.getPageId().compareTo(o2.getPageId());
                    }
                });
                List<Pair<BufferedImage, String>> grouping = GroupUtil.getGrouping(matches, new GroupUtil.Joiner<KWS.Match>() {
                    @Override
                    public boolean isGroup(List<KWS.Match> group, KWS.Match element) {
                        if (!group.isEmpty()) {
                            return group.get(0).getPageId().equals(element.getPageId());
                        }
                        return false;
                    }

                    @Override
                    public boolean keepElement(KWS.Match element) {
                        return true;
                    }
                }, new GroupUtil.Mapper<KWS.Match, Pair<BufferedImage, String>>() {
                    @Override
                    public Pair<BufferedImage, String> map(List<KWS.Match> elements) {
                        HybridImage hi = HybridImage.newInstance(elements.get(0).getPageId());
                        BufferedImage img = new BufferedImage(hi.getWidth(), hi.getHeight(), BufferedImage.TYPE_INT_RGB);
                        Graphics2D graphics = (Graphics2D) img.getGraphics();
                        graphics.drawImage(hi.getAsBufferedImage(), 0, 0, null);
                        graphics.setStroke(new BasicStroke(5));
                        graphics.setFont(new Font("Arial", 0, 25));
                        for (KWS.Match el : elements) {
                            if (el.getGT() != null) {
                                graphics.setColor(new Color(255, 255, 0, 120));
                                graphics.drawPolyline(el.getGT().xpoints, el.getGT().ypoints, el.getGT().npoints);
                            }
                            switch (el.type) {
                                case FALSE_NEGATIVE:
                                    graphics.setColor(new Color(0, 0, 255));
                                    break;
                                case FALSE_POSITIVE:
                                    graphics.setColor(new Color(255, 0, 0));
                                    break;
                                case TRUE_POSITIVE:
                                    graphics.setColor(new Color(0, 255, 0));
                                    break;
                            }
                            Polygon p = el.getKeyword();
                            graphics.drawPolyline(p.xpoints, p.ypoints, p.npoints);
                            graphics.setColor(new Color(0, 0, 0, 170));
                            graphics.drawString(String.format("%s %.3f", el.getHyp(), el.conf < 0 ? 0 : el.conf), p.xpoints[0], p.ypoints[0] + 25);
                        }
                        return new Pair<>(img, elements.get(0).getPageId());
                    }
                });
                for (Pair<BufferedImage, String> bufferedImage : grouping) {
                    fr.addImage(HybridImage.newInstance(bufferedImage.getFirst()), bufferedImage.getSecond());
                    fr.next();
                }
                break;
            case "FP":
            case "FN":
                KWS.Type t = show.equals("FN") ? KWS.Type.FALSE_NEGATIVE : KWS.Type.FALSE_POSITIVE;
                matches.sort(new Comparator<KWS.Match>() {
                    @Override
                    public int compare(KWS.Match o1, KWS.Match o2) {
                        return Double.compare(o2.getConf(), o1.getConf());
                    }
                });
                for (KWS.Match el : matches) {
                    if (el.type != t) {
                        continue;
                    }

                    HybridImage hi = HybridImage.newInstance(el.getPageId());
                    BufferedImage img = new BufferedImage(hi.getWidth(), hi.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = (Graphics2D) img.getGraphics();
                    graphics.drawImage(hi.getAsBufferedImage(), 0, 0, null);
                    graphics.setStroke(new BasicStroke(5));
                    graphics.setFont(new Font("Arial", 0, 25));
                    if (el.getGT() != null) {
                        graphics.setColor(new Color(255, 255, 0, 120));
                        graphics.drawPolyline(el.getGT().xpoints, el.getGT().ypoints, el.getGT().npoints);
                    }
                    graphics.setColor(new Color(255, 0, 0));
                    Polygon p = el.getKeyword();
                    graphics.drawPolyline(p.xpoints, p.ypoints, p.npoints);
                    graphics.setColor(new Color(0, 0, 0, 170));
                    graphics.drawString(String.format("%s %.3f", el.getHyp(), el.conf < 0 ? 0 : el.conf), p.xpoints[0], p.ypoints[0] + 25);
                    fr.addImage(HybridImage.newInstance(img), el.toString());
                    fr.next();
                }

        }
        fr.dispose();
        return res;
    }

    public static void main(String[] args) throws InvalidParameterException, ClassNotFoundException, Exception {
//        String htr = "dft_excl_10";
//            String htr = "dft_excl_20";
//            String htr = "dft_excl_40";
//            String htr = "dft_excl_80";
//            String htr = "dft_excl_160";
//            String htr = null;
        if (args.length == 0) {
            String htr = "dft_320";
            args = (""
                    + "-show fn "
                    + "-d true "
                    + "-hyp " + HomeDir.getFile("results/" + htr + "/out.json").toString() + " "
                    + "-gt " + HomeDir.getFile("result_all.json").toString() + " "
                    + "").trim().split(" ");
        }

        EvalKws instance = new EvalKws();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
