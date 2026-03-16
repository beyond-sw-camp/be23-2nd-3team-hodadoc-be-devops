package com.beyond.hodadoc.account.repository;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.account.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByEmail(String email);
    Optional<Account> findByEmailAndDelYn(String email, String delYn);
    Optional<Account> findBySocialId(String socialId);
    // 활성 계정만 조회 (탈퇴 여부 포함 조건)
    Optional<Account> findBySocialIdAndDelYn(String socialId, String delYn);
    Optional<Account> findByIdAndDelYn(Long id, String delYn);
    Optional<Account> findFirstByRoleAndDelYn(Role role, String delYn);
}