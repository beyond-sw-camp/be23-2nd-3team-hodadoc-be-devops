package com.beyond.hodadoc.common.service;

import com.beyond.hodadoc.common.configs.RabbitMQConfig;
import com.beyond.hodadoc.common.dtos.SseMessageDto;
import com.beyond.hodadoc.common.repository.SseEmitterRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class SseAlarmService implements MessageListener {
    private final SseEmitterRegistry sseEmitterRegistry;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;

    @Autowired
    public SseAlarmService(SseEmitterRegistry sseEmitterRegistry, ObjectMapper objectMapper,
                           @Qualifier("ssePubSub") RedisTemplate<String, String> redisTemplate,
                           RabbitTemplate rabbitTemplate, RabbitAdmin rabbitAdmin) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitAdmin = rabbitAdmin;
    }

    // 알림을 보내는 쪽 (발신)
    public void sendMessage(Long receiverId, String message, String type, Long reservationId) {
        log.info("[SSE 발송] receiverId={}, type={}, message={}", receiverId, type, message);
        SseMessageDto dto = SseMessageDto.builder()
                .receiverId(receiverId)
                .message(message)
                .type(type)
                .reservationId(reservationId)
                .build();
        try {
            String data = objectMapper.writeValueAsString(dto);
            SseEmitter emitter = sseEmitterRegistry.getEmitter(receiverId);
            // 현재 서버에 emitter 있으면 바로 전송
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event().name(type).data(data));
                    log.info("[SSE 발송] 직접 전송 완료 - receiverId={}, type={}", receiverId, type);
                } catch (Exception sendEx) {
                    log.warn("[SSE 발송] emitter 전송 실패 → 제거 후 RabbitMQ 저장 - receiverId={}", receiverId);
                    // 연결이 끊겼으면 죽은 emitter 치우고, 알림은 잃어버리지 말고 RabbitMQ에 보관한다는 로직
                    sseEmitterRegistry.remove(receiverId);
                    saveToPendingQueue(receiverId, dto);
                }
            } else {
                // 없으면 Redis로 publish → 다른 서버가 받아서 전송
                redisTemplate.convertAndSend("notification-channel", data);
                log.info("[SSE 발송] emitter 없음 → Redis publish - receiverId={}", receiverId);
                saveToPendingQueue(receiverId, dto);
            }
        }catch (Exception e){
            log.error("[SSE 발송] 실패 - receiverId={}, type={}", receiverId, type, e);
        }
    }

    // 알림을 받는쪽 (수신) - Redis Pub/Sub으로 다른 서버에서 온 메시지 처리
    // RabbitMQ 저장은 sendMessage()에서 이미 처리했으므로 여기서는 SSE 전달만 시도
    @Override
    public void onMessage(Message message, byte[] pattern){
        try {
            SseMessageDto dto = objectMapper.readValue(message.getBody(), SseMessageDto.class);
            log.info("[SSE 수신] Redis 메시지 수신 - receiverId={}, type={}", dto.getReceiverId(), dto.getType());
            String data = objectMapper.writeValueAsString(dto);
            SseEmitter emitter = sseEmitterRegistry.getEmitter(dto.getReceiverId());
            if(emitter != null){
                try {
                    emitter.send(SseEmitter.event().name(dto.getType()).data(data));
                    log.info("[SSE 수신] 전달 완료 - receiverId={}, type={}", dto.getReceiverId(), dto.getType());
                } catch (Exception sendEx) {
                    log.warn("[SSE 수신] emitter 전송 실패 → 제거 - receiverId={}", dto.getReceiverId());
                    sseEmitterRegistry.remove(dto.getReceiverId());
                }
            } else {
                log.info("[SSE 수신] emitter 없음 → 무시 (이미 RabbitMQ에 저장됨) - receiverId={}", dto.getReceiverId());
            }
        }catch (Exception e){
            log.error("[SSE 수신] 처리 실패", e);
        }
    }

    private void saveToPendingQueue(Long receiverId, SseMessageDto dto) {
        String queueName = RabbitMQConfig.pendingQueueName(receiverId);
        rabbitAdmin.declareQueue(new Queue(queueName, true));
        rabbitTemplate.convertAndSend(queueName, dto);
        log.info("[SSE] RabbitMQ 큐 저장 완료 - queue={}", queueName);
    }
}
