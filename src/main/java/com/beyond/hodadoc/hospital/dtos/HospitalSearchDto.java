package com.beyond.hodadoc.hospital.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class HospitalSearchDto {
    private String name;
    private String sido;
    private String sigungu;
    private String emdName;
    private String departmentName;

    private String nightFilter;
    private String holidayFilter;
    private String facilityFilter;
    private String serviceFilter;
    private Boolean isCurrentlyOpen;

    private Double userLat;
    private Double userLng;

    // DB 쿼리 비교용 서버 현재 시간 및 요일
    private LocalTime currentTime;
    private String currentDayOfWeekString; // 예: "MONDAY"
}
