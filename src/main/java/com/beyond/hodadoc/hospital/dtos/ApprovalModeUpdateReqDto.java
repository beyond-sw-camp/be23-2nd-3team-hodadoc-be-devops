package com.beyond.hodadoc.hospital.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalModeUpdateReqDto {
    private String approvalMode; // "AUTO" 또는 "MANUAL"
}
