package com.beyond.hodadoc.account.service;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.account.domain.Role;
import com.beyond.hodadoc.account.domain.SocialType;
import com.beyond.hodadoc.account.dtos.*;
import com.beyond.hodadoc.account.repository.AccountRepository;
import com.beyond.hodadoc.common.auth.JwtTokenProvider;
import com.beyond.hodadoc.common.domain.AlarmType;
import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.hospital.repository.HospitalRepository;
import com.beyond.hodadoc.sms.service.SmsService;
import com.beyond.hodadoc.patient.domain.Patient;
import com.beyond.hodadoc.patient.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final SmsService smsService;

    public AccountService(AccountRepository accountRepository,
                          PatientRepository patientRepository,
                          HospitalRepository hospitalRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider,
                          RedisTemplate<String, String> redisTemplate,
                          SmsService smsService) {
        this.accountRepository = accountRepository;
        this.patientRepository = patientRepository;
        this.hospitalRepository = hospitalRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        this.smsService = smsService;
    }

    /**
     * 로그인: AccessToken + RefreshToken 함께 발급
     * RefreshToken은 Redis에 저장 (key: "RT:{accountId}", value: refreshToken)
     */
    public TokenDto login(AccountLoginDto memberLoginDto) {
        Account account = accountRepository.findByEmailAndDelYn(memberLoginDto.getEmail(), "N")
                .orElseThrow(() -> new IllegalArgumentException("email이 존재하지 않거나 탈퇴한 계정입니다."));

        if (!passwordEncoder.matches(memberLoginDto.getPassword(), account.getPassword())) {
            throw new IllegalArgumentException("password가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createToken(account.getId(), account.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(account.getId());

        // Redis에 저장: key = "RT:{accountId}", TTL = refreshExpiration
        redisTemplate.opsForValue().set(
                "RT:" + account.getId(),
                refreshToken,
                jwtTokenProvider.getRefreshExpirationSeconds(),
                TimeUnit.SECONDS
        );

        return TokenDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * RefreshToken으로 AccessToken 재발급
     * 1. refreshToken 유효성 검사
     * 2. Redis에 저장된 토큰과 일치 여부 확인
     * 3. 새 AccessToken + RefreshToken 발급 (Rotation)
     */
    public TokenDto refresh(String refreshToken) {
        // 1. 토큰 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 RefreshToken입니다.");
        }

        Long accountId = jwtTokenProvider.getAccountId(refreshToken);

        // 2. Redis에서 저장된 refreshToken 조회 및 일치 확인
        String savedToken = redisTemplate.opsForValue().get("RT:" + accountId);
        if (savedToken == null || !savedToken.equals(refreshToken)) {
            throw new IllegalArgumentException("RefreshToken이 일치하지 않습니다. 다시 로그인 해주세요.");
        }

        // 3. 계정 조회 (role 가져오기 위함)
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("계정을 찾을 수 없습니다."));

        // 4. 새 토큰 발급 (Refresh Token Rotation - 보안 강화)
        String newAccessToken = jwtTokenProvider.createToken(account.getId(), account.getRole().name());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(account.getId());

        // 5. Redis 업데이트
        redisTemplate.opsForValue().set(
                "RT:" + account.getId(),
                newRefreshToken,
                jwtTokenProvider.getRefreshExpirationSeconds(),
                TimeUnit.SECONDS
        );

        return TokenDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    /**
     * 로그아웃: Redis에서 RefreshToken 삭제
     */
    public void logout(Long accountId) {
        redisTemplate.delete("RT:" + accountId);
    }

    // ===== 기존 메서드 유지 =====

    public void updatePw(AccountUpdatePwDto dto) {
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Account account = accountRepository.findByIdAndDelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("없는 계정입니다."));

        if (account.getRole() != Role.HOSPITAL_ADMIN) {
            throw new IllegalStateException("병원 관리자만 비밀번호를 변경할 수 있습니다.");
        }

        if (!passwordEncoder.matches(dto.getCurrentPassword(), account.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 틀립니다.");
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("비밀번호가 비어있으면 안됩니다. 새 비밀번호를 입력해주세요.");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), account.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호와 동일합니다. 다른 비밀번호를 입력해주세요.");
        }

        account.updatePassword(passwordEncoder.encode(dto.getNewPassword()));
    }

    public AccountDetailDto create(AccountCreateDto dto) {
        if (accountRepository.findByEmailAndDelYn(dto.getEmail(), "N").isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        Account account = dto.toEntity(encodedPassword);
        Account saved = accountRepository.save(account);
        return AccountDetailDto.fromEntity(saved);
    }

    /**
     * 카카오 로그인 등에서 Account 객체로 바로 토큰 발급할 때 사용
     */
    public TokenDto issueTokens(Account account) {
        String accessToken = jwtTokenProvider.createToken(account.getId(), account.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(account.getId());

        redisTemplate.opsForValue().set(
                "RT:" + account.getId(),
                refreshToken,
                jwtTokenProvider.getRefreshExpirationSeconds(),
                TimeUnit.SECONDS
        );

        return TokenDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 활성(delYn=N) 카카오 계정만 반환 — 탈퇴 계정은 null 반환하여 재가입 플로우 진입
    public Account getMemberBySocialId(String socialId) {
        return accountRepository.findBySocialIdAndDelYn(socialId, "N").orElse(null);
    }

    // 신규 카카오 계정 생성 (patient 레코드는 생성하지 않음 — 첫 로그인 후 프론트에서 register API 호출)
    public Account createOauth(String socialId, String email, SocialType socialType) {
        String randomPassword = UUID.randomUUID().toString();
        String encodedPassword = passwordEncoder.encode(randomPassword);

        Account account = Account.builder()
                .email(email)
                .password(encodedPassword)
                .socialId(socialId)
                .socialType(socialType)
                .role(Role.PATIENT)
                .build();
        return accountRepository.save(account);
    }

//    // ===== 비밀번호 재설정 (SMS 인증) =====
//
//    public void sendPasswordResetCode(String email) {
//        Account account = accountRepository.findByEmailAndDelYn(email, "N")
//                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 이메일입니다."));
//
//        if (account.getRole() != Role.HOSPITAL_ADMIN) {
//            throw new IllegalArgumentException("병원 관리자 계정만 비밀번호 재설정이 가능합니다.");
//        }
//
//        // 60초 재발송 제한
//        String rateLimitKey = "PWR_RATE:" + email;
//        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateLimitKey))) {
//            throw new IllegalArgumentException("인증 코드는 60초에 한 번만 요청할 수 있습니다.");
//        }
//
//        Hospital hospital = hospitalRepository.findByAccount_IdAndAccount_DelYn(account.getId(), "N")
//                .orElseThrow(() -> new IllegalArgumentException("등록된 병원 정보가 없습니다."));
//
//        if (hospital.getPhone() == null || hospital.getPhone().isBlank()) {
//            throw new IllegalArgumentException("병원 전화번호가 등록되어 있지 않습니다.");
//        }
//
//        String code = String.format("%06d", new Random().nextInt(1000000));
//
//        redisTemplate.opsForValue().set("PWR:" + email, code, 5, TimeUnit.MINUTES);
//        redisTemplate.opsForValue().set(rateLimitKey, "1", 60, TimeUnit.SECONDS);
//
//        String message = "[호다닥] 비밀번호 재설정 인증번호: " + code + " (5분 이내 입력)";
//        smsService.sendSms(hospital.getPhone(), message, AlarmType.VERIFICATION_CODE, null);
//    }

//    public void resetPassword(PasswordResetDto dto) {
//        String codeKey = "PWR:" + dto.getEmail();
//        String savedCode = redisTemplate.opsForValue().get(codeKey);
//
//        if (savedCode == null) {
//            throw new IllegalArgumentException("인증 코드가 만료되었습니다. 다시 요청해주세요.");
//        }
//        if (!savedCode.equals(dto.getCode())) {
//            throw new IllegalArgumentException("인증 코드가 일치하지 않습니다.");
//        }
//
//        Account account = accountRepository.findByEmailAndDelYn(dto.getEmail(), "N")
//                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 이메일입니다."));
//
//        if (passwordEncoder.matches(dto.getNewPassword(), account.getPassword())) {
//            throw new IllegalArgumentException("기존 비밀번호와 동일합니다. 다른 비밀번호를 입력해주세요.");
//        }
//
//        account.updatePassword(passwordEncoder.encode(dto.getNewPassword()));
//        redisTemplate.delete(codeKey);
//    }
}