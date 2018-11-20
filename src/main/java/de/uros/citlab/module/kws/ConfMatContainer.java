/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.kws;

import com.achteck.misc.log.Logger;
import com.achteck.misc.types.CharMap;
import com.achteck.misc.types.ConfMat;
import com.achteck.misc.util.IO;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.IOUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.util.PolygonUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.util.PageXmlUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.util.Pair;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author gundram
 */
public class ConfMatContainer implements Serializable {

    public static final String CONFMAT_CONTAINER_FILENAME = "container.bin";
    private static final long serialVersionUID = 3921097241003276637L;
    private transient List<ConfMat> confmatList = new LinkedList<>();
    private transient HashMap<ConfMat, Polygon2DInt> baselines = new HashMap<>();
    private transient HashMap<ConfMat, String> lines = new HashMap<>();
    private transient HashMap<Polygon2DInt, ConfMat> confMats = new HashMap<>();
//    private String[] keys;
    private short[][][] mats;
    private short[][] bls;
    private String[] lineIDs;
    private transient boolean isStremable = true;
    public transient static Logger LOG = Logger.getLogger(ConfMatContainer.class.getName());
    private CharMap<Integer> cm;

    public static ConfMatContainer newInstance(File dirStorage) {
        return newInstance(dirStorage, null);
    }

    /**
     * use instance without fileImage
     *
     * @param dirStorage
     * @param fileImage
     * @return
     * @deprecated
     */
    @Deprecated
    public static ConfMatContainer newInstance(File dirStorage, File fileImage) {
        File storageFile = new File(dirStorage, CONFMAT_CONTAINER_FILENAME);
        if (storageFile.exists()) { //Version 1: one simple container
                return (ConfMatContainer) IOUtil.load(storageFile);
        } else if (dirStorage.exists() && dirStorage.isFile()) {
                return (ConfMatContainer) IOUtil.load(dirStorage);
        } else { //Version 0: many small files: Will be transformed to version 1
            ConfMatContainer cmc = new ConfMatContainer();
            try {
                File xmlPath = PageXmlUtil.getXmlPath(fileImage);
                PcGtsType page = PageXmlUtil.unmarshal(xmlPath);
                List<File> listFiles = FileUtil.listFiles(dirStorage, "cm".split(" "), true);
                for (File fileCM : listFiles) {
                    ConfMat confMat = (ConfMat) IOUtil.load(fileCM);
                    String name = fileCM.getName();
                    name = name.substring(0, name.lastIndexOf("."));
                    String[] ids = name.split("_");
                    String id_region = ids[ids.length - 2];
                    String id_line = ids[ids.length - 1];
                    TextRegionType region = null;
                    {
                        List<TextRegionType> textRegions = PageXmlUtils.getTextRegions(page);
                        for (TextRegionType textRegion : textRegions) {
                            if (textRegion.getId().equals(id_region)) {
                                region = textRegion;
                                break;
                            }
                        }
                    }
                    if (region == null) {
                        throw new RuntimeException("cannot find region " + ids[0] + " in document " + fileImage.getPath());
                    }
                    TextLineType tlt = null;
                    {
                        for (TextLineType textLineType : region.getTextLine()) {
                            if (textLineType.getId().equals(id_line)) {
                                tlt = textLineType;
                                break;
                            }
                        }
                    }
                    if (tlt == null) {
                        throw new RuntimeException("cannot find line " + ids[1] + " in document " + fileImage.getPath());
                    }
                    cmc.add(confMat, tlt);
                }
                IO.save(cmc, storageFile);
                File[] deleteFiles = dirStorage.listFiles();
                for (File deleteFile : deleteFiles) {
                    if (!deleteFile.equals(storageFile)) {
                        if (!FileUtils.deleteQuietly(deleteFile)) {
                            LOG.log(Logger.WARN, "cannot delete file " + deleteFile.getAbsolutePath());
                        }
                    }
                }
                return cmc;
            } catch (Throwable ex) {
                LOG.log(Logger.WARN, "cannot load confmats - either because of bug of not-existing confmats");
            }
        }
        return new ConfMatContainer();

    }

    public void add(ConfMat cm, TextLineType tlt) {
        Polygon2DInt bl = PolygonUtil.convert(PolygonUtil.getBaseline(tlt));
        isStremable = false;
        if (confmatList.isEmpty()) {
            this.cm = cm.getCharMap();
        }
        confmatList.add(cm);
        baselines.put(cm, bl);
        lines.put(cm, tlt.getId());
        confMats.put(bl, cm);
    }

    /**
     * returns a copy of the list
     *
     * @return
     */
    public List<ConfMat> getConfmats() {
        return new LinkedList<>(confmatList);
    }

    public void substituteConfmat(ConfMat oldConfmat, ConfMat newConfmat) {
        isStremable = false;
        int indexOf = confmatList.indexOf(oldConfmat);
        if (indexOf < 0) {
            throw new RuntimeException("cannot find confmat in container");
        }
        confmatList.set(indexOf, newConfmat);
        if (lines != null) {
            String removeLine = lines.remove(oldConfmat);
            lines.put(newConfmat, removeLine);
        }
        if (baselines != null) {
            Polygon2DInt removePolygon = baselines.remove(oldConfmat);
            baselines.put(newConfmat, removePolygon);
            if (removePolygon != null) {
                confMats.remove(removePolygon);
                confMats.put(removePolygon, newConfmat);
            }
        }
    }

    public ConfMat getConfMat(TextLineType tlt) {
        restoreBaselinesAndLineIDs();
        return confMats.get(PolygonUtil.convert(PolygonUtil.getBaseline(tlt)));
    }

    public String getLineID(ConfMat cm) {
        restoreBaselinesAndLineIDs();
        String res = lines.get(cm);
        if (res != null) {
            return res;
        }
        if (lineIDs == null) {
            return null;
        }
        int index = confmatList.indexOf(cm);
        if (index < 0) {
            throw new NullPointerException("confmat not in this container");
        }
        res = lineIDs[index];
        lines.put(cm, res);
        return res;
    }

    public Polygon2DInt getBaseline(ConfMat cm) {
        restoreBaselinesAndLineIDs();
        Polygon2DInt res = baselines.get(cm);
        if (res != null) {
            return res;
        }
        int index = confmatList.indexOf(cm);
        if (index < 0) {
            throw new NullPointerException("confmat not in this container");
        }
        res = fromStream(bls[index]);
        baselines.put(cm, res);
        return res;
    }

    public Pair<Polygon2DInt, String> remove(ConfMat cm) {
        isStremable = false;
        if (!confmatList.remove(cm)) {
            throw new RuntimeException("cannot delete confmat from container - is not available");
        }
        String removeLine = lines.remove(cm);
        Polygon2DInt removePolygon = baselines.remove(cm);
        if (removePolygon != null) {
            confMats.remove(removePolygon);
        }
        return new Pair<>(removePolygon, removeLine);
    }

    //NIIIIIICCCCHHHHH LÖÖÖÖSCHEN!!!!!!!!!
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        //Nicht Löschen!
        in.defaultReadObject();
        //Nicht Löschen!
        restoreLoad();
        if (LOG == null) {
            LOG = Logger.getLogger(ConfMatContainer.class.getName());
        }
    }

    private short[] toStream(Polygon2DInt bl) {
        final int[] x = bl.xpoints;
        final int[] y = bl.ypoints;
        final int n = bl.npoints;
        final short[] res = new short[bl.npoints * 2];
        for (int i = 0; i < n; i++) {
            res[i] = (short) x[i];
            res[i + n] = (short) y[i];
        }
        return res;
    }

    private Polygon2DInt fromStream(short[] bl) {
        final int n = bl.length / 2;
        final int[] x = new int[n];
        final int[] y = new int[n];
        for (int i = 0; i < n; i++) {
            x[i] = bl[i];
            y[i] = bl[i + n];
        }
        return new Polygon2DInt(x, y);
    }

    private void prepareSave() {
        if (!isStremable) {
            mats = new short[confmatList.size()][][];
            bls = new short[confmatList.size()][];
            lineIDs = new String[confmatList.size()];
//            keys = new String[map.size()];
            int i = 0;
            for (int j = 0; j < confmatList.size(); j++) {
                ConfMat cm = confmatList.get(j);
                mats[i] = cm.getShortMat();
                bls[i] = toStream(baselines.get(cm));
                lineIDs[i] = lines.get(cm);
                i++;
            }
            isStremable = true;
        }
    }

    private void restoreBaselinesAndLineIDs() {
        if (baselines == null) {
            baselines = new LinkedHashMap<>();
            confMats = new LinkedHashMap<>();
            lines = new LinkedHashMap<>();
            for (ConfMat confMat : confmatList) {
                Polygon2DInt baseline = getBaseline(confMat);
                confMats.put(baseline, confMat);
            }
        }
    }

    public void freePhysStruct() {
        isStremable = false;
        mats = null;
    }

    private void restoreLoad() {
        if (confmatList == null) {
            confmatList = new LinkedList<>();
            if (mats == null) {
                mats = new short[0][][];
            }
            for (short[][] mat : mats) {
                ConfMat confMat = new ConfMat(cm);
                confMat.set(mat);
                confmatList.add(confMat);
            }
        }
        isStremable = true;
    }

    //NIIIIIICCCCHHHHH LÖÖÖÖSCHEN!!!!!!!!!
    private void writeObject(ObjectOutputStream out) throws IOException {
        //Nicht Löschen!
        prepareSave();
        out.defaultWriteObject();
//        paa.getParam(KEY_TRAININFO).setObject(traininfo);
        LOG.log(Logger.DEBUG, "Method 'writeObject' done");
        //Nicht Löschen!
//        init();
    }

}
