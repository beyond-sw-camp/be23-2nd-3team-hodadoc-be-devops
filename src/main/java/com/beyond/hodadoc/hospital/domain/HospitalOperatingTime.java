package com.beyond.hodadoc.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@Builder
@Entity
public class HospitalOperatingTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    // 요일 (월~일) - Java의 DayOfWeek Enum 사용
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    // 운영 시간
    private LocalTime openTime;
    private LocalTime closeTime;

    // 점심시간
    private LocalTime breakStartTime;
    private LocalTime breakEndTime;

    // 휴무 여부 (is prefix + Builder 조합 버그 방지 → 필드명 dayOff로 변경)
    @Column(name = "is_day_off")
    @Builder.Default
    private boolean dayOff = false;

    public boolean isDayOff() {
        return dayOff;
    }
}
