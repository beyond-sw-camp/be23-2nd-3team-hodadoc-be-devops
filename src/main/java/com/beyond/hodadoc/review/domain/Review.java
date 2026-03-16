package com.beyond.hodadoc.review.domain;


import com.beyond.hodadoc.checkin.domain.Checkin;
import com.beyond.hodadoc.common.domain.BaseTimeEntity;
import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.patient.domain.Patient;
import com.beyond.hodadoc.reservation.domain.ReservationPatient;
import com.beyond.hodadoc.review.dtos.ReviewUpdateDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 병원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    // 환자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    //예약
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private ReservationPatient reservation;

    //접수
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkin_id")
    private Checkin checkin;

    // 내용
    @Column(nullable = false, length = 255)
    private String contents;

    //리뷰 상태
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.NORMAL;

    //신고 횟수
    @Column(nullable = false)
    @Builder.Default
    private  int reportCount = 0;

    // 평점
    @Min(1)
    @Max(5)
    private int rating;

    //삭제
    public void reviewDeleted() {
        this.status = ReviewStatus.DELETED;
    }

    // 전체 수정
    public void updateAll(ReviewUpdateDto dto) {
        this.contents = dto.getContents();
        this.rating = dto.getRating();
    }
    // 신고 횟수
    public void increaseReportCount() {
        this.reportCount += 1;
    }
    //신고 상태
    public void markAsReported() {
        // ReviewStatus.REPORTED 또는 "REPORTED" 등 프로젝트 설정에 맞게 변경
        this.status = ReviewStatus.REPORTED;
    }
    //신고 -> 삭제
    public void deleteByAdmin() {
        this.status = ReviewStatus.DELETED;
    }
    //신고 -> 정상
    public void markAsNormal() {
        this.status = ReviewStatus.NORMAL;
        this.reportCount = 0; // 신고 횟수도 깨끗하게 0으로 초기화!
    }
}

