package com.beyond.hodadoc.review.controller;


import com.beyond.hodadoc.review.dtos.ReviewReportAdminListDto;
import com.beyond.hodadoc.review.dtos.ReviewReportCreateDto;
import com.beyond.hodadoc.review.dtos.ReviewReportDetailDto;
import com.beyond.hodadoc.review.service.ReviewReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviewreports")
@RequiredArgsConstructor
public class ReviewReportController {

    private final ReviewReportService reviewReportService;

    @PostMapping("/{reviewId}")
    public ResponseEntity<String> createReport(
            @PathVariable Long reviewId,
            @RequestBody ReviewReportCreateDto dto) {

        // 서비스에게 모든 처리를 위임 (검증, 중복 체크, 저장 등)
        reviewReportService.report(reviewId, dto);

        // 처리가 완료되면 성공 메시지와 함께 200 OK 응답
        return ResponseEntity.ok("리뷰 신고가 정상적으로 접수되었습니다.");
    }

    //리뷰 신고 최신순 조회(관리자용)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reviews")
    public ResponseEntity<Page<ReviewReportAdminListDto>> getReportedReviews(
            // [정렬] 1순위: 신고 횟수 높은 순, 2순위: ID(최신) 순
            @PageableDefault(
                    size = 10,
                    sort = {"reportCount", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        // 서비스에게 "가공된 데이터 가져와!"라고 시킨 뒤 응답합니다.
        return ResponseEntity.ok(reviewReportService.getReportedReviewList(pageable));
    }
    //삭제
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{reviewId}/delete")
    public ResponseEntity<String> approve(@PathVariable Long reviewId) {
        reviewReportService.approveReport(reviewId);
        return ResponseEntity.ok("삭제 완료");
    }
    //반려
    @PreAuthorize("hasRole('ADMIN')")    
    @PostMapping("/{reviewId}/reject")
    public ResponseEntity<String> rejectReport(@PathVariable Long reviewId) {
        reviewReportService.rejectReviewReport(reviewId);
        return ResponseEntity.ok("신고가 반려되어 리뷰가 정상 상태로 복구되었습니다.");
    }

}
