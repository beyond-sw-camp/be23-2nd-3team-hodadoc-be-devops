package com.beyond.hodadoc.review.repository;

import com.beyond.hodadoc.review.domain.Review;
import com.beyond.hodadoc.review.domain.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByHospitalIdAndStatusNot(Long hospitalId, ReviewStatus status, Pageable pageable);

    Page<Review> findByPatientIdAndStatusNot(Long patientId, ReviewStatus status, Pageable pageable);

    Optional<Review> findByIdAndStatusNot(Long id, ReviewStatus status);

    Page<Review> findByStatusAndReportCountGreaterThan(ReviewStatus status, int count, Pageable pageable);

    boolean existsByReservationId(Long reservationId);

    boolean existsByCheckinId(Long checkinId);

    // 병원별 삭제되지 않은 리뷰 평균 별점 (Redis 장애 시 fallback용)
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.hospital.id = :hospitalId AND r.status <> 'DELETED'")
    Double findAvgRatingByHospitalId(@Param("hospitalId") Long hospitalId);

    // 병원별 삭제되지 않은 리뷰 개수
    @Query("SELECT COUNT(r) FROM Review r WHERE r.hospital.id = :hospitalId AND r.status <> 'DELETED'")
    Long countByHospitalId(@Param("hospitalId") Long hospitalId);

    // ✅ 추가: 병원별 별점 합계 (캐시 미스 시 초기값 계산용)
    @Query("SELECT SUM(r.rating) FROM Review r WHERE r.hospital.id = :hospitalId AND r.status <> 'DELETED'")
    Double findSumRatingByHospitalId(@Param("hospitalId") Long hospitalId);
}