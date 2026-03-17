package com.beyond.hodadoc.common.controller;

import com.beyond.hodadoc.common.configs.RabbitMQConfig;
import com.beyond.hodadoc.common.dtos.SseMessageDto;
import com.beyond.hodadoc.common.repository.SseEmitterRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/sse")
public class SseController {

    private final SseEmitterRegistry sseEmitterRegistry;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;
    private final ObjectMapper objectMapper;

    @Autowired
    public SseController(SseEmitterRegistry sseEmitterRegistry,
                         RabbitTemplate rabbitTemplate, RabbitAdmin rabbitAdmin,
                         ObjectMapper objectMapper) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitAdmin = rabbitAdmin;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() throws IOException {

        // ✅ Authentication 자체가 null인 경우 방어 (비로그인 요청)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            SseEmitter emitter = new SseEmitter(0L);
            emitter.send(SseEmitter.event().name("error").data("인증이 필요합니다."));
            emitter.complete();
            return emitter;
        }

        Object principal = authentication.getPrincipal();

        // ✅ anonymousUser 방어
        if (principal == null || "anonymousUser".equals(principal.toString())) {
            SseEmitter emitter = new SseEmitter(0L);
            emitter.send(SseEmitter.event().name("error").data("인증이 필요합니다."));
            emitter.complete();
            return emitter;
        }

        Long userId = Long.parseLong(principal.toString());
        log.info("[SSE] 연결 요청 - userId={}", userId);

        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L); // 1시간 유효
        sseEmitterRegistry.save(userId, emitter);

        emitter.send(SseEmitter.event().name("connect").data("연결완료"));
        log.info("[SSE] 연결 완료 - userId={}", userId);

        // RabbitMQ에서 미전송 알림 꺼내서 전송
        flushPendingAlarms(userId, emitter);

        emitter.onCompletion(() -> {
            log.info("[SSE] onCompletion 콜백 - userId={}", userId);
            sseEmitterRegistry.removeIfSame(userId, emitter);
        });
        emitter.onTimeout(() -> {
            log.info("[SSE] onTimeout 콜백 - userId={}", userId);
            sseEmitterRegistry.removeIfSame(userId, emitter);
        });
        emitter.onError(e -> {
            log.warn("[SSE] onError 콜백 - userId={}, error={}", userId, e.getMessage());
            sseEmitterRegistry.removeIfSame(userId, emitter);
        });

        return emitter;
    }

    @GetMapping("/disconnect")
    public void disconnect() {
        // ✅ Authentication null 방어
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.warn("[SSE] disconnect 실패 - authentication이 null");
            return;
        }

        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal.toString())) {
            log.warn("[SSE] disconnect 실패 - principal={}", principal);
            return;
        }

        Long userId = Long.parseLong(principal.toString());
        log.info("[SSE] disconnect 요청 - userId={}", userId);

        SseEmitter emitter = sseEmitterRegistry.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("[SSE] disconnect 완료 - userId={}, emitter.complete() 호출됨", userId);
        } else {
            log.warn("[SSE] disconnect - userId={}의 emitter가 이미 없음", userId);
        }
    }

    private void flushPendingAlarms(Long userId, SseEmitter emitter) {
        String queueName = RabbitMQConfig.pendingQueueName(userId);
        try {
            rabbitAdmin.declareQueue(new Queue(queueName, true));
            int count = 0;
            while (true) {
                SseMessageDto dto = (SseMessageDto) rabbitTemplate.receiveAndConvert(queueName);
                if (dto == null) break;
                String data = objectMapper.writeValueAsString(dto);
                emitter.send(SseEmitter.event().name(dto.getType()).data(data));
                count++;
            }
            if (count > 0) {
                log.info("[SSE] 미전송 알림 {} 건 전송 완료 - userId={}", count, userId);
            }
        } catch (Exception e) {
            log.error("[SSE] 미전송 알림 전송 실패 - userId={}", userId, e);
        }
    }
}