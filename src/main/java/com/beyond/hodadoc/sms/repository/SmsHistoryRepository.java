package com.beyond.hodadoc.sms.repository;

import com.beyond.hodadoc.common.domain.AlarmType;
import com.beyond.hodadoc.sms.domain.SmsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmsHistoryRepository extends JpaRepository<SmsHistory, Long> {
    boolean existsByReservationIdAndAlarmType(Long reservationId, AlarmType alarmType);
}
