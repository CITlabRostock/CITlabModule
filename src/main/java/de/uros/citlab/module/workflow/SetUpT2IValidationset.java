package de.uros.citlab.module.workflow;

import com.achteck.misc.exception.InvalidParameterException;
import com.achteck.misc.param.ParamSet;
import de.uros.citlab.module.baseline2polygon.B2PSeamMultiOriented;
import de.uros.citlab.module.types.ArgumentLine;
import de.uros.citlab.module.util.FileUtil;
import de.uros.citlab.module.util.PageXmlUtil;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SetUpT2IValidationset {
    public static Logger LOG = LoggerFactory.getLogger(SetUpT2IValidationset.class);

    public static void main(String[] args) throws InvalidParameterException, IOException, JAXBException {
        File file = HomeDir.getFile("data/sets_b2p/valid");
        File t2I_valid = HomeDir.getFile("data/T2I_valid");
        File t2I_LA_valid = HomeDir.getFile("data/T2I_LA_valid");
        t2I_valid.mkdirs();
        File[] images = file.listFiles((dir, name) -> name.endsWith("jpg"));
        for (int i = 0; i < images.length; i++) {
            File image = images[i];
            File folder = new File(t2I_valid, image.getName().substring(0, image.getName().lastIndexOf(".")));
            folder.mkdirs();
            FileUtil.copyFile(image, new File(folder, image.getName()));
            File folderPage = new File(folder, "page");
            folderPage.mkdirs();
            File xmlPath = PageXmlUtil.getXmlPath(image, true);
            PcGtsType xml = PageXmlUtil.unmarshal(xmlPath);
            List<String> text = PageXmlUtil.getText(xml);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < text.size(); j++) {
                sb.append(' ').append(text.get(j));
            }
            FileUtil.writeLines(new File(folder, "ref.txt"), Arrays.asList(sb.toString().trim()));
            List<TextLineType> textLines = PageXmlUtil.getTextLines(xml);
//            for (TextLineType tlt : textLines) {
//                PageXmlUtil.setTextEquiv(tlt, null, null);
//            }
            PageXmlUtil.marshal(xml, new File(folderPage, xmlPath.getName()));
        }
        for (int i = 0; i < images.length; i++) {
            File image = images[i];
            File folder = new File(t2I_LA_valid, image.getName().substring(0, image.getName().lastIndexOf(".")));
            folder.mkdirs();
            FileUtil.copyFile(image, new File(folder, image.getName()));
            File folderPage = new File(folder, "page");
            folderPage.mkdirs();
            File xmlPath = PageXmlUtil.getXmlPath(image, true);
            PcGtsType xml = PageXmlUtil.unmarshal(xmlPath);
            List<String> text = PageXmlUtil.getText(xml);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < text.size(); j++) {
                sb.append(' ').append(text.get(j));
            }
            FileUtil.writeLines(new File(folder, "ref.txt"), Arrays.asList(sb.toString().trim()));
            List<TextLineType> textLines = PageXmlUtil.getTextLines(xml);
//            for (TextLineType tlt : textLines) {
//                PageXmlUtil.setTextEquiv(tlt, null, null);
//            }
        }
        ArgumentLine al = new ArgumentLine();
        al.addArgument("xml_in", t2I_LA_valid);
        al.addArgument("xml_out", t2I_LA_valid);
        al.addArgument("b2p", B2PSeamMultiOriented.class.getName());
        args = al.getArgs();
        String folder = "", folderOut = "", htr = "", lr = "", la = "", b2p = "";
        String[] props = null;
//        props = PropertyUtil.setProperty(props, Key.LA_DELETESCHEME, LayoutAnalysisURO_ML.DEL_REGIONS);
//        props = PropertyUtil.setProperty(props, Key.LA_ROTSCHEME, LayoutAnalysisURO_ML.ROT_HOM);
//        props = PropertyUtil.setProperty(props, Key.LA_SEPSCHEME, LayoutAnalysisURO_ML.SEP_NEVER);
        Apply2Folder_ML instance = new Apply2Folder_ML(htr, lr, la, folder, folderOut, b2p, false, false, props);
        ParamSet ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.ACCEPT); // be strict, don't accept generic parameter
        String[] remainingArgumentList = ps.getRemainingArgumentList();
        if (remainingArgumentList != null && remainingArgumentList.length > 0) {
            LOG.warn("parameters {} are used for properties", Arrays.asList(remainingArgumentList));
        }
//        System.out.println(ps);
//        System.out.println(Arrays.toString(remainingArgumentList));
        props = ArgumentLine.getPropertiesFromArgs(remainingArgumentList, props);
        LOG.info("set properties {}", Arrays.toString(props)); //        System.out.println("==>" + Arrays.toString(props));
        instance = new Apply2Folder_ML(htr, lr, la, folder, folderOut, b2p, false, false, props);
        ps = new ParamSet();
        ps.setCommandLineArgs(args);    // allow early parsing
        ps = instance.getDefaultParamSet(ps);
        ps = ParamSet.parse(ps, args, ParamSet.ParseMode.ACCEPT); // be strict, don't accept generic parameter
        instance.setParamSet(ps);
        instance.init();
        instance.run();

    }
}
