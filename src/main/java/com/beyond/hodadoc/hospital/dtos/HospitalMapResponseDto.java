package com.beyond.hodadoc.hospital.dtos;

import com.beyond.hodadoc.hospital.domain.Hospital;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class HospitalMapResponseDto {
    private Long id;
    private String name;
    private String phone;
    private Double latitude;
    private Double longitude;
    private Boolean isOpenNow;

    public static HospitalMapResponseDto fromEntity(Hospital hospital, boolean isOpenNow){
        return HospitalMapResponseDto.builder()
                .id(hospital.getId())
                .name(hospital.getName())
                .phone(hospital.getPhone())
                .latitude(hospital.getAddress().getLatitude())
                .longitude(hospital.getAddress().getLongitude())
                .isOpenNow(isOpenNow)
                .build();
    }
}
