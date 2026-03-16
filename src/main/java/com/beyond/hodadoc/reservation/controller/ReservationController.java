package com.beyond.hodadoc.reservation.controller;

import com.beyond.hodadoc.reservation.dtos.*;
import com.beyond.hodadoc.reservation.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reservation")
public class ReservationController {

    private final ReservationService reservationService;

    @Autowired
    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // ==================== 환자 ====================

    // 환자 예약 생성 (AUTO: APPROVED / MANUAL: WAITING)
    // POST /reservation/patient
    @PostMapping("/patient")
    public ReservationPatientListDto createByPatient(
            @AuthenticationPrincipal Long principal,
            @RequestBody ReservationCreateReqDto dto) {
        return reservationService.createByPatient(principal, dto);
    }

    // 환자 예약 취소 (CANCELLED)
    // DELETE /reservation/patient/{reservationPatientId}
    @DeleteMapping("/patient/{reservationPatientId}")
    public String cancelByPatient(
            @AuthenticationPrincipal Long principal,
            @PathVariable Long reservationPatientId) {
        reservationService.cancelByPatient(principal, reservationPatientId);
        return "예약이 취소되었습니다.";
    }

    // 환자 -> 내 예약목록 조회
    // GET /reservation/patient
    @GetMapping("/patient")
    public Page<ReservationPatientListDto> findAllMyReservation(
            @AuthenticationPrincipal Long principal,
            @PageableDefault(size = 10, sort = {"reservationDate", "reservationTime"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return reservationService.findAllMyReservation(principal, pageable);
    }

    // ==================== 병원 관리자 ====================

    // 예약 가능 슬롯 조회 (on-the-fly 실시간 계산)
    // GET /reservation/slots?doctorId=1&date=2026-02-28
    @GetMapping("/slots")
    public List<AvailableSlotDto> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reservationService.getAvailableSlots(doctorId, date);
    }

    // 슬롯 수동 블록 (수술/부재 등으로 예약 차단)
    // POST /reservation/hospital/block
    @PostMapping("/hospital/block")
    public String blockSlot(
            @AuthenticationPrincipal Long principal,
            @RequestBody BlockSlotReqDto dto) {
        reservationService.blockSlot(principal, dto);
        return "해당 시간이 블록 처리되었습니다.";
    }

    // 블록 해제
    // DELETE /reservation/hospital/block/{reservationId}
    @DeleteMapping("/hospital/block/{reservationId}")
    public String unblockSlot(
            @AuthenticationPrincipal Long principal,
            @PathVariable Long reservationId) {
        reservationService.unblockSlot(principal, reservationId);
        return "블록이 해제되었습니다.";
    }

    // 병원 -> 환자 예약목록 조회 (BLOCKED 제외)
    // GET /reservation/hospital
    @GetMapping("/hospital")
    public Page<ReservationHospitalListDto> findAllPatientReservation(
            @AuthenticationPrincipal Long principal,
            @PageableDefault(size = 10, sort = {"reservationDate", "reservationTime"},
                    direction = Sort.Direction.DESC) Pageable pageable,
            @ModelAttribute ReservationSearchDto searchDto) {
        return reservationService.findAllPatientReservation(principal, pageable, searchDto);
    }
    // ══════════════════════════════════════════════════════════════
// 추가 엔드포인트 1: 주간 달력용 예약 목록
// GET /reservation/list?doctorId=1&startDate=2026-02-24&endDate=2026-03-02
// ══════════════════════════════════════════════════════════════
    @GetMapping("/list")
    public List<ReservationWeekResDto> weeklyList(
            @AuthenticationPrincipal Long principal,
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reservationService.findWeeklyByDoctor(principal, doctorId, startDate, endDate);
    }

    // ══════════════════════════════════════════════════════════════
// 추가 엔드포인트 2: 예약 승인
// PATCH /reservation/{reservationId}/approve
// ══════════════════════════════════════════════════════════════
    @PatchMapping("/{reservationId}/approve")
    public ReservationWeekResDto approve(
            @AuthenticationPrincipal Long principal,
            @PathVariable Long reservationId) {
        return reservationService.approve(principal, reservationId);
    }

    // ══════════════════════════════════════════════════════════════
// 추가 엔드포인트 3: 예약 거절 (병원 관리자) - 수동승인일때
// PATCH /reservation/{reservationId}/reject
// ══════════════════════════════════════════════════════════════
    @PatchMapping("/{reservationId}/reject")
    public ReservationWeekResDto reject(
            @AuthenticationPrincipal Long principal,
            @PathVariable Long reservationId) {
        return reservationService.reject(principal, reservationId);
    }

    // ══════════════════════════════════════════════════════════════
// 추가 엔드포인트 4: 예약 취소 (병원 관리자) - 자동/승인 둘다일때
// PATCH /reservation/{reservationId}/cancel
// ══════════════════════════════════════════════════════════════
    @PatchMapping("/{reservationId}/cancel")
    public ReservationWeekResDto cancelByAdmin(
            @AuthenticationPrincipal Long principal,
            @PathVariable Long reservationId) {
        return reservationService.cancelByAdmin(principal, reservationId);
    }

    // ══════════════════════════════════════════════════════════════
// 추가 엔드포인트 5: 진료 완료 처리
// PATCH /reservation/{reservationId}/complete
// ══════════════════════════════════════════════════════════════
    @PatchMapping("/{reservationId}/complete")
    public ReservationWeekResDto complete(
            @AuthenticationPrincipal Long principal,
            @PathVariable Long reservationId) {
        return reservationService.complete(principal, reservationId);
    }
}
