package com.aws.s3;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class S3UploaderImpl implements Uploader {

    private static final String TEMP_FILE_PATH = "src/main/resources/";

    private final AmazonS3Client amazonS3Client;
    private final S3UploadComponent s3UploadComponent;

    public String upload(MultipartFile multipartFile, String dirName) throws IOException {
        File convertedFile = convert(multipartFile);
        return upload(convertedFile, dirName);
    }

    private File convert(MultipartFile file) throws IOException {
        File convertFile = new File(TEMP_FILE_PATH + file.getOriginalFilename());
        if(convertFile.exists()){
            convertFile.delete();
        }

        if (convertFile.createNewFile()) {
            try (FileOutputStream fos = new FileOutputStream(convertFile)) {
                fos.write(file.getBytes());
            }
            return convertFile;
        }
        throw new IllegalArgumentException(String.format("파일 변환이 실패했습니다. 파일 이름: %s", file.getName()));
    }

    private String upload(File uploadFile, String dirName) {
        String fileName = dirName + "/" + uploadFile.getName();
        String uploadImageUrl = putS3(uploadFile, fileName);
        removeNewFile(uploadFile);
        return uploadImageUrl;
    }

    private String putS3(File uploadFile, String fileName) {
        amazonS3Client.putObject(new PutObjectRequest(s3UploadComponent.getBucket(), fileName, uploadFile).withCannedAcl(CannedAccessControlList.PublicRead));
        return amazonS3Client.getUrl(s3UploadComponent.getBucket(), fileName).toString();
    }

    private void removeNewFile(File targetFile) {
        if (targetFile.delete()) {
            return;
        }
        log.info("임시 파일이 삭제 되지 못했습니다. 파일 이름: {}", targetFile.getName());
    }
}
