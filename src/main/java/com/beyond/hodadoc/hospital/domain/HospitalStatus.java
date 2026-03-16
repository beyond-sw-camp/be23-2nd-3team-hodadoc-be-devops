package com.beyond.hodadoc.hospital.domain;

public enum HospitalStatus {
    PENDING,   // 승인 대기 중
    APPROVED,  // 승인 완료 (서비스 노출 가능)
    REJECTED,  // 승인 거절
    DELETED    // 서비스 종료 (Soft Delete)
}