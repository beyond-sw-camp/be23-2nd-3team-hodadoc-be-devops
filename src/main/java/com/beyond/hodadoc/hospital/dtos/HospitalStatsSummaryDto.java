package com.beyond.hodadoc.hospital.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HospitalStatsSummaryDto {
    private long todayReservation;   // 오늘 예약 건수
    private long monthReservation;   // 이번달 예약 건수
    private long todayCheckin;       // 오늘 접수 건수
    private long monthCheckin;       // 이번달 접수 건수
}
