package com.beyond.hodadoc.reservation.repository;

import com.beyond.hodadoc.reservation.domain.ReservationPatient;
import com.beyond.hodadoc.reservation.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationPatient, Long>,
        JpaSpecificationExecutor<ReservationPatient> {

    @EntityGraph(attributePaths = {"patient", "doctor", "hospital"})
    Optional<ReservationPatient> findWithDetailById(Long id);

    @Query("select rp.reservationTime from ReservationPatient rp " +
            "where rp.doctor.id = :doctorId " +
            "and rp.reservationDate = :date " +
            "and rp.status NOT IN :excludedStatuses")
    List<LocalTime> findOccupiedTimes(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("excludedStatuses") List<ReservationStatus> excludedStatuses);

    @Query("select rp.reservationTime from ReservationPatient rp " +
            "where rp.doctor.id = :doctorId " +
            "and rp.reservationDate = :date " +
            "and rp.status = 'BLOCKED'")
    List<LocalTime> findBlockedTimes(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date);

    @EntityGraph(attributePaths = {"doctor", "hospital"})
    @Query("select rp from ReservationPatient rp " +
            "where rp.patient.id = :patientId " +
            "and rp.status <> 'BLOCKED'")
    Page<ReservationPatient> findAllMyReservation(
            @Param("patientId") Long patientId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"patient", "doctor"})
    @Query("select r from ReservationPatient r " +
            "where r.doctor.id = :doctorId " +
            "and r.reservationDate between :startDate and :endDate " +
            "order by r.reservationDate, r.reservationTime")
    List<ReservationPatient> findWeeklyByDoctorId(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 근무규칙 변경 시 특정 의사의 특정 날짜 범위 BLOCKED 슬롯 일괄 삭제
    @Modifying
    @Query("delete from ReservationPatient r " +
            "where r.doctor.id = :doctorId " +
            "and r.reservationDate in :dates " +
            "and r.status = 'BLOCKED'")
    void deleteBlockedByDoctorIdAndDates(
            @Param("doctorId") Long doctorId,
            @Param("dates") List<LocalDate> dates);

    // 리마인더 스케줄러: 특정 날짜 + 시간 범위의 APPROVED 예약 조회
    @EntityGraph(attributePaths = {"patient", "hospital"})
    @Query("select r from ReservationPatient r " +
            "where r.reservationDate = :date " +
            "and r.reservationTime between :fromTime and :toTime " +
            "and r.status = 'APPROVED'")
    List<ReservationPatient> findUpcomingApprovedReservations(
            @Param("date") LocalDate date,
            @Param("fromTime") LocalTime fromTime,
            @Param("toTime") LocalTime toTime);

    // 동일 슬롯에 활성 예약(WAITING/APPROVED/BLOCKED) 존재 여부 확인
    @Query("select count(r) > 0 from ReservationPatient r " +
            "where r.doctor.id = :doctorId " +
            "and r.reservationDate = :date " +
            "and r.reservationTime = :time " +
            "and r.status IN ('WAITING', 'APPROVED', 'BLOCKED')")
    boolean existsActiveByDoctorDateAndTime(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time);

    // ✅ 추가: 이미 BLOCKED 상태인지 확인 (blockSlot 멱등성 처리용)
    boolean existsByDoctorIdAndReservationDateAndReservationTimeAndStatus(
            Long doctorId,
            LocalDate reservationDate,
            LocalTime reservationTime,
            ReservationStatus status);

    // 통계: 오늘 예약 건수 (WAITING, APPROVED, COMPLETED만)
    @Query("SELECT COUNT(r) FROM ReservationPatient r " +
            "WHERE r.hospital.id = :hospitalId " +
            "AND r.reservationDate = :today " +
            "AND r.status IN ('WAITING', 'APPROVED', 'COMPLETED')")
    long countTodayReservations(
            @Param("hospitalId") Long hospitalId,
            @Param("today") LocalDate today);

    // 통계: 이번달 예약 건수 (WAITING, APPROVED, COMPLETED만, 1일~말일)
    @Query("SELECT COUNT(r) FROM ReservationPatient r " +
            "WHERE r.hospital.id = :hospitalId " +
            "AND r.reservationDate >= :startOfMonth " +
            "AND r.reservationDate <= :endOfMonth " +
            "AND r.status IN ('WAITING', 'APPROVED', 'COMPLETED')")
    long countMonthReservations(
            @Param("hospitalId") Long hospitalId,
            @Param("startOfMonth") LocalDate startOfMonth,
            @Param("endOfMonth") LocalDate endOfMonth);

    // 통계: 일별 예약 건수 (꺾은선 그래프용)
    @Query("SELECT r.reservationDate, COUNT(r) FROM ReservationPatient r " +
            "WHERE r.hospital.id = :hospitalId " +
            "AND r.reservationDate BETWEEN :startDate AND :endDate " +
            "AND r.status IN ('WAITING', 'APPROVED', 'COMPLETED') " +
            "GROUP BY r.reservationDate " +
            "ORDER BY r.reservationDate")
    List<Object[]> countDailyReservations(
            @Param("hospitalId") Long hospitalId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 통계: 이번달 의사별 예약 건수 순위 (WAITING, APPROVED, COMPLETED만, 1일~말일)
    @Query("SELECT r.doctor.id, r.doctor.name, COUNT(r) FROM ReservationPatient r " +
            "WHERE r.hospital.id = :hospitalId " +
            "AND r.reservationDate >= :startOfMonth " +
            "AND r.reservationDate <= :endOfMonth " +
            "AND r.status IN ('WAITING', 'APPROVED', 'COMPLETED') " +
            "GROUP BY r.doctor.id, r.doctor.name " +
            "ORDER BY COUNT(r) DESC")
    List<Object[]> findMonthDoctorRanking(
            @Param("hospitalId") Long hospitalId,
            @Param("startOfMonth") LocalDate startOfMonth,
            @Param("endOfMonth") LocalDate endOfMonth);
    // 특정 슬롯의 비활성(CANCELLED) 예약 삭제 - 분산락 처리 후 정리용
    @Modifying
    @Query("delete from ReservationPatient r " +
            "where r.doctor.id = :doctorId " +
            "and r.reservationDate = :date " +
            "and r.reservationTime = :time " +
            "and r.status NOT IN ('WAITING', 'APPROVED', 'BLOCKED')")
    void deleteInactiveByDoctorDateAndTime(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time);
}