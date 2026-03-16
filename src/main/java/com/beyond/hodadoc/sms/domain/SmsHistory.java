package com.beyond.hodadoc.sms.domain;

import com.beyond.hodadoc.common.domain.AlarmType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
@Table(name = "sms_history")
public class SmsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reservationId;

    private String receiverPhone;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    private AlarmType alarmType;

    @Enumerated(EnumType.STRING)
    private SendStatus sendStatus;

    private String failReason;

    private LocalDateTime sentAt;
}
