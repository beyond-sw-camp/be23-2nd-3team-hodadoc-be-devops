package com.beyond.hodadoc.review.service;

import com.beyond.hodadoc.checkin.domain.Checkin;
import com.beyond.hodadoc.checkin.domain.CheckinStatus;
import com.beyond.hodadoc.checkin.repository.CheckinRepository;
import com.beyond.hodadoc.reservation.domain.ReservationPatient;
import com.beyond.hodadoc.review.repository.BadWordRepository;
import com.beyond.hodadoc.reservation.domain.ReservationStatus;
import com.beyond.hodadoc.reservation.repository.ReservationRepository;
import com.beyond.hodadoc.review.domain.ReviewStatus;
import com.beyond.hodadoc.review.dtos.ReviewCreateDto;
import com.beyond.hodadoc.review.dtos.ReviewListDto;
import com.beyond.hodadoc.review.dtos.ReviewStatsDto;
import com.beyond.hodadoc.review.dtos.ReviewUpdateDto;
import com.beyond.hodadoc.review.repository.ReviewRepository;
import com.beyond.hodadoc.review.domain.Review;
import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.hospital.repository.HospitalRepository;
import com.beyond.hodadoc.patient.domain.Patient;
import com.beyond.hodadoc.patient.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository        reviewRepository;
    private final HospitalRepository      hospitalRepository;
    private final PatientRepository       patientRepository;
    private final ReservationRepository   reservationPatientRepository;
    private final CheckinRepository       checkinRepository;
    private final BadWordRepository       badWordRepository;
    private final RatingCacheService      ratingCacheService; // ✅ 추가

    // ── 욕설 필터 ─────────────────────────────────────────────────────────
    private String filterBadWords(String text) {
        if (text == null) return null;
        List<String> badWords = badWordRepository.findAllWords();
        for (String word : badWords) {
            String stars = "*".repeat(word.length());
            text = text.replaceAll("(?i)" + word, stars);
        }
        return text;
    }

    // ── 리뷰 등록 ─────────────────────────────────────────────────────────
    public void save(ReviewCreateDto dto) {
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Patient patient = patientRepository.findByAccountIdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        ReservationPatient reservation = null;
        Checkin            checkin     = null;
        Hospital           hospital;

        if (dto.getReservationId() != null) {
            // 예약 경로
            reservation = reservationPatientRepository.findById(dto.getReservationId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));
            hospital = reservation.getHospital();

            if (!reservation.getPatient().getId().equals(patient.getId()))
                throw new IllegalStateException("본인의 예약만 리뷰를 작성할 수 있습니다.");
            if (reservation.getStatus() != ReservationStatus.COMPLETED)
                throw new IllegalStateException("진료가 완료된 후에만 리뷰를 작성할 수 있습니다.");
            if (reviewRepository.existsByReservationId(reservation.getId()))
                throw new IllegalStateException("이미 작성된 리뷰가 있습니다.");

        } else if (dto.getCheckinId() != null) {
            // 접수 경로
            checkin  = checkinRepository.findByIdAndStatus(dto.getCheckinId(), CheckinStatus.COMPLETED)
                    .orElseThrow(() -> new IllegalStateException("진료가 완료된 접수가 아닙니다."));
            hospital = checkin.getHospital();

            if (!checkin.getPatient().getId().equals(patient.getId()))
                throw new IllegalStateException("본인의 접수만 리뷰를 작성할 수 있습니다.");
            if (reviewRepository.existsByCheckinId(checkin.getId()))
                throw new IllegalStateException("이미 작성된 리뷰가 있습니다.");

        } else {
            throw new IllegalArgumentException("예약 ID 또는 접수 ID 중 하나는 필수입니다.");
        }

        dto.setContents(filterBadWords(dto.getContents()));
        Review review = dto.toEntity(hospital, patient, reservation, checkin);
        reviewRepository.save(review);

        // ✅ 캐시에 별점 추가
        ratingCacheService.onReviewCreated(hospital.getId(), dto.getRating());
    }

    // ── 병원별 리뷰 목록 조회 ─────────────────────────────────────────────
    public Page<ReviewListDto> findByHospital(Long hospitalId, Pageable pageable) {
        hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 병원입니다."));
        return reviewRepository
                .findByHospitalIdAndStatusNot(hospitalId, ReviewStatus.DELETED, pageable)
                .map(ReviewListDto::new);
    }

    // ── 환자 본인 리뷰 목록 조회 ──────────────────────────────────────────
    public Page<ReviewListDto> findByPatientId(Pageable pageable) {
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Patient patient = patientRepository.findByAccountIdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 환자입니다."));
        return reviewRepository
                .findByPatientIdAndStatusNot(patient.getId(), ReviewStatus.DELETED, pageable)
                .map(ReviewListDto::new);
    }

    // ── 리뷰 삭제 ─────────────────────────────────────────────────────────
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findByIdAndStatusNot(reviewId, ReviewStatus.DELETED)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 이미 삭제된 리뷰입니다."));

        Long loginAccountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ADMIN"));
        Long accountId = review.getPatient().getAccount().getId();

        if (!isAdmin && !accountId.equals(loginAccountId))
            throw new IllegalArgumentException("삭제 권한이 없습니다.");

        Long   hospitalId = review.getHospital().getId();
        int    oldRating  = review.getRating();

        review.reviewDeleted();

        // ✅ 캐시에서 별점 제거
        ratingCacheService.onReviewDeleted(hospitalId, oldRating);
    }

    // ── 리뷰 수정 ─────────────────────────────────────────────────────────
    @Transactional
    public void updateReview(Long reviewId, ReviewUpdateDto dto) {
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Patient patient = patientRepository.findByAccountIdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("등록된 환자 정보가 없습니다."));

        Review review = reviewRepository.findByIdAndStatusNot(reviewId, ReviewStatus.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("리뷰가 존재하지 않습니다."));

        if (!review.getPatient().getId().equals(patient.getId()))
            throw new IllegalStateException("본인이 작성한 리뷰만 수정할 수 있습니다.");

        int    oldRating  = review.getRating();
        Long   hospitalId = review.getHospital().getId();

        dto.setContents(filterBadWords(dto.getContents()));
        review.updateAll(dto);

        // ✅ 캐시에서 별점 차이만큼 반영
        ratingCacheService.onReviewUpdated(hospitalId, oldRating, dto.getRating());
    }

    // ── 평균 별점 + 리뷰 수 조회 (Redis 캐시 우선) ──────────────────────
    public ReviewStatsDto getStats(Long hospitalId) {
        hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 병원입니다."));

        // ✅ DB 집계 쿼리 대신 Redis에서 O(1) 조회
        double avg   = ratingCacheService.getAvgRating(hospitalId);
        long   count = ratingCacheService.getReviewCount(hospitalId);

        return new ReviewStatsDto(avg, count);
    }

    // ── 리뷰 작성 여부 확인 ───────────────────────────────────────────────
    public boolean existsReviewByReservationId(Long reservationId) {
        return reviewRepository.existsByReservationId(reservationId);
    }

    public boolean existsReviewByCheckinId(Long checkinId) {
        return reviewRepository.existsByCheckinId(checkinId);
    }
}