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

    public void save(Long userId, SseEmitter emitter) {
        emitterMap.put(userId, emitter);
    }
    public SseEmitter getEmitter(Long userId){
        return emitterMap.get(userId);
    }
    public SseEmitter remove(Long userId){
        return emitterMap.remove(userId);
    }

}
