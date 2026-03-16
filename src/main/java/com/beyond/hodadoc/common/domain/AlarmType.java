package com.beyond.hodadoc.common.domain;

public enum AlarmType {
    // 예약 관련
    RESERVATION_AUTO_APPROVED,    // 자동승인 (병원에게)
    RESERVATION_WAITING,          // 새 예약 대기 (병원에게)
    RESERVATION_APPROVED,         // 승인됨 (환자에게)
    RESERVATION_REJECTED,         // 거절됨 (환자에게)
    RESERVATION_CANCELLED,        // 취소됨 (양쪽)
    RESERVATION_REMINDER,         // 예약 2시간 전 리마인더 (환자에게 SMS)

    // 접수 관련
    RECEPTION_CREATED,            // 새 접수 (병원에게)
    RECEPTION_CALLED,             // 호출 (환자에게)
    RECEPTION_QUEUE_UPDATE,       // 대기순번 업데이트 (환자에게)

    // 리뷰 관련
    REVIEW_DELETED,               // 신고 리뷰 삭제됨 (환자에게)

    // 인증 관련
    VERIFICATION_CODE,            // SMS 인증번호 발송

    // 채팅 관련
    CHAT_MESSAGE                  // 새 채팅 메시지 (수신자에게 SSE)
}
