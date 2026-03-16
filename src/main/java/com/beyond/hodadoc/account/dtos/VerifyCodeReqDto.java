package com.beyond.hodadoc.account.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyCodeReqDto {
    @NotBlank
    private String email;
    @NotBlank
    private String code;
}
