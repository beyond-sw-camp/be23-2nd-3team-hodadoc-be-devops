package com.beyond.hodadoc.common.repository;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SseEmitterRegistry {
    // 환자 ID -> SseEmitter 매핑
    private final Map<Long, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    public SseEmitter save(Long userId, SseEmitter emitter) {
        SseEmitter oldEmitter = emitterMap.put(userId, emitter);
        if (oldEmitter != null) {
            oldEmitter.complete(); // 기존 emitter 정리 → 좀비 방지
        }
        return oldEmitter;
    }
    public SseEmitter getEmitter(Long userId){
        return emitterMap.get(userId);
    }
    public SseEmitter remove(Long userId){
        return emitterMap.remove(userId);
    }
    /**
     * 현재 등록된 emitter가 주어진 emitter와 동일한 경우에만 제거.
     * 재연결 시 이전 emitter의 콜백이 새 emitter를 삭제하는 문제 방지.
     */
    public boolean removeIfSame(Long userId, SseEmitter emitter) {
        return emitterMap.remove(userId, emitter);
    }

}
