package com.beyond.hodadoc.review.service;

import com.beyond.hodadoc.review.repository.ReviewRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 병원별 평균 별점 Redis 캐시 서비스
 *
 * 키 구조: review:rating:{hospitalId}
 * Hash 필드:
 *   - sum   : 별점 합계 (long) ← 별점은 1~5 정수이므로 합계도 항상 정수
 *   - count : 리뷰 개수 (long)
 *
 * 평균 = sum / count  →  O(1) 계산, DB 집계 쿼리 불필요
 * Cache-Aside 패턴: 캐시 미스 시 DB에서 재계산 후 적재
 */
@Slf4j
@Service
public class RatingCacheService {

    @Resource(name = "redisTemplateForRating")
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ReviewRepository reviewRepository;

    private static final String KEY_PREFIX = "review:rating:";
    private static final String FIELD_SUM   = "sum";
    private static final String FIELD_COUNT = "count";

    private String key(Long hospitalId) {
        return KEY_PREFIX + hospitalId;
    }

    // ── 캐시 초기화 (DB에서 재계산) ────────────────────────────────────────
    private void initFromDb(Long hospitalId) {
        Double sumDouble = reviewRepository.findSumRatingByHospitalId(hospitalId);
        Long   count     = reviewRepository.countByHospitalId(hospitalId);

        // ✅ sum을 long으로 저장 (increment가 정수만 지원하므로)
        long s = (sumDouble != null) ? sumDouble.longValue() : 0L;
        long c = (count     != null) ? count                : 0L;

        String k = key(hospitalId);
        redisTemplate.opsForHash().put(k, FIELD_SUM,   String.valueOf(s));
        redisTemplate.opsForHash().put(k, FIELD_COUNT, String.valueOf(c));
        log.info("[RatingCache] DB 재계산 완료 - hospitalId={}, sum={}, count={}", hospitalId, s, c);
    }

    private boolean exists(Long hospitalId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(hospitalId)));
    }

    // ── 리뷰 등록: sum += rating, count += 1 ──────────────────────────────
    public void onReviewCreated(Long hospitalId, int rating) {
        try {
            if (!exists(hospitalId)) initFromDb(hospitalId);
            String k = key(hospitalId);
            redisTemplate.opsForHash().increment(k, FIELD_SUM,   rating);
            redisTemplate.opsForHash().increment(k, FIELD_COUNT, 1);
            log.debug("[RatingCache] 등록 - hospitalId={}, +rating={}", hospitalId, rating);
        } catch (Exception e) {
            log.error("[RatingCache] 등록 실패 - hospitalId={}", hospitalId, e);
        }
    }

    // ── 리뷰 삭제: sum -= rating, count -= 1 ──────────────────────────────
    public void onReviewDeleted(Long hospitalId, int rating) {
        try {
            if (!exists(hospitalId)) initFromDb(hospitalId);
            String k = key(hospitalId);
            redisTemplate.opsForHash().increment(k, FIELD_SUM,   -rating);
            redisTemplate.opsForHash().increment(k, FIELD_COUNT, -1);
            log.debug("[RatingCache] 삭제 - hospitalId={}, -rating={}", hospitalId, rating);
        } catch (Exception e) {
            log.error("[RatingCache] 삭제 실패 - hospitalId={}", hospitalId, e);
        }
    }

    // ── 리뷰 수정: sum += (newRating - oldRating) ─────────────────────────
    public void onReviewUpdated(Long hospitalId, int oldRating, int newRating) {
        try {
            if (!exists(hospitalId)) initFromDb(hospitalId);
            int diff = newRating - oldRating;
            if (diff != 0) {
                redisTemplate.opsForHash().increment(key(hospitalId), FIELD_SUM, diff);
                log.debug("[RatingCache] 수정 - hospitalId={}, diff={}", hospitalId, diff);
            }
        } catch (Exception e) {
            log.error("[RatingCache] 수정 실패 - hospitalId={}", hospitalId, e);
        }
    }

    // ── 평균 별점 조회 ────────────────────────────────────────────────────
    public double getAvgRating(Long hospitalId) {
        try {
            if (!exists(hospitalId)) initFromDb(hospitalId);
            Object sumObj   = redisTemplate.opsForHash().get(key(hospitalId), FIELD_SUM);
            Object countObj = redisTemplate.opsForHash().get(key(hospitalId), FIELD_COUNT);

            long sum   = toLong(sumObj);
            long count = toLong(countObj);

            return (count == 0) ? 0.0 : Math.round((double) sum / count * 10.0) / 10.0;
        } catch (Exception e) {
            log.error("[RatingCache] 조회 실패 - hospitalId={}, DB fallback", hospitalId, e);
            Double avg = reviewRepository.findAvgRatingByHospitalId(hospitalId);
            return (avg != null) ? Math.round(avg * 10.0) / 10.0 : 0.0;
        }
    }

    // ── 리뷰 수 조회 ─────────────────────────────────────────────────────
    public long getReviewCount(Long hospitalId) {
        try {
            if (!exists(hospitalId)) initFromDb(hospitalId);
            Object countObj = redisTemplate.opsForHash().get(key(hospitalId), FIELD_COUNT);
            return toLong(countObj);
        } catch (Exception e) {
            log.error("[RatingCache] count 조회 실패 - hospitalId={}, DB fallback", hospitalId, e);
            Long count = reviewRepository.countByHospitalId(hospitalId);
            return (count != null) ? count : 0L;
        }
    }

    // ── 캐시 무효화 ───────────────────────────────────────────────────────
    public void evict(Long hospitalId) {
        redisTemplate.delete(key(hospitalId));
        log.info("[RatingCache] 캐시 삭제 - hospitalId={}", hospitalId);
    }

    // ── 타입 변환 유틸 ────────────────────────────────────────────────────
    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); }
        catch (NumberFormatException e) { return 0L; }
    }
}