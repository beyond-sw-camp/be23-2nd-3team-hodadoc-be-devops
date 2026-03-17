package com.beyond.hodadoc.doctor.controller;

import com.beyond.hodadoc.doctor.dtos.*;
import com.beyond.hodadoc.doctor.service.DoctorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/doctor")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    // ── 의사 CRUD ──────────────────────────────────────────────────────────────

    /** GET /doctor/hospital/{hospitalId} - 병원 소속 의사 목록 (공개) */
    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<List<DoctorResDto>> getByHospital(@PathVariable Long hospitalId) {
        return ResponseEntity.ok(doctorService.findByHospitalId(hospitalId));
    }

    /** GET /doctor/list - 내 병원 의사 목록 (관리자) */
    @GetMapping("/list")
    public ResponseEntity<List<DoctorResDto>> listByHospital(
            @AuthenticationPrincipal Long accountId,
            @RequestParam(required = false) Long departmentId) {
        return ResponseEntity.ok(doctorService.findAllByHospital(accountId, departmentId));
    }

    /** POST /doctor/create */
    @PostMapping("/create")
    public ResponseEntity<DoctorResDto> create(
            @AuthenticationPrincipal Long accountId,
            @ModelAttribute DoctorCreateReqDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(doctorService.create(accountId, dto));
    }

    /** PUT /doctor/{doctorId} */
    @PutMapping("/{doctorId}")
    public ResponseEntity<DoctorResDto> update(
            @AuthenticationPrincipal Long accountId,
            @PathVariable Long doctorId,
            @ModelAttribute DoctorUpdateReqDto dto) {
        return ResponseEntity.ok(doctorService.update(accountId, doctorId, dto));
    }

    /** DELETE /doctor/{doctorId} */
    @DeleteMapping("/{doctorId}")
    public ResponseEntity<String> delete(
            @AuthenticationPrincipal Long accountId,
            @PathVariable Long doctorId) {
        doctorService.delete(accountId, doctorId);
        return ResponseEntity.ok("의사가 삭제되었습니다.");
    }

    // ── 근무규칙(스케줄) ────────────────────────────────────────────────────────

    /** GET /doctor/{doctorId}/schedule - 관리자용 (인증 필요) */
    @GetMapping("/{doctorId}/schedule")
    public ResponseEntity<List<DoctorScheduleResDto>> getSchedule(
            @AuthenticationPrincipal Long accountId,
            @PathVariable Long doctorId) {
        return ResponseEntity.ok(doctorService.getSchedules(accountId, doctorId));
    }

    /**
     * [추가] GET /doctor/{doctorId}/schedule/public - 환자용 (인증 불필요)
     * 환자 예약 달력에서 의사 정기 휴무 요일을 미리 표시하기 위해 사용
     */
    @GetMapping("/{doctorId}/schedule/public")
    public ResponseEntity<List<DoctorScheduleResDto>> getSchedulePublic(
            @PathVariable Long doctorId) {
        return ResponseEntity.ok(doctorService.getSchedulesPublic(doctorId));
    }

    /** PUT /doctor/{doctorId}/schedule */
    @PutMapping("/{doctorId}/schedule")
    public ResponseEntity<List<DoctorScheduleResDto>> saveSchedule(
            @AuthenticationPrincipal Long accountId,
            @PathVariable Long doctorId,
            @RequestBody List<DoctorScheduleReqDto> dtos) {
        return ResponseEntity.ok(doctorService.saveSchedules(accountId, doctorId, dtos));
    }

    // ── 휴무/연차 ───────────────────────────────────────────────────────────────

    /** GET /doctor/{doctorId}/offdays/public - 환자용 (인증 불필요) */
    @GetMapping("/{doctorId}/offdays/public")
    public ResponseEntity<List<DoctorOffDayResDto>> getOffDaysPublic(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(doctorService.getOffDaysPublic(doctorId, startDate, endDate));
    }

    /** GET /doctor/{doctorId}/offdays - 관리자용 (인증 필요) */
    @GetMapping("/{doctorId}/offdays")
    public ResponseEntity<List<DoctorOffDayResDto>> getOffDays(
            @AuthenticationPrincipal Long accountId,
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(doctorService.getOffDays(accountId, doctorId, startDate, endDate));
    }

    /** POST /doctor/offday */
    @PostMapping("/offday")
    public ResponseEntity<DoctorOffDayResDto> createOffDay(
            @AuthenticationPrincipal Long accountId,
            @RequestBody DoctorOffDayReqDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(doctorService.createOffDay(accountId, dto));
    }

    // ── 배치 오프데이 API ────────────────────────────────────────────────────────

    /**
     * 여러 날짜 일괄 OFF / BLOCKED 등록
     * POST /doctor/{doctorId}/off-days/batch
     * Body: { "dates": ["2025-03-15", "2025-03-20"], "type": "OFF" }
     */
    @PostMapping("/{doctorId}/off-days/batch")
    public ResponseEntity<Void> batchSetOffDays(
            @AuthenticationPrincipal Long accountId,
            @PathVariable Long doctorId,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> dates = (List<String>) body.get("dates");
        String type = (String) body.get("type");
        doctorService.batchSetOffDays(accountId, doctorId, dates, type);
        return ResponseEntity.ok().build();
    }

    /**
     * 여러 날짜 일괄 초기화
     * DELETE /doctor/{doctorId}/off-days/batch
     * Body: { "dates": ["2025-03-15"] }
     */
    @DeleteMapping("/{doctorId}/off-days/batch")
    public ResponseEntity<Void> batchResetOffDays(
            @AuthenticationPrincipal Long accountId,
            @PathVariable Long doctorId,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> dates = (List<String>) body.get("dates");
        doctorService.batchResetOffDays(accountId, doctorId, dates);
        return ResponseEntity.noContent().build();
    }

    /**
     * 특정 월 전체 오프데이 초기화
     * DELETE /doctor/{doctorId}/off-days/month
     * Body: { "year": 2025, "month": 3 }
     */
    @DeleteMapping("/{doctorId}/off-days/month")
    public ResponseEntity<Void> resetMonthOffDays(
            @AuthenticationPrincipal Long accountId,
            @PathVariable Long doctorId,
            @RequestBody Map<String, Object> body) {
        int year  = (int) body.get("year");
        int month = (int) body.get("month");
        doctorService.resetMonthOffDays(accountId, doctorId, year, month);
        return ResponseEntity.noContent().build();
    }
}