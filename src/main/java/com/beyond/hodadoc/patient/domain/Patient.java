package com.beyond.hodadoc.patient.domain;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.patient.dtos.PatientUpdateDto;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter @ToString
@Builder
@Entity
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String name;

    @Column(nullable = true, unique = true)
    private String email;

    private String phone;

//    @Column(nullable = false)
    private String address;

    @Column(length = 13, nullable = true ,unique = true)
    private String rrn; //주민번호 (sns 로그인 할때 그때 쓰일 것)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT),nullable = false)
    private Account account;

    public void updateProfile(PatientUpdateDto dto){
//        null이면 수정을 안하겠다는 뜻, null이 아니면 dto대로 수정하겠다는 뜻
        if(dto.getPhone() != null){
            this.phone = dto.getPhone();
        }
        if(dto.getName() != null){
            this.name = dto.getName();
        }
        if(dto.getAddress() != null){
            this.address = dto.getAddress();
        }
    }

    // 탈퇴 시 개인정보 초기화
    public void clearPersonalInfo(){
        this.name = null;
        this.email = null;
        this.phone = null;
        this.address = null;
    }

//    Account 엔티티에 delete 메소드 추가한 후 soft delete 시행
    public void delete(){
        this.account.delete();
    }


}
