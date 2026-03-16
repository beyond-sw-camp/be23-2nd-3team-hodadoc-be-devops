package com.beyond.hodadoc.hospital.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class AwsS3Service {
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;
    @Autowired
    public AwsS3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String upload(MultipartFile file, String fileName) {
        if (file.isEmpty()) {
            return null;
        }

        try {
            // 1. PutObjectRequest 생성 (받아온 fileName을 Key로 사용)
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            // 2. S3 업로드 실행
            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // 3. URL 반환
            return generateUrl(fileName);

        } catch (IOException e) {
            log.error("S3 파일 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.");
        }
    }

    private String generateUrl(String fileName) {
        URL url = s3Client.utilities().getUrl(builder -> builder
                .bucket(bucketName)
                .key(fileName)
        );
        return url.toExternalForm();
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;

        try {
            // 1. URL 객체로 파싱 (가장 안전한 방법)
            URL url = new URL(fileUrl);

            // 2. 경로(Path) 추출 (예: /bucket-name/filename.jpg 또는 /filename.jpg)
            // S3 URL은 보통 /key 형태이므로 getPath()를 쓰면 맨 앞에 '/'가 붙어 나옵니다.
            String path = url.getPath();

            // 3. 맨 앞의 '/' 제거
            String key = path.startsWith("/") ? path.substring(1) : path;

            // 4. ★핵심★ URL 디코딩 (한글, 공백 처리)
            // %EC%A7%84%EB%A3%8C%EC%8B%A4.jpg -> 진료실.jpg 로 변환
            String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);

            // 5. 삭제 요청
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(decodedKey) // 디코딩된 키 사용
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 요청 완료: {}", decodedKey);

        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: url={}, error={}", fileUrl, e.getMessage());
        }
    }
}
