/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import com.achteck.misc.log.Logger;
import eu.transkribus.core.model.beans.pagecontent.MetadataType;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.interfaces.IModule;

import javax.xml.datatype.DatatypeFactory;
import java.util.GregorianCalendar;

/**
 * @author gundram
 */
public class MetadataUtil {

    public static Logger LOG = Logger.getLogger(MetadataUtil.class.getName());

    public static void addMetadata(PcGtsType page, IModule module) {
        try {

            MetadataType metadata = page.getMetadata();
            metadata.setLastChange(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
            String version = metadata.getClass().getPackage().getImplementationVersion();
            if (version == null) {
                version = "?" + module.getVersion();
            }
            metadata.setCreator(("prov=" + module.getProvider() + ":name=" + module.getToolName() + ":v=" + version + "").replace("\n", "/") + "\n" + metadata.getCreator());
        } catch (Exception e) {
            LOG.log(Logger.ERROR, "cannot create metadata from file - ignore adding matadata", e);
        }
    }

    public static String getProvider(String nameOfPerson, String email) {
        return "University of Rostock/Institute of Mathematics/CITlab"
                + (nameOfPerson != null && !nameOfPerson.isEmpty() ? "/" + nameOfPerson : "")
                + (email != null && !email.isEmpty() ? "/" + email : "");
    }

    public static String getSoftwareVersion() {
        return "2.2.2";
    }

}
