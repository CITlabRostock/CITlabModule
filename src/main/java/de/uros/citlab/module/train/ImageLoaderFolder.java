/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.train;

import com.achteck.misc.log.Logger;
import com.achteck.misc.types.ParamAnnotation;
import com.achteck.misc.types.ParamSetOrganizer;
import de.planet.citech.trainer.loader.IImageLoader;
import de.planet.citech.trainer.loader.IImageLoader.IImageHolder;
import de.planet.util.LoaderIO;
import de.uros.citlab.module.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author gundram
 */
public class ImageLoaderFolder extends ParamSetOrganizer implements IImageLoader {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ImageLoaderFolder.class.getName());

    @ParamAnnotation(descr = "directory with training files (can be in subfolders) or list-file to images (use ':' for more more folder/files.")
    private String d = "";

    @ParamAnnotation(descr = "size of one epoch (-1=> number of training samples in directory 'd')")
    private int s = -1;

    @ParamAnnotation(descr = "seed (<0==>no shuffle, =0==>no seed, >0==> use seed for shuffle")
    private int seed = 0;

    @ParamAnnotation(descr = "ingore missing target saved in the corresponding .txt file")
    private boolean it = false;

    private List<File> files = null;
    private int cnt = 0;
    private int runner = 0;
    private Random rnd;
    private int size;

    public ImageLoaderFolder() {
        addReflection(this, ImageLoaderFolder.class);
    }

    @Override
    public void init() {
        super.init();
        files = FileUtil.getFilesListsOrFolders(d, FileUtil.IMAGE_SUFFIXES, true);
        if (files.isEmpty()) {
            throw new RuntimeException("no file found ending with png jpg tif PNG JPG TIF in folder(s) " + d + ".");
        }
        rnd = seed == 0 ? new Random() : new Random(seed);
        size = s < 0 ? files.size() : s;
        Collections.shuffle(files, rnd);
        LOG.log(Logger.INFO, "imageloader loaded directory '" + d + "' with " + size + " images each epoch and " + files.size() + " images in total.");
    }

    @Override
    public void reset() {
        cnt = 0;
        if (seed < 0) {
            runner = 0;
        }
    }

    @Override
    public synchronized IImageHolder next() {
        if (cnt < size) {
            if (runner >= files.size()) {
                runner = 0;
                if (seed >= 0) {
                    Collections.shuffle(files, rnd);
                }
            }
            File fn = files.get(runner);
            IImageHolder loadImageHolder = new LoaderIO.ImageHolderFileOnDemand(fn.getAbsolutePath(), it, false);
//            ISampleHolder res = new ISampleLoader.SampleHolderDft(loadImageHolder.getImage(), loadImageHolder.getTarget(), loadImageHolder.getInfo(), loadImageHolder.getUniqueId());
//            IImageHolder loadImageHolder = LoaderIO.loadImageHolder(fn, getParam(P_IGNORETARGET).getBoolean(), getParam(P_SIMPLE).getBoolean());
            if (loadImageHolder == null) {
                throw new RuntimeException("cannot load imageloader from '" + fn + "' and its .txt");
            }
            cnt++;
            runner++;
            return loadImageHolder;
        }
        return null;
    }

    @Override
    public void setEOF() {
        cnt = size;
    }

    @Override
    public IImageHolder[] next(int i) {
        IImageHolder[] res = new IImageHolder[i];
        for (int j = 0; j < i; j++) {
            IImageHolder img = next();
            if (img == null) {
                return null;
            }
            res[j] = img;
        }
        return res;
    }

}
