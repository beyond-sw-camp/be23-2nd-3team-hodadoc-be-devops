package com.beyond.hodadoc.account.service;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.account.repository.AccountRepository;
import com.beyond.hodadoc.common.domain.AlarmType;
import com.beyond.hodadoc.sms.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RedisTemplate<String, String> redisTemplate;
    private final AccountRepository accountRepository;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;

    private static final String VERIFY_PREFIX = "VERIFY:";
    private static final String VERIFY_RATE_PREFIX = "VERIFY_RATE:";
    private static final String VERIFY_PASS_PREFIX = "VERIFY_PASS:";

    public void sendVerification(String email, String phone) {
        // 이메일로 계정 존재 여부 확인
        accountRepository.findByEmailAndDelYn(email, "N")
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 등록된 계정이 없습니다."));

        // 1분 내 재발송 방지
        if (Boolean.TRUE.equals(redisTemplate.hasKey(VERIFY_RATE_PREFIX + email))) {
            throw new IllegalStateException("인증번호는 1분 후에 재발송할 수 있습니다.");
        }

        // 6자리 인증번호 생성
        String code = String.format("%06d", new Random().nextInt(1000000));

        // Redis 저장 (email 기준)
        redisTemplate.opsForValue().set(VERIFY_PREFIX + email, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(VERIFY_RATE_PREFIX + email, "1", 1, TimeUnit.MINUTES);

        // SMS 발송 (사용자가 입력한 phone으로)
        String message = "[호다닥] 인증번호는 [" + code + "]입니다. 5분 내로 입력해주세요.";
        smsService.sendSms(phone, message, AlarmType.VERIFICATION_CODE, null);

        log.info("[인증] 인증번호 발송 - email={}, phone={}", email, phone);
    }

    public void verifyCode(String email, String code) {
        String storedCode = redisTemplate.opsForValue().get(VERIFY_PREFIX + email);

        if (storedCode == null) {
            throw new IllegalStateException("인증번호가 만료되었습니다. 다시 요청해주세요.");
        }

        if (!storedCode.equals(code)) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        // 인증 완료 처리
        redisTemplate.delete(VERIFY_PREFIX + email);
        redisTemplate.opsForValue().set(VERIFY_PASS_PREFIX + email, "verified", 5, TimeUnit.MINUTES);

        log.info("[인증] 인증 완료 - email={}", email);
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        // 인증 완료 확인
        String verified = redisTemplate.opsForValue().get(VERIFY_PASS_PREFIX + email);
        if (verified == null) {
            throw new IllegalStateException("인증이 완료되지 않았습니다. 인증번호를 먼저 확인해주세요.");
        }

        // 이메일로 계정 조회
        Account account = accountRepository.findByEmailAndDelYn(email, "N")
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 등록된 계정이 없습니다."));

        // 비밀번호 변경
        account.updatePassword(passwordEncoder.encode(newPassword));

        // 인증 완료 키 삭제
        redisTemplate.delete(VERIFY_PASS_PREFIX + email);

        log.info("[인증] 비밀번호 재설정 완료 - accountId={}", account.getId());
    }
}
