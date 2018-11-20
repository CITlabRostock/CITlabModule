/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ConfMat;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import com.achteck.misc.util.IO;
import com.achteck.misc.util.StopWatch;
import de.uros.citlab.module.kws.KWSParser;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PropertyUtil;
import eu.transkribus.interfaces.IKeywordSpotter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author gundram
 */
public class RunKWS extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(RunKWS.class.getName());
    @ParamAnnotation(descr = "path to query-dictionary")
    private String q;

//    @ParamAnnotation(descr = "path to folder containing storage and images")
//    private String f = "";
    @ParamAnnotation(descr = "folder containing images")
    private String i;

    @ParamAnnotation(descr = "folder containing storages")
    private String s;

    @ParamAnnotation(descr = "path to output file")
    private String o;

    @ParamAnnotation(descr = "number of threads")
    private int kws_threads;

    @ParamAnnotation(descr = "minimal confidence")
    private double kws_min_conf;

    @ParamAnnotation(descr = "maximal number of hits")
    private int kws_max_anz;

    @ParamAnnotation(descr = "keyword can be substring of word")
    private boolean kws_part;

    @ParamAnnotation(descr = "keyword search is case-insensitive")
    private boolean kws_upper;

    @ParamAnnotation(descr = "maximal size of loaded confmats in MB")
    private int kws_cache_size;

//    @ParamAnnotation(descr = "module for kws", visible = false, member = "impl", name = "impl")
//    private String module = KWSParser.class.getName();
    private IKeywordSpotter impl = new KWSParser();

    public RunKWS(String q, String i, String s, String o, int kws_threads, double kws_min_conf, int kws_max_anz, boolean kws_part, boolean kws_upper, int kws_cache_size) {
        this.q = q;
        this.i = i;
        this.s = s;
        this.o = o;
        this.kws_threads = kws_threads;
        this.kws_min_conf = kws_min_conf;
        this.kws_max_anz = kws_max_anz;
        this.kws_part = kws_part;
        this.kws_upper = kws_upper;
        this.kws_cache_size = kws_cache_size;
        addReflection(this, RunKWS.class);
    }

    public RunKWS() {
        this("", "", "", "", 1, 0.135, -1, false, false, 1000);
    }

    public void setQuery(String q) {
        this.q = q;
    }

    public void setS(String s) {
        this.s = s;
    }

    public void setO(String o) {
        this.o = o;
    }

    public void setI(String i) {
        this.i = i;
    }

    public void setUpper(boolean kws_upper) {
        this.kws_upper = kws_upper;
    }

    public void setPart(boolean kws_part) {
        this.kws_part = kws_part;
    }

    public void run() {
        List<File> storage = FileUtil.getFilesListsOrFolders(s, "bin".split(" "), true);
        Collections.sort(storage);
        List<File> image = null;
        if (i != null && !i.isEmpty()) {
            image = FileUtil.getFilesListsOrFolders(i, FileUtil.IMAGE_SUFFIXES, true);
            Collections.sort(image);
            if (storage.size() != image.size()) {
                throw new RuntimeException("number of storage files = " + storage.size() + " and image files = " + image.size());
//            image=image.subList(0, storage.size());
            }
        }
        String[] storages = FileUtil.asStringList(storage);
        String[] images = FileUtil.asStringList(image);
        String[] queries = FileUtil.readLines(new File(q)).toArray(new String[0]);
        String[] props = PropertyUtil.setProperty(null, Key.KWS_THREADS, String.valueOf(kws_threads));
        props = PropertyUtil.setProperty(props, Key.KWS_MIN_CONF, String.valueOf(kws_min_conf));
        props = PropertyUtil.setProperty(props, Key.KWS_MAX_ANZ, String.valueOf(kws_max_anz));
        props = PropertyUtil.setProperty(props, Key.KWS_PART, String.valueOf(kws_part));
        props = PropertyUtil.setProperty(props, Key.KWS_CACHE_SIZE, String.valueOf(kws_cache_size));
        props = PropertyUtil.setProperty(props, Key.KWS_UPPER, String.valueOf(kws_upper));
        String process = impl.process(images, storages, queries, null, props);
        FileUtil.writeLines(new File(o), Arrays.asList(process));
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidParameterException, IOException, ClassNotFoundException {
        if (args.length == 0) {
//        Object load = IO.load(new File("/home/gundram/devel/projects/read/data/25278_LA_HTR135/KWS-DEMO_HS_118_duplicated/storage/025278_0004_706378.jpg/container.bin"));
//        System.out.println(load);
            List<String> htrs = new LinkedList<>();
            htrs.add("dft_320");
//        htrs.add("dft_excl_10");
//        htrs.add("dft_excl_20");
//        htrs.add("dft_excl_40");
//        htrs.add("dft_excl_80");
//        htrs.add("dft_excl_160");
            for (String htr : htrs) {
                ArgumentLine al = new ArgumentLine();
//            String[] case1 = new String[]{"kws_bot", "TEST_CITlab_Konzilsprotokolle_M4_LA_HTR"};
                al.addArgument("o", HomeDir.getFile("results/" + htr + "/speed_out_xmx2g.json"));
                al.addArgument("q", HomeDir.getFile("query_richard.txt"));
//        al.addArgument("s", HomeDir.getFile("index/val_b2p/" + htr+"/val_c"));
                al.addArgument("s", HomeDir.getFile("index/speed/" + htr + ""));
                al.addArgument("i", HomeDir.getFile("index/speed/" + htr + ""));
//        al.addArgument("i", HomeDir.getFile("data/val_b2p/val_c"));
//            al.addArgument("impl/maxNum", -1);
                al.addArgument(Key.KWS_THREADS, 4);
                al.addArgument(Key.KWS_MIN_CONF, 0.135);
                al.addArgument(Key.KWS_PART, true);
                al.addArgument(Key.KWS_UPPER, false);
                args = al.getArgs();
                RunKWS instance = new RunKWS();
                ParamSet ps = new ParamSet();
                ps.setCommandLineArgs(args);    // allow early parsing
                ps = instance.getDefaultParamSet(ps);
                ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
                instance.setParamSet(ps);
                instance.init();
                instance.run();
//        }
            }
        } else {
            RunKWS instance = new RunKWS();
            ParamSet ps = new ParamSet();
            ps.setCommandLineArgs(args);    // allow early parsing
            ps = instance.getDefaultParamSet(ps);
            ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
            instance.setParamSet(ps);
            instance.init();
            instance.run();
//   
        }
    }
}
