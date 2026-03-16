package com.beyond.hodadoc.checkin.repository;

import com.beyond.hodadoc.checkin.domain.Checkin;
import com.beyond.hodadoc.checkin.domain.CheckinStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CheckinRepository extends JpaRepository<Checkin, Long> {

    // 당일 특정 병원의 대기 번호 최댓값 조회 (다음 번호 계산용)
    @Query("SELECT COALESCE(MAX(c.waitingNumber), 0) FROM Checkin c " +
            "WHERE c.hospital.id = :hospitalId " +
            "AND c.checkinTime >= :startOfDay")
    Integer findMaxWaitingNumberToday(
            @Param("hospitalId") Long hospitalId,
            @Param("startOfDay") LocalDateTime startOfDay
    );

    // 환자 본인의 접수 목록 (페이징)
    Page<Checkin> findByPatientId(Long patientId, Pageable pageable);

    // 병원 측 당일 접수 목록 (페이징)
    @Query("SELECT c FROM Checkin c " +
            "WHERE c.hospital.id = :hospitalId " +
            "AND c.checkinTime >= :startOfDay " +
            "ORDER BY c.waitingNumber ASC")
    Page<Checkin> findTodayByHospitalId(
            @Param("hospitalId") Long hospitalId,
            @Param("startOfDay") LocalDateTime startOfDay,
            Pageable pageable
    );

    Optional<Checkin> findByIdAndStatus(Long id, CheckinStatus status);

    // 당일 해당 병원에서 내 앞에 대기 중인 환자 수 (현재 대기 순번 계산용)
    @Query("SELECT COUNT(c) FROM Checkin c " +
            "WHERE c.hospital.id = :hospitalId " +
            "AND c.checkinTime >= :startOfDay " +
            "AND c.status = :status " +
            "AND c.waitingNumber < :waitingNumber")
    int countWaitingBefore(
            @Param("hospitalId") Long hospitalId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("status") CheckinStatus status,
            @Param("waitingNumber") int waitingNumber
    );

    // 당일 해당 병원의 WAITING 상태 접수 목록 (대기순번 업데이트 알림용)
    @Query("SELECT c FROM Checkin c JOIN FETCH c.patient p JOIN FETCH p.account " +
            "WHERE c.hospital.id = :hospitalId " +
            "AND c.checkinTime >= :startOfDay " +
            "AND c.status = :status " +
            "ORDER BY c.waitingNumber ASC")
    List<Checkin> findTodayWaitingByHospitalId(
            @Param("hospitalId") Long hospitalId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("status") CheckinStatus status
    );

    // ✅ 추가: 당일 특정 환자가 특정 병원에 WAITING 상태 접수가 있는지 확인 (중복 접수 방지)
    @Query("SELECT COUNT(c) > 0 FROM Checkin c " +
            "WHERE c.hospital.id = :hospitalId " +
            "AND c.patient.id = :patientId " +
            "AND c.checkinTime >= :startOfDay " +
            "AND c.status = :status")
    boolean existsTodayActiveCheckin(
            @Param("hospitalId") Long hospitalId,
            @Param("patientId") Long patientId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("status") CheckinStatus status
    );

    // 통계: 오늘 접수 건수 (CANCELLED 제외)
    @Query("SELECT COUNT(c) FROM Checkin c " +
            "WHERE c.hospital.id = :hospitalId " +
            "AND c.checkinTime >= :startOfDay " +
            "AND c.checkinTime < :endOfDay " +
            "AND c.status <> 'CANCELLED'")
    long countTodayCheckins(
            @Param("hospitalId") Long hospitalId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    // 통계: 이번달 접수 건수 (CANCELLED 제외)
    @Query("SELECT COUNT(c) FROM Checkin c " +
            "WHERE c.hospital.id = :hospitalId " +
            "AND c.checkinTime >= :startOfMonth " +
            "AND c.checkinTime < :endOfDay " +
            "AND c.status <> 'CANCELLED'")
    long countMonthCheckins(
            @Param("hospitalId") Long hospitalId,
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    // 통계: 일별 접수 건수 (꺾은선 그래프용, checkinTime은 LocalDateTime이므로 DATE 함수로 날짜 추출)
    @Query("SELECT FUNCTION('DATE', c.checkinTime), COUNT(c) FROM Checkin c " +
            "WHERE c.hospital.id = :hospitalId " +
            "AND c.checkinTime >= :startDateTime " +
            "AND c.checkinTime < :endDateTime " +
            "AND c.status <> 'CANCELLED' " +
            "GROUP BY FUNCTION('DATE', c.checkinTime) " +
            "ORDER BY FUNCTION('DATE', c.checkinTime)")
    List<Object[]> countDailyCheckins(
            @Param("hospitalId") Long hospitalId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}