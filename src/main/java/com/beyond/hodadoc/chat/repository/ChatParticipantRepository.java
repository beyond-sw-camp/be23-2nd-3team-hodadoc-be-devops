package com.beyond.hodadoc.chat.repository;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.chat.domain.ChatParticipant;
import com.beyond.hodadoc.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    List<ChatParticipant> findAllByChatRoom(ChatRoom chatRoom);

    Optional<ChatParticipant> findByChatRoomAndAccount(ChatRoom chatRoom, Account account);

    @Query("SELECT cp1.chatRoom FROM ChatParticipant cp1 JOIN ChatParticipant cp2 ON cp1.chatRoom.id = cp2.chatRoom.id WHERE cp1.account.id = :myId AND cp2.account.id = :otherAccountId AND cp1.leftYn = 'N' AND cp2.leftYn = 'N'")
    Optional<ChatRoom> findExistingPrivateRoom(@Param("myId")Long myid, @Param("otherAccountId")Long otherAccountId);

    List<ChatParticipant> findAllByAccountAndLeftYn(Account account, String leftYn);

    // 내 채팅방 목록 조회 시 chatRoom을 fetch join (N+1 방지)
    @Query("SELECT cp FROM ChatParticipant cp " +
           "JOIN FETCH cp.chatRoom " +
           "JOIN FETCH cp.account " +
           "WHERE cp.account = :account AND cp.leftYn = :leftYn")
    List<ChatParticipant> findAllByAccountAndLeftYnWithChatRoom(
            @Param("account") Account account, @Param("leftYn") String leftYn);

    // 여러 채팅방의 참가자를 한번에 조회 (N+1 방지)
    @Query("SELECT cp FROM ChatParticipant cp " +
           "JOIN FETCH cp.account " +
           "WHERE cp.chatRoom IN :chatRooms")
    List<ChatParticipant> findAllByChatRoomInWithAccount(@Param("chatRooms") List<ChatRoom> chatRooms);

    List<ChatParticipant> findAllByChatRoomAndLeftYn(ChatRoom chatRoom, String leftYn);
}
