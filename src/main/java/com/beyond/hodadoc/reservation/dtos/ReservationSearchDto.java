package com.beyond.hodadoc.reservation.dtos;

import com.beyond.hodadoc.reservation.domain.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ReservationSearchDto {
    private Long hospitalId;
    private Long patientId;
    private ReservationStatus status; //예약상태 필터
}
