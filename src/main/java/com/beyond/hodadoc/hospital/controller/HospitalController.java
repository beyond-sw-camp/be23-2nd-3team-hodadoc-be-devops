package com.beyond.hodadoc.hospital.controller;

import com.beyond.hodadoc.hospital.dtos.*;
import com.beyond.hodadoc.hospital.service.HospitalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/hospital")
public class HospitalController {
    private final HospitalService hospitalService;
    @Autowired
    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    // 병원 등록
    @PostMapping("/create")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<?> save(@ModelAttribute HospitalCreateDto hospitalCreateDto,
                                  @AuthenticationPrincipal Long principal){
        Long id = hospitalService.save(hospitalCreateDto, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    // 병원 수정
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @ModelAttribute HospitalUpdateDto hospitalUpdateDto,
                                    @AuthenticationPrincipal Long principal) {
        Long updatedId = hospitalService.update(id, hospitalUpdateDto, principal);
        return ResponseEntity.status(HttpStatus.OK).body(updatedId);
    }

    // 병원 삭제
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal Long principal) {
        hospitalService.delete(id, principal);
        return ResponseEntity.status(HttpStatus.OK).body("병원이 성공적으로 삭제되었습니다.");
    }

    // 내 병원 상세 조회
    @GetMapping("/my")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<?> getMyHospital(@AuthenticationPrincipal Long principal) {
        System.out.println(principal);
        HospitalDetailDto response = hospitalService.getMyHospital(principal);
        return ResponseEntity.ok(response);
    }

    // 특정 병원 상세 조회
    // SecurityConfig에서 permitAll() 설정완료
    @GetMapping("/{id}")
    public ResponseEntity<?> getHospitalDetail(@PathVariable Long id) {
        HospitalPublicDetailDto response = hospitalService.getHospitalDetail(id);
        return ResponseEntity.ok(response);
    }

    // 병원 목록 필터 검색
    @GetMapping("/list")
    public ResponseEntity<?> searchHospitals(@PageableDefault(size = 10) Pageable pageable,
                                             @ModelAttribute HospitalSearchDto searchDto) {
        if (searchDto.getUserLat() == null || searchDto.getUserLng() == null) {
            return ResponseEntity.badRequest().body("위도(userLat)와 경도(userLng)는 필수입니다.");
        }

        Page<HospitalListDto> hospitalList = hospitalService.getHospitalList(searchDto, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(hospitalList);
    }

    // 병원 예약 승인 모드 변경 (자동/수동)
    @PatchMapping("/{hospitalId}/approval-mode")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<?> updateApprovalMode(
            @PathVariable Long hospitalId,
            @RequestBody ApprovalModeUpdateReqDto dto,
            @AuthenticationPrincipal Long principal) {
        hospitalService.updateApprovalMode(hospitalId, principal, dto);
        return ResponseEntity.ok("승인 모드가 변경되었습니다.");
    }

    // 접수 마감/재개 토글
    @PatchMapping("/{hospitalId}/checkin-close")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<?> toggleCheckinClose(
            @PathVariable Long hospitalId,
            @RequestBody CheckinCloseReqDto dto,
            @AuthenticationPrincipal Long principal) {
        boolean isClosed = hospitalService.toggleCheckinClose(hospitalId, principal, dto);
        return ResponseEntity.ok(java.util.Map.of(
                "message", isClosed ? "접수가 마감되었습니다." : "접수가 재개되었습니다.",
                "checkinClosed", isClosed
        ));
    }

    // 통계 요약 카드 (오늘 예약, 이번달 예약, 오늘 접수, 이번달 접수)
    @GetMapping("/stats/summary")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<?> getStatsSummary(@AuthenticationPrincipal Long principal) {
        return ResponseEntity.ok(hospitalService.getStatsSummary(principal));
    }

    // 통계: 이번달 의사별 예약 순위
    @GetMapping("/stats/doctor-ranking")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<?> getDoctorRanking(@AuthenticationPrincipal Long principal) {
        return ResponseEntity.ok(hospitalService.getDoctorRanking(principal));
    }

    // 통계: 일별 추이 (꺾은선 그래프용)
    @GetMapping("/stats/daily-trend")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<?> getDailyTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String type,
            @AuthenticationPrincipal Long principal) {
        return ResponseEntity.ok(hospitalService.getDailyTrend(principal, startDate, endDate, type));
    }

    // 해시태그 생성
    @PostMapping("/hashtags")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<?> addHashtag(@RequestBody HospitalHashtagCreateDto dto,
                                        @AuthenticationPrincipal Long principal) {
        Long id = hospitalService.addHashtag(principal, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    // 해시태그 삭제
    @DeleteMapping("/hashtags/{hashtagId}")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<?> deleteHashtag(@PathVariable Long hashtagId,
                                           @AuthenticationPrincipal Long principal) {
        hospitalService.deleteHashtag(hashtagId, principal);
        return ResponseEntity.ok("해시태그가 삭제되었습니다.");
    }

    // map api (지도 마커용)
    @GetMapping("/mapApi")
    public ResponseEntity<?>  mapApi(){
        List<HospitalMapResponseDto> dto = hospitalService.mapApi();
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }

    // 거리계산 api - 사용자의 위도와 경도를 프론트에서 받아옴 -> db에 있는 병원들의 좌표를 사용자의 좌표와 계산해서 거리계산
    @GetMapping("/nearby")
    public ResponseEntity<?> nearby(@RequestParam double userLat,
                                    @RequestParam double userLng,
                                    @RequestParam(defaultValue = "5") double radius){
        List<HospitalNearbyResponseDto> dto = hospitalService.nearby(userLat, userLng, radius);
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }
}
