package com.beyond.hodadoc.doctor.dtos;

import com.beyond.hodadoc.doctor.domain.DoctorOffDay;
import com.beyond.hodadoc.doctor.domain.OffDayType;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class DoctorOffDayResDto {
    private Long id;
    private LocalDate offDate;
    private OffDayType type;
    private String reason;

    public static DoctorOffDayResDto from(DoctorOffDay o) {
        return DoctorOffDayResDto.builder()
                .id(o.getId())
                .offDate(o.getOffDate())
                .type(o.getType())
                .build();
    }

    public static DoctorOffDayResDto fromHospitalHoliday(LocalDate date, String reason) {
        return DoctorOffDayResDto.builder()
                .offDate(date)
                .type(OffDayType.HOSPITAL_HOLIDAY)
                .reason(reason)
                .build();
    }
}