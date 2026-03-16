package com.beyond.hodadoc.common.controller;

import com.beyond.hodadoc.common.dtos.PublicHolidayResDto;
import com.beyond.hodadoc.common.service.PublicHolidayService;
import com.beyond.hodadoc.hospital.service.HospitalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/public-holidays")
public class PublicHolidayController {

    private final PublicHolidayService publicHolidayService;
    private final HospitalService hospitalService;

    @Autowired
    public PublicHolidayController(PublicHolidayService publicHolidayService,
                                   HospitalService hospitalService) {
        this.publicHolidayService = publicHolidayService;
        this.hospitalService = hospitalService;
    }

    // 연도별 공휴일 목록 조회
    @GetMapping
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<List<PublicHolidayResDto>> getHolidaysByYear(@RequestParam int year) {
        List<PublicHolidayResDto> holidays = publicHolidayService.getHolidaysForYear(year)
                .stream()
                .map(PublicHolidayResDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(holidays);
    }

    // 선택한 공휴일을 병원 휴무일로 일괄 등록
    @PostMapping("/apply")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<?> applyToHospital(@RequestBody Map<String, List<String>> body,
                                             @AuthenticationPrincipal Long principal) {
        List<LocalDate> dates = body.get("dates").stream()
                .map(LocalDate::parse)
                .collect(Collectors.toList());
        hospitalService.applyPublicHolidays(principal, dates);
        return ResponseEntity.ok("공휴일이 병원 휴무일로 등록되었습니다.");
    }

    // 병원 휴무일에서 선택한 공휴일 제거
    @DeleteMapping("/remove")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<?> removeFromHospital(@RequestBody Map<String, List<String>> body,
                                                @AuthenticationPrincipal Long principal) {
        List<LocalDate> dates = body.get("dates").stream()
                .map(LocalDate::parse)
                .collect(Collectors.toList());
        hospitalService.removePublicHolidays(principal, dates);
        return ResponseEntity.ok("공휴일이 병원 휴무일에서 제거되었습니다.");
    }
}
