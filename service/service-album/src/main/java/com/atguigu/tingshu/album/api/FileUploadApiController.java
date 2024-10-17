package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.common.result.Result;
import io.minio.*;
import io.minio.errors.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Tag(name = "上传管理接口")
@RestController
@RequestMapping("api/album")
public class FileUploadApiController {

    @Autowired
    private MinioConstantProperties minioConstantProperties;

    @Operation(summary = "文件上传")
    @PostMapping("/fileUpload")
    public Result fileUpload(MultipartFile file) {
        String url = "";
        try {
            // Create a minioClient with the MinIO server playground, its access key and secret key.
            MinioClient minioClient = MinioClient.builder().endpoint(minioConstantProperties.getEndpointUrl()).credentials(minioConstantProperties.getAccessKey(), minioConstantProperties.getSecreKey()).build();

            // Make bucket if not exist.
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConstantProperties.getBucketName()).build());
            if (!found) {
                // Make a new bucket.
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConstantProperties.getBucketName()).build());
            } else {
                System.out.println("Bucket " + minioConstantProperties.getBucketName() + " already exists.");
            }
            // 生成一个上传后的文件名称
            String originalFilename = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString().replaceAll("-", "") + "." + FilenameUtils.getExtension(originalFilename);
            // Upload file
            minioClient.putObject(PutObjectArgs.builder().bucket(minioConstantProperties.getBucketName()).object(fileName).stream(file.getInputStream(), file.getSize(), -1).contentType(file.getContentType()).build());
            url = minioConstantProperties.getEndpointUrl() + "/" + minioConstantProperties.getBucketName() + "/" + fileName;
            System.out.println("url:\t" + url);
            return Result.ok(url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
