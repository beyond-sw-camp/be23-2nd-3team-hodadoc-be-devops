package com.beyond.hodadoc.hospital.service;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.account.repository.AccountRepository;
import com.beyond.hodadoc.admin.repository.DepartmentRepository;
import com.beyond.hodadoc.admin.repository.FilterRepository;
import com.beyond.hodadoc.hospital.domain.*;
import com.beyond.hodadoc.hospital.dtos.*;
import com.beyond.hodadoc.admin.domain.Department;
import com.beyond.hodadoc.admin.domain.Filter;
import com.beyond.hodadoc.checkin.repository.CheckinRepository;
import com.beyond.hodadoc.hospital.repository.HospitalFilterRepositoy;
import com.beyond.hodadoc.hospital.repository.HospitalHashtagRepository;
import com.beyond.hodadoc.hospital.repository.HospitalRepository;
import com.beyond.hodadoc.reservation.repository.ReservationRepository;
import com.beyond.hodadoc.common.domain.PublicHoliday;
import com.beyond.hodadoc.common.service.PublicHolidayService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final AccountRepository accountRepository;
    private final DepartmentRepository departmentRepository;
    private final FilterRepository filterRepository;
    private final AwsS3Service awsS3Service;
    private final HospitalFilterRepositoy hospitalFilterRepositoy;
    private final HospitalHashtagRepository hospitalHashtagRepository;
    private final ReservationRepository reservationRepository;
    private final CheckinRepository checkinRepository;
    private final PublicHolidayService publicHolidayService;

    @Autowired
    public HospitalService(HospitalRepository hospitalRepository,
                           AccountRepository accountRepository,
                           DepartmentRepository departmentRepository,
                           FilterRepository filterRepository,
                           AwsS3Service awsS3Service,
                           HospitalFilterRepositoy hospitalFilterRepositoy,
                           HospitalHashtagRepository hospitalHashtagRepository,
                           PublicHolidayService publicHolidayService,
                           ReservationRepository reservationRepository,
                           CheckinRepository checkinRepository) {
        this.hospitalRepository = hospitalRepository;
        this.accountRepository = accountRepository;
        this.departmentRepository = departmentRepository;
        this.filterRepository = filterRepository;
        this.awsS3Service = awsS3Service;
        this.hospitalFilterRepositoy = hospitalFilterRepositoy;
        this.hospitalHashtagRepository = hospitalHashtagRepository;
        this.reservationRepository = reservationRepository;
        this.checkinRepository = checkinRepository;
        this.publicHolidayService = publicHolidayService;
    }

    @Transactional
    public Long save(HospitalCreateDto dto, Long accountId) {
        log.info("[병원 등록] 받은 latitude={}, longitude={}", dto.getLatitude(), dto.getLongitude());

        Account account = accountRepository.findByIdAndDelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("계정을 찾을 수 없습니다."));

        if (hospitalRepository.findByAccount_IdAndAccount_DelYn(accountId, "N").isPresent()) {
            throw new IllegalArgumentException("이미 이 계정에 등록된 병원이 존재합니다.");
        }

        Hospital hospital = dto.toEntity(account);

        if (dto.getDepartmentIds() != null) {
            List<Department> departments = departmentRepository.findAllById(dto.getDepartmentIds());
            for (Department dept : departments) {
                hospital.getHospitalDepartments().add(HospitalDepartment.builder()
                        .hospital(hospital).department(dept).build());
            }
        }

        if (dto.getFilterIds() != null) {
            List<Filter> filters = filterRepository.findAllById(dto.getFilterIds());
            for (Filter filter : filters) {
                hospital.getHospitalFilters().add(HospitalFilter.builder()
                        .hospital(hospital).filter(filter).build());
            }
        }

        Hospital hospitalDb = hospitalRepository.saveAndFlush(hospital);
        Long hospitalId = hospitalDb.getId();

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            List<MultipartFile> images = dto.getImages();
            for (int i = 0; i < images.size(); i++) {
                MultipartFile file = images.get(i);
                if (file.isEmpty()) continue;
                String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
                String fileName = hospitalDb.getId() + "_" + (i + 1) + "." + ext;
                String imageUrl = awsS3Service.upload(file, fileName);
                hospitalDb.getImages().add(HospitalImage.builder()
                        .imageUrl(imageUrl)
                        .hospital(hospitalDb)
                        .build());
            }
        }

        return hospitalId;
    }

    @Transactional
    public Long update(Long hospitalId, HospitalUpdateDto dto, Long accountId) {
        log.info("[병원 수정] 받은 latitude={}, longitude={}", dto.getLatitude(), dto.getLongitude());

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("병원을 찾을 수 없습니다."));

        if (!hospital.getAccount().getId().equals(accountId)) {
            throw new IllegalArgumentException("본인의 병원 정보만 수정할 수 있습니다.");
        }

        if (hospital.getStatus() == HospitalStatus.REJECTED) {
            hospital.changeStatus(HospitalStatus.PENDING);
        }

        hospital.updateBasicInfo(dto.getName(), dto.getPhone());
        hospital.updateAddress(dto);

        hospital.getHospitalDepartments().clear();
        if (dto.getDepartmentIds() != null) {
            List<Department> departments = departmentRepository.findAllById(dto.getDepartmentIds());
            for (Department dept : departments) {
                hospital.getHospitalDepartments().add(HospitalDepartment.builder()
                        .hospital(hospital).department(dept).build());
            }
        }

        hospital.getHospitalFilters().clear();
        if (dto.getFilterIds() != null) {
            List<Filter> filters = filterRepository.findAllById(dto.getFilterIds());
            for (Filter filter : filters) {
                hospital.getHospitalFilters().add(HospitalFilter.builder()
                        .hospital(hospital).filter(filter).build());
            }
        }

        hospital.getOperatingHours().clear();
        if (dto.getOperatingHours() != null) {
            for (OperatingTimeDto timeDto : dto.getOperatingHours()) {
                hospital.getOperatingHours().add(timeDto.toEntity(hospital));
            }
        }

        hospital.getHolidays().removeIf(h -> !publicHolidayService.isPublicHoliday(h.getHolidayDate()));
        if (dto.getHolidays() != null) {
            Set<LocalDate> existingDates = hospital.getHolidays().stream()
                    .map(HospitalHoliday::getHolidayDate)
                    .collect(Collectors.toSet());
            for (HolidayDto holidayDto : dto.getHolidays()) {
                if (!existingDates.contains(holidayDto.getHolidayDate())) {
                    hospital.getHolidays().add(holidayDto.toEntity(hospital));
                }
            }
        }

        if (dto.getKeepImageUrls() != null) {
            List<HospitalImage> toDelete = hospital.getImages().stream()
                    .filter(img -> !dto.getKeepImageUrls().contains(img.getImageUrl()))
                    .collect(Collectors.toList());
            for (HospitalImage img : toDelete) {
                awsS3Service.deleteFile(img.getImageUrl());
            }
            hospital.getImages().removeAll(toDelete);

            if (dto.getImages() != null && !dto.getImages().isEmpty()) {
                int startIndex = hospital.getImages().size() + 1;
                List<MultipartFile> newImages = dto.getImages();
                for (int i = 0; i < newImages.size(); i++) {
                    MultipartFile file = newImages.get(i);
                    if (file.isEmpty()) continue;
                    String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
                    String fileName = hospital.getId() + "_" + (startIndex + i) + "." + ext;
                    String imageUrl = awsS3Service.upload(file, fileName);
                    hospital.getImages().add(HospitalImage.builder()
                            .imageUrl(imageUrl)
                            .hospital(hospital)
                            .build());
                }
            }
        }

        if (dto.getStatus() != null) {
            hospital.changeStatus(dto.getStatus());
        }

        return hospital.getId();
    }

    // 내 병원 조회
    @Transactional(readOnly = true)
    public HospitalDetailDto getMyHospital(Long accountId) {
        System.out.println(accountId);
        Hospital hospital = hospitalRepository.findByAccount_IdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("등록된 병원 정보가 없습니다."));
        boolean accepting = checkIsAcceptingCheckin(hospital);
        HospitalDetailDto dto = HospitalDetailDto.fromEntity(hospital, accepting);
        log.info("[내 병원 조회]DTO 내용: {}", dto);
        return dto;
    }

    // [추가] 내 병원에 등록된 진료과 목록만 반환
    // HospitalDetailDto를 건드리지 않고 별도 API로 분리
    // → 의사 등록/수정 폼에서 병원 등록 과목만 드롭다운에 표시하기 위함
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyHospitalDepartments(Long accountId) {
        Hospital hospital = hospitalRepository.findByAccount_IdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("등록된 병원 정보가 없습니다."));

        return hospital.getHospitalDepartments().stream()
                .map(hd -> {
                    Map<String, Object> dept = new LinkedHashMap<>();
                    dept.put("id", hd.getDepartment().getId());
                    dept.put("name", hd.getDepartment().getName());
                    return dept;
                })
                .collect(Collectors.toList());
    }

    // 특정 병원 상세 조회
    @Transactional(readOnly = true)
    public HospitalPublicDetailDto getHospitalDetail(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("병원을 찾을 수 없습니다."));

        if (hospital.getStatus() == HospitalStatus.DELETED) {
            return HospitalPublicDetailDto.fromTerminatedEntity(hospital);
        }

        boolean isOpenNow = checkIsOpenNow(hospital);
        java.util.List<PublicHoliday> publicHolidays = publicHolidayService.getHolidaysForYear(
                LocalDate.now().getYear());
        return HospitalPublicDetailDto.fromEntity(hospital, isOpenNow, publicHolidays);
    }

    // 접수 가능 여부 계산
    private boolean checkIsAcceptingCheckin(Hospital hospital) {
        if (hospital.isCheckinClosedToday()) return false;

        LocalDate today = LocalDate.now();
        boolean isTodayHoliday = hospital.getHolidays().stream()
                .anyMatch(h -> h.getHolidayDate().equals(today));
        if (isTodayHoliday) return false;

        java.time.LocalTime now = java.time.LocalTime.now();
        java.time.DayOfWeek dayOfWeek = today.getDayOfWeek();

        return hospital.getOperatingHours().stream()
                .filter(h -> h.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .map(h -> !h.isDayOff()
                        && h.getOpenTime() != null
                        && h.getCloseTime() != null
                        && !now.isBefore(h.getOpenTime())
                        && now.isBefore(h.getCloseTime().minusMinutes(30))
                )
                .orElse(false);
    }

    // 현재 영업 여부 계산
    private boolean checkIsOpenNow(Hospital hospital) {
        LocalDate today = LocalDate.now();
        boolean isTodayHoliday = hospital.getHolidays().stream()
                .anyMatch(h -> h.getHolidayDate().equals(today));
        if (isTodayHoliday) return false;

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.DayOfWeek dayOfWeek = now.getDayOfWeek();
        java.time.LocalTime time = now.toLocalTime();

        return hospital.getOperatingHours().stream()
                .filter(h -> h.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .map(h -> !h.isDayOff()
                        && h.getOpenTime() != null
                        && h.getCloseTime() != null
                        && !time.isBefore(h.getOpenTime())
                        && !time.isAfter(h.getCloseTime())
                        && (h.getBreakStartTime() == null
                        || time.isBefore(h.getBreakStartTime())
                        || time.isAfter(h.getBreakEndTime()))
                )
                .orElse(false);
    }

    public void delete(Long hospitalId, Long accountId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("병원을 찾을 수 없습니다."));
        if (!hospital.getAccount().getId().equals(accountId)) {
            throw new IllegalArgumentException("본인의 병원만 삭제할 수 있습니다.");
        }
        hospital.changeStatus(HospitalStatus.DELETED);
    }

    // 병원 목록 조회
    public Page<HospitalListDto> getHospitalList(HospitalSearchDto dto, Pageable pageable) {
        LocalDate today = LocalDate.now();
        dto.setCurrentTime(LocalTime.now());
        dto.setCurrentDayOfWeekString(today.getDayOfWeek().name());
        Page<Hospital> hospitalPage = hospitalRepository.searchHospitalsNative(dto, pageable);
        return hospitalPage.map(hospital ->
                HospitalListDto.fromEntity(hospital, dto.getUserLat(), dto.getUserLng(), today, checkIsOpenNow(hospital))
        );
    }

    // map api
    public List<HospitalMapResponseDto> mapApi() {
        List<Hospital> hospitalList = hospitalRepository.findAllForMap(HospitalStatus.DELETED);
        List<HospitalMapResponseDto> hospitalMapResponseDtoList = new ArrayList<>();
        for (Hospital h : hospitalList) {
            boolean isOpenNow = checkIsOpenNow(h);
            HospitalMapResponseDto dto = HospitalMapResponseDto.fromEntity(h, isOpenNow);
            hospitalMapResponseDtoList.add(dto);
        }
        return hospitalMapResponseDtoList;
    }

    // 거리계산 함수 (Haversine 공식)
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // 거리계산 api
    public List<HospitalNearbyResponseDto> nearby(double userLat, double userLng, double radius) {
        List<Hospital> hospitalList = hospitalRepository.findAllForMap(HospitalStatus.DELETED);
        List<HospitalNearbyResponseDto> nearbyResponseDtoList = new ArrayList<>();
        for (Hospital h : hospitalList) {
            double hospitalLat = h.getAddress().getLatitude();
            double hospitalLng = h.getAddress().getLongitude();
            double distance = calculateDistance(userLat, userLng, hospitalLat, hospitalLng);
            if (distance <= radius) {
                HospitalNearbyResponseDto dto = HospitalNearbyResponseDto.builder()
                        .id(h.getId())
                        .name(h.getName())
                        .phone(h.getPhone())
                        .latitude(hospitalLat)
                        .longitude(hospitalLng)
                        .distance(Math.round(distance * 100.0) / 100.0)
                        .build();
                nearbyResponseDtoList.add(dto);
            }
        }
        nearbyResponseDtoList.sort(Comparator.comparing(HospitalNearbyResponseDto::getDistance));
        return nearbyResponseDtoList;
    }

    public void updateStatus(Long hospitalId, HospitalStatusUpdateDto dto) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("병원을 찾을 수 없습니다."));
        hospital.changeStatus(dto.getStatus());
    }

    public void updateApprovalMode(Long hospitalId, Long accountId, ApprovalModeUpdateReqDto dto) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("병원을 찾을 수 없습니다."));
        if (!hospital.getAccount().getId().equals(accountId)) {
            throw new IllegalArgumentException("본인의 병원만 수정할 수 있습니다.");
        }
        ReservationApprovalMode mode = ReservationApprovalMode.valueOf(dto.getApprovalMode());
        hospital.changeApprovalMode(mode);
    }

    @Transactional
    public boolean toggleCheckinClose(Long hospitalId, Long accountId, CheckinCloseReqDto dto) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("병원을 찾을 수 없습니다."));
        if (!hospital.getAccount().getId().equals(accountId)) {
            throw new IllegalArgumentException("본인의 병원만 수정할 수 있습니다.");
        }
        if (dto.isClosed()) {
            hospital.closeCheckinForToday();
        } else {
            hospital.reopenCheckin();
        }
        return hospital.isCheckinClosedToday();
    }

    public Long addHashtag(Long accountId, HospitalHashtagCreateDto dto) {
        Hospital hospital = hospitalRepository.findByAccount_IdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("등록된 병원 정보가 없습니다."));
        if (dto.getTag() == null || dto.getTag().isBlank()) {
            throw new IllegalArgumentException("태그 내용을 입력해주세요.");
        }
        if (dto.getTag().length() > 10) {
            throw new IllegalArgumentException("10자 이하로 입력해주세요.");
        }
        boolean duplicate = hospital.getHashtags().stream()
                .anyMatch(h -> h.getTag().equals(dto.getTag()));
        if (duplicate) {
            throw new IllegalArgumentException("이미 등록된 해시태그입니다.");
        }
        long count = hospitalHashtagRepository.countByHospital_Id(hospital.getId());
        if (count >= 5) {
            throw new IllegalStateException("해시태그는 5개 이상 생성이 불가능합니다.");
        }
        HospitalHashtag hashtag = HospitalHashtag.builder()
                .hospital(hospital)
                .tag(dto.getTag())
                .build();
        return hospitalHashtagRepository.save(hashtag).getId();
    }

    public void deleteHashtag(Long hashtagId, Long accountId) {
        HospitalHashtag hashtag = hospitalHashtagRepository.findById(hashtagId)
                .orElseThrow(() -> new EntityNotFoundException("해시태그를 찾을 수 없습니다."));
        if (!hashtag.getHospital().getAccount().getId().equals(accountId)) {
            throw new IllegalArgumentException("본인의 병원 해시태그만 삭제할 수 있습니다.");
        }
        hospitalHashtagRepository.delete(hashtag);
    }

    // 통계: 이번달 의사별 예약 순위
    @Transactional(readOnly = true)
    public List<HospitalDoctorRankingDto> getDoctorRanking(Long accountId) {
        Hospital hospital = hospitalRepository.findByAccount_IdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("등록된 병원 정보가 없습니다."));
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        List<Object[]> results = reservationRepository.findMonthDoctorRanking(hospital.getId(), startOfMonth, endOfMonth);
        List<HospitalDoctorRankingDto> ranking = new ArrayList<>();
        int limit = Math.min(3, results.size());
        for (int i = 0; i < limit; i++) {
            Object[] row = results.get(i);
            ranking.add(HospitalDoctorRankingDto.builder()
                    .rank(i + 1)
                    .doctorName((String) row[1])
                    .reservationCount((Long) row[2])
                    .build());
        }
        return ranking;
    }

    // 통계 요약 카드 4개
    @Transactional(readOnly = true)
    public HospitalStatsSummaryDto getStatsSummary(Long accountId) {
        Hospital hospital = hospitalRepository.findByAccount_IdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("등록된 병원 정보가 없습니다."));
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        LocalDateTime startOfMonthDateTime = startOfMonth.atStartOfDay();

        long todayReservation = reservationRepository.countTodayReservations(hospital.getId(), today);
        long monthReservation = reservationRepository.countMonthReservations(hospital.getId(), startOfMonth, endOfMonth);
        long todayCheckin = checkinRepository.countTodayCheckins(hospital.getId(), startOfDay, endOfDay);
        long monthCheckin = checkinRepository.countMonthCheckins(hospital.getId(), startOfMonthDateTime, endOfDay);

        return HospitalStatsSummaryDto.builder()
                .todayReservation(todayReservation)
                .monthReservation(monthReservation)
                .todayCheckin(todayCheckin)
                .monthCheckin(monthCheckin)
                .build();
    }

    // 통계: 일별 추이
    @Transactional(readOnly = true)
    public List<HospitalDailyStatsDto> getDailyTrend(Long accountId, LocalDate startDate, LocalDate endDate, String type) {
        Hospital hospital = hospitalRepository.findByAccount_IdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("등록된 병원 정보가 없습니다."));
        Long hospitalId = hospital.getId();

        Map<LocalDate, Long> countMap = new LinkedHashMap<>();
        if ("reservation".equals(type)) {
            List<Object[]> results = reservationRepository.countDailyReservations(hospitalId, startDate, endDate);
            for (Object[] row : results) {
                countMap.put((LocalDate) row[0], (Long) row[1]);
            }
        } else if ("checkin".equals(type)) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
            List<Object[]> results = checkinRepository.countDailyCheckins(hospitalId, startDateTime, endDateTime);
            for (Object[] row : results) {
                countMap.put(((java.sql.Date) row[0]).toLocalDate(), (Long) row[1]);
            }
        } else {
            throw new IllegalArgumentException("type은 'reservation' 또는 'checkin'만 가능합니다.");
        }

        List<HospitalDailyStatsDto> result = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            result.add(HospitalDailyStatsDto.builder()
                    .date(current)
                    .count(countMap.getOrDefault(current, 0L))
                    .build());
            current = current.plusDays(1);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<HospitalDetailDto> findPendingHospitals() {
        return hospitalRepository.findByStatus(HospitalStatus.PENDING).stream()
                .map(HospitalDetailDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 선택한 공휴일을 병원 휴무일로 일괄 등록
    @Transactional
    public void applyPublicHolidays(Long accountId, List<LocalDate> dates) {
        Hospital hospital = hospitalRepository.findByAccount_IdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("등록된 병원 정보가 없습니다."));
        Set<LocalDate> existingDates = hospital.getHolidays().stream()
                .map(HospitalHoliday::getHolidayDate)
                .collect(Collectors.toSet());

        Map<Integer, List<PublicHoliday>> holidaysByYear = new HashMap<>();
        for (LocalDate date : dates) {
            if (existingDates.contains(date)) continue;
            holidaysByYear.computeIfAbsent(date.getYear(), y -> publicHolidayService.getHolidaysForYear(y));
        }

        for (LocalDate date : dates) {
            if (existingDates.contains(date)) continue;
            String reason = holidaysByYear.getOrDefault(date.getYear(), Collections.emptyList()).stream()
                    .filter(ph -> ph.getHolidayDate().equals(date))
                    .map(PublicHoliday::getDateName)
                    .findFirst()
                    .orElse("공휴일");
            hospital.getHolidays().add(HospitalHoliday.builder()
                    .hospital(hospital)
                    .holidayDate(date)
                    .reason(reason)
                    .build());
        }
    }

    // 병원 휴무일에서 선택한 공휴일 제거
    @Transactional
    public void removePublicHolidays(Long accountId, List<LocalDate> dates) {
        Hospital hospital = hospitalRepository.findByAccount_IdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("등록된 병원 정보가 없습니다."));
        Set<LocalDate> datesToRemove = new HashSet<>(dates);
        hospital.getHolidays().removeIf(h -> datesToRemove.contains(h.getHolidayDate()));
    }
}