package com.beyond.hodadoc.doctor.dtos;

import com.beyond.hodadoc.doctor.domain.OffDayType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class DoctorOffDayReqDto {
    private Long doctorId;
    @JsonFormat(pattern = "yyyy-MM-dd")

    private LocalDate offDate;
    private OffDayType type; // OFF, VACATION
}