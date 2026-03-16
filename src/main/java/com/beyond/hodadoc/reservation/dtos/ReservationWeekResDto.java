package com.beyond.hodadoc.reservation.dtos;

import com.beyond.hodadoc.reservation.domain.ReservationPatient;
import com.beyond.hodadoc.reservation.domain.ReservationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * GET /reservation/list 응답용 DTO
 * 프론트 주간 달력에서 reservationDate + reservationTime 조합으로 셀 매핑
 */
@Getter
@Builder
public class ReservationWeekResDto {

    private Long id;
    private String patientName;
    private LocalDate reservationDate;
    private String reservationTime;   // "HH:mm" 형태
    private String symptoms;
    private ReservationStatus status;
    private Long doctorId;
    private String doctorName;

    public static ReservationWeekResDto from(ReservationPatient r) {
        return ReservationWeekResDto.builder()
                .id(r.getId())
                .patientName(r.getPatient().getName())
                .reservationDate(r.getReservationDate())
                .reservationTime(r.getReservationTime() != null
                        ? r.getReservationTime().toString().substring(0, 5)
                        : null)
                .symptoms(r.getSymptoms())
                .status(r.getStatus())
                .doctorId(r.getDoctor().getId())
                .doctorName(r.getDoctor().getName())
                .build();
    }
}