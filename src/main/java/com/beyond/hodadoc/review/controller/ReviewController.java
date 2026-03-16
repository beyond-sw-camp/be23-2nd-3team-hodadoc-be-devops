package com.beyond.hodadoc.review.controller;


import com.beyond.hodadoc.review.dtos.ReviewCreateDto;
import com.beyond.hodadoc.review.dtos.ReviewListDto;
import com.beyond.hodadoc.review.dtos.ReviewStatsDto;
import com.beyond.hodadoc.review.dtos.ReviewUpdateDto;
import com.beyond.hodadoc.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/create")
    public void create(
            @RequestBody @Valid ReviewCreateDto dto
    ) {
        reviewService.save(dto);
    }
    @GetMapping("/{hospitalId}/hospitalist")
    public ResponseEntity<Page<ReviewListDto>> findByHospital(
            @PathVariable Long hospitalId,
            @PageableDefault(size = 10,sort = "id",direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ReviewListDto> result =
                reviewService.findByHospital(hospitalId, pageable);

        return ResponseEntity.ok(result);
    }
    @GetMapping("/myreviews")
    public ResponseEntity<Page<ReviewListDto>> getPatientReviews(
            @PageableDefault(size = 10,sort = "id",direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ReviewListDto> result =
                reviewService.findByPatientId(pageable);
        return  ResponseEntity.ok(result);
    }

    // 환자 본인 리뷰 삭제
    @DeleteMapping("/{reviewId}/delete")
    public ResponseEntity<String> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok("리뷰가 삭제되었습니다.");
    }
    //수정
    @PutMapping("/{reviewId}/update")
    public ResponseEntity<String> updateReview(
            @PathVariable Long reviewId,
            @RequestBody @Valid ReviewUpdateDto dto
    ) {
        // Service에서 SecurityContextHolder로 로그인 계정 ID를 가져와서 처리
        reviewService.updateReview(reviewId, dto);

        return ResponseEntity.ok("리뷰가 수정되었습니다.");
    }
    // 병원별 리뷰 평균 별점 + 전체 개수 조회 (삭제된 리뷰 제외)
    @GetMapping("/{hospitalId}/stats")
    public ResponseEntity<ReviewStatsDto> getStats(@PathVariable Long hospitalId) {
        return ResponseEntity.ok(reviewService.getStats(hospitalId));
    }

    // 예약에 대한 리뷰 작성 여부 확인
    @GetMapping("/exists/reservation/{reservationId}")
    public ResponseEntity<Boolean> existsByReservation(@PathVariable Long reservationId) {
        return ResponseEntity.ok(reviewService.existsReviewByReservationId(reservationId));
    }

    // 접수에 대한 리뷰 작성 여부 확인
    @GetMapping("/exists/checkin/{checkinId}")
    public ResponseEntity<Boolean> existsByCheckin(@PathVariable Long checkinId) {
        return ResponseEntity.ok(reviewService.existsReviewByCheckinId(checkinId));
    }
}

