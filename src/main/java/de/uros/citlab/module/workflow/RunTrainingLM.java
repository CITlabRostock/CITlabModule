package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import de.uros.citlab.languagemodel.TrainLM;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PageXmlUtil;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class RunTrainingLM {
    public RunTrainingLM() {
    }

    public static void run(File folder, File out) {
        List<File> xml = FileUtil.listFiles(folder, "xml", true);
        FileUtil.deleteMetadataAndMetsFiles(xml);
        List<String> text = new LinkedList<>();
        for (File file : xml) {
            text.addAll(PageXmlUtil.getText(PageXmlUtil.unmarshal(file)));
        }
        TrainLM.train(text, 5, out);
    }

    public static void main(String[] args) throws InvalidParameterException {

        RunTrainingLM training = new RunTrainingLM();
        training.run(
                new File("/home/gundram/devel/projects/read/data/TRAIN_CITlab_Bentham_himself_M4"),
                new File("/home/gundram/devel/projects/read/langmods/M4.arpa"));
    }
}
