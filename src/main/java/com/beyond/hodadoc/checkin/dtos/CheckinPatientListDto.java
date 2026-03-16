package com.beyond.hodadoc.checkin.dtos;

import com.beyond.hodadoc.checkin.domain.Checkin;
import com.beyond.hodadoc.checkin.domain.CheckinStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CheckinPatientListDto {

    private Long checkinId;
    private Long hospitalId;
    private String hospitalName;
    private Integer waitingNumber;
    private Integer queuePosition; // 현재 실제 대기 순번 (WAITING 상태일 때만 유효)
    private CheckinStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkinTime;

    public static CheckinPatientListDto fromEntity(Checkin checkin) {
        return CheckinPatientListDto.builder()
                .checkinId(checkin.getId())
                .hospitalId(checkin.getHospital().getId())
                .hospitalName(checkin.getHospital().getName())
                .waitingNumber(checkin.getWaitingNumber())
                .status(checkin.getStatus())
                .checkinTime(checkin.getCheckinTime())
                .build();
    }
}