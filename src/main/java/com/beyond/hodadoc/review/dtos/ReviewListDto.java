package com.beyond.hodadoc.review.dtos;


import com.beyond.hodadoc.review.domain.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewListDto {
    private Long id;
    private String contents;
    private int rating;
    private String patientName;
    private Long hospitalId;    // 병원 상세 페이지 이동용
    private String hospitalName;
    private String status;
    private int reportCount;
    private LocalDateTime createdTime;  // 리뷰 작성 날짜/시간

    public ReviewListDto(Review review) {
        this.id = review.getId();
        this.contents = review.getContents();
        this.rating = review.getRating();
        this.status = review.getStatus().name();
        this.reportCount = review.getReportCount();
        this.createdTime = review.getCreatedTime();
        // 환자가 없거나(연결 끊김) 이름이 null(탈퇴 처리)이면 "탈퇴한 회원" 표시
        if (review.getPatient() == null || review.getPatient().getName() == null) {
            this.patientName = "탈퇴한 회원";
        } else {
            this.patientName = review.getPatient().getName();
        }

        // 혹시 모를 삭제를 대비하여 null 대비, 변경없을 경우 추후 삭제 필요
        if (review.getHospital() != null) {
            this.hospitalId = review.getHospital().getId();
            this.hospitalName = review.getHospital().getName();
        }
    }

}
