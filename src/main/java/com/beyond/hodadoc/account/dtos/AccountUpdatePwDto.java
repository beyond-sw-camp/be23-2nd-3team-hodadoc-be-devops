package com.beyond.hodadoc.account.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AccountUpdatePwDto {
    private String currentPassword;
    private String newPassword;
}
