package com.beyond.hodadoc.hospital.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HospitalDoctorRankingDto {
    private int rank;
    private String doctorName;
    private long reservationCount;
}
