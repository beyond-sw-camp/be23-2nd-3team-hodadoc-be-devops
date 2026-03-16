package com.beyond.hodadoc.account.dtos;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.account.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AccountDetailDto {
    private Long id;
    private String email;
    private Role role;

    public static AccountDetailDto fromEntity(Account account) {
        return AccountDetailDto.builder()
                .id(account.getId())
                .email(account.getEmail())
                .role(account.getRole())
                .build();
    }
}
