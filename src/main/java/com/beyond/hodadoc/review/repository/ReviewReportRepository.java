package com.beyond.hodadoc.review.repository;

import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.patient.domain.Patient;
import com.beyond.hodadoc.review.domain.Review;
import com.beyond.hodadoc.review.domain.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    // 특정 환자가 특정 리뷰를 이미 신고했는지 확인합니다.
    boolean existsByReviewAndPatient(Review review, Patient patient);

    // 특정 병원이 특정 리뷰를 이미 신고했는지 확인합니다.
    boolean existsByReviewAndHospital(Review review, Hospital hospital);
    List<ReviewReport> findByReviewId(Long reviewId);
    void deleteByReviewId(Long reviewId);
    @Query("SELECT rr.review.id, rr.reason, COUNT(rr) FROM ReviewReport rr WHERE rr.review.id IN :reviewIds GROUP BY rr.review.id, rr.reason")
    List<Object[]> countByReasonAndReviewIds(@Param("reviewIds") List<Long> reviewIds);
}
