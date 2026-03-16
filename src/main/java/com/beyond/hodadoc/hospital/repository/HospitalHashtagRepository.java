package com.beyond.hodadoc.hospital.repository;

import com.beyond.hodadoc.hospital.domain.HospitalHashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HospitalHashtagRepository extends JpaRepository<HospitalHashtag, Long> {
    long countByHospital_Id(Long hospitalId);
}
