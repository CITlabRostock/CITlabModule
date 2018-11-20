package de.uros.citlab.module.interfaces;

import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.interfaces.IHtr;
import eu.transkribus.interfaces.types.Image;

public interface IHtrCITlab extends IHtr {
    public void process(String pathToOpticalModel, String pathToLanguageModel, String pathCharMap, Image image, PcGtsType xmlFile, String storageDir, String[] lineIds, String[] props);
}
