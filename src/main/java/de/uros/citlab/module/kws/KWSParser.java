/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.kws;

import com.achteck.misc.types.ConfMat;
import com.achteck.misc.util.IO;
import com.achteck.misc.util.StopWatch;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.planet.citech.types.ISortingFunction;
import de.planet.decoding.interfaces.IDecodingOccurrence;
import de.planet.decoding.interfaces.IDecodingResult;
import de.planet.decoding.shortmat.CTCbestPathShort;
import de.planet.math.geom2d.types.Polygon2DInt;
import de.planet.regexdecoding.OptPathStruct;
import de.planet.regexdecoding.RegexDecoder;
import de.planet.regexdecoding.util.RegExHelper;
import de.planet.util.sortfct.SortFunctionOOVPrior;
import de.uros.citlab.errorrate.types.KWS;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.BidiUtil;
import de.uros.citlab.module.util.MetadataUtil;
import de.uros.citlab.module.util.PolygonUtil;
import de.uros.citlab.module.util.PropertyUtil;
import eu.transkribus.interfaces.IKeywordSpotter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 *
 * @author gundram
 */
public class KWSParser extends Observable implements IKeywordSpotter {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(KWSParser.class.getName());
    private static final String version = "1.0";
//    private transient final HashMap<Integer, HTR> htrs = new HashMap<>();
//    private transient final HashMap<String, ISNetwork> networks = new HashMap<>();
    private String nameCurrent = "?";
//    @ParamAnnotation(descr = "maximal number of results")
//    private int maxNum = 1000;
//    private boolean toUpper = false;
//    private boolean escape = false;
//    private boolean expert = false;
    private boolean multiWords = false;
//    private double minConf = 0.02;
//    private int threads = 1;
    private RegexDecoder regexDec;
    private IDecodingOccurrence decoderOcc = new CTCbestPathShort();
//    @ParamAnnotation(name = "sf", member = "sortFunction")
    private String sfName = SortFunctionOOVPrior.class.getName();
    protected ISortingFunction sortFunction;

    private HashMap<ConfMat, String> confMatMap = null;
    private HashMap<ConfMat, ConfMatContainer> containerMap = null;
    private int storageRunner = 0;
    private int storageRunnerBefore = 0;
    private List<FindResult> workers;
    private ExecutorService executor;
    private Status status;

    @Override
    public void notifyObservers(Object arg) {
        LOG.debug("notify observers: {}", arg);
        super.setChanged();
        super.notifyObservers(arg);
    }

    @Override
    public String usage() {
        return "no usage written so far.";
    }

    @Override
    public String getToolName() {
        return "no name written so far.";
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getProvider() {
        return MetadataUtil.getProvider("Tobias Strauss", "tobias.strauss@uni-rostock.de");
    }

    @Override
    public String process(String[] imagesIn, String[] storageIn, String[] queriesIn, String dictIn, String[] props) {
        KWS.Result resultStruct = getResultStruct(imagesIn, storageIn, queriesIn, dictIn, props);
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
        return gson.toJson(resultStruct);

    }

    public Set<KWS.Word> setUpKeywords(String[] queriesIn, boolean upper, int maxAnz, double minConf) {
        Set<KWS.Word> keywords = new LinkedHashSet<>();
        for (int i = 0; i < queriesIn.length; i++) {
            String query = upper ? queriesIn[i].toUpperCase() : queriesIn[i];
            keywords.add(new KWS.Word(query, maxAnz, minConf));
        }
        return keywords;
    }

    public KWS.Result getResultStruct(String[] imagesIn, String[] storageIn, String[] queriesIn, String dictIn, String[] props) {
        StopWatch sw = new StopWatch();
        sw.start();
        status = new Status(queriesIn.length * storageIn.length);
        storageRunner = 0;
        if (dictIn != null && !dictIn.isEmpty()) {
            LOG.warn("dictionary '" + dictIn + "' has no influence.");
        }
        double minConf = Double.parseDouble(PropertyUtil.getProperty(props, Key.KWS_MIN_CONF, "0.05"));
        boolean upper = PropertyUtil.isPropertyTrue(props, Key.KWS_UPPER);
        boolean expert = PropertyUtil.isPropertyTrue(props, Key.KWS_EXPERT);
        int maxAnz = Integer.parseInt(PropertyUtil.getProperty(props, Key.KWS_MAX_ANZ, "-1"));
        boolean part = PropertyUtil.isPropertyTrue(props, Key.KWS_PART);
        int threads = Integer.valueOf(PropertyUtil.getProperty(props, Key.KWS_THREADS, "1"));
        long cacheSize = 200000 * Integer.valueOf(PropertyUtil.getProperty(props, Key.KWS_CACHE_SIZE, "1000"));
        if (expert && PropertyUtil.hasProperty(props, Key.KWS_PART)) {
            LOG.warn("expert modus activated but property {} is set to {}", Key.KWS_PART, PropertyUtil.getProperty(props, Key.KWS_PART));
            part = false;
        }
        Set<KWS.Word> res = setUpKeywords(queriesIn, upper, maxAnz, minConf);
        executor = Executors.newFixedThreadPool(threads);
        while (loadConfMats(imagesIn, storageIn, upper, cacheSize)) {
            workers = new LinkedList<>();
            for (int i = 0; i < threads; i++) {
                workers.add(new FindResult(expert, part));
            }
            int i = 0;
            for (KWS.Word word : res) {
                status.update(storageRunnerBefore * queriesIn.length + (storageRunner - storageRunnerBefore) * i);
                i++;
                StopWatch swKw = new StopWatch();
                swKw.start();
                getSearchResultMultiThread(word);
                swKw.stop();
                word.addTime(swKw.getCurrentMillis());
                LOG.debug("for kw {}/{} '{}' {} ms needed and filter-statistic {}.", i, queriesIn.length, word.getKeyWord(), swKw.getCurrentMillis(), word.getStatistic());
            }
        }
        executor.shutdown();
        sw.stop();
        LOG.debug("for " + queriesIn.length + " KWs " + String.format("%.0f", ((double) sw.getCumulatedMillis()) / queriesIn.length) + " ms in average.");
        return new KWS.Result(res, sw.getCumulatedMillis());
    }

    private void getSearchResultMultiThread(KWS.Word keyWord) {
        Iterator<ConfMat> iterator = confMatMap.keySet().iterator();
        for (FindResult worker : workers) {
            worker.setTask(iterator, keyWord, confMatMap, containerMap);
        }
        try {
            executor.invokeAll(workers);
        } catch (InterruptedException ex) {
            LOG.error("workers are interupted", ex);
        }
//        try {
//            executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
//        } catch (InterruptedException ex) {
//            throw new RuntimeException(ex);
//        }
        Collections.sort(keyWord.getPos());
    }

    private class FindResult implements Runnable, Callable<KWS.Word> {

        private Iterator<ConfMat> cm;
        private KWS.Word kw;
//        private RegexDecoder red = new RegexDecoder(true);
        private IDecodingOccurrence decOcc = new CTCbestPathShort();
//        private ISortingFunction sf;
        private HashMap<ConfMat, String> confMatMap;
        private HashMap<ConfMat, ConfMatContainer> containerMap;

        private final boolean isExpert;
        private final boolean isPart;
        private String regex = null;

        public FindResult(boolean isExpert, boolean isPart) {
            this.isExpert = isExpert;
            this.isPart = isPart;
//            sf = new SortFunctionOOVPrior();
//            sf.setParamSet(sf.getDefaultParamSet(null));
//            sf.init();
        }

        public void setTask(Iterator<ConfMat> cm, KWS.Word kw, HashMap<ConfMat, String> confMatMap, HashMap<ConfMat, ConfMatContainer> containerMap) {
            this.confMatMap = confMatMap;
            this.containerMap = containerMap;
            this.cm = cm;
            this.kw = kw;
            regex = null;
        }

        private synchronized ConfMat next(Iterator<ConfMat> cm) {
            if (cm.hasNext()) {
                return cm.next();
            }
            return null;
        }

        public String getRegex(ConfMat cm, String keyWord) {
            if (regex == null) {
                if (isExpert && keyWord.contains("(?<KW>")) {
                    regex = keyWord;
                } else {
                    String word = isExpert ? keyWord : RegExHelper.escapeControlChar(keyWord);
                    if (isPart) {
                        regex = ".*(?<KW>" + word + ").*";
                    } else {
                        Pattern compile = Pattern.compile("[\\pZ\\pP\\pS]");
                        StringBuilder sb = new StringBuilder();
                        for (Character value : cm.getCharMap().getValues()) {
                            if (compile.matcher(String.valueOf(value)).find()) {
                                sb.append(value);
                            }
                        }
                        String prePost = sb.toString();
                        if (prePost.contains("-")) {
                            prePost = prePost.replace("-", "") + "-";
                        }
                        prePost = prePost.replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]");
                        String pre = "(?<PRE>[" + prePost + "])";
                        String post = "(?<POST>[" + prePost + "])";
                        regex = "(.*" + pre + ")?(?<KW>" + word + ")(" + post + "(.*))?";
                    }
                }
            }
            return regex;
        }

        @Override
        public void run() {
            HashMap<Integer, RegexDecoder> charMapRegexMap = new LinkedHashMap<>();
//            CompiledRegEx compiledRegEx = red.compile(regEx);
//            final double threshOcc = thresh * 1.2;
            String keyWordLogical = kw.getKeyWord();
            String keyWordVisual = BidiUtil.logical2visual(keyWordLogical, BidiUtil.SOFT_LTR);
            if (!keyWordLogical.equals(keyWordVisual)) {
                LOG.info("BIDI-Algorithm change logical '{}' to visual '{}'", keyWordLogical, keyWordVisual);
            }
            final double leastcostScale = (double) keyWordVisual.length() + (isPart ? 0 : 2);
//            String escapedRegex = RegExHelper.escapeControlChar(keyWord);
            boolean doFilter = !isExpert;
            try {
                while (true) {
                    ConfMat confMat = next(cm);
                    double thresh = -Math.log(kw.getMinConf());
                    if (confMat == null) {
                        break;
                    }
                    kw.addCount("all");
                    if (doFilter) {
//                    LOG.log(Logger.INFO, "caluculate " + cm);
//                        double leastCostAbs = confMat.getLeastCosts(keyWord);
//                        double leastCostRel = leastCostAbs / keyWord.length();
                        if (confMat.getLeastCosts(keyWordVisual) >= thresh * leastcostScale) {
                            kw.addCount("leastcost");
                            continue;
                        }
                        decOcc.setInput(confMat);
                        IDecodingResult[] decRes = decOcc.getMatchCostsOcc(keyWordVisual, new IDecodingResult[1], 0, confMat.getLength(), thresh * leastcostScale);
                        IDecodingResult dr = decRes[0];
                        if (dr.getCostAbs() > thresh * leastcostScale) {
//                            if (dr.getCostAbs() >= thresh* leastcostScale) {
                            kw.addCount("occurance");
                            continue;
//                            }
                        }
                        if (isPart) {
                            String bestPath = confMat.getBestPath();
                            int start = dr.getStart() - 1;
                            char startChar = keyWordVisual.charAt(0);
                            while (start >= 0 && bestPath.charAt(start) == startChar) {
                                start--;
                            }
                            while (start >= 0 && bestPath.charAt(start) == ConfMat.NaC) {
                                start--;
                            }
                            start++;
                            char endChar = keyWordVisual.charAt(keyWordVisual.length() - 1);
                            int end = dr.getEnd();
                            while (end < bestPath.length() && bestPath.charAt(end) == endChar) {
                                end++;
                            }
                            while (end < bestPath.length() && bestPath.charAt(end) == ConfMat.NaC) {
                                end++;
                            }
//                            end--;
                            double costAbs = decOcc.getCostAbs(keyWordVisual, start, end, Double.MAX_VALUE);
                            double relCosts = costAbs / keyWordVisual.length();
                            if (relCosts <= thresh) {
                                KWS.Entry entry = getEntry(confMat, Math.exp(-relCosts), start, end);
                                kw.add(entry);
                                kw.addCount("found");
                            } else {
                                kw.addCount("occurance expand");
                            }
                            continue;
                        }
                    }
                    int hash = Arrays.hashCode(confMat.getCharMap().toShortArray());
                    RegexDecoder red = charMapRegexMap.get(hash);
                    if (red == null) {
                        red = new RegexDecoder(true);
                        red.setCachMode(RegexDecoder.CACHE_SOFT);
                        charMapRegexMap.put(hash, red);
                    }
                    red.setConfMat(confMat);
                    OptPathStruct bps = null;
                    try {
//                        Collection<OptPathStruct> optPathStructs = red.getOptPathStructs(getRegex(confMat));
//                        if (optPathStructs.size() > 1) {
//                            System.out.println("stop");
//                        }
                        bps = red.getOptPathStruct(getRegex(confMat, keyWordVisual));
                    } catch (Exception ex) {
                        LOG.warn("exception in regexdecoder for ConfMat: " + cm.toString() + "[" + confMatMap.get(confMat) + "] with regex: " + keyWordVisual + "  reason: " + ex.getMessage(), ex);
                        kw.addCount("regex exception");
                        continue;
                    }
                    if (bps == null) {
                        kw.addCount("regex null");
//                        LOG.log(Logger.DEBUG, "no results in regexdecoder for ConfMat: " + cm.toString() + "[" + confMatMap.get(confMat) + "] with regex: " + keyWord);
                        continue;
                    }
                    for (OptPathStruct.Group group : bps.getGroupsInclNaC("KW")) {
                        OptPathStruct.Group preGroup = null;
                        OptPathStruct.Group postGroup = null;
                        if (!isPart) {
                            List<OptPathStruct.Group> g1 = bps.getGroupsInclNaC("PRE");
                            for (OptPathStruct.Group small : g1) {
                                if (small.getStart() < group.getStart() && group.getStart() <= small.getEnd()) {
                                    preGroup = small;
                                    break;
                                }
                            }
                            List<OptPathStruct.Group> g2 = bps.getGroupsInclNaC("POST");
                            for (OptPathStruct.Group small : g2) {
                                if (small.getStart() <= group.getEnd() && group.getEnd() < small.getEnd()) {
                                    postGroup = small;
                                    break;
                                }
                            }
                        }
//                        if (preGroup != null && preGroup.getCostAbs() > 1 * thresh) {
//                            continue;
//                        }
//                        if (postGroup != null && postGroup.getCostAbs() > 1 * thresh) {
//                            continue;
//                        }
                        String bestString = group.getText();
                        if (!bestString.isEmpty()) {
                            double absCosts = group.getCostAbs();
                            int lenPost = postGroup != null ? postGroup.getText().length() : -1;
                            if (lenPost >= 0) {
                                absCosts += postGroup.getCostAbs();
                            }
                            int lenPre = preGroup != null ? preGroup.getText().length() : -1;
                            if (lenPre >= 0) {
                                absCosts += preGroup.getCostAbs();
                            }
                            double relCosts = absCosts / (bestString.length()
                                    + (preGroup != null ? Math.max(1, preGroup.getText().length()) : 0)
                                    + (postGroup != null ? Math.max(1, postGroup.getText().length()) : 0));
//                            double score = Math.exp(-1 * sf.getSortingCost(group));
//                            if (preGroup != null) {
//                                relCosts = Math.max(relCosts, preGroup.getCostAbs() / Math.max(1, lenPre));
//                            }
//                            if (postGroup != null) {
//                                relCosts = Math.max(relCosts, postGroup.getCostAbs() / Math.max(1, lenPost));
//                            }
                            if (relCosts <= thresh) {
                                kw.addCount("found");
//                                if (keyWord.equals("Jahr") && containerMap.get(confMat).getLineID(confMat).equals("r1009")) {
//                                double exp = Math.exp(-relCosts);
//                                    System.out.println(preGroup + "---" + preGroup.getText().length());
//                                    System.out.println(postGroup + "---" + postGroup.getText().length());
//                                    System.out.println(group);
                                if (LOG.isTraceEnabled()) {
                                    int lenPre2 = preGroup != null ? preGroup.getText().length() : -1;
                                    int lenPost2 = postGroup != null ? postGroup.getText().length() : -1;
                                    if (lenPost == 0 || lenPre == 0 || lenPost != lenPost2 || lenPre != lenPre2) {
                                        LOG.trace("pre- and postgroup found but lengths are {} vs. {} and {} vs. {} (-1=null)", lenPre, lenPre2, lenPost, lenPost2);
                                    }
                                }
//                       }         }
                                KWS.Entry entry = getEntry(confMat, Math.exp(-relCosts), group.getStart(), group.getEnd());
                                kw.add(entry);
//                                if (!doFilter && LOG.isErrorEnabled()) {
//                                    double leastCostAbs = confMat.getLeastCosts(keyWord);
//                                    double leastCostRel = leastCostAbs / keyWord.length();
//                                    if (leastCostRel >= thresh) {
//                                        double leastCostRel2 = leastCostAbs / (keyWord.length() + 2);
//                                        LOG.warn("skip word '{}' because relcost = {} but leastcost relcosts are {}.", keyWord, relCosts, leastCostRel);
//                                    }
//
//                                    decOcc.setInput(confMat);
//                                    IDecodingResult[] decRes = decOcc.getMatchCostsOcc(keyWord, new IDecodingResult[1], 0, confMat.getLength(), threshOcc * keyWord.length());
//                                    IDecodingResult dr = decRes[0];
//                                    if (dr.getCostAbs() < Double.MAX_VALUE && dr.getText() != null) {
//                                        double costRel = dr.getCostAbs() / dr.getText().length();
//                                        if (costRel >= threshOcc) {
//                                            LOG.warn("skip word '{}' because relcost = {} but regex relcosts are {}.", keyWord, relCosts, costRel);
//                                        }
//                                    }
//
//                                }
                            } else {
                                kw.addCount("regex costs");
                            }
                        } else {
                            LOG.error("group <KW> returns but text is empty for keyword {}", keyWordVisual);
                        }
                    }
                }
            } catch (NoSuchElementException ex) {
                LOG.warn("caught exception an stop searching with this thread.", ex);
            }
        }

        @Override
        public KWS.Word call() throws Exception {
            run();
            return null;
        }

    }

    private static String getPolygonPart(Polygon2DInt baseline, double beginRel, double endRel) {
        Polygon2DInt blowUp = PolygonUtil.blowUp(baseline);
        int begin = (int) Math.floor(beginRel * blowUp.npoints);
        int end = Math.min(blowUp.npoints, (int) Math.ceil(endRel * blowUp.npoints)) - 1;
        return PolygonUtil.polygon2String(new Polygon2DInt(new int[]{blowUp.xpoints[begin], blowUp.xpoints[end]}, new int[]{blowUp.ypoints[begin], blowUp.ypoints[end]}, 2));
    }

    private KWS.Entry getEntry(ConfMat confMat, double conf, int start, int end) {
        double relStart = (double) start / confMat.getLength();
        double relEnd = (double) end / confMat.getLength();
        String pageID = confMatMap.get(confMat);
        ConfMatContainer container = containerMap.get(confMat);
        Polygon2DInt baseline = container.getBaseline(confMat);
//                                Polygon2DInt baseline = container.getBaseline(confMat);
        String lineID = container.getLineID(confMat);
//    private static KWS.Entry getEntry(ConfMat cm, String lineID, double absCosts, double relCosts, double conf, double relStart, double relEnd, Polygon2DInt polygon, String pageId) {
        return new KWS.Entry(conf, lineID, getPolygonPart(baseline, relStart, relEnd), pageID);
    }

    private boolean loadConfMats(String[] imagesIn, String[] storageIn, boolean upper, long cacheSize) {
        if (storageRunner >= storageIn.length) {
            return false;
        }
        long sumBytes = 0;
        LOG.debug("load confmats...");
        StopWatch sw = new StopWatch();
        sw.start();
        int cntNotLoaded = 0;
        confMatMap = new LinkedHashMap<>();
        containerMap = new LinkedHashMap<>();
        System.gc();
        System.runFinalization();
//        List<String> storages = FileUtil.readLines(new File(storagesIn));
//        List<String> images = FileUtil.readLines(new File(imagesIn));
        if (imagesIn != null && storageIn.length != imagesIn.length) {
            throw new RuntimeException("lists of images and storages differ in number of lines (" + imagesIn.length + " vs. " + storageIn.length + ").");
        }
        storageRunnerBefore = storageRunner;
        while (storageRunner < storageIn.length) {
            String image = imagesIn == null ? String.valueOf(storageRunner) : imagesIn[storageRunner];
            File storageDir = new File(storageIn[storageRunner]);
            File storageFile = storageDir.isFile() ? storageDir : new File(storageDir, "container.bin");
            sumBytes += storageDir.length();
            if (cacheSize > 0 && sumBytes > cacheSize) {
                if (confMatMap.isEmpty()) {
                    LOG.warn("maximal number of bytes is {} but first confmat has {} bytes. Take one ConfMat anyway.");
                } else {
                    break;
                }
            }
            try {
                ConfMatContainer container = (ConfMatContainer) IO.load(storageFile,"de.uro.citlab","de.uros.citlab");
                if (container == null) {
                    throw new IOException("loading file '" + storageFile + "' produces a null element.");
                }
                container.freePhysStruct();
                for (ConfMat entry : container.getConfmats()) {
                    if (upper) {
                        ConfMat entryNew = entry.toUpper();
                        container.substituteConfmat(entry, entryNew);
                        entry = entryNew;
                    }
                    confMatMap.put(entry, image);
                    containerMap.put(entry, container);
                }
            } catch (NullPointerException | IOException | ClassNotFoundException ex) {
                LOG.info("cannot load file " + storageFile, ex);
                cntNotLoaded++;
            }
            storageRunner++;
        }
        sw.stop();
        LOG.debug("load confmats...[DONE] loaded " + confMatMap.size() + " confmats in " + sw.formatCurrentTime() + " ms.");
        if (cntNotLoaded > 0) {
            LOG.warn("could not load " + cntNotLoaded + " of " + imagesIn.length + " confmats - set logging to INFO for more details.");
        }
        return !confMatMap.isEmpty();
    }

    public class Status {

        private int size;
        private int cnt = -1;
        private int last = 0;

        public void update(int cnt) {
            this.cnt = cnt;
            int next = cnt * 100 / size;
            if (next != last) {
                last = next;
                notifyObservers(this);
            }
        }

        public Status(int size) {
            this.size = size;
            cnt = 0;
            notifyObservers(this);
        }

        public int getCnt() {
            return cnt;
        }

        public int getSize() {
            return size;
        }

        @Override
        public String toString() {
            return String.format("%d%% completed", last);
        }

    }

}
