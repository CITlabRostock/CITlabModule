package de.uros.citlab.module.workflow;

import de.uros.citlab.module.util.FileUtil;

import java.io.File;
import java.util.List;

public class CreatePartitions {
    public static void main(String[] args) {
        List<File> partitions = FileUtil.listFiles(HomeDir.getFile("partitions"), "lst", true);
        for (File partition : partitions) {
            String name = partition.getName();
            name = name.substring(0, name.lastIndexOf("."));
            File folderPartition = HomeDir.getFile("data/sets_orig/" + name);
            folderPartition.mkdirs();
            List<String> strings = FileUtil.readLines(partition);
            int cnt = strings.size();
            List<File> files = FileUtil.listFiles(HomeDir.getFile("data/raw"), FileUtil.IMAGE_SUFFIXES, true);
            for (File file : files) {
                String nameFile = file.getName();
                nameFile = nameFile.substring(0, nameFile.lastIndexOf("."));
                if (strings.contains(nameFile)) {
                    FileUtil.copyFile(file, new File(folderPartition, file.getName()));
                    cnt--;
                }
            }
            System.out.println("missed " + cnt + " image files in " + name);
            files = FileUtil.listFiles(HomeDir.getFile("data/raw"), "xml", true);
            FileUtil.deleteMetadataAndMetsFiles(files);
            cnt = strings.size();
            for (File file : files) {
                String nameFile = file.getName();
                nameFile = nameFile.substring(0, nameFile.lastIndexOf("."));
                if (strings.contains(nameFile)) {
                    FileUtil.copyFile(file, new File(new File(folderPartition, "page"), file.getName()));
                    cnt--;
                }
            }
            System.out.println("missed " + cnt + " xml files in " + name);
        }
    }
}
