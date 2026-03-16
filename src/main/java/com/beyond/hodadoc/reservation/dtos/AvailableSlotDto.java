package com.beyond.hodadoc.reservation.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AvailableSlotDto {
    private LocalTime time; // 예약 가능한 시간 (on-the-fly 계산 결과)
    @Builder.Default
    private boolean available = true; // 예약 가능 여부
    @Builder.Default
    private boolean blocked = false; // 병원 관리자가 블록한 슬롯 여부
}
