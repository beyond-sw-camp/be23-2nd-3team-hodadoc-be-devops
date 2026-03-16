package com.beyond.hodadoc.chat.domain;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter @ToString
@Entity
public class ChatParticipant extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    @Builder.Default
    private String leftYn = "N";

    public void updateLeftYn(String leftYn) {
        this.leftYn = leftYn;
    }
}
