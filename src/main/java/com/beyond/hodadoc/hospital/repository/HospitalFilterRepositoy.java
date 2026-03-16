package com.beyond.hodadoc.hospital.repository;

import com.beyond.hodadoc.admin.domain.Filter;
import com.beyond.hodadoc.hospital.domain.HospitalFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalFilterRepositoy extends JpaRepository<HospitalFilter, Long> {
    @Query("select f from HospitalFilter hf join hf.filter f where hf.hospital.id = :hospitalId")
    List<Filter> findByFiltersHospitalId(@Param("hospitalId") Long hospitalId);

    boolean existsByFilter(Filter filter);
}
