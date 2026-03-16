package com.beyond.hodadoc.reservation.dtos;

import com.beyond.hodadoc.reservation.domain.ReservationStatus;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReservationStatusUpdateReqDto {
    private ReservationStatus status;
}