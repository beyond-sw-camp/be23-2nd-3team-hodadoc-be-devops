package com.beyond.hodadoc.reservation.dtos;

import com.beyond.hodadoc.reservation.domain.ReservationPatient;
import com.beyond.hodadoc.reservation.domain.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ReservationPatientListDto {
    private Long reservationPatientId;
    private String hospitalName;
    private Long hospitalId;
    private String doctorName;

    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private String symptoms;
    private ReservationStatus status;

    public static ReservationPatientListDto fromEntity(ReservationPatient rp) {
        return ReservationPatientListDto.builder()
                .reservationPatientId(rp.getId())
                .hospitalName(rp.getHospital().getName())
                .hospitalId(rp.getHospital().getId())
                .doctorName(rp.getDoctor().getName())
                .reservationDate(rp.getReservationDate())
                .reservationTime(rp.getReservationTime())
                .symptoms(rp.getSymptoms())
                .status(rp.getStatus())
                .build();
    }
}
