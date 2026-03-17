package com.beyond.hodadoc.doctor.repository;

import com.beyond.hodadoc.doctor.domain.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // 병원 소속 전체 의사 조회 (삭제되지 않은 의사만)
    List<Doctor> findByHospitalIdAndDelYn(Long hospitalId, String delYn);

    // ✅ 병원 + 진료과 필터 조회 (삭제되지 않은 의사만)
    List<Doctor> findByHospitalIdAndDepartmentIdAndDelYn(Long hospitalId, Long departmentId, String delYn);

    // Hospital을 함께 fetch join (LazyInitializationException 방지)
    @Query("SELECT d FROM Doctor d JOIN FETCH d.hospital WHERE d.id = :id")
    Optional<Doctor> findByIdWithHospital(@Param("id") Long id);
}