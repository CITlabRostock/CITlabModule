/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.CharMap;
import com.achteck.misc.types.ConfMat;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.util.CharMapUtil;
import de.uros.citlab.module.util.CreateDict;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.TrainDataUtil;
import de.uros.citlab.errorrate.util.ObjectCounter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author gundram
 */
public class CreateDictFromFolder extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(CreateDictFromFolder.class.getName());

    @ParamAnnotation(descr = "folder with txt-files")
    private String i;
    @ParamAnnotation(descr = "file to save P-Dict")
    private String dict;
    @ParamAnnotation(descr = "file to save CharMap")
    private String cm;

    public CreateDictFromFolder(String i, String dict) {
        this.i = i;
        this.dict = dict;
        addReflection(this, CreateDictFromFolder.class);
    }

    public CreateDictFromFolder() {
        this("", "");
    }

    @Override
    public void init() {
        super.init();
        File f = new File(i);
        if (!f.exists() || !f.isDirectory()) {
            throw new RuntimeException("path '" + i + "' does not exist or is no directory");
        }
        if (dict.isEmpty()) {
            throw new RuntimeException("no output file is set (-dict)");
        }
    }

    public void run() throws IOException {
        List<File> filesTxt = FileUtil.listFiles(new File(i), "txt".split(" "), true);
        CreateDict cd = new CreateDict(null);
        for (File fileTxt : filesTxt) {
            try (FileInputStream fileInputStream = new FileInputStream(fileTxt)) {
                List<String> readLines = IOUtils.readLines(fileInputStream);
                cd.processLines(readLines);
            }
        }
        cd.savePDict(dict);
        if (!cm.isEmpty()) {
            String[] txtFiles = new String[filesTxt.size()];
            for (int j = 0; j < filesTxt.size(); j++) {
                txtFiles[j] = filesTxt.get(j).getPath();
            }
            TrainDataUtil.Statistic statistic = TrainDataUtil.getStatistic(txtFiles, null);
            CharMap<Integer> charMap = CharMapUtil.getCharMap(statistic.getStatChar().getResult(), false);
            CharMapUtil.saveCharMap(charMap, new File(cm));
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidParameterException, IOException {
        if (args.length == 0) {
            ArgumentLine al = new ArgumentLine();
            al.addArgument("i", HomeDir.PATH + "data/StaZh_FirstTestCollection/Testdaten_TKR/T2I/");
            al.addArgument("o", HomeDir.PATH + "dicts/firstTestCollection.csv");
            args = al.getArgs();
        }
        CreateDictFromFolder instance = new CreateDictFromFolder();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
