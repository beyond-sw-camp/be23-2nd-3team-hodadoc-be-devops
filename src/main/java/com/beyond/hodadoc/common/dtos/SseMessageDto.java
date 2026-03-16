package com.beyond.hodadoc.common.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SseMessageDto {
    private Long receiverId;
    private String message;
    private String type;
    private Long reservationId;
}
