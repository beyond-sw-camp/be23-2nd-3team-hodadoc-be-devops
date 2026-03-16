package com.beyond.hodadoc.doctor.repository;

import com.beyond.hodadoc.doctor.domain.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    // 특정 의사의 특정 요일 스케줄 조회
    Optional<DoctorSchedule> findByDoctorIdAndDayOfWeek(Long doctorId, DayOfWeek dayOfWeek);
    List<DoctorSchedule> findByDoctorId(Long doctorId);
    void deleteByDoctorId(Long doctorId);

}
