package com.beyond.hodadoc.admin.repository;

import com.beyond.hodadoc.admin.domain.Filter;
import com.beyond.hodadoc.admin.domain.FilterCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilterRepository extends JpaRepository<Filter, Long> {
    List<Filter> findByCategory(FilterCategory category);
}
