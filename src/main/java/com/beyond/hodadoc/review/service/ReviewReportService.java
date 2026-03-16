package com.beyond.hodadoc.review.service;

import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.hospital.repository.HospitalRepository;
import com.beyond.hodadoc.patient.domain.Patient;
import com.beyond.hodadoc.patient.repository.PatientRepository;
import com.beyond.hodadoc.review.domain.ReportReason;
import com.beyond.hodadoc.review.domain.Review;
import com.beyond.hodadoc.review.domain.ReviewReport;
import com.beyond.hodadoc.review.domain.ReviewStatus;
import com.beyond.hodadoc.review.dtos.ReviewReportAdminListDto;
import com.beyond.hodadoc.review.dtos.ReviewReportCreateDto;
import com.beyond.hodadoc.review.dtos.ReviewReportDetailDto;
import com.beyond.hodadoc.review.repository.ReviewReportRepository;
import com.beyond.hodadoc.review.repository.ReviewRepository;
import com.beyond.hodadoc.common.domain.AlarmType;
import com.beyond.hodadoc.common.service.SseAlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewReportService {

    private final ReviewReportRepository reviewReportRepository;
    private final ReviewRepository reviewRepository;
    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;
    private final SseAlarmService sseAlarmService;
    private final RatingCacheService ratingCacheService;

    public void report(Long reviewId, ReviewReportCreateDto dto) {
        // 1. 리뷰 존재 확인
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다."));

        // 2. 인증 정보에서 계정 ID와 권한 추출
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long accountId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().iterator().next().getAuthority();

        Patient patient = null;
        Hospital hospital = null;

        // 3. 권한별 신고자 식별 및 중복 체크
        if (role.equals("ROLE_PATIENT")) {
            patient = patientRepository.findByAccountIdAndAccount_DelYn(accountId, "N")
                    .orElseThrow(() -> new IllegalArgumentException("환자 정보를 찾을 수 없습니다."));
            hospital = review.getHospital();

            // 본인 리뷰 신고 방지 추가
            if (review.getPatient().getId().equals(patient.getId())) {
                throw new IllegalStateException("본인이 작성한 리뷰는 신고할 수 없습니다.");
            }
            if (reviewReportRepository.existsByReviewAndPatient(review, patient)) {
                throw new IllegalStateException("이미 신고한 리뷰입니다.");
            }
        } else if (role.equals("ROLE_HOSPITAL_ADMIN")) {
            hospital = hospitalRepository.findByAccount_IdAndAccount_DelYn(accountId, "N")
                    .orElseThrow(() -> new IllegalArgumentException("병원 정보를 찾을 수 없습니다."));
            // 본인 병원 리뷰인지 검증 추가
            if (!review.getHospital().getId().equals(hospital.getId())) {
                throw new IllegalStateException("본인 병원의 리뷰만 신고할 수 있습니다.");
            }
            if (reviewReportRepository.existsByReviewAndHospital(review, hospital)) {
                throw new IllegalStateException("이미 신고한 리뷰입니다.");
            }
        } else {
            // ADMIN 등 나머지 권한 신고 불가 추가
            throw new IllegalStateException("신고 권한이 없습니다.");
        }

        // 5. 엔티티 생성 및 저장
        ReviewReport report = dto.toEntity(review, patient, hospital);
        reviewReportRepository.save(report);

        // 6. 리뷰 엔티티의 신고 카운트 증가
        review.increaseReportCount();

        // 7. 리뷰의 상태를 신고됨(REPORTED)으로 변경
        review.markAsReported();
    }
public Page<ReviewReportAdminListDto> getReportedReviewList(Pageable pageable) {
    Page<Review> reviewPage = reviewRepository
            .findByStatusAndReportCountGreaterThan(ReviewStatus.REPORTED, 0, pageable);

    List<Long> reviewIds = reviewPage.getContent().stream()
            .map(Review::getId).toList();

    Map<Long, Map<String, Integer>> reasonCountMap = reviewReportRepository
            .countByReasonAndReviewIds(reviewIds)
            .stream()
            .collect(Collectors.groupingBy(
                    row -> (Long) row[0],
                    Collectors.toMap(
                            row -> ((ReportReason) row[1]).getDescription(),
                            row -> ((Long) row[2]).intValue()
                    )
            ));

    return reviewPage.map(review -> new ReviewReportAdminListDto(
            review.getId(),
            review.getContents(),
            review.getRating(),
            review.getReportCount(),
            review.getHospital().getId(),
            review.getHospital().getName(),
            review.getStatus().toString(),
            reasonCountMap.getOrDefault(review.getId(), Map.of()),
            review.getPatient() != null ? review.getPatient().getName() : null,
            review.getCreatedTime() != null ? review.getCreatedTime().toString() : null
    ));
}

    @Transactional
    public void approveReport(Long reviewId) {
        // 1. 리뷰 엔티티 조회
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰 없음"));

        // 2. [수정 지점] setStatus 대신 파트너님이 만든 메서드 호출!
        Long hospitalId = review.getHospital().getId();
        int oldRating = review.getRating();

        review.deleteByAdmin();

        // ✅ 캐시에서 별점 제거
        ratingCacheService.onReviewDeleted(hospitalId, oldRating);

        // 3. 신고 테이블에서도 물리적 삭제 (하드 딜리트)
        reviewReportRepository.deleteByReviewId(reviewId);

        // 4. SSE 알림: 리뷰 작성자(환자)에게 삭제 알림 발송
        if (review.getPatient() != null && review.getPatient().getAccount() != null) {
            sseAlarmService.sendMessage(
                    review.getPatient().getAccount().getId(),
                    "작성하신 리뷰가 운영 정책 위반으로 삭제되었습니다.",
                    AlarmType.REVIEW_DELETED.name(),
                    null);
        }
    }
    @Transactional
    public void rejectReviewReport(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰 없음"));

        // 상태를 NORMAL로 바꾸고 횟수도 0으로 만듦
        review.markAsNormal();

        // 신고 내역은 물리적으로 삭제
        reviewReportRepository.deleteByReviewId(reviewId);
    }
}
