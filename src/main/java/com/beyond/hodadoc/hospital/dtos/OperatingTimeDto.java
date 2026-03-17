package com.beyond.hodadoc.hospital.dtos;

import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.hospital.domain.HospitalOperatingTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OperatingTimeDto {
    private String dayOfWeek;       // 월화수목금토일
    private String openTime;        // "09:00"
    private String closeTime;       // "18:00"
    private String breakStartTime;  // "13:00"
    private String breakEndTime;    // "14:00"
    @JsonProperty("isDayOff")
    private boolean isDayOff;       // 정기 휴무 여부 (매주 반복되는 쉬는 날)

    public boolean isDayOff() {
        return isDayOff;
    }

    // DTO -> Entity 변환 메서드
    public HospitalOperatingTime toEntity(Hospital hospital) {
        return HospitalOperatingTime.builder()
                .hospital(hospital) // 부모 병원 연결
                .dayOfWeek(DayOfWeek.valueOf(this.dayOfWeek.toUpperCase())) // 대소문자 무시하고 Enum 변환
                .openTime(parseTime(this.openTime))
                .closeTime("23:59".equals(this.closeTime) ? LocalTime.of(23, 59, 59, 900_000_000) : parseTime(this.closeTime))
                .breakStartTime(parseTime(this.breakStartTime))
                .breakEndTime(parseTime(this.breakEndTime))
                .dayOff(this.isDayOff)
                .build();
    }

    // 시간 문자열 파싱 헬퍼 메서드
    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(timeStr); // HH:mm:ss 또는 HH:mm 지원
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("시간 형식이 올바르지 않습니다: " + timeStr);
        }
    }
}
