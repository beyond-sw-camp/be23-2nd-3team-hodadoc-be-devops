package com.beyond.hodadoc.sms.service;

import com.beyond.hodadoc.common.domain.AlarmType;
import com.beyond.hodadoc.sms.domain.SendStatus;
import com.beyond.hodadoc.sms.domain.SmsHistory;
import com.beyond.hodadoc.sms.repository.SmsHistoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class SmsService {

    @Value("${solapi.api-key}")
    private String apiKey;

    @Value("${solapi.api-secret}")
    private String apiSecret;

    @Value("${solapi.from-number}")
    private String fromNumber;

    @Value("${solapi.enabled}")
    private boolean enabled;

    private final SmsHistoryRepository smsHistoryRepository;
    private DefaultMessageService messageService;

    public SmsService(SmsHistoryRepository smsHistoryRepository) {
        this.smsHistoryRepository = smsHistoryRepository;
    }

    @PostConstruct
    public void init() {
        if (enabled) {
            this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.solapi.com");
            log.info("[SMS] Solapi MessageService 초기화 완료");
        } else {
            log.info("[SMS] solapi.enabled=false → Solapi 초기화 생략 (로그만 출력)");
        }
    }

    @Async("smsExecutor")
    public void sendSms(String toPhone, String message, AlarmType alarmType, Long reservationId) {
        if (toPhone == null || toPhone.isBlank()) {
            log.warn("[SMS] 수신자 전화번호가 없어 발송 생략 - alarmType={}, reservationId={}", alarmType, reservationId);
            return;
        }

        String cleanPhone = toPhone.replaceAll("-", "");

        if (!enabled) {
            log.info("[SMS] solapi.enabled=false → 실제 발송 생략 - to={}, alarmType={}, message={}", cleanPhone, alarmType, message);
            return;
        }

        try {
            Message msg = new Message();
            msg.setFrom(fromNumber.replaceAll("-", ""));
            msg.setTo(cleanPhone);
            msg.setText(message);

            SingleMessageSentResponse response = messageService.sendOne(new SingleMessageSendingRequest(msg));
            log.info("[SMS] 발송 성공 - to={}, alarmType={}, messageId={}", cleanPhone, alarmType, response.getMessageId());

            smsHistoryRepository.save(SmsHistory.builder()
                    .reservationId(reservationId)
                    .receiverPhone(toPhone)
                    .message(message)
                    .alarmType(alarmType)
                    .sendStatus(SendStatus.SUCCESS)
                    .sentAt(LocalDateTime.now())
                    .build());

        } catch (Exception e) {
            log.error("[SMS] 발송 실패 - to={}, alarmType={}, error={}", cleanPhone, alarmType, e.getMessage(), e);

            smsHistoryRepository.save(SmsHistory.builder()
                    .reservationId(reservationId)
                    .receiverPhone(toPhone)
                    .message(message)
                    .alarmType(alarmType)
                    .sendStatus(SendStatus.FAILED)
                    .failReason(e.getMessage())
                    .sentAt(LocalDateTime.now())
                    .build());
        }
    }
}
