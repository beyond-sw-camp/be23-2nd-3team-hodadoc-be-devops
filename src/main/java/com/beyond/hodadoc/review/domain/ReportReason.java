package com.beyond.hodadoc.review.domain;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason {
    FALSE_INFORMATION("허위사실 유포"),
    INAPPROPRIATE_LANGUAGE("부적절 언어"),
    PRIVACY_VIOLATION("개인정보 노출"),
    SPAM("광고 및 도배"),
    OTHER("기타");

    private final String description;
}
