package de.uros.citlab.module.types;

import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TranskribusMetadataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorNotification {
    private TranskribusMetadataType transkribusMetadata;
    private String imageName;
    private String lineID;
    private Throwable reason;
    private Class classReportedError;
    Logger LOG = LoggerFactory.getLogger(ErrorNotification.class);

    public ErrorNotification(PcGtsType page, String lineID, Throwable reason, Class classReportedError) {
        this.transkribusMetadata = page != null ? page.getMetadata() != null ? page.getMetadata().getTranskribusMetadata() : null : null;
        this.imageName = page != null ? page.getPage() != null ? page.getPage().getImageFilename() : null : null;
        this.lineID = lineID;
        this.reason = reason;
        this.classReportedError = classReportedError;
        LOG.error("created error notification with message {}", toString());
    }

    public TranskribusMetadataType getTranskribusMetadata() {
        return transkribusMetadata;
    }

    public String getImageName() {
        return imageName;
    }

    public String getLineID() {
        return lineID;
    }

    public Throwable getReason() {
        return reason;
    }

    public Class getClassReportedError() {
        return classReportedError;
    }

    @Override
    public String toString() {
        return "Error{" +
                "imageName='" + imageName + '\'' +
                ", lineID='" + lineID + '\'' +
                ", reason='" + reason + '\'' +
                ", classReportedError=" + classReportedError +
                '}';
    }
}
