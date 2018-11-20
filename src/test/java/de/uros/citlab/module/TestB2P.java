/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module;

import com.achteck.misc.types.ConfMat;
import com.achteck.misc.util.IO;
import de.planet.citech.trainer.loader.IImageLoader;
import de.planet.reco.types.SNetwork;
import de.planet.util.LoaderIO;
import de.uros.citlab.errorrate.costcalculator.CostCalculatorDft;
import de.uros.citlab.errorrate.htr.ErrorModuleDynProg;
import de.uros.citlab.errorrate.htr.ErrorRateCalcer;
import de.uros.citlab.errorrate.interfaces.IErrorModule;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.types.Method;
import de.uros.citlab.errorrate.types.Metric;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.module.baseline2polygon.B2PSeamMultiOriented;
import de.uros.citlab.module.la.B2PSimple;
import de.uros.citlab.module.train.TrainHtr;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.TrainDataUtil;
import de.uros.citlab.module.workflow.Apply2Folder;
import de.uros.citlab.tokenizer.categorizer.CategorizerCharacterDft;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author gundram
 */
public class TestB2P {

    private static File folder = new File(TestFiles.getPrefix(), "test_b2p");
    private static File folderGT = new File(TestFiles.getPrefix(), "test_workflow");
    private static File folderSnipets = new File(folder, "Snipets");
    private static File folderB2PSeamMultiOriented = new File(folder, "B2PSeamMultiOriented");
    private static File folderB2PSimple = new File(folder, "B2PSimple");
    private static File folderNoB2P = new File(folder, "noB2P");
    private static File fileNet = TestFiles.getHtrs().get(1);
    private static final Class throwException = Test.None.class;
    private static List<File> xmlList;
    private static SNetwork net;

    @BeforeClass
    public static void setUp() {
        if (TestFiles.skipLargeTests()) {
            return;
        }
        try {
            net = (SNetwork) IO.load(TrainHtr.getNet(fileNet));
            net.setParamSet(net.getDefaultParamSet(null));
            net.init();
        } catch (IOException ex) {
            Logger.getLogger(TestB2P.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(TestB2P.class.getName()).log(Level.SEVERE, null, ex);
        }
        xmlList = FileUtil.listFiles(folderGT, "xml".split(" "), true);
        FileUtil.deleteMetadataAndMetsFiles(xmlList);
        Collections.sort(xmlList);
        folderSnipets.mkdirs();
        folderB2PSeamMultiOriented.mkdirs();
        folderB2PSimple.mkdirs();
        folderNoB2P.mkdirs();
    }

    @AfterClass
    public static void tearDown() {
        if (TestFiles.skipLargeTests()) {
            return;
        }
        FileUtils.deleteQuietly(folderSnipets);
        FileUtils.deleteQuietly(folderB2PSeamMultiOriented);
        FileUtils.deleteQuietly(folderB2PSimple);
        FileUtils.deleteQuietly(folderNoB2P);
    }

    @Test
    public void testB2PSimple() {
        if (TestFiles.skipLargeTests()) {
            return;
        }
        System.out.println("testB2PSimple");
        Apply2Folder f = new Apply2Folder(fileNet, null, null, folderGT, folderB2PSimple, B2PSimple.class.getName(), true, false);
        f.setParamSet(f.getDefaultParamSet(null));
        f.init();
        try {
            f.run();
        } catch (IOException | JAXBException ex) {
            throw new RuntimeException(ex);

        }
        ErrorRateCalcer erc = new ErrorRateCalcer();
        List<File> xmlListMO = FileUtil.listFiles(folderB2PSimple, "xml".split(" "), true);
        Collections.sort(xmlListMO);
        ErrorRateCalcer.Result process = erc.process(xmlListMO.toArray(new File[0]), xmlList.toArray(new File[0]), Method.CER);
        System.out.println(process.getCounts());
        double metric = process.getMetric(Metric.ERR);
        System.out.println(metric);
        try {
            FileUtils.write(new File(folderB2PSimple, "count.txt"), process.getCounts().toString());
            FileUtils.write(new File(folderB2PSimple, "cer.txt"), "" + metric);
        } catch (IOException ex) {
        }
    }

    @Test
    public void testApplyWithB2PSeamMultiOriented() {
        if (TestFiles.skipLargeTests()) {
            return;
        }
        System.out.println("testApplyWithB2PSeamMultiOriented");
        Apply2Folder f = new Apply2Folder(fileNet, null, null, folderGT, folderB2PSeamMultiOriented, B2PSeamMultiOriented.class.getName(), true, false);
        f.setParamSet(f.getDefaultParamSet(null));
        f.init();
        try {
            f.run();
        } catch (IOException | JAXBException ex) {
            throw new RuntimeException(ex);
        }
        ErrorRateCalcer erc = new ErrorRateCalcer();
        List<File> xmlListMO = FileUtil.listFiles(folderB2PSeamMultiOriented, "xml".split(" "), true);
        Collections.sort(xmlListMO);
        ErrorRateCalcer.Result process = erc.process(xmlListMO.toArray(new File[0]), xmlList.toArray(new File[0]), Method.CER);
        System.out.println(process.getCounts());
        double metric = process.getMetric(Metric.ERR);
        System.out.println(metric);
        try {
            FileUtils.write(new File(folderB2PSeamMultiOriented, "count.txt"), process.getCounts().toString());
            FileUtils.write(new File(folderB2PSeamMultiOriented, "cer.txt"), "" + metric);
        } catch (IOException ex) {
        }
    }

    @Test
    public void testApply() {
        if (TestFiles.skipLargeTests()) {
            return;
        }
        System.out.println("testApply");
        Apply2Folder f = new Apply2Folder(fileNet, null, null, folderGT, folderNoB2P, null, true, false);
        f.setParamSet(f.getDefaultParamSet(null));
        f.init();
        try {
            f.run();
        } catch (IOException | JAXBException ex) {
            throw new RuntimeException(ex);
        }
        ErrorRateCalcer erc = new ErrorRateCalcer();
        List<File> xmlListMO = FileUtil.listFiles(folderNoB2P, "xml".split(" "), true);
        Collections.sort(xmlListMO);
        ErrorRateCalcer.Result process = erc.process(xmlListMO.toArray(new File[0]), xmlList.toArray(new File[0]), Method.CER);
        System.out.println(process.getCounts());
        double metric = process.getMetric(Metric.ERR);
        System.out.println(metric);
        try {
            FileUtils.write(new File(folderNoB2P, "count.txt"), process.getCounts().toString());
            FileUtils.write(new File(folderNoB2P, "cer.txt"), "" + metric);
        } catch (IOException ex) {
        }
    }

    @Test
    public void testSnipets() {
        if (TestFiles.skipLargeTests()) {
            return;
        }
        TrainDataUtil.createTrainData(FileUtil.asStringList(xmlList), folderSnipets.getAbsolutePath(), null);
        TrainHtr thtr = new TrainHtr();
        thtr.trainHtr(fileNet.getPath(), null, null, folderSnipets.getPath(), null);
        List<File> listFiles = FileUtil.listFiles(folderSnipets, "jpg".split(" "), true);
        IErrorModule err = new ErrorModuleDynProg(new CostCalculatorDft(), new CategorizerCharacterDft(), null, false);
        for (File snipet : listFiles) {
            IImageLoader.IImageHolder loadImageHolder = LoaderIO.loadImageHolder(snipet.getPath(), false, true);
            net.setInput(loadImageHolder);
            net.update();
            ConfMat confMat = net.getConfMat();
            String reco = confMat.toString();
            err.calculate(reco, loadImageHolder.getTarget().toString());
        }
        ObjectCounter<Count> counter = err.getCounter();
        System.out.println(counter);
        long gt = counter.getMap().getOrDefault(Count.GT, 0L);
        long ins = counter.getMap().getOrDefault(Count.INS, 0L);
        long del = counter.getMap().getOrDefault(Count.DEL, 0L);
        long sub = counter.getMap().getOrDefault(Count.SUB, 0L);
        System.out.println("CER = " + ((double) ins + del + sub) / ((double) gt));
        try {
            FileUtils.write(new File(folderSnipets, "count.txt"), counter.toString());
            FileUtils.write(new File(folderSnipets, "cer.txt"), "" + ((double) ins + del + sub) / ((double) gt));
        } catch (IOException ex) {
        }
    }

}
