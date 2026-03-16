package com.beyond.hodadoc.account.controller;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.account.domain.SocialType;
import com.beyond.hodadoc.account.dtos.*;
import com.beyond.hodadoc.account.service.AccountService;
import com.beyond.hodadoc.account.service.KakaoService;
import com.beyond.hodadoc.common.auth.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoService kakaoService;

    public AccountController(AccountService accountService, JwtTokenProvider jwtTokenProvider, KakaoService kakaoService) {
        this.accountService = accountService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.kakaoService = kakaoService;
    }

    // 회원가입
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody @Valid AccountCreateDto dto) {
        AccountDetailDto res = accountService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    // 로그인 - AccessToken + RefreshToken 함께 반환
    @PostMapping("/doLogin")
    public ResponseEntity<?> doLogin(@RequestBody AccountLoginDto accountLoginDto) {
        TokenDto tokenDto = accountService.login(accountLoginDto);
        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("accessToken", tokenDto.getAccessToken());
        loginInfo.put("refreshToken", tokenDto.getRefreshToken());
        return new ResponseEntity<>(loginInfo, HttpStatus.OK);
    }

    // 카카오 로그인
    @PostMapping("/kakao/doLogin")
    public ResponseEntity<?> kakaoLogin(@RequestBody RedirectDto redirectDto) {
        AccessTokenDto accessTokenDto = kakaoService.getAccessToken(redirectDto.getCode());
        KakaoProfileDto kakaoProfileDto = kakaoService.getKakaoProfile(accessTokenDto.getAccess_token());
        Account originalMember = accountService.getMemberBySocialId(kakaoProfileDto.getId());
        if (originalMember == null) {
            originalMember = accountService.createOauth(
                    kakaoProfileDto.getId(),
                    kakaoProfileDto.getKakao_account().getEmail(),
                    SocialType.KAKAO
            );
        }
        TokenDto tokenDto = accountService.issueTokens(originalMember);
        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("accessToken", tokenDto.getAccessToken());
        loginInfo.put("refreshToken", tokenDto.getRefreshToken());
        return new ResponseEntity<>(loginInfo, HttpStatus.OK);
    }

    // ✅ AccessToken 재발급 - Body: { "refreshToken": "eyJ..." }
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body("refreshToken이 필요합니다.");
        }
        TokenDto tokenDto = accountService.refresh(refreshToken);
        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", tokenDto.getAccessToken());
        result.put("refreshToken", tokenDto.getRefreshToken());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // 로그아웃 - Redis에서 RefreshToken 삭제
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal Long accountId) {
        accountService.logout(accountId);
        return ResponseEntity.status(HttpStatus.OK).body("로그아웃 완료");
    }

//    // 비밀번호 변경 (병원 관리자 전용)
//    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
//    @PatchMapping("/update/password")
//    public ResponseEntity<?> updatePw(@RequestBody AccountUpdatePwDto dto) {
//        accountService.updatePw(dto);
//        return ResponseEntity.status(HttpStatus.OK).body("비밀번호 변경 완료");
//    }

//    // 비밀번호 재설정 - 인증 코드 발송
//    @PostMapping("/password-reset/send-code")
//    public ResponseEntity<?> sendResetCode(@RequestBody PasswordResetSendCodeDto dto) {
//        accountService.sendPasswordResetCode(dto.getEmail());
//        return ResponseEntity.ok("인증 코드가 발송되었습니다.");
//    }
//
//    // 비밀번호 재설정 - 코드 확인 및 비밀번호 변경
//    @PostMapping("/password-reset/verify")
//    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetDto dto) {
//        accountService.resetPassword(dto);
//        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
//    }
}