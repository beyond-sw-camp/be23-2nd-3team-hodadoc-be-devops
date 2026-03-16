package com.beyond.hodadoc.hospital.dtos;

import com.beyond.hodadoc.hospital.domain.HospitalStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class HospitalStatusUpdateDto {
    private HospitalStatus status; // PENDING, APPROVED, REJECTED
}
