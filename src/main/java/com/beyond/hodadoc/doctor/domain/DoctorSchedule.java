package com.beyond.hodadoc.doctor.domain;

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
public class DoctorSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private LocalTime lunchStartTime;
    private LocalTime lunchEndTime;
    private int consultationInterval;

    // ✅ boolean + is prefix + @Builder.Default 조합 버그 방지
    // → 필드명을 dayOff로 변경하고 getter를 명시적으로 선언
    @Column(name = "is_day_off", nullable = false)
    @Builder.Default
    private boolean dayOff = false;

    public boolean isDayOff() {
        return dayOff;
    }

    public void update(LocalTime workStartTime, LocalTime workEndTime,
                       LocalTime lunchStartTime, LocalTime lunchEndTime,
                       int consultationInterval, boolean isDayOff) {
        this.workStartTime = workStartTime;
        this.workEndTime = workEndTime;
        this.lunchStartTime = lunchStartTime;
        this.lunchEndTime = lunchEndTime;
        this.consultationInterval = consultationInterval;
        this.dayOff = isDayOff;
    }
}