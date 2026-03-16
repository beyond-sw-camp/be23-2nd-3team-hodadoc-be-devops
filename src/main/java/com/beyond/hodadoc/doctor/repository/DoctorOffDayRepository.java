package com.beyond.hodadoc.doctor.repository;

import com.beyond.hodadoc.doctor.domain.DoctorOffDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorOffDayRepository extends JpaRepository<DoctorOffDay, Long> {

    // 기존: 특정 날짜에 오프데이 존재 여부 확인
    boolean existsByDoctorIdAndOffDate(Long doctorId, LocalDate offDate);

    // 기존: 날짜 범위 조회 (주간/월간 달력 로딩)
    List<DoctorOffDay> findByDoctorIdAndOffDateBetween(Long doctorId, LocalDate startDate, LocalDate endDate);

    // ✅ 추가: 특정 날짜 단건 조회 (배치 등록 시 upsert용)
    Optional<DoctorOffDay> findByDoctorIdAndOffDate(Long doctorId, LocalDate offDate);

    // ✅ 추가: 특정 날짜 삭제 (선택 날짜 초기화)
    void deleteByDoctorIdAndOffDate(Long doctorId, LocalDate offDate);

    // ✅ 추가: 날짜 범위 삭제 (월 전체 초기화)
    void deleteByDoctorIdAndOffDateBetween(Long doctorId, LocalDate start, LocalDate end);
}