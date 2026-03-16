package com.beyond.hodadoc.checkin.dtos;

import com.beyond.hodadoc.checkin.domain.CheckinStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CheckinStatusUpdateReqDto {
    private CheckinStatus status;
}