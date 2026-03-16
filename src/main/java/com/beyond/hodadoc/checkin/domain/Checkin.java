package com.beyond.hodadoc.checkin.domain;

import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.patient.domain.Patient;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
@Table(name = "checkin")
public class Checkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 접수한 환자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private Patient patient;

    // 접수 대상 병원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private Hospital hospital;

    // 접수 시각 (서버 시간 자동 기록)
    @Column(nullable = false, updatable = false)
    private LocalDateTime checkinTime;

    // 대기 번호 (같은 병원 당일 접수 순서)
    @Column(nullable = false)
    private Integer waitingNumber;

    // 접수 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CheckinStatus status = CheckinStatus.WAITING;

    // 상태 변경 (병원 측에서 호출/완료 처리)
    public void updateStatus(CheckinStatus status) {
        this.status = status;
    }
}