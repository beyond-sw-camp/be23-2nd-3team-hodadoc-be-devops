package com.beyond.hodadoc.account.controller;

import com.beyond.hodadoc.account.dtos.ResetPasswordReqDto;
import com.beyond.hodadoc.account.dtos.SendVerificationReqDto;
import com.beyond.hodadoc.account.dtos.VerifyCodeReqDto;
import com.beyond.hodadoc.account.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-verification") //인증번호 발송하는 api
    public ResponseEntity<?> sendVerification(@RequestBody @Valid SendVerificationReqDto dto) {
        authService.sendVerification(dto.getEmail(), dto.getPhone());
        return ResponseEntity.ok("인증번호가 발송되었습니다.");
    }

    @PostMapping("/verify-code") //인증번호 입력해서 확인하는 api
    public ResponseEntity<?> verifyCode(@RequestBody @Valid VerifyCodeReqDto dto) {
        authService.verifyCode(dto.getEmail(), dto.getCode());
        return ResponseEntity.ok("인증이 완료되었습니다.");
    }

    @PostMapping("/reset-password") //새로운 비밀번호로 변경하는 api
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordReqDto dto) {
        authService.resetPassword(dto.getEmail(), dto.getNewPassword());
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }
}
