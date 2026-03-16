package com.beyond.hodadoc.chat.controller;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.account.repository.AccountRepository;
import com.beyond.hodadoc.chat.dto.ChatMessageDto;
import com.beyond.hodadoc.chat.service.ChatService;
import com.beyond.hodadoc.chat.service.RedisPubSubService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;


@Controller
@Slf4j
public class StompController {

    private final SimpMessageSendingOperations messageTemplate;
    private final ChatService chatService;
    private final RedisPubSubService redisPubSubService;
    private final AccountRepository accountRepository;

    @Autowired
    public StompController(SimpMessageSendingOperations messageTemplate, ChatService chatService, RedisPubSubService redisPubSubService, AccountRepository accountRepository) {
        this.messageTemplate = messageTemplate;
        this.chatService = chatService;
        this.redisPubSubService = redisPubSubService;
        this.accountRepository = accountRepository;
    }

    @MessageMapping("/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, ChatMessageDto chatMessageReqDto, Principal principal) throws JsonProcessingException {
//        STOMP 세션의 Principal에서 accountId를 꺼내서 발신자 이메일을 서버에서 설정 (위조 방지)
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        Long accountId = (Long) auth.getPrincipal();
        Account sender = accountRepository.findById(accountId).orElseThrow(() -> new EntityNotFoundException("계정을 찾을 수 없습니다."));
        chatMessageReqDto.setSenderEmail(sender.getEmail());
        chatMessageReqDto.setSenderId(accountId);

        log.info("채팅 메시지: roomId={}, sender={}", roomId, sender.getEmail());
        chatService.saveMessage(roomId, sender, chatMessageReqDto);
        chatMessageReqDto.setRoomId(roomId);
        ObjectMapper objectMapper = new ObjectMapper();
        String message = objectMapper.writeValueAsString(chatMessageReqDto);
        redisPubSubService.publish("chat", message);
    }

}
