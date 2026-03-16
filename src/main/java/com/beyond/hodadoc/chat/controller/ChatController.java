package com.beyond.hodadoc.chat.controller;

import com.beyond.hodadoc.chat.dto.ChatMessageDto;
import com.beyond.hodadoc.chat.dto.MyChatListResDto;
import com.beyond.hodadoc.chat.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;
    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

//    이전 메시지 조회
    @GetMapping("/history/{roomId}")
    public ResponseEntity<?> getChatHistory(@PathVariable Long roomId){
        List<ChatMessageDto> chatMessageDtos = chatService.getChatHistory(roomId);
        return ResponseEntity.status(HttpStatus.OK).body(chatMessageDtos);
    }

//    채팅메시지 읽음 처리
    @PostMapping("/room/{roomId}/read")
    public ResponseEntity<?> messageRead(@PathVariable Long roomId){
        chatService.messageRead(roomId);
        return ResponseEntity.ok().build();
    }

//    내 채팅방 목록 조회 : roomId, roomName, 메시지 읽음 개수
    @GetMapping("/my/rooms")
    public ResponseEntity<?> getMyChatRooms(){
        List<MyChatListResDto> myChatListResDtos = chatService.getMyChatRooms();
        return ResponseEntity.status(HttpStatus.OK).body(myChatListResDtos);
    }

//    채팅방 나가기
    @DeleteMapping("/room/{roomId}/leave")
    public ResponseEntity<?> leaveChatRoom(@PathVariable Long roomId){
        chatService.leaveChatRoom(roomId);
        return ResponseEntity.ok().build();
    }

//    환자 → 병원 관리자 1:1 채팅방 개설
    @PostMapping("/room/private/hospital")
    public ResponseEntity<?> getOrCreateHospitalRoom(@RequestParam Long hospitalId){
        Long roomId = chatService.getOrCreateHospitalRoom(hospitalId);
        return ResponseEntity.status(HttpStatus.OK).body(roomId);
    }

//    환자/병원 관리자 → ADMIN 서버 문의 채팅방 개설
    @PostMapping("/room/private/admin")
    public ResponseEntity<?> getOrCreateAdminRoom(){
        Long roomId = chatService.getOrCreateAdminRoom();
        return ResponseEntity.status(HttpStatus.OK).body(roomId);
    }
}
