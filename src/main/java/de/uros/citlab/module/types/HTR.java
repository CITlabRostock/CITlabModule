/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.types;

import com.achteck.misc.types.CharMap;
import com.achteck.misc.types.ConfMat;
import com.achteck.misc.util.IO;
import de.planet.citech.types.IDecodingType;
import de.planet.imaging.types.HybridImage;
import de.planet.itrtech.reco.ISNetwork;
import de.planet.langmod.LangModFullText;
import de.planet.langmod.types.ILangMod;
import de.planet.langmod.types.ILangMod.ILangModResult;
import de.uros.citlab.module.kws.ConfMatContainer;
import de.uros.citlab.module.util.GroupUtil;
import de.uros.citlab.module.util.IOUtil;
import de.uros.citlab.module.util.LangModConfigurator;
import de.uros.citlab.module.util.PropertyUtil;
import eu.transkribus.core.model.beans.customtags.CustomTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @author gundram
 */
public class HTR {

    public static Logger LOG = LoggerFactory.getLogger(HTR.class);
    private final ISNetwork htrImpl;
    private ILangMod lmImpl;
    private final String om, lm, cm;
    private String name = null;

    private File storageFile;
    private boolean loadFromStorage;
    private boolean saveToFile;
    private ConfMatContainer cmc;
    private String[] props;

    public HTR(String om, String lm, String cm) {
        this(null, null, om, lm, cm, null);
    }

    public HTR(ISNetwork htrImpl, ILangMod lmImpl, String om, String lm, String cm, String[] props) {
        this.htrImpl = htrImpl;
        this.lmImpl = lmImpl;
        this.om = om;
        this.lm = lm;
        this.cm = cm;
        this.props = props;
    }

    public void setStorageFile(File storageFile) {
        if (storageFile == null) {
            loadFromStorage = false;
            cmc = null;
            return;
        }
        saveToFile = storageFile != null;
        this.storageFile = storageFile;
        loadFromStorage = storageFile.exists();
        if (loadFromStorage) {
            cmc = (ConfMatContainer) IOUtil.load(storageFile);
            LOG.debug("use stored ConfMats");
        } else {
            cmc = new ConfMatContainer();
        }
    }

    public String getName() {
        if (name == null) {
            StringBuilder sb = new StringBuilder();
            if (PropertyUtil.hasProperty(props, Key.TRANSKRIBUS_HTR_ID) || PropertyUtil.hasProperty(props, Key.TRANSKRIBUS_HTR_NAME)) {
                String htrName = PropertyUtil.getProperty(props, Key.TRANSKRIBUS_HTR_NAME);
                String htrID = PropertyUtil.getProperty(props, Key.TRANSKRIBUS_HTR_ID);
                if (htrName != null) {
                    sb.append(htrName);
                    if (htrID != null) {
                        sb.append("(").append(Key.TRANSKRIBUS_HTR_ID).append("=").append(htrID).append(")");
                    }
                } else {
                    sb.append(Key.TRANSKRIBUS_HTR_ID).append("=").append(htrID);
                }
            } else {
                sb.append(new File(om).getName());
            }
            sb.append(File.pathSeparator);
            if (lm != null && !lm.isEmpty()) {
                sb.append(new File(lm).getName());
            }
            sb.append(File.pathSeparator);
            if (cm != null && !cm.isEmpty()) {
                sb.append(new File(cm).getName());
            }
            sb.append(File.pathSeparator);
            name = sb.toString();
        }
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.om);
        hash = 17 * hash + Objects.hashCode(this.lm);
        hash = 17 * hash + Objects.hashCode(this.cm);
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
            return false;
        }
        final HTR other = (HTR) obj;
        if (!Objects.equals(this.om, other.om)) {
            return false;
        }
        if (!Objects.equals(this.lm, other.lm)) {
            return false;
        }
        if (!Objects.equals(this.cm, other.cm)) {
            return false;
        }
        return true;
    }

    //    protected Pair<String, ConfMat> getText(ConfMat confMat, String[] props) {
//        if (PropertyUtil.isPropertyTrue(props, Key.RAW) || lmImpl == null) {
//            return new Pair<>(confMat.toString(), confMat);
//        }
//        lmImpl.setConfMat(confMat);
//        ILangMod.ILangModResult result = lmImpl.getResult();
//        if (result == null) {
//            LOG.log(Logger.ERROR, "no result returned from langMod.");
//            throw new RuntimeException("no result returned from langMod.");
//        }
//        return new Pair<>(result.getText(), confMat);
//    }
    public Result getText(LineImage li, String[] props) {
        ConfMat confMat = getConfMat(li, props);
        if (PropertyUtil.isPropertyTrue(props, Key.RAW) || lmImpl == null) {
            return new Result(confMat.toString(), confMat);
        }
//        ConfMatUtil.getProbMat(confMat).save(new File("debug_res", li.getTextLine().getId() + "_cm.png").getAbsolutePath());
//        ConfMatUtil.saveProbMat(new File("debug_res", li.getTextLine().getId() + "_pm.txt").getAbsolutePath(),confMat,"%.4e");
        lmImpl.setConfMat(confMat);
        ILangModResult result = lmImpl.getResult();
        return new Result(result.getText(), confMat);
    }

    public static class Result {

        private String text;
        private List<CustomTag> tags;
        private ConfMat confMat;

        public Result(String text, ConfMat confMat) {
            this(text, null, confMat);
        }

        public Result(String text, List<CustomTag> tags, ConfMat confMat) {
            this.text = text;
            this.tags = tags;
            this.confMat = confMat;
        }

        public String getText() {
            return text;
        }

        public List<CustomTag> getTags() {
            return tags;
        }

        public ConfMat getConfMat() {
            return confMat;
        }

    }

    public static class ConfMatPart {

        private ConfMat cm;
        private int start;
        private int end;

        public ConfMatPart(ConfMat cm, int start, int end) {
            this.cm = cm;
            this.start = start;
            this.end = end;
        }

        public CharMap<Integer> getCharMap() {
            return cm.getCharMap();
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public ConfMat getConfMat() {
            return cm;
        }

        @Override
        public String toString() {
            return cm.getString(cm.getBestPathSubstr(start, end));
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 37 * hash + Objects.hashCode(this.cm);
            hash = 37 * hash + this.start;
            hash = 37 * hash + this.end;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ConfMatPart) {
                ConfMatPart cmPart = (ConfMatPart) obj;
                return getConfMat() == cmPart.getConfMat() && getStart() == cmPart.getStart() && getEnd() == cmPart.getEnd();
            }
            return false;
        }
    }

    public List<ConfMatPart> getCofMatParts(ConfMat cm, String[] props, final ILangMod.ILangModGroup.Label... except) {
        if (lmImpl == null) {
            LangModFullText lmFT = new LangModFullText();
            lmFT.setParamSet(lmFT.getDefaultParamSet(null));
            lmFT.init();
            LangModConfigurator lmConfig = new LangModConfigurator(null, cm.getCharMap(), null);
            try {
                lmConfig.initLangMod(lmFT);
            } catch (LangModConfigurator.EmptyDictException ex) {
                throw new RuntimeException(ex);
            }
            lmImpl = lmFT;
        }
        lmImpl.setConfMat(cm);
        ILangMod.ILangModResult result = lmImpl.getResult();
        List<ILangMod.ILangModLeaf> list = new LinkedList<>();
        for (ILangMod.ILangModGroup group : result.getGroups()) {
            group.fillLeafList(list);
        }
        List<ConfMatPart> grouping;
        grouping = GroupUtil.getGrouping(list, new GroupUtil.Joiner<ILangMod.ILangModLeaf>() {
            @Override
            public boolean isGroup(List<ILangMod.ILangModLeaf> group, ILangMod.ILangModLeaf element) {
                return keepElement(element);
            }

            @Override
            public boolean keepElement(ILangMod.ILangModLeaf element) {
                for (IDecodingType.Label label : except) {
                    if (element.getLabel().equals(label)) {
                        return false;
                    }
                }
                return true;
            }
        }, new GroupUtil.Mapper<ILangMod.ILangModLeaf, ConfMatPart>() {
            @Override
            public ConfMatPart map(List<ILangMod.ILangModLeaf> elements) {
                ILangMod.ILangModLeaf last = elements.get(elements.size() - 1);
                return new ConfMatPart(last.getConfMat(), elements.get(0).getStart(), last.getEnd());
            }
        });
        return grouping;
    }

    public ConfMat getConfMat(LineImage lineImage, String[] props) {
        ConfMat res = null;
        if (loadFromStorage) {
            res = cmc.getConfMat(lineImage.getTextLine());
        }
        if (res == null) {
            HybridImage subImage = lineImage.getSubImage();
            if (subImage.getHeight() * subImage.getWidth() < 200 || subImage.getHeight() < 10 || subImage.getWidth() < 10) {
                throw new RuntimeException("image is too small with hxw=" + subImage.getHeight() + "x" + subImage.getWidth());
            }
            htrImpl.setInput(subImage);
//            IImagePreProcess preproc = ((SNetworkTF) htrImpl).getPreproc();
//            File folderOut = new File("debug_res");
//            folderOut.mkdir();
//            subImage.save(new File(folderOut, lineImage.getTextLine().getId() + ".png").getAbsolutePath());
//            preproc.preProcess(subImage).save(new File(folderOut, lineImage.getTextLine().getId() + "_pp.png").getAbsolutePath());
            htrImpl.update();
            subImage.clear();
            res = htrImpl.getConfMat();
//            if (LOG.isDebugEnabled()) {
//            LOG.error("bestpath is '" + res.getBestPath().replace(ConfMat.NaC, '*') + "'");
//            }
            if (cmc != null) {
                cmc.add(res, lineImage.getTextLine());
                saveToFile = true;
            }
        }
        return res;

    }

    public ISNetwork getNetwork() {
        return htrImpl;
    }

    public void finalizePage() {
        if (saveToFile) {
            try {
                IO.save(cmc, storageFile);
            } catch (IOException ex) {
                throw new RuntimeException("cannot save container to file " + storageFile.getPath(), ex);
            }
        }
    }
//        private IDictOccurrence dict;
}
