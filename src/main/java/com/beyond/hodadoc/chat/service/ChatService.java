package com.beyond.hodadoc.chat.service;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.account.domain.Role;
import com.beyond.hodadoc.account.repository.AccountRepository;
import com.beyond.hodadoc.chat.domain.ChatMessage;
import com.beyond.hodadoc.chat.domain.ChatParticipant;
import com.beyond.hodadoc.chat.domain.ChatRoom;
import com.beyond.hodadoc.chat.domain.ReadStatus;
import com.beyond.hodadoc.chat.dto.ChatMessageDto;
import com.beyond.hodadoc.chat.dto.MyChatListResDto;
import com.beyond.hodadoc.chat.repository.ChatMessageRepository;
import com.beyond.hodadoc.chat.repository.ChatParticipantRepository;
import com.beyond.hodadoc.chat.repository.ChatRoomRepository;
import com.beyond.hodadoc.chat.repository.ReadStatusRepository;
import com.beyond.hodadoc.common.domain.AlarmType;
import com.beyond.hodadoc.common.service.SseAlarmService;
import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.hospital.repository.HospitalRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ReadStatusRepository readStatusRepository;
    private final AccountRepository accountRepository;
    private final HospitalRepository hospitalRepository;
    private final SseAlarmService sseAlarmService;
    private final RedisPubSubService redisPubSubService;

    @Autowired
    public ChatService(ChatRoomRepository chatRoomRepository, ChatParticipantRepository chatParticipantRepository, ChatMessageRepository chatMessageRepository, ReadStatusRepository readStatusRepository, AccountRepository accountRepository, HospitalRepository hospitalRepository, SseAlarmService sseAlarmService, RedisPubSubService redisPubSubService) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.readStatusRepository = readStatusRepository;
        this.accountRepository = accountRepository;
        this.hospitalRepository = hospitalRepository;
        this.sseAlarmService = sseAlarmService;
        this.redisPubSubService = redisPubSubService;
    }

    public void saveMessage(Long roomId, Account sender, ChatMessageDto chatMessageReqDto) {
//        채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()-> new EntityNotFoundException("room cannot be found."));

//        나간 참여자가 있는 채팅방에는 메시지를 보낼 수 없음
        List<ChatParticipant> allParticipants = chatParticipantRepository.findAllByChatRoom(chatRoom);
        boolean hasLeftParticipant = allParticipants.stream()
                .anyMatch(p -> "Y".equals(p.getLeftYn()));
        if (hasLeftParticipant) {
            throw new IllegalArgumentException("종료된 채팅방에는 메시지를 보낼 수 없습니다.");
        }

//        메시지 저장
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .account(sender)
                .content(chatMessageReqDto.getMessage())
                .build();
        chatMessageRepository.save(chatMessage);

//        사용자별로 읽음 여부 저장 (나간 사람 제외)
        List<ChatParticipant> chatParticipantList = chatParticipantRepository.findAllByChatRoomAndLeftYn(chatRoom, "N");
        for(ChatParticipant c : chatParticipantList){
            ReadStatus readStatus = ReadStatus.builder()
                    .chatRoom(chatRoom)
                    .account(c.getAccount())
                    .chatMessage(chatMessage)
                    .isRead(c.getAccount().getId().equals(sender.getId()))
                    .build();
            readStatusRepository.save(readStatus);
        }

//        수신자에게 SSE 알림 발송 (발신자 제외)
        for(ChatParticipant c : chatParticipantList){
            if(!c.getAccount().getId().equals(sender.getId())){
                sseAlarmService.sendMessage(
                        c.getAccount().getId(),
                        chatMessageReqDto.getMessage(),
                        AlarmType.CHAT_MESSAGE.name(),
                        roomId
                );
            }
        }

    }

//        ChatParticipant 객체 생성 후 저장
    public void addParticipantToRoom(ChatRoom chatRoom, Account account){
        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chatRoom(chatRoom)
                .account(account)
                .build();
        chatParticipantRepository.save(chatParticipant);
    }

    public List<ChatMessageDto> getChatHistory(Long roomId) {
//        내가 해당 채팅방의 참여자가 아닐 경우 에러
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("room cannot be found."));
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new EntityNotFoundException("계정을 찾을 수 없습니다."));
        List<ChatParticipant> chatParticipants = chatParticipantRepository.findAllByChatRoom(chatRoom);

//        특정 room에 대한 message 조회
        boolean check = false;
        for(ChatParticipant c : chatParticipants){
            if(c.getAccount().equals(account)){
                check = true;
            }
        }
        if(!check) throw new IllegalArgumentException("본인이 속하지 않은 채팅방입니다.");
        List<ChatMessage> chatMessages = chatMessageRepository.findByChatRoomOrderByCreatedTimeAsc(chatRoom);

//        현재 사용자의 읽음 상태를 메시지ID 기준 Map으로 변환
        List<ReadStatus> myReadStatuses = readStatusRepository.findByChatRoomAndAccount(chatRoom, account);
        Map<Long, Boolean> readMap = new HashMap<>();
        for (ReadStatus rs : myReadStatuses) {
            readMap.put(rs.getChatMessage().getId(), rs.getIsRead());
        }

        List<ChatMessageDto> chatMessageDtos = new ArrayList<>();
        for(ChatMessage c : chatMessages){
            ChatMessageDto chatMessageDto = ChatMessageDto.builder()
                    .message(c.getContent())
                    .senderEmail(c.getAccount().getEmail())
                    .senderId(c.getAccount().getId())
                    .isRead(readMap.getOrDefault(c.getId(), true))
                    .build();
            chatMessageDtos.add(chatMessageDto);
        }
        return chatMessageDtos;
    }

//    환자 → 병원 관리자 1:1 채팅방 개설 (채팅방 이름: 병원 이름)
    public Long getOrCreateHospitalRoom(Long hospitalId) {
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new EntityNotFoundException("계정을 찾을 수 없습니다."));
        if (account.getRole() != Role.PATIENT) {
            throw new IllegalArgumentException("환자만 병원에 문의할 수 있습니다.");
        }

        Hospital hospital = hospitalRepository.findById(hospitalId).orElseThrow(() -> new EntityNotFoundException("병원을 찾을 수 없습니다."));
        Account hospitalAccount = hospital.getAccount();

        Optional<ChatRoom> chatRoom = chatParticipantRepository.findExistingPrivateRoom(account.getId(), hospitalAccount.getId());
        if (chatRoom.isPresent()) {
            return chatRoom.get().getId();
        }

        ChatRoom newRoom = ChatRoom.builder()
                .name(hospital.getName())
                .build();
        chatRoomRepository.save(newRoom);
        addParticipantToRoom(newRoom, account);
        addParticipantToRoom(newRoom, hospitalAccount);

        return newRoom.getId();
    }

//    환자 또는 병원 관리자 → ADMIN 서비스 문의 채팅방 개설 (채팅방 이름: 서버 문의)
    public Long getOrCreateAdminRoom() {
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new EntityNotFoundException("계정을 찾을 수 없습니다."));
        if (account.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("관리자는 자기 자신에게 문의할 수 없습니다.");
        }

        Account adminAccount = accountRepository.findFirstByRoleAndDelYn(Role.ADMIN, "N").orElseThrow(() -> new EntityNotFoundException("관리자 계정을 찾을 수 없습니다."));

        Optional<ChatRoom> chatRoom = chatParticipantRepository.findExistingPrivateRoom(account.getId(), adminAccount.getId());
        if (chatRoom.isPresent()) {
            return chatRoom.get().getId();
        }

        ChatRoom newRoom = ChatRoom.builder()
                .name("서버 문의")
                .build();
        chatRoomRepository.save(newRoom);
        addParticipantToRoom(newRoom, account);
        addParticipantToRoom(newRoom, adminAccount);

        return newRoom.getId();
    }

    public void messageRead(Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("room cannot be found."));
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new EntityNotFoundException("계정을 찾을 수 없습니다."));
        List<ReadStatus> readStatuses = readStatusRepository.findByChatRoomAndAccount(chatRoom, account);
        for(ReadStatus r : readStatuses){
            r.updateIsRead(true);
        }
    }

    public void leaveChatRoom(Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("room cannot be found."));
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new EntityNotFoundException("계정을 찾을 수 없습니다."));

        ChatParticipant chatParticipant = chatParticipantRepository.findByChatRoomAndAccount(chatRoom, account)
                .orElseThrow(() -> new IllegalArgumentException("본인이 속하지 않은 채팅방입니다."));

        if ("Y".equals(chatParticipant.getLeftYn())) {
            throw new IllegalArgumentException("이미 나간 채팅방입니다.");
        }

//        나간 상태로 변경 (데이터 보존)
        chatParticipant.updateLeftYn("Y");

//        상대방에게 WebSocket으로 퇴장 알림 발송
        ChatMessageDto leaveNotification = ChatMessageDto.builder()
                .roomId(roomId)
                .senderEmail(account.getEmail())
                .senderId(accountId)
                .message("상대방이 나갔습니다.")
                .build();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String message = objectMapper.writeValueAsString(leaveNotification);
            redisPubSubService.publish("chat", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<MyChatListResDto> getMyChatRooms() {
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new EntityNotFoundException("계정을 찾을 수 없습니다."));

        // 1. 내 채팅방 목록 (chatRoom fetch join) - 쿼리 1개
        List<ChatParticipant> myParticipants = chatParticipantRepository.findAllByAccountAndLeftYnWithChatRoom(account, "N");
        if (myParticipants.isEmpty()) return new ArrayList<>();

        List<ChatRoom> myRooms = myParticipants.stream().map(ChatParticipant::getChatRoom).collect(Collectors.toList());

        // 2. 모든 채팅방의 참가자를 한번에 조회 (account fetch join) - 쿼리 1개
        List<ChatParticipant> allParticipants = chatParticipantRepository.findAllByChatRoomInWithAccount(myRooms);
        Map<Long, List<ChatParticipant>> participantsByRoom = allParticipants.stream()
                .collect(Collectors.groupingBy(cp -> cp.getChatRoom().getId()));

        // 3. 모든 채팅방의 미읽음 카운트를 한번에 조회 - 쿼리 1개
        List<Object[]> unreadCounts = readStatusRepository.countUnreadByChatRoomsAndAccount(myRooms, account);
        Map<Long, Long> unreadMap = unreadCounts.stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        // 4. DTO 조립 (추가 쿼리 없음)
        List<MyChatListResDto> chatListResDtos = new ArrayList<>();
        for (ChatParticipant c : myParticipants) {
            Long roomId = c.getChatRoom().getId();
            Long count = unreadMap.getOrDefault(roomId, 0L);

            String otherRole = null;
            String participantName = null;
            List<ChatParticipant> roomParticipants = participantsByRoom.getOrDefault(roomId, List.of());
            for (ChatParticipant p : roomParticipants) {
                if (!p.getAccount().getId().equals(accountId)) {
                    Account otherAccount = p.getAccount();
                    otherRole = otherAccount.getRole().name();
                    participantName = resolveParticipantName(otherAccount);
                    break;
                }
            }

            chatListResDtos.add(MyChatListResDto.builder()
                    .roomId(roomId)
                    .roomName(c.getChatRoom().getName())
                    .participantName(participantName)
                    .unReadCount(count)
                    .participantRole(otherRole)
                    .build());
        }
        return chatListResDtos;
    }

    private String resolveParticipantName(Account account) {
        if (account.getRole() == Role.ADMIN) {
            return "시스템 관리자";
        }
        if (account.getRole() == Role.HOSPITAL_ADMIN) {
            return hospitalRepository.findByAccount_IdAndAccount_DelYn(account.getId(), "N")
                    .map(Hospital::getName)
                    .orElse(account.getEmail());
        }
        // PATIENT: name 필드가 없으므로 이메일 표시
        return account.getEmail();
    }
}
