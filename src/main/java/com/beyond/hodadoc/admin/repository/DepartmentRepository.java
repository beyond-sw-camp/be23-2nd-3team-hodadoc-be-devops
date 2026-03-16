package com.beyond.hodadoc.admin.repository;

import com.beyond.hodadoc.admin.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
