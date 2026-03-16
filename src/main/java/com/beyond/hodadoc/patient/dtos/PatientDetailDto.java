package com.beyond.hodadoc.patient.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PatientDetailDto {
    private Long patientId;
    private String name;
    private String email;
    private String phone;
    private String address;
}
