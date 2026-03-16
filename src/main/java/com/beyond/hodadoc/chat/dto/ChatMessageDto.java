package com.beyond.hodadoc.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ChatMessageDto {
    private Long roomId;
    private String message;
    private String senderEmail;
    private Long senderId;
    private Boolean isRead;
}
