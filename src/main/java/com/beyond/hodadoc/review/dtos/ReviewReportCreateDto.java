package com.beyond.hodadoc.review.dtos;


import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.patient.domain.Patient;
import com.beyond.hodadoc.review.domain.ReportReason;
import com.beyond.hodadoc.review.domain.Review;
import com.beyond.hodadoc.review.domain.ReviewReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//리뷰 신고 생성 dto

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReportCreateDto {

    private ReportReason reason;

    // DTO를 엔티티 객체로 변환해주는 '변환기' 메서드
    public ReviewReport toEntity(Review review, Patient patient, Hospital hospital) {
        return ReviewReport.builder()
                .review(review)
                .patient(patient)
                .hospital(hospital)
                .reason(this.reason)
                .build();
    }
}
