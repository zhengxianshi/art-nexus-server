package com.artnexus.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.artnexus.config.OssConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OssUtil {

    private final OssConfig ossConfig;

    /**
     * 上传文件到 OSS
     * @param file 文件
     * @param dir  目录前缀（如 artwork / avatar / relay / canvas）
     * @return 文件访问 URL
     */
    public String upload(MultipartFile file, String dir) {
        OSS ossClient = new OSSClientBuilder().build(
                ossConfig.getEndpoint(),
                ossConfig.getAccessKeyId(),
                ossConfig.getAccessKeySecret());

        try (InputStream inputStream = file.getInputStream()) {
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String objectKey = String.format("%s/%s/%s%s", dir, datePath, UUID.randomUUID(), ext);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setObjectAcl(CannedAccessControlList.PublicRead);

            PutObjectRequest putRequest = new PutObjectRequest(
                    ossConfig.getBucketName(), objectKey, inputStream, metadata);
            ossClient.putObject(putRequest);

            return ossConfig.getBaseUrl() + "/" + objectKey;
        } catch (Exception e) {
            log.error("OSS 上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        } finally {
            ossClient.shutdown();
        }
    }
}
