package com.beyond.hodadoc.patient.repository;

import com.beyond.hodadoc.patient.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByAccountIdAndAccount_DelYn(Long accountId, String delYn);
}
