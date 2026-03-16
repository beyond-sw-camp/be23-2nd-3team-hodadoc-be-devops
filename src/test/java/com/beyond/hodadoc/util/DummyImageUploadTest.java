package com.beyond.hodadoc.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 1회성 더미 이미지 일괄 업로드 스크립트
 *
 * [실행 방법]
 * 1. 아래 @Disabled 어노테이션을 제거한다.
 * 2. MariaDB · Redis 서버가 실행 중인 상태에서 실행한다.
 * - IDE: 테스트 메서드 옆 ▶ 버튼 클릭
 * - CLI: ./gradlew test --tests "com.beyond.hodadoc.util.DummyImageUploadTest"
 * 3. 완료 후 src/main/resources/dummy_image_updates.sql 이 생성된다.
 * 4. 해당 SQL 파일을 DB에서 실행하면 이미지 URL이 반영된다.
 *
 * [업로드 대상]
 * - 병원 이미지: 100개 병원 × 2장 = 200장  →  hospital_image 테이블
 * - 의사 이미지: 200명 × 1장  = 200장  →  doctor.image_url 컬럼
 *
 * [S3 경로]
 * - dummy/hospitals/{hospitalId}/image_{1,2}.jpg
 * - dummy/doctors/{doctorId}/profile.jpg
 */
@Disabled("1회성 더미 이미지 업로드 — 실행 전 @Disabled 제거")
@SpringBootTest
class DummyImageUploadTest {

    // ──────────────── 상수 ────────────────────────────────────────────────
    private static final int HOSPITAL_COUNT       = 100;
    private static final int IMAGES_PER_HOSPITAL  = 2;
    private static final int DOCTOR_COUNT         = 200;

    /** loremflickr 이미지 소스 URL (lock 값으로 중복 방지) */
    private static final String HOSPITAL_IMG_URL  = "https://loremflickr.com/800/600/hospital,building?lock=";
    private static final String DOCTOR_IMG_URL    = "https://loremflickr.com/400/400/doctor,face?lock=";

    private static final int    CONNECT_TIMEOUT   = 15_000; // ms
    private static final int    READ_TIMEOUT      = 15_000; // ms
    private static final int    MAX_REDIRECTS     = 5;
    private static final int    MAX_RETRY         = 3;

    /** 생성될 SQL 파일 경로 */
    private static final String SQL_OUTPUT_PATH   = "src/main/resources/dummy_image_updates.sql";

    // ──────────────── 주입 ────────────────────────────────────────────────
    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket1}")
    private String bucket;

    @Value("${aws.region}")
    private String region;

    // ──────────────── 메인 테스트 ─────────────────────────────────────────
    @Test
    void uploadAllDummyImages() throws IOException {
        List<String> sql = new ArrayList<>();
        sql.add("-- =============================================================");
        sql.add("-- 더미 이미지 URL 업데이트 SQL");
        sql.add("-- 생성 기준: DummyImageUploadTest 업로드 완료 후 자동 생성");
        sql.add("-- 사용법: 이 파일을 DB 클라이언트(DBeaver 등)에서 직접 실행");
        sql.add("-- =============================================================");

        int lock = 1; // loremflickr lock 번호 (전역 카운터, 중복 없이 증가)

        // ── 병원 이미지 (100 × 2 = 200장) ──────────────────────────────────
        sql.add("");
        sql.add("-- [병원 이미지] hospital_image 테이블 INSERT");
        for (int hId = 1; hId <= HOSPITAL_COUNT; hId++) {
            for (int imgNo = 1; imgNo <= IMAGES_PER_HOSPITAL; imgNo++) {
                String srcUrl = HOSPITAL_IMG_URL + lock;
                String key    = "dummy/hospitals/" + hId + "/image_" + imgNo + ".jpg";

                String s3Url  = uploadWithRetry(srcUrl, key, lock);
                sql.add("INSERT INTO hospital_image (image_url, hospital_id) VALUES ('" + s3Url + "', " + hId + ");");
                lock++;
            }
            System.out.println("[병원] " + hId + "/" + HOSPITAL_COUNT + " 완료");
        }

        // ── 의사 이미지 (200 × 1 = 200장) ──────────────────────────────────
        sql.add("");
        sql.add("-- [의사 이미지] doctor 테이블 image_url UPDATE");
        for (int dId = 1; dId <= DOCTOR_COUNT; dId++) {
            String srcUrl = DOCTOR_IMG_URL + lock;
            String key    = "dummy/doctors/" + dId + "/profile.jpg";

            String s3Url  = uploadWithRetry(srcUrl, key, lock);
            sql.add("UPDATE doctor SET image_url = '" + s3Url + "' WHERE id = " + dId + ";");
            lock++;
            System.out.println("[의사] " + dId + "/" + DOCTOR_COUNT + " 완료");
        }

        // ── SQL 파일 저장 ────────────────────────────────────────────────
        Path outPath = Paths.get(SQL_OUTPUT_PATH);
        Files.write(outPath, sql, StandardCharsets.UTF_8);

        int totalUploads = HOSPITAL_COUNT * IMAGES_PER_HOSPITAL + DOCTOR_COUNT;
        System.out.println("=== 업로드 완료: " + totalUploads + "장 | SQL 파일 → " + outPath.toAbsolutePath() + " ===");
    }

    // ──────────────── 재시도 포함 업로드 ──────────────────────────────────
    /**
     * 업로드 실패 시 최대 MAX_RETRY 회 재시도.
     * 모든 시도가 실패하면 S3 Key 기반 URL을 반환하여 SQL 파일에는 기록이 남도록 함.
     */
    private String uploadWithRetry(String srcUrl, String key, int lockNum) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                String s3Url = doUpload(srcUrl, key);
                System.out.println("  ✅ lock=" + lockNum + " | " + key + " → " + s3Url);
                return s3Url;
            } catch (Exception e) {
                System.err.println("  ⚠ 업로드 실패 (" + attempt + "/" + MAX_RETRY + ") lock=" + lockNum + " key=" + key + " → " + e.getMessage());
                if (attempt < MAX_RETRY) {
                    sleep(3_000L * attempt); // 3초, 6초 대기 후 재시도
                }
            }
        }
        // 모든 시도 실패 → URL만 기록 (실제 오브젝트는 없으나 SQL 형식은 유지)
        System.err.println("  ❌ 최종 실패 (lock=" + lockNum + "): " + key + " → 빈 URL 기록");
        return generateS3Url(key);
    }

    // ──────────────── 단건 업로드 ─────────────────────────────────────────
    /**
     * loremflickr URL에서 이미지를 다운로드(리다이렉트 추적)하여 S3에 업로드.
     */
    private String doUpload(String srcUrl, String key) throws IOException {
        // 1. 외부 URL → 메모리 (리다이렉트 수동 추적)
        byte[] imageBytes = downloadWithRedirectTracking(srcUrl);

        // 2. S3 업로드 (InputStream 없이 byte[] 로 업로드 → Content-Length 보장)
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("image/jpeg")
                .contentLength((long) imageBytes.length)
                .build();

        s3Client.putObject(req, RequestBody.fromBytes(imageBytes));
        return generateS3Url(key);
    }

    // ──────────────── 리다이렉트 수동 추적 다운로드 ──────────────────────
    /**
     * loremflickr.com → 302 → Flickr CDN 과 같이 도메인이 바뀌는 리다이렉트를
     * HttpURLConnection.setInstanceFollowRedirects(false) 로 직접 처리한다.
     *
     * setInstanceFollowRedirects(true) 는 HTTP → HTTPS 또는 도메인 변경 리다이렉트를
     * 따르지 않을 수 있어 수동 처리가 더 안전하다.
     */
    private byte[] downloadWithRedirectTracking(String urlStr) throws IOException {
        String current = urlStr;
        for (int hop = 0; hop < MAX_REDIRECTS; hop++) {
            HttpURLConnection conn = openConnection(current);
            int status = conn.getResponseCode();

            // 리다이렉트 상태 코드 처리
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null || location.isBlank()) {
                    throw new IOException("Location 헤더 없음 (status=" + status + "): " + current);
                }
                // 상대 경로 리다이렉트 → 절대 경로로 변환
                current = location.startsWith("http")
                        ? location
                        : new URL(new URL(current), location).toString();
                continue;
            }

            // 200 OK → 바이트 읽기
            try (InputStream is = conn.getInputStream()) {
                return is.readAllBytes();
            } finally {
                conn.disconnect();
            }
        }
        throw new IOException("최대 리다이렉트(" + MAX_REDIRECTS + "회) 초과: " + urlStr);
    }

    private HttpURLConnection openConnection(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setInstanceFollowRedirects(false); // 수동 리다이렉트 처리
        return conn;
    }

    // ──────────────── 유틸 ────────────────────────────────────────────────
    /**
     * S3 퍼블릭 URL 생성
     * 형식: https://{bucket}.s3.{region}.amazonaws.com/{key}
     */
    private String generateS3Url(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}