package com.rag.app.api.dto;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class UploadDocumentRequest {
    @RestForm("file")
    public FileUpload file;

    @RestForm("userId")
    public String userId;
}
