package com.beyond.hodadoc.review.dtos;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter

public class ReviewReportAdminListDto {

    private Long id;
    private String content;      // 리뷰 내용
    private int rating;          // 별점
    private int reportCount;     // 신고 횟수
    private Long hospitalId;
    private String hospitalName; // 병원 이름
    private String status;       // 상태
    private Map<String, Integer> reportReasonCount;
    private String patientName;  // 리뷰 작성자
    private String createdAt;    // 리뷰 작성 시각

    public ReviewReportAdminListDto(
            Long id,
            String content,
            int rating,
            int reportCount,
            Long hospitalId,
            String hospitalName,
            String status,
            Map<String, Integer> reportReasonCount,
            String patientName,
            String createdAt
    ) {
        this.id = id;
        this.content = content;
        this.rating = rating;
        this.reportCount = reportCount;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.status = status;
        this.reportReasonCount = reportReasonCount;
        this.patientName = patientName;
        this.createdAt = createdAt;
    }
}
