/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import de.planet.citech.trainer.loader.IImageLoader;
import de.planet.itrtech.reco.IImagePreProcess;
import de.planet.util.LoaderIO;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.IOUtil;
import de.uros.citlab.module.util.PropertyUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author gundram
 */
public class ApplyPreProc extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(ApplyPreProc.class.getName());
    @ParamAnnotation(descr = "in folder or list with image-files")
    private String in;
    @ParamAnnotation(descr = "out folder")
    private String out;
    @ParamAnnotation(descr = "noise on traindata")
    private boolean noise;
    @ParamAnnotation(descr = "how many copies of each image (>1 does only make sense when noise=true")
    private int n;
    @ParamAnnotation(descr = "path to preproc-binary or", name = "pp")
    private String pp = "";
    private IImagePreProcess ppImpl = null;
    private IImagePreProcess ppImplPreset = null;
    @ParamAnnotation(descr = "copy groundtruth (which is in the sampe folder as the imagex with additional ending .txt)")
    private boolean cp_gt;
    private boolean isFolder = false;

    public ApplyPreProc(String folderIn, String folderOut, boolean noise, int n, IImagePreProcess pp, boolean cp_gt) {
        this.in = folderIn;
        this.out = folderOut;
        this.noise = noise;
        this.pp = "";
        this.ppImpl = null;
        this.ppImplPreset = pp;
        this.n = n;
        this.cp_gt = cp_gt;
        addReflection(this, ApplyPreProc.class);
    }

    public ApplyPreProc() {
        this("", "", false, 1, null, true);
    }

    @Override
    public void init() {
        super.init();
        if (noise == false && n > 1) {
            throw new RuntimeException("noise is false and n>1: duplicates would be generated.");
        }
        File fIn = new File(in);
        if (fIn.isDirectory()) {
            isFolder = true;
        }
        if (!pp.isEmpty()) {
            ppImpl = (IImagePreProcess) IOUtil.load(pp);
            ppImpl.setParamSet(ppImpl.getDefaultParamSet(null));
            ppImpl.init();
        }
        if (ppImplPreset != null) {
            if (ppImpl != null) {
                LOG.warn("overwrite preproc instance {} by instance {}", ppImpl.getClass(), ppImplPreset.getClass());
            }
            ppImpl = ppImplPreset;
        }
        if (ppImpl == null) {
            throw new RuntimeException("no preprocess set");
        }
        if (noise) {
            ppImpl.activateModes(de.planet.reco.types.RecoTrainMode.NOISE);
        } else {
            ppImpl.deactivateModes(de.planet.reco.types.RecoTrainMode.NOISE);
        }
    }

    private File getTgtFile(File srcFolder, File tgtFolder, File srcFile) {
        if (isFolder) {
            return FileUtil.getTgtFile(srcFolder, tgtFolder, srcFile);
        }
        return new File(tgtFolder, srcFile.getPath());
    }

    public void run() throws IOException {
        if (in == null || in.isEmpty() || out == null || out.isEmpty()) {
            throw new RuntimeException("in folder and out folder have to be set");
        }
        File fIn = new File(in);
        File fOut = new File(out);
        fOut.mkdirs();
        FileUtil.copyFile(new File(pp), new File(out, "preproc.bin"));
        String[] props = noise ? PropertyUtil.setProperty(null, Key.NOISE, "true") : null;
        List<File> listFiles = null;
        if (isFolder) {
            listFiles = FileUtil.listFiles(fIn, "png jpg tif tiff".split(" "), true);
        } else {
            List<String> readLines = IOUtils.readLines(new FileReader(fIn));
            listFiles = new LinkedList<>();
            for (String readLine : readLines) {
                listFiles.add(new File(readLine));
            }
        }
        for (int i = 0; i < n; i++) {
            File fSubOut = n == 1 ? fOut : new File(fOut, String.format("sample_%02d", i));
            for (File srcFile : listFiles) {
                File tgtFile = getTgtFile(fIn, fSubOut, srcFile);
                tgtFile.getParentFile().mkdirs();
                IImageLoader.IImageHolder loadImageHolder = LoaderIO.loadImageHolder(srcFile.getAbsolutePath());
                ppImpl.preProcess(loadImageHolder.getImage()).save(tgtFile.getAbsolutePath());
                FileUtil.writeLine(new File(tgtFile.getAbsolutePath() + ".txt"), loadImageHolder.getTarget().toString());
                if (cp_gt) {
                    File gt = new File(srcFile.getPath() + ".txt");
                    if (!gt.exists()) {
                        throw new RuntimeException("cannnot find groundtruth file form file " + srcFile);
                    }
                    FileUtil.copyFile(gt, getTgtFile(fIn, fSubOut, gt));
                }
                LOG.info(srcFile + " ==> " + tgtFile);
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidParameterException, IOException {
        if (args.length == 0) {
            ArgumentLine al = new ArgumentLine();
            al.addArgument("in", HomeDir.PATH + "traindata/TEST_CITlab_Test_Tuni_duplicated");
            al.addArgument("out", HomeDir.PATH + "preproced/TEST_CITlab_Test_Tuni_duplicated");
//            al.addArgument("h", "64");
//            al.addArgument("s", "64");
//            al.addArgument("in", HomeDir.PATH + "lines/train");
//            al.addArgument("out", HomeDir.PATH + "samples/train");
            al.addArgument("pp", HomeDir.getFile("models/12113/preproc.bin"));
//            al.addArgument("n", "20");
//            al.addArgument("noise", "true");
//            al.setHelp();
            args = al.getArgs();
        }
        ApplyPreProc instance = new ApplyPreProc();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
