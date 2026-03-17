package com.beyond.hodadoc.hospital.repository;

import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.hospital.domain.HospitalStatus;
import com.beyond.hodadoc.hospital.dtos.HospitalSearchDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    @Query("select h from Hospital h inner join fetch h.address a " +
            "where a.latitude is not null and a.longitude is not null " +
            "and h.status <> :excluded")
    List<Hospital> findAllForMap(@Param("excluded") HospitalStatus excluded);

    List<Hospital> findByStatus(HospitalStatus status);

    long countByStatus(HospitalStatus status);

    Optional<Hospital> findByAccount_IdAndAccount_DelYn(Long accountId, String delYn);

    //    Filter 조회
    Page<Hospital> findAll(Specification<Hospital> specification, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT h.*
            FROM hospital h
            LEFT JOIN hospital_address a ON h.address_id = a.id

            LEFT JOIN hospital_department hd ON h.id = hd.hospital_id
            LEFT JOIN department d ON hd.department_id = d.id

            LEFT JOIN hospital_operating_time ot ON h.id = ot.hospital_id
            WHERE h.status = 'APPROVED'
              AND (:#{#dto.name} IS NULL OR (
                  h.name LIKE CONCAT('%', :#{#dto.name}, '%')
                  OR d.name LIKE CONCAT('%', :#{#dto.name}, '%')
                  OR a.street_address LIKE CONCAT('%', :#{#dto.name}, '%')
                  OR a.sido LIKE CONCAT('%', :#{#dto.name}, '%')
                  OR a.sigungu LIKE CONCAT('%', :#{#dto.name}, '%')
                  OR a.emd_name LIKE CONCAT('%', :#{#dto.name}, '%')
              ))
              AND (:#{#dto.sido} IS NULL OR a.sido LIKE CONCAT(:#{#dto.sido}, '%'))
              AND (:#{#dto.sigungu} IS NULL OR a.sigungu LIKE CONCAT('%', :#{#dto.sigungu}, '%'))
              AND (:#{#dto.emdName} IS NULL OR a.emd_name LIKE CONCAT(:#{#dto.emdName}, '%'))

              AND (:#{#dto.departmentName} IS NULL OR d.name = :#{#dto.departmentName})

              AND (:#{#dto.nightFilter} IS NULL OR EXISTS (
                  SELECT 1 FROM hospital_filter hf_n
                  JOIN filter f_n ON hf_n.filter_id = f_n.id
                  WHERE hf_n.hospital_id = h.id AND f_n.name = :#{#dto.nightFilter}
              ))
              AND (:#{#dto.holidayFilter} IS NULL OR EXISTS (
                  SELECT 1 FROM hospital_filter hf_h
                  JOIN filter f_h ON hf_h.filter_id = f_h.id
                  WHERE hf_h.hospital_id = h.id AND f_h.name = :#{#dto.holidayFilter}
              ))
              AND (:#{#dto.facilityFilter} IS NULL OR EXISTS (
                  SELECT 1 FROM hospital_filter hf_f
                  JOIN filter f_f ON hf_f.filter_id = f_f.id
                  WHERE hf_f.hospital_id = h.id AND f_f.name = :#{#dto.facilityFilter}
              ))
              AND (:#{#dto.serviceFilter} IS NULL OR EXISTS (
                  SELECT 1 FROM hospital_filter hf_s
                  JOIN filter f_s ON hf_s.filter_id = f_s.id
                  WHERE hf_s.hospital_id = h.id AND f_s.name = :#{#dto.serviceFilter}
              ))

              AND (
                  :#{#dto.isCurrentlyOpen} IS NULL
                  OR :#{#dto.isCurrentlyOpen} = false
                  OR (
                      ot.day_of_week = :#{#dto.currentDayOfWeekString}
                      AND ot.is_day_off = false
                      AND :#{#dto.currentTime} >= ot.open_time
                      AND :#{#dto.currentTime} < ot.close_time
                      AND (
                          ot.break_start_time IS NULL
                          OR :#{#dto.currentTime} < ot.break_start_time
                          OR :#{#dto.currentTime} >= ot.break_end_time
                      )
                      AND NOT EXISTS (
                          SELECT 1 FROM hospital_holiday hh
                          WHERE hh.hospital_id = h.id
                          AND hh.holiday_date = CURDATE()
                      )
                      AND NOT EXISTS (
                          SELECT 1 FROM public_holiday ph
                          WHERE ph.holiday_date = CURDATE()
                      )
                  )
              )
            ORDER BY
              CASE
                WHEN :#{#dto.name} IS NOT NULL AND h.name = :#{#dto.name} THEN 0
                WHEN :#{#dto.name} IS NOT NULL AND h.name LIKE CONCAT(:#{#dto.name}, '%') THEN 1
                ELSE 2
              END ASC,
              CASE WHEN EXISTS (
                  SELECT 1 FROM hospital_operating_time ot2
                  WHERE ot2.hospital_id = h.id
                    AND ot2.day_of_week = :#{#dto.currentDayOfWeekString}
                    AND ot2.is_day_off = false
                    AND :#{#dto.currentTime} >= ot2.open_time
                    AND :#{#dto.currentTime} < ot2.close_time
                    AND (
                        ot2.break_start_time IS NULL
                        OR :#{#dto.currentTime} < ot2.break_start_time
                        OR :#{#dto.currentTime} >= ot2.break_end_time
                    )
              ) AND NOT EXISTS (
                  SELECT 1 FROM hospital_holiday hh2
                  WHERE hh2.hospital_id = h.id
                    AND hh2.holiday_date = CURDATE()
              ) AND NOT EXISTS (
                  SELECT 1 FROM public_holiday ph2
                  WHERE ph2.holiday_date = CURDATE()
              ) THEN 0 ELSE 1 END ASC,
              ST_Distance_Sphere(POINT(a.longitude, a.latitude), POINT(:#{#dto.userLng}, :#{#dto.userLat})) ASC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT h.id)
            FROM hospital h
            LEFT JOIN hospital_address a ON h.address_id = a.id

            LEFT JOIN hospital_department hd ON h.id = hd.hospital_id
            LEFT JOIN department d ON hd.department_id = d.id

            LEFT JOIN hospital_operating_time ot ON h.id = ot.hospital_id
            WHERE h.status = 'APPROVED'
              AND (:#{#dto.name} IS NULL OR (
                  h.name LIKE CONCAT('%', :#{#dto.name}, '%')
                  OR d.name LIKE CONCAT('%', :#{#dto.name}, '%')
                  OR a.street_address LIKE CONCAT('%', :#{#dto.name}, '%')
                  OR a.sido LIKE CONCAT('%', :#{#dto.name}, '%')
                  OR a.sigungu LIKE CONCAT('%', :#{#dto.name}, '%')
                  OR a.emd_name LIKE CONCAT('%', :#{#dto.name}, '%')
              ))
              AND (:#{#dto.sido} IS NULL OR a.sido LIKE CONCAT(:#{#dto.sido}, '%'))
              AND (:#{#dto.sigungu} IS NULL OR a.sigungu LIKE CONCAT('%', :#{#dto.sigungu}, '%'))
              AND (:#{#dto.emdName} IS NULL OR a.emd_name LIKE CONCAT(:#{#dto.emdName}, '%'))
              AND (:#{#dto.departmentName} IS NULL OR d.name = :#{#dto.departmentName})
              AND (:#{#dto.nightFilter} IS NULL OR EXISTS (
                  SELECT 1 FROM hospital_filter hf_n
                  JOIN filter f_n ON hf_n.filter_id = f_n.id
                  WHERE hf_n.hospital_id = h.id AND f_n.name = :#{#dto.nightFilter}
              ))
              AND (:#{#dto.holidayFilter} IS NULL OR EXISTS (
                  SELECT 1 FROM hospital_filter hf_h
                  JOIN filter f_h ON hf_h.filter_id = f_h.id
                  WHERE hf_h.hospital_id = h.id AND f_h.name = :#{#dto.holidayFilter}
              ))
              AND (:#{#dto.facilityFilter} IS NULL OR EXISTS (
                  SELECT 1 FROM hospital_filter hf_f
                  JOIN filter f_f ON hf_f.filter_id = f_f.id
                  WHERE hf_f.hospital_id = h.id AND f_f.name = :#{#dto.facilityFilter}
              ))
              AND (:#{#dto.serviceFilter} IS NULL OR EXISTS (
                  SELECT 1 FROM hospital_filter hf_s
                  JOIN filter f_s ON hf_s.filter_id = f_s.id
                  WHERE hf_s.hospital_id = h.id AND f_s.name = :#{#dto.serviceFilter}
              ))
              AND (
                  :#{#dto.isCurrentlyOpen} IS NULL
                  OR :#{#dto.isCurrentlyOpen} = false
                  OR (
                      ot.day_of_week = :#{#dto.currentDayOfWeekString}
                      AND ot.is_day_off = false
                      AND :#{#dto.currentTime} >= ot.open_time
                      AND :#{#dto.currentTime} < ot.close_time
                      AND (
                          ot.break_start_time IS NULL
                          OR :#{#dto.currentTime} < ot.break_start_time
                          OR :#{#dto.currentTime} >= ot.break_end_time
                      )
                      AND NOT EXISTS (
                          SELECT 1 FROM hospital_holiday hh
                          WHERE hh.hospital_id = h.id
                          AND hh.holiday_date = CURDATE()
                      )
                      AND NOT EXISTS (
                          SELECT 1 FROM public_holiday ph
                          WHERE ph.holiday_date = CURDATE()
                      )
                  )
              )
            """,
            nativeQuery = true)
    Page<Hospital> searchHospitalsNative(@Param("dto") HospitalSearchDto dto, Pageable pageable);

    Optional<Hospital> findByPhone(String phone);
}