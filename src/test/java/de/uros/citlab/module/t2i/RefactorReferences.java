/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.t2i;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import de.uros.citlab.module.workflow.HomeDir;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 *
 * @author gundram
 */
public class RefactorReferences extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(RefactorReferences.class.getName());
//    @ParamAnnotation(descr = "create text-files")
//    private boolean create_gt = true;
    @ParamAnnotation(descr = "delete hypens")
    private boolean delete_hypens = false;
    @ParamAnnotation(descr = "propability of swapping two lines")
    private double p_swap = 0.0;
    @ParamAnnotation(descr = "propability append a word at the beginning or end of line")
    private double p_append = 0.0;
    private String dirIn = HomeDir.getFile("data/HTRTS14/gt_txt").getAbsolutePath();
//    private String dirList = HomeDir.getFile("data/HTRTS14/lists").getAbsolutePath();
//    private String dirNets = HomeDir.getFile("data/HTRTS14/nets").getAbsolutePath();
    private String dirOut = HomeDir.getFile("data/HTRTS14/ref_txt").getAbsolutePath();
    private final String[] hyphenSuffixes = new String[]{":", "-", "=", "¬"};
    private final String[] hyphenPrefixes = new String[]{":", "=", "-"};
    private Random rnd = new Random(1236);
    String[] words = "the skip word".split(" ");

    static {
        HomeDir.setPath("/home/gundram/devel/projects/read");
    }

    public RefactorReferences() {
        addReflection(this, RefactorReferences.class);
    }

    public void run() throws InvalidParameterException, IOException {
        int cntHyphens = 0;
        List<File> files = FileUtil.getFilesListsOrFolders(dirIn, "txt".split(" "), true);
        for (File file : files) {
            int cntFile = 0;
            //cleanup
            List<String> readLines = FileUtil.readLines(file);
            for (int i = 0; i < readLines.size(); i++) {
                String line = readLines.get(i).trim();
                if (line.isEmpty()) {
                    readLines.remove(i--);
                } else {
                    readLines.set(i, line);
                }
            }
            if (p_swap != 0.0) {
                for (int i = 0; i < readLines.size() - 1; i++) {
                    if (rnd.nextDouble() < p_swap) {
                        String line = readLines.get(i);
                        readLines.set(i, readLines.get(i + 1));
                        readLines.set(i + 1, line);
                        i++;
                    }

                }
            }
            if (p_append != 0.0) {
                for (int i = 0; i < readLines.size(); i++) {
                    if (rnd.nextDouble() < p_append) {
                        readLines.set(i, readLines.get(i) + " " + words[rnd.nextInt(words.length)]);
                    }
                    if (rnd.nextDouble() < p_append) {
                        readLines.set(i, words[rnd.nextInt(words.length)] + " " + readLines.get(i));
                    }

                }
            }
            if (delete_hypens) {
                for (int i = 0; i < readLines.size(); i++) {
                    String line = readLines.get(i);
                    int suffixLength = getHyphLengthSuffix(line);
                    if (i + 1 < readLines.size() && suffixLength >= 0) {
                        String line2 = readLines.get(i + 1);
                        int prefixLength = getHyphLengthPrefix(line2);
                        if (prefixLength >= 0) {
                            LOG.log(Logger.DEBUG, "found line hyphen-lines '" + line + "' and '" + line2 + "'");
                            String[] split = line2.split(" ", 2);
//                            if (split.length != 2) {
//                                throw new RuntimeException("unexpected lenght of split: " + split.length);
//                            }
                            readLines.set(i, line.substring(0, line.length() - suffixLength) + split[0].substring(prefixLength));
                            if (split.length < 2) {
                                readLines.remove(i + 1);
                                i--;
                            } else {
                                readLines.set(i + 1, split[1]);
                            }
                            cntFile++;
                        }
                    }
                }
            }
            File tgtFile = FileUtil.getTgtFile(new File(dirIn), new File(dirOut), file);
            tgtFile.getParentFile().mkdirs();
            FileUtil.writeLines(tgtFile, readLines);
            cntHyphens += cntFile;
            LOG.log(Logger.INFO, "found " + cntFile + " hyphens in " + file.getName());
        }
        LOG.log(Logger.INFO, "found " + cntHyphens + " hyphens in " + files.size() + " files.");
    }

    private int getHyphLengthSuffix(String string) {
        for (String hyphenSuffixe : hyphenSuffixes) {
            if (string.endsWith(hyphenSuffixe)) {
                return hyphenSuffixe.length();
            }
        }
        return -1;
    }

    private int getHyphLengthPrefix(String string) {
        for (String hyphenPrefix : hyphenPrefixes) {
            if (string.startsWith(hyphenPrefix)) {
                int res = hyphenPrefix.length();
                if (Character.isLowerCase(string.charAt(res))) {
                    return res;
                }
            }
        }
        return -1;
    }

    /**
     * @param args the command line arguments
     * @throws com.achteck.misc.exception.InvalidParameterException
     */
    public static void main(String[] args) throws InvalidParameterException, IOException {
        ArgumentLine al = new ArgumentLine();
        al.addArgument("delete_hypens", "true");
        al.addArgument("p_swap", "0.1");
        al.addArgument("p_append", "0.1");
        args = al.getArgs();
//        PageXmlUtil.marshal(PageXmlUtil.unmarshal(HomeDir.getFile("data/HTRTS14_full/gt/page/115_082_004.xml")), HomeDir.getFile("data/HTRTS14/gt/page/115_082_004.xml"));
//        System.exit(1);
        RefactorReferences instance = new RefactorReferences();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
