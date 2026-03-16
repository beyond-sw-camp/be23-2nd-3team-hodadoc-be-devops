package com.beyond.hodadoc.common.dtos;

import com.beyond.hodadoc.common.domain.PublicHoliday;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublicHolidayResDto {
    private Long id;
    private LocalDate holidayDate;
    private String dateName;
    private int year;

    public static PublicHolidayResDto fromEntity(PublicHoliday entity) {
        return PublicHolidayResDto.builder()
                .id(entity.getId())
                .holidayDate(entity.getHolidayDate())
                .dateName(entity.getDateName())
                .year(entity.getYear())
                .build();
    }
}
