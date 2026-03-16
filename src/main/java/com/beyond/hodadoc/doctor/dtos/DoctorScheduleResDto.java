package com.beyond.hodadoc.doctor.dtos;

import com.beyond.hodadoc.doctor.domain.DoctorSchedule;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import java.time.DayOfWeek;

@Getter
@Builder
public class DoctorScheduleResDto {
    private Long id;
    private DayOfWeek dayOfWeek;
    private String workStartTime;
    private String workEndTime;
    private String lunchStartTime;
    private String lunchEndTime;
    private int consultationInterval;

    // ✅ boolean + is prefix → JSON 직렬화 시 "dayOff"로 나가는 문제 해결
    @JsonProperty("isDayOff")
    private boolean isDayOff;

    public boolean isDayOff() {
        return isDayOff;
    }

    public static DoctorScheduleResDto from(DoctorSchedule s) {
        return DoctorScheduleResDto.builder()
                .id(s.getId())
                .dayOfWeek(s.getDayOfWeek())
                .workStartTime(s.getWorkStartTime() != null ? s.getWorkStartTime().toString() : null)
                .workEndTime(s.getWorkEndTime() != null ? s.getWorkEndTime().toString() : null)
                .lunchStartTime(s.getLunchStartTime() != null ? s.getLunchStartTime().toString() : null)
                .lunchEndTime(s.getLunchEndTime() != null ? s.getLunchEndTime().toString() : null)
                .consultationInterval(s.getConsultationInterval())
                .isDayOff(s.isDayOff())
                .build();
    }
}