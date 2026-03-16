package com.beyond.hodadoc.reservation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

/**
 * 예약 슬롯 동시성 제어 - Redis 분산락
 *
 * 락 키 구조: lock:reservation:{doctorId}:{date}:{time}
 * 예)        lock:reservation:5:2026-03-04:09:00
 *
 * SET NX PX 패턴:
 *   - NX: 키가 없을 때만 저장 (Not eXists)
 *   - PX: 만료 시간(ms) 설정 → 서버 장애 시 자동 해제 (데드락 방지)
 *
 * 락 TTL: 10초 (예약 생성 로직이 충분히 끝날 시간)
 */
@Slf4j
@Service
public class ReservationLockService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String LOCK_PREFIX = "lock:reservation:";
    private static final long   LOCK_TTL_SECONDS = 10L;

    @Autowired
    public ReservationLockService(@Qualifier("ssePubSub") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String lockKey(Long doctorId, LocalDate date, LocalTime time) {
        return LOCK_PREFIX + doctorId + ":" + date + ":" + time;
    }

    /**
     * 락 획득 시도
     * @return true = 락 획득 성공 / false = 이미 다른 요청이 점유 중
     */
    public boolean tryLock(Long doctorId, LocalDate date, LocalTime time) {
        String key    = lockKey(doctorId, date, time);
        String value  = "locked";
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, value, LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        boolean result = Boolean.TRUE.equals(acquired);
        log.debug("[예약락] {} - key={}", result ? "획득" : "실패", key);
        return result;
    }

    /**
     * 락 해제
     * 예약 성공/실패 모두 finally에서 반드시 호출
     */
    public void unlock(Long doctorId, LocalDate date, LocalTime time) {
        String key = lockKey(doctorId, date, time);
        redisTemplate.delete(key);
        log.debug("[예약락] 해제 - key={}", key);
    }
}