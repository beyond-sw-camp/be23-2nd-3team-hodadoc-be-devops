package com.beyond.hodadoc.sms.scheduler;

import com.beyond.hodadoc.common.domain.AlarmType;
import com.beyond.hodadoc.reservation.domain.ReservationPatient;
import com.beyond.hodadoc.reservation.repository.ReservationRepository;
import com.beyond.hodadoc.sms.repository.SmsHistoryRepository;
import com.beyond.hodadoc.sms.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsReminderScheduler {

    private final ReservationRepository reservationRepository;
    private final SmsService smsService;
    private final SmsHistoryRepository smsHistoryRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional(readOnly = true)
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime fromTime = now.plusMinutes(115).toLocalTime(); //2시간 뒤
        LocalTime toTime = now.plusMinutes(125).toLocalTime();   //5분 간격 -> 2시간 후 예약을 찾아서 리마인더 문자를 발송

        List<ReservationPatient> upcoming = reservationRepository
                .findUpcomingApprovedReservations(today, fromTime, toTime);

        for (ReservationPatient rp : upcoming) {
            if (smsHistoryRepository.existsByReservationIdAndAlarmType(
                    rp.getId(), AlarmType.RESERVATION_REMINDER)) {
                continue;
            }

            String phone = rp.getPatient().getPhone();
            if (phone == null || phone.isBlank()) {
                continue;
            }

            String msg = "[호다닥] " + rp.getHospital().getName() + " 예약 2시간 전입니다. "
                    + rp.getReservationDate() + " " + rp.getReservationTime();
            smsService.sendSms(phone, msg, AlarmType.RESERVATION_REMINDER, rp.getId());
            log.info("[리마인더] SMS 발송 요청 - reservationId={}, phone={}", rp.getId(), phone);
        }
    }
}
