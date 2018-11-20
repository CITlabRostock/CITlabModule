/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import com.achteck.misc.log.Logger;
import de.planet.util.PathCalculatorExpanded;
import de.planet.util.PathCalculatorGraph;
import de.planet.util.gui.Display;
import de.planet.util.gui.PanelObject;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

/**
 *
 * @author gundram
 */
public class GuiUtil {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(GuiUtil.class.getName());

    public static class DynProgPanel<TypeA, TypeB> extends JPanel {

        public DynProgPanel(PathCalculatorExpanded.DistanceMat<TypeA, TypeB> dynProg) {
            super(new GridLayout(dynProg.getSizeY(), dynProg.getSizeX()));
            List<PathCalculatorExpanded.IDistance<TypeA, TypeB>> bestPath = dynProg.getBestPath();
            for (int i = 0; i < dynProg.getSizeY(); i++) {
                for (int j = 0; j < dynProg.getSizeX(); j++) {
                    PathCalculatorExpanded.IDistance element = dynProg.get(i, j);
                    JButton b = new JButtonDistance(element, bestPath.contains(element));
                    add(b);
                }
            }

        }
    }

    public static class DynProgPanelGraph<TypeA, TypeB> extends JPanel {

        public DynProgPanelGraph(PathCalculatorGraph.DistanceMat<TypeA, TypeB> dynProg) {
            super(new GridLayout(dynProg.getSizeY(), dynProg.getSizeX()));
            List<PathCalculatorGraph.IDistance<TypeA, TypeB>> bestPath = dynProg.getBestPath();
            for (int i = 0; i < dynProg.getSizeY(); i++) {
                for (int j = 0; j < dynProg.getSizeX(); j++) {
                    PathCalculatorGraph.IDistance element = dynProg.get(i, j);
                    JButton b = new JButtonDistanceGraph(element, bestPath.contains(element));
                    add(b);
                }
            }

        }
    }

    public static class JButtonDistance extends JButton implements ActionListener {

        private PathCalculatorExpanded.IDistance element;

        public JButtonDistance(PathCalculatorExpanded.IDistance element, boolean isBP) {
            this.element = element;
            if (element.getManipulation() != null) {
                setText(String.format("<html>" + element.getManipulation() + "<br/>%.3f<br/>%.3f</html>", element.getCosts(), element.getCostsAcc()));
                setBorder(new BevelBorder(isBP ? BevelBorder.RAISED : BevelBorder.LOWERED));
            } else {
                setText(String.format("<html>ROOT<br/>%.3f<br/>%.3f</html>", element.getCosts(), element.getCostsAcc()));
                setBorder(null);
            }
            setPreferredSize(new Dimension(80, 60));
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == this) {
//                ArrayList<ConfMat> recos = new ArrayList<>();
//                ArrayList<String> refs = new ArrayList<>();
//                {
//                    List recs = element.getRecos();
//                    if (recs != null) {
//                        for (Object reco : recs) {
//                            recos.add((ConfMat) reco);
//                        }
//                    }
//
//                    List rfs = element.getReferences();
//                    if (rfs != null) {
//                        for (Object r : rfs) {
//                            refs.add((String) r);
//                        }
//                    }
//                }

                PanelObject o = new PanelObject();
                JLabel lRef = new JLabel("REF:");
                JLabel lReco = new JLabel("REC:");
                for (Object reco : element.getRecos()) {
                    lReco.setText(lReco.getText() + reco.toString());
                }
                if (element.getRecos().isEmpty()) {
                    lReco.setText("<EMPTY>");
                }
                for (Object ref : element.getReferences()) {
                    lRef.setText(lRef.getText() + ref);
                }
                if (element.getReferences().isEmpty()) {
                    lRef.setText("<EMPTY>");
                }
                o.addBottom(lReco);
                o.addBottom(lRef);
//                if (ref != null) {
//                    o.addBottom(ref);
//                }
                if (element != null) {
                    String out = "<html>";
                    out += "<br/>costs=" + String.format("%.3f", element.getCosts()) + "<br/>";
                    out += "acc=" + String.format("%.3f", element.getCostsAcc()) + "<br/>";
                    out += "calculator=" + (element.getCostCalculator() == null ? "null" : element.getCostCalculator().getClass().getSimpleName());
                    out += "</html>";
                    o.addBottom(new JLabel(out));
                }
                Display.show(o, true);
            }
        }

    }

    public static class JButtonDistanceGraph extends JButton implements ActionListener {

        private PathCalculatorGraph.IDistance element;

        public JButtonDistanceGraph(PathCalculatorGraph.IDistance element, boolean isBP) {
            this.element = element;
            if (element == null) {
                setText(String.format("<html>SKIP<br/>%.3f<br/>%.3f</html>", Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
                setBorder(null);
            } else if (element.getManipulation() != null) {
                setText(String.format("<html>" + element.getManipulation() + "<br/>%.3f<br/>%.3f</html>", element.getCosts(), element.getCostsAcc()));
                setBorder(new BevelBorder(isBP ? BevelBorder.RAISED : BevelBorder.LOWERED));
            } else {
                setText(String.format("<html>ROOT<br/>%.3f<br/>%.3f</html>", element.getCosts(), element.getCostsAcc()));
                setBorder(null);
            }
            setPreferredSize(new Dimension(80, 60));
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == this) {
//                ArrayList<ConfMat> recos = new ArrayList<>();
//                ArrayList<String> refs = new ArrayList<>();
//                {
//                    List recs = element.getRecos();
//                    if (recs != null) {
//                        for (Object reco : recs) {
//                            recos.add((ConfMat) reco);
//                        }
//                    }
//
//                    List rfs = element.getReferences();
//                    if (rfs != null) {
//                        for (Object r : rfs) {
//                            refs.add((String) r);
//                        }
//                    }
//                }

                PanelObject o = new PanelObject();
                JLabel lRef = new JLabel("REF:");
                JLabel lReco = new JLabel("REC:");
                for (Object reco : element.getRecos()) {
                    lReco.setText(lReco.getText() + reco.toString());
                }
                if (element.getRecos().length == 0) {
                    lReco.setText("<EMPTY>");
                }
                for (Object ref : element.getReferences()) {
                    lRef.setText(lRef.getText() + ref.toString());
                }
                if (element.getReferences().length == 0) {
                    lRef.setText("<EMPTY>");
                }
                o.addBottom(lReco);
                o.addBottom(lRef);
//                if (ref != null) {
//                    o.addBottom(ref);
//                }
                if (element != null) {
                    String out = "<html>";
                    out += "<br/>costs=" + String.format("%.3f", element.getCosts()) + "<br/>";
                    out += "acc=" + String.format("%.3f", element.getCostsAcc()) + "<br/>";
//                    out += "calculator=" + (element.getCostCalculator() == null ? "null" : element.getCostCalculator().getClass().getSimpleName());
                    out += "</html>";
                    o.addBottom(new JLabel(out));
                }
                Display.show(o, true);
            }
        }

    }

}
