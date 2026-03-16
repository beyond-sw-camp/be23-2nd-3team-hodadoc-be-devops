package com.beyond.hodadoc.admin.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminSummaryDto {
    private long pendingHospitalCount;      // 승인 대기 병원 수
    private long reportedReviewCount;       // 미처리 신고 리뷰 수
    private long patientUnansweredCount;    // 환자 미답변 채팅방 수
    private long hospitalUnansweredCount;   // 병원관리자 미답변 채팅방 수
}
