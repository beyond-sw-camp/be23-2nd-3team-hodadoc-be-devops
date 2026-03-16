package com.beyond.hodadoc.chat.repository;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.chat.domain.ChatRoom;
import com.beyond.hodadoc.chat.domain.ReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadStatusRepository extends JpaRepository<ReadStatus, Long> {
    List<ReadStatus> findByChatRoomAndAccount(ChatRoom chatRoom, Account account);

    Long countByChatRoomAndAccountAndIsReadFalse(ChatRoom chatRoom, Account account);

    // 여러 채팅방의 미읽음 카운트를 한번에 조회 (N+1 방지)
    @Query("SELECT rs.chatRoom.id, COUNT(rs) FROM ReadStatus rs " +
           "WHERE rs.chatRoom IN :chatRooms AND rs.account = :account AND rs.isRead = false " +
           "GROUP BY rs.chatRoom.id")
    List<Object[]> countUnreadByChatRoomsAndAccount(
            @Param("chatRooms") List<ChatRoom> chatRooms, @Param("account") Account account);

    void deleteByChatRoom(ChatRoom chatRoom);
}
