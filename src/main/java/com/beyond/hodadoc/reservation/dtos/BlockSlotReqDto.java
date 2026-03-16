package com.beyond.hodadoc.reservation.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class BlockSlotReqDto {
    private Long doctorId;
    private LocalDate date;
    private LocalTime time;
}
