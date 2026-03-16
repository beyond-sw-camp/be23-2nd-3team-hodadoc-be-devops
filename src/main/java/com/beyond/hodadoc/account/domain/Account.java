package com.beyond.hodadoc.account.domain;


import com.beyond.hodadoc.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter @ToString
@Builder
@Entity
public class Account extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, unique = true)
    private String email;

    @Column(length = 255, nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.PATIENT;

    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    private String socialId;

    @Builder.Default
    @Column(length = 1)
    private String delYn = "N";

    public void updatePassword(String encodedPassword){
        this.password = encodedPassword;
    }

    public void delete(){
        this.delYn = "Y";
        this.email = null;
    }
}
