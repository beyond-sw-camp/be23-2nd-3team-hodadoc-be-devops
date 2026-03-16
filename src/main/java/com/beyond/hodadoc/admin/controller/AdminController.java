package com.beyond.hodadoc.admin.controller;

import com.beyond.hodadoc.admin.domain.Department;
import com.beyond.hodadoc.admin.domain.Filter;
import com.beyond.hodadoc.admin.dtos.AdminSummaryDto;
import com.beyond.hodadoc.admin.dtos.FilterCreateDto;
import com.beyond.hodadoc.admin.dtos.NameDto;
import com.beyond.hodadoc.admin.service.AdminService;
import com.beyond.hodadoc.hospital.domain.HospitalStatus;
import com.beyond.hodadoc.hospital.dtos.HospitalDetailDto;
import com.beyond.hodadoc.hospital.dtos.HospitalStatusUpdateDto;
import com.beyond.hodadoc.hospital.service.HospitalService;
import com.beyond.hodadoc.review.service.ReviewReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;
    private final HospitalService hospitalService;
    private final ReviewReportService reviewReportService;

    @Autowired
    public AdminController(AdminService adminService,
                           HospitalService hospitalService,
                           ReviewReportService reviewReportService) {
        this.adminService = adminService;
        this.hospitalService = hospitalService;
        this.reviewReportService = reviewReportService;
    }

    // ==================== 대시보드 ====================

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminSummaryDto> getDashboardSummary() {
        return ResponseEntity.ok(adminService.getDashboardSummary());
    }

    // ==================== 병원 승인 관리 ====================

    @GetMapping("/hospital/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<HospitalDetailDto>> getPendingHospitals() {
        return ResponseEntity.ok(hospitalService.findPendingHospitals());
    }

    @PatchMapping("/hospital/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> approveHospital(@PathVariable Long id) {
        hospitalService.updateStatus(id, new HospitalStatusUpdateDto(HospitalStatus.APPROVED));
        return ResponseEntity.ok("병원이 승인되었습니다.");
    }

    @PatchMapping("/hospital/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> rejectHospital(@PathVariable Long id) {
        hospitalService.updateStatus(id, new HospitalStatusUpdateDto(HospitalStatus.REJECTED));
        return ResponseEntity.ok("병원이 반려되었습니다.");
    }

    // ==================== 진료과 관리 ====================

    @PostMapping("/department")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> departmentCreate(@RequestBody NameDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createDepartment(dto));
    }

    @GetMapping("/department/list")
    public ResponseEntity<List<Department>> departmentFindAll() {
        return ResponseEntity.ok(adminService.findAllDepartments());
    }

    @DeleteMapping("/department/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> departmentDelete(@PathVariable Long id) {
        adminService.deleteDepartment(id);
        return ResponseEntity.ok("진료과가 삭제되었습니다.");
    }

    // ==================== 검색 필터 관리 ====================

    @PostMapping("/filter")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Filter> filterCreate(@RequestBody FilterCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createFilter(dto));
    }

    @GetMapping("/filter/list")
    public ResponseEntity<List<Filter>> filterFindAll() {
        return ResponseEntity.ok(adminService.findAllFilters());
    }

    @DeleteMapping("/filter/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> filterDelete(@PathVariable Long id) {
        adminService.deleteFilter(id);
        return ResponseEntity.ok("필터가 삭제되었습니다.");
    }
}
