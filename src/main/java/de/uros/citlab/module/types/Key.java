/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.types;

/**
 * @author gundram
 */
public class Key {

    public static final String GLOBAL_CER_TRAIN = "CER.txt";
    public static final String GLOBAL_CER_VAL = "CER_test.txt";
    public static final String GLOBAL_CHARMAP = "chars.txt";

    /**
     * indicates which gpu should be used for training. ("-1" for CPU, "0",...,"N-1" for GPUs)
     * If nothing is set, Process tries to take environment variable $GPU_DEVICE (which can have the same values).
     * If nothing is set, CPU is used.
     */
    public static final String TRAIN_GPU = "train_gpu";

    /**
     * if HTR+ is used, this property have to point to the folder where tf_htrs
     * is linked
     */
    public static final String PYTHON_REPO_PATH = "python_repo_path";
    public static final String MIN_CONF = "min_conf";
    public static final String STATISTIC = "stat";
    public static final String CREATEDICT = "create_dict";
    /**
     * saves language resource.
     * If value is set, it can be true or a path where the language resouces have to be saved:
     * value = "true" ==> save file lr.txt in the folder, where training data are saved.
     * value = "path/file.txt" saves the file to the given path.
     */
    public static final String CREATE_LR = "create_lr";
    public static final String DEBUG = "debug";
    public static final String DEBUG_DIR = "debug_dir";
    public static final String DELETE = "delete";
    public static final String METHOD_LA = "method_la";
    public static final String NOISE = "noise";
    public static final String RAW = "raw";
    public static final String VIEWER = "viewer";
    public static final String HELP = "help";
    public static final String THREADS = "Threads";
    public static final String NORMALIZE_FORM = "form";
    public static final String MAX_ANZ = "maxanz";
    public static final String TRAINSIZE = "TrainSizePerEpoch";
    public static final String TRAIN_STATUS = "train_status";
    public static final String EST = "EarlyStopping";
    public static final String EPOCHS = "NumEpochs";
    public static final String MINI_BATCH = "mini_batch";
    public static final String LEARNINGRATE = "LearningRate";
    public static final String CREATETRAINDATA = "traindata";
    public static final String PATH_BEST_NET = "path_best_net";
    public static final String PATH_NET = "path_net";
    public static final String PATH_TRAIN_LOG = "path_train_log";
    public static final String PATH_TEST_LOG = "path_test_log";
    /**
     * if Value is set, save htr ConfMatContainer to this file name (instead of container.bin)
     */
    public static final String HTR_CONFMAT_CONTAINER_FILENAME ="";

    /**
     * path to a temporal folder which is used to save temporarily important
     * files. The process assumes, that the foler already exists. The process
     * will not delete the folder or its items at the end of the process - the
     * user have to care about that. Default null
     */
    public static final String TMP_FOLDER = "htr_train_tmp_path";
    /**
     * if htr_id is set, the given id will be saved into the metadata of the
     * xml-file. It should be an integer.
     */
    public static final String TRANSKRIBUS_HTR_ID = "htr_id";
    /**
     * if htr_name is set, the given id will be saved into the metadata of the
     * xml-file. The name should match the regex "[a-zA-Z0-0 _-()]+".
     */
    public static final String TRANSKRIBUS_HTR_NAME = "htr_name";

    /**
     * threshold for text alignment. If the confidence of a text-to-image
     * alignment above this threshold, an alignment is done (default = 0.0). A
     * good value is between 0.01 and 0.05. Note that the confidence is stored
     * in the pageXML anyway, so deleting text alignments with low confidence
     * can also be made later.
     */
    public static final String T2I_THRESH = "thresh";
    /**
     * can be null, a non-negative double value or a json-string (default:
     * null). If no value is set or value is "Infinity", no hyphenation is done.
     * If value is a positive double value, the value are the additional costs
     * to recognize a hyphenation. The default hyphenation signs at the end of
     * the line are '¬', '-', ':', '='. The default hyphenation signs at the
     * beginning of the line are empty. There can be hyphenations between all
     * letter-pairs. If one wants to use hyphenation rules for a specific
     * language, this can be configured using the key 'hyphen_lang'.<br>
     * The hyphenation sign in the groundtruth will be '¬'.<br>
     * If one wants to configure more, one has to write a j-son-string.
     * Keys:<br> prefixes: list of hyphenation sign that can be hyphens at the
     * beginning of a line (default: empty)
     * <br> suffixes: list of hyphenation sign that can be hyphens at the end of
     * a line (default: empty)
     * <br> skipSuffix: boolean if suffix is optional (true) of forced (false)
     * (default: false)
     * <br> skipPrefix: boolean if prefix is optional (true) of forced (false)
     * (default: false)
     * <br> hypCosts: non-negative value that produces additional costs to
     * recognize a hyphenation. (default: 0.0)
     * <br>pattern: language pattern (e.g. EN_GB, EN_US, DE, ES, FR,...)
     * (default: empty)
     * <br> example: "{
     * <br>"skipSuffix":false,
     * <br>"skipPrefix":true,
     * <br>"suffixes":["¬","-",":","\u003d"],
     * <br>"prefixes":[":","\u003d"],
     * <br>"hypCosts":6.0,
     * <br>"pattern":"EN_GB"
     * <br>}"
     * <br> one of the 4 suffixes have to be recognized and one of the both
     * prefixes can be recognized. Hyphenation costs of 6.0 are added.
     * Hyphenation is only possible as defined for language EN_GB.
     */
    public static final String T2I_HYPHEN = "hyphen";

    /**
     * if hyphen is given, hyphenation-rules from different languages can be
     * applied. If value = null or empty, a linebreak between all letters is
     * possible (unicode-characters of Category L). Otherwise, a rule is applied
     * ( see https://github.com/mfietz/JHyphenator.git for details). The
     * language have e.g. "DE" for German and "EN" for English. Default = null.
     */
    public static final String T2I_HYPHEN_LANG = "hyphen_lang";

    /**
     * makes it possible to skip a word, for example if a baseline is too short
     * (default: null). The value have to be a positive double value. It
     * repesents the default delete-costs for each character. A good value is
     * 4.0. The higher the value, the less words were skipped. If value = 0, a
     * word can be deleted without producing costs (destroys the algorithm), if
     * value = Infinity, no characters can be deleted.
     */
    public static final String T2I_SKIP_WORD = "skip_word";

    /**
     * makes it possible to skip a baseline (default: null). Sometimes the LA
     * finds a baseline in noise (aka false positive). It is possible to delete
     * those baselines instead of "pressing" a sequence into the line. The value
     * has to be positive double value. The lower the value, the easier a line
     * is ignored. A good value is 0.2.
     */
    public static final String T2I_SKIP_BASELINE = "skip_bl";

    /**
     * maximal number of edges which have to be calculated. If the algorithm
     * reaches this count, it stops automatically without getting a final
     * result. If the value is negative, there is no limit. A good value is 1e8.
     * Default: -1
     */
    public static final String T2I_MAX_COUNT = "max_count";

    /**
     * makes it possible to handle wrong reading order in the LA (default: null)
     * The value makes it possible to jump instead of the after a line to every
     * other line. If value = 0, the reading order has no effect at all. If
     * value = Infinity is the same like value = null. If you cannot trust the
     * reading order, set value = 0.
     */
    public static final String T2I_JUMP_BASELINE = "jump_bl";

    /**
     * if the number of confmats and references gets too large, one can only
     * keep a specific number of paths at each reference. As default all paths
     * are calculated (like setting value = Infinity). A good value is 200.0
     */
    public static final String T2I_BEST_PATHES = "best_pathes";

//    
//    /**
//     * Decide whether LA should be able to handle text which is rotated by 90°, 180°, 270°. 
//     * If false (default) the text is assumed to be more or less (+- 10°) NOT rotated.
//     * If true an different network is used which was trained on randomly rotated (0,90,180,270)
//     * images. This network is expected to be less accurate on oriented scenarios.
//     */
//    public static final String LA_MOD90 = "modulo_90";
    /**
     * Decide whether Tensorflow is restricted to one CORE.
     * <p>
     * <p>
     * default: Tensorflow is allowed to use multiple cores. <br>
     * <p>
     * true - Tensorflow is allowed to use just one core.
     */
    public static final String LA_SINGLECORE = "la_single_core";

    /**
     * default: within a given TextRegion/CellRegion DONT use Seperators, if no
     * regions are given, use them<br>
     * <p>
     * always - uses seperators also within given Regions/Cells<br>
     * <p>
     * never - never uses separator information
     */
    public static final String LA_SEPSCHEME = "la_sepscheme";

    /**
     * Determines what to do with existing informations.
     * <p>
     * default: nothing is deleted, regions which already contain lines are
     * skipped and not further processed <br>
     * <p>
     * regions - all regions (and lines are deleted)<br>
     * <p>
     * lines - just lines in given regions are deleted
     */
    public static final String LA_DELETESCHEME = "la_deletescheme";

    /**
     * Determines how to handle different orientations. If you do not set a
     * network explicitly, this property has to be set in CONSTRUCTOR since it
     * affects the * choice of the default network.
     * <p>
     * default: it is assumed that all present text is 0° oriented<br>
     * <p>
     * hom - it is assumed that the text is homogeneously oriented (all text
     * lines have the same orientation) - 0°, 90°, 180° OR 270°<br>
     * <p>
     * het - text could be oriented heterogenious, a mix of 0°, 90°, 180° and
     * 270° is possible,
     */
    public static final String LA_ROTSCHEME = "la_rotscheme";

    /**
     * makes kws case insensitive<br>
     * value: true|false (default: false)
     */
    public static final String KWS_UPPER = "kws_upper";
    /**
     * activates the expert-modus<br>
     * some regular expressions are allowed<br>
     * value: true|false (default: false)
     */
    public static final String KWS_EXPERT = "kws_expert";
    /**
     * keyword can be part of longer word<br>
     * value: true|false (default: false)<br>
     * if true, e.g. keyword "man" can be found in "mental" or "romantic"
     */
    public static final String KWS_PART = "kws_part";

    /**
     * minimal confidence for a hit<br>
     * value: in [0.0,1.0]<br>
     * default: 0.02<br>
     * a threshold for a keyword-confidence. A good value is in [0.01,0.1]<br>
     * a low value mean low precision and high recall<br>
     * a hight value mean hight precision and low recall
     */
    public static final String KWS_MIN_CONF = "kws_min_conf";

    /**
     * number of threads for searching in confmats<br>
     * value: positive number<br>
     * default: 1
     */
    public static final String KWS_THREADS = "kws_threads";

    /**
     * maximal number of keywords<br>
     * value: positive number or -1<br>
     * takes only the top-N keywords. For value=-1 no limit is given<br>
     * default: -1
     */
    public static final String KWS_MAX_ANZ = "kws_max_anz";

    /**
     * maximal size of cache for confmats im MB value: positive number default:
     * 1000
     */
    public static final String KWS_CACHE_SIZE = "kws_cache_size";
}
