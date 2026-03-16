package com.beyond.hodadoc.doctor.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter @ToString
@Builder
@Entity
public class DoctorOffDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDate offDate; // 특정 휴무/블록 날짜

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OffDayType type = OffDayType.OFF;

    // ✅ 추가: 배치 등록 시 기존 레코드 타입 변경용
    public void updateType(OffDayType type) {
        this.type = type;
    }
}