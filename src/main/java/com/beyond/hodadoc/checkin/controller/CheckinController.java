package com.beyond.hodadoc.checkin.controller;

import com.beyond.hodadoc.checkin.dtos.*;
import com.beyond.hodadoc.checkin.service.CheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    /**
     * POST /checkin/patient
     * 환자: 병원에 온라인 접수 (대기번호 발급)
     * Body: { hospitalId }
     */
    @PostMapping("/patient")
    public ResponseEntity<?> createCheckin(
            @AuthenticationPrincipal Long principal,
            @RequestBody CheckinCreateReqDto dto
    ) {
        try {
            return ResponseEntity.ok(checkinService.createCheckin(principal, dto));
        } catch (IllegalStateException e) {
            // ✅ 중복 접수 / 휴무일 등 비즈니스 규칙 위반 → 409 Conflict
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            // 병원/환자 정보 없음 → 400 Bad Request
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * GET /checkin/patient?page=&size=
     * 환자: 본인 접수 목록 조회
     */
    @GetMapping("/patient")
    public ResponseEntity<Page<CheckinPatientListDto>> findMyCheckins(
            @AuthenticationPrincipal Long principal,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(checkinService.findMyCheckins(principal, pageable));
    }

    /**
     * DELETE /checkin/patient/{checkinId}
     * 환자: 본인 접수 취소
     */
    @DeleteMapping("/patient/{checkinId}")
    public ResponseEntity<Void> cancelCheckin(
            @AuthenticationPrincipal Long principal,
            @PathVariable Long checkinId
    ) {
        checkinService.cancelCheckin(principal, checkinId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /checkin/hospital?page=&size=
     * 병원 관리자: 당일 접수 목록 조회
     */
    @GetMapping("/hospital")
    public ResponseEntity<Page<CheckinHospitalListDto>> findTodayCheckins(
            @AuthenticationPrincipal Long principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(checkinService.findTodayCheckins(principal, pageable));
    }

    /**
     * PATCH /checkin/{checkinId}/status
     * 병원 관리자: 접수 상태 변경 (CALLED / COMPLETED)
     * Body: { status }
     */
    @PatchMapping("/{checkinId}/status")
    public ResponseEntity<CheckinHospitalListDto> updateStatus(
            @AuthenticationPrincipal Long principal,
            @PathVariable Long checkinId,
            @RequestBody CheckinStatusUpdateReqDto dto
    ) {
        return ResponseEntity.ok(checkinService.updateCheckinStatus(principal, checkinId, dto));
    }
}