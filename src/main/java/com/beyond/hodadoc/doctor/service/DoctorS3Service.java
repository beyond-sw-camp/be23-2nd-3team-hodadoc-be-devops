package com.beyond.hodadoc.doctor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class DoctorS3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region}")
    private String region;

    public DoctorS3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * MultipartFile을 S3에 업로드하고 URL 반환
     */
    public String upload(MultipartFile file) {
        String fileName = "doctor/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new RuntimeException("S3 업로드 실패: " + e.getMessage(), e);
        }

        // URL 직접 조합 (SDK v2는 getUrl() 없음)
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + fileName;
    }

    /**
     * S3에서 파일 삭제 (imageUrl이 null이거나 비어있으면 무시)
     */
    public void deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;

        try {
            // URL에서 key 추출: https://bucket.s3.region.amazonaws.com/doctor/xxx.jpg → doctor/xxx.jpg
            String key = imageUrl.substring(imageUrl.indexOf(".amazonaws.com/") + ".amazonaws.com/".length());

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
        } catch (Exception e) {
            // 삭제 실패해도 서비스 로직은 계속 진행
            System.err.println("S3 파일 삭제 실패 (무시): " + e.getMessage());
        }
    }
}