package com.beyond.hodadoc.hospital.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class HospitalNearbyResponseDto {
    private Long id;
    private String name;
    private String phone;
    private Double latitude;
    private Double longitude;
    private Double distance; //km 단위


}
