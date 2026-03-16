package com.beyond.hodadoc.checkin.domain;

public enum CheckinStatus {
    WAITING,   // 대기중
    CALLED,    // 호출됨 (병원 측에서 번호 불렀을 때)
    COMPLETED, // 진료 완료
    CANCELLED  // 환자가 취소
}
