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
public class ReservationHospitalListDto {
    private Long reservationPatientId;
    private Long patientId;
    private String patientName;
    private String doctorName;

    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private String symptoms;
    private ReservationStatus status;

    public static ReservationHospitalListDto fromEntity(ReservationPatient rp) {
        String patientName = null;
        if (rp.getPatient() != null) {
            patientName = rp.getPatient().getName() != null ? rp.getPatient().getName() : "탈퇴한 회원";
        }
        return ReservationHospitalListDto.builder()
                .reservationPatientId(rp.getId())
                .patientId(rp.getPatient() != null ? rp.getPatient().getId() : null)
                .patientName(patientName)
                .doctorName(rp.getDoctor().getName())
                .reservationDate(rp.getReservationDate())
                .reservationTime(rp.getReservationTime())
                .symptoms(rp.getSymptoms())
                .status(rp.getStatus())
                .build();
    }
}
