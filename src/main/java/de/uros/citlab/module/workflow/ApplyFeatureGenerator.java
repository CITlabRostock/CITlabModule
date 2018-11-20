/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.log.Logger;
import com.achteck.misc.param.ParamSet;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamTreeOrganizer;
import com.achteck.misc.util.IO;
import de.planet.imaging.types.HybridImage;
import de.planet.itrtech.reco.IImagePreProcess;
import de.planet.math.geom2d.types.Rectangle2DInt;
import de.planet.citech.trainer.loader.IImageLoader;
import de.planet.trainer.factory.ImagePreprocessDft;
import de.planet.util.LoaderIO;
import de.uros.citlab.module.norm.NormWritingDft;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.types.Key;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PropertyUtil;
import eu.transkribus.interfaces.IFeatureGenerator;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author gundram
 */
public class ApplyFeatureGenerator extends ParamTreeOrganizer {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ApplyFeatureGenerator.class.getName());
    @ParamAnnotation(descr = "in folder or list with image-files")
    private String in;
    @ParamAnnotation(descr = "out folder")
    private String out;
    @ParamAnnotation(descr = "noise on traindata")
    private boolean noise;
    @ParamAnnotation(descr = "how many copies of each image (>1 does only make sense when noise=true")
    private int n;
    @ParamAnnotation(descr = "desired height of images")
    private int h;
    @ParamAnnotation(descr = "desired size of mainbody (target inter-quantile distance 0.25 to 0.75)")
    private int s;
    @ParamAnnotation(descr = "copy groundtruth (which is in the sampe folder as the imagex with additional ending .txt)")
    private boolean cp_gt;
    private boolean isFolder = false;
    private IImagePreProcess ppImpl;

    public ApplyFeatureGenerator(String folderIn, String folderOut, boolean noise, int n, int height, int size, boolean cp_gt) {
        this.in = folderIn;
        this.out = folderOut;
        this.noise = noise;
        this.n = n;
        h = height;
        s = size;
        this.cp_gt = cp_gt;
        addReflection(this, ApplyFeatureGenerator.class);
    }

    public ApplyFeatureGenerator() {
        this("", "", false, 1, 64, 20, true);
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
        ppImpl = ImagePreprocessDft.getPreProcess(h, 0.5, s, true);
        if (noise) {
            ppImpl.activateModes(de.planet.reco.types.RecoTrainMode.NOISE);
        } else {
            ppImpl.deactivateModes(de.planet.reco.types.RecoTrainMode.NOISE);
        }
        if (!out.isEmpty()) {
            try {
                IO.save(ppImpl, new File(new File(out), "preproc.bin"));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
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
        String[] props = noise ? PropertyUtil.setProperty(null, Key.NOISE, "true") : null;
        List<File> listFiles = null;
        if (isFolder) {
            listFiles = FileUtil.listFiles(fIn, FileUtil.IMAGE_SUFFIXES, true);
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
                HybridImage preProcess = ppImpl.preProcess(loadImageHolder.getImage());
                preProcess.save(tgtFile.getAbsolutePath());
                FileUtil.writeLine(new File(tgtFile.getAbsolutePath() + ".txt"), loadImageHolder.getTarget().toString());
                if (cp_gt) {
                    File gt = new File(srcFile.getPath() + ".txt");
                    if (!gt.exists()) {
                        throw new RuntimeException("cannnot find groundtruth file form file " + srcFile);
                    }
                    FileUtil.copyFile(gt, getTgtFile(fIn, fSubOut, gt));
                }
                LOG.log(Logger.INFO, srcFile + " ==> " + tgtFile);
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidParameterException, IOException {
        if (args.length == 0) {
            ArgumentLine al = new ArgumentLine();
            al.addArgument("in", HomeDir.PATH + "lines/val/6877/MM_1_005/");
            al.addArgument("out", HomeDir.PATH + "samples_64_64");
            al.addArgument("h", "64");
            al.addArgument("s", "64");

//        al.addArgument("noise", "true");
//        al.addArgument("n", "20");
//        al.setHelp();
            args = al.getArgs();
        }
        ApplyFeatureGenerator instance = new ApplyFeatureGenerator();
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.FORCE); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();
    }

}
