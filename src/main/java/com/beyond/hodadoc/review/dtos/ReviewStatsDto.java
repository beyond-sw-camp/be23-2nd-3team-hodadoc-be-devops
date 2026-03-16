package com.beyond.hodadoc.review.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 병원별 리뷰 통계 (평균 별점 + 전체 리뷰 개수) 응답 DTO
@Getter
@AllArgsConstructor
public class ReviewStatsDto {
    private double avgRating;   // 삭제되지 않은 리뷰 전체 평균 별점
    private long reviewCount;   // 삭제되지 않은 리뷰 전체 개수
}
