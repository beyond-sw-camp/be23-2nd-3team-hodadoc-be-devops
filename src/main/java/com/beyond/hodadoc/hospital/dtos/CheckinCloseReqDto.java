package com.beyond.hodadoc.hospital.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckinCloseReqDto {
    private boolean closed; // true = 마감, false = 재개
}
