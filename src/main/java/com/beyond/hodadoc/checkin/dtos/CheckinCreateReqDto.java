package com.beyond.hodadoc.checkin.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CheckinCreateReqDto {
    // 환자가 접수할 병원 ID
    private Long hospitalId;
}