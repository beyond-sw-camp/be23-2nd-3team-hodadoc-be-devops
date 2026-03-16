package com.beyond.hodadoc.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@Builder
@Entity
public class HospitalHoliday {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    // 특정 휴무 날짜
    private LocalDate holidayDate;

    // 휴무 사유 (선택) - 예: "크리스마스", "내부 공사"
    private String reason;
}
