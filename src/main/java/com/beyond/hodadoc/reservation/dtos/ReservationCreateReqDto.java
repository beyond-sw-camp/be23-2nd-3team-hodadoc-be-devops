package com.beyond.hodadoc.reservation.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReservationCreateReqDto {
    private Long doctorId;
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private String symptoms; // 증상 메모 (선택)
}
