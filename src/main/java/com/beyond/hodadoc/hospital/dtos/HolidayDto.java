package com.beyond.hodadoc.hospital.dtos;

import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.hospital.domain.HospitalHoliday;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class HolidayDto {
    private LocalDate holidayDate;  // "2024-12-25"
    private String reason;          // "성탄절"

    public HospitalHoliday toEntity(Hospital hospital) {
        return HospitalHoliday.builder()
                .hospital(hospital)
                .holidayDate(this.holidayDate)
                .reason(this.reason)
                .build();
    }
}
