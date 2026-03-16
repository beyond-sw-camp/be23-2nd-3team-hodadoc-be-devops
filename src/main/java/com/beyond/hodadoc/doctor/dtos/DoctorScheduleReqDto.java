package com.beyond.hodadoc.doctor.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.DayOfWeek;

@Getter
@NoArgsConstructor
public class DoctorScheduleReqDto {
    private DayOfWeek dayOfWeek;
    private String workStartTime;     // "09:00"
    private String workEndTime;       // "18:00"
    private String lunchStartTime;    // "12:00"
    private String lunchEndTime;      // "13:00"
    private int consultationInterval; // 30

    // ✅ Lombok boolean + is prefix → Jackson이 "dayOff"로 인식하는 문제 해결
    @JsonProperty("isDayOff")
    private boolean isDayOff;

    public boolean isDayOff() {
        return isDayOff;
    }
}