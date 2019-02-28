package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import de.planet.languagemodel.train.TrainLM;
import de.uros.citlab.module.util.FileUtil;
import edu.berkeley.nlp.lm.io.MakeLmBinaryFromArpa;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class RunTrainingLM {
    public RunTrainingLM() {
    }

    public static void run(File folder, File out) {
        List<File> xml = FileUtil.listFiles(folder, "txt", true);
        FileUtil.deleteMetadataAndMetsFiles(xml);
        List<String> text = new LinkedList<>();
        for (File file : xml) {
            text.addAll(FileUtil.readLines(file));
        }
        File arpa=new File(out.getAbsolutePath().replace(".bin",".arpa"));
        TrainLM.train(text, 7, arpa);
        MakeLmBinaryFromArpa.main(new String[]{arpa.getAbsolutePath(),out.getAbsolutePath()});

    }

    public static void main(String[] args) throws InvalidParameterException {

        RunTrainingLM training = new RunTrainingLM();
        training.run(
                new File("/home/gundram/devel/projects/bentham_academical/data/LA"),
                new File("/home/gundram/devel/projects/bentham_academical/lm/lm_t2i_7gram.bin"));
    }
}
