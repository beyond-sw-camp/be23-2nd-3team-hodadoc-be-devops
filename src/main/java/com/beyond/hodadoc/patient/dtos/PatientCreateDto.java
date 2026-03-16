package com.beyond.hodadoc.patient.dtos;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.patient.domain.Patient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PatientCreateDto {
    @NotBlank(message = "이름이 비어있으면 안됩니다.")
    private String name;
    @NotBlank(message = "전화번호가 비어있으면 안됩니다.")
    @Pattern(regexp = "^01[016789]-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    private String phone;
    // email은 선택 - 없으면 account.email 사용 (카카오 재가입 플로우 지원)
    private String email;
//    @NotBlank(message = "비밀번호가 비어있으면 안됩니다.")
//    private String password;
    private String address;

}
