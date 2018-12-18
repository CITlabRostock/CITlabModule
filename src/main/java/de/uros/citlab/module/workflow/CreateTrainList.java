package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamSetOrganizer;
import de.uros.citlab.module.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class CreateTrainList extends ParamSetOrganizer {

    @ParamAnnotation(descr = "directory with image snipets")
    private String i;

    @ParamAnnotation(descr = "minimal confidence")
    private double c;

    @ParamAnnotation(descr = "path to file list")
    private String o;

    public CreateTrainList() {
        addReflection(this, CreateTrainList.class);
    }

    public void run() {
        List<File> files = FileUtil.listFiles(new File(i), FileUtil.IMAGE_SUFFIXES, true);
        LinkedList<String> res = new LinkedList<>();
        for (File file : files) {
            File infofile = new File(file.getParentFile(), file.getName() + ".info");
            if (!infofile.exists()) {
                System.out.println("cannot find file " + infofile.getPath() + " - skip image " + file + ".");
                continue;
            }
            List<String> strings = FileUtil.readLines(infofile);
            boolean found = false;
            for (int j = 0; j < strings.size(); j++) {
                if (strings.get(j).equals("conf")) {
                    double val = Double.valueOf(strings.get(j + 1));
                    if (val > c) {
                        res.add(file.getPath());
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("cannot find 'conf' in file " + infofile + " - skip image " + file + ".");
            }
        }
        FileUtil.writeLines(new File(o), res);
        System.out.println("used " + res.size() + " of " + files.size() + " files");
    }

    public static void main(String[] args) throws InvalidParameterException, IOException, ClassNotFoundException, Exception {
        CreateTrainList instance = new CreateTrainList();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
