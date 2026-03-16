package com.beyond.hodadoc.common.repository;

import com.beyond.hodadoc.common.domain.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {

    boolean existsByHolidayDate(LocalDate holidayDate);

    List<PublicHoliday> findByYear(int year);

    @Modifying
    @Query("DELETE FROM PublicHoliday p WHERE p.year = :year")
    void deleteByYear(@Param("year") int year);
}
