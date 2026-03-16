package com.beyond.hodadoc.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MyChatListResDto {
    private Long roomId;
    private String roomName;
    private String participantName;
    private Long unReadCount;
    private String participantRole;
}
