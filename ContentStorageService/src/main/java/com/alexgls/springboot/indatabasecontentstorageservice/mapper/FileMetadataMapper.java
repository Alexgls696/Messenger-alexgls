package com.alexgls.springboot.indatabasecontentstorageservice.mapper;

import com.alexgls.springboot.indatabasecontentstorageservice.dto.CreateFileMetadataRequest;
import com.alexgls.springboot.indatabasecontentstorageservice.entity.FileMetadata;

import java.util.Objects;

public class FileMetadataMapper {

    public static FileMetadata toFileMetadata(CreateFileMetadataRequest createFileMetadataRequest) {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setPath(createFileMetadataRequest.path());
        fileMetadata.setFilename(createFileMetadataRequest.filename());
        fileMetadata.setChatId(createFileMetadataRequest.chatId());
        if(!Objects.isNull(createFileMetadataRequest.securityType())){
            String securityType = createFileMetadataRequest.securityType();
            if(securityType.equals("public") || securityType.equals("private")){
                fileMetadata.setSecurityType(securityType);
            }
        }
        return fileMetadata;
    }
}
