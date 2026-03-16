package com.beyond.hodadoc.account.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class AccountLoginResDto {
    private String accessToken;
    private String refreshToken;
}