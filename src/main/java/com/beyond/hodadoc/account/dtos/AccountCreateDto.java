package com.beyond.hodadoc.account.dtos;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.account.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AccountCreateDto {
    @NotBlank(message = "email은 필수 값입니다.")
    private String email;

    @NotBlank(message = "password는 필수 값입니다.")
    @Size(min = 8, message = "password의 길이가 너무 짧습니다.")
    private String password;

    @NotNull(message = "role은 필수 값입니다.")
    private Role role; // ADMIN / PATIENT / HOSPITAL_ADMIN

    public Account toEntity(String encodedPassword) {
        return Account.builder()
                .email(this.email)
                .password(encodedPassword)
                .role(this.role)
                .delYn("N")
                .build();

    }
}
