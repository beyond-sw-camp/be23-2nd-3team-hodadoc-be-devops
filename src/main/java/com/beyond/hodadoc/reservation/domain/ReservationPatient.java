package com.beyond.hodadoc.reservation.domain;

import com.beyond.hodadoc.doctor.domain.Doctor;
import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.patient.domain.Patient;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@Builder
@Entity
@Table(
        name = "reservation",
        uniqueConstraints = {
                // [핵심 추가] 같은 의사 + 날짜 + 시간 슬롯에는 단 하나의 예약만 허용
                // 분산락이 순서를 보장하고, 이 제약이 DB 레벨 최후 방어선 역할
                @UniqueConstraint(
                        name = "uk_reservation_slot",
                        columnNames = {"doctor_id", "reservation_date", "reservation_time"}
                )
        }
)
public class ReservationPatient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 환자 (BLOCKED 상태일 때는 null 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT))
    private Patient patient;

    // 담당 의사 (필수)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private Doctor doctor;

    // 병원 (필수 - 빠른 병원별 조회용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private Hospital hospital;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "reservation_time", nullable = false)
    private LocalTime reservationTime;

    // 증상 메모
    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.WAITING;

    public void updateStatus(ReservationStatus status) {
        this.status = status;
    }
}