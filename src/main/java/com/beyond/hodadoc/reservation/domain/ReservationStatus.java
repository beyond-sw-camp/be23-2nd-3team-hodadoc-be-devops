package com.beyond.hodadoc.reservation.domain;

public enum ReservationStatus {
    WAITING,    // 대기 (수동예약에서 병원 승인 전)
    APPROVED,   // 승인됨
    REJECTED,   // 거절됨 (병원이 거절)
    CANCELLED,  // 취소됨 (환자 또는 병원이 취소)
    COMPLETED,  // 진료 완료 (리뷰 작성 가능)
    BLOCKED     // 병원 수동 블록 (가짜 예약 - 환자에게 노출 안 됨)
}
