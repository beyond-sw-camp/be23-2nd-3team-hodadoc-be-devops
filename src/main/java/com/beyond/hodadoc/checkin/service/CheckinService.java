package com.beyond.hodadoc.checkin.service;

import com.beyond.hodadoc.checkin.domain.Checkin;
import com.beyond.hodadoc.checkin.domain.CheckinStatus;
import com.beyond.hodadoc.checkin.dtos.*;
import com.beyond.hodadoc.checkin.repository.CheckinRepository;
import com.beyond.hodadoc.common.domain.AlarmType;
import com.beyond.hodadoc.common.service.PublicHolidayService;
import com.beyond.hodadoc.common.service.SseAlarmService;
import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.hospital.domain.HospitalStatus;
import com.beyond.hodadoc.hospital.repository.HospitalRepository;
import com.beyond.hodadoc.patient.domain.Patient;
import com.beyond.hodadoc.patient.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CheckinService {

    private final CheckinRepository checkinRepository;
    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;
    private final SseAlarmService sseAlarmService;
    private final RedisTemplate<String, String> checkinRedisTemplate;
    private final PublicHolidayService publicHolidayService;

    public CheckinService(CheckinRepository checkinRepository,
                          PatientRepository patientRepository,
                          HospitalRepository hospitalRepository,
                          SseAlarmService sseAlarmService,
                          @Qualifier("checkin") RedisTemplate<String, String> checkinRedisTemplate,
                          PublicHolidayService publicHolidayService) {
        this.checkinRepository = checkinRepository;
        this.patientRepository = patientRepository;
        this.hospitalRepository = hospitalRepository;
        this.sseAlarmService = sseAlarmService;
        this.checkinRedisTemplate = checkinRedisTemplate;
        this.publicHolidayService = publicHolidayService;
    }

    /**
     * 환자가 병원에 온라인 접수를 요청
     * 당일 해당 병원의 대기 번호를 자동으로 발급
     */
    @Transactional
    public CheckinPatientListDto createCheckin(Long patientAccountId, CheckinCreateReqDto dto) {
        Patient patient = patientRepository.findByAccountIdAndAccount_DelYn(patientAccountId, "N")
                .orElseThrow(() -> new IllegalArgumentException("환자 정보를 찾을 수 없습니다."));

        Hospital hospital = hospitalRepository.findById(dto.getHospitalId())
                .orElseThrow(() -> new IllegalArgumentException("병원 정보를 찾을 수 없습니다."));

        if (hospital.getStatus() == HospitalStatus.DELETED) {
            throw new IllegalStateException("서비스 종료된 병원에는 접수할 수 없습니다.");
        }

        // 접수 마감 확인
        if (hospital.isCheckinClosedToday()) {
            throw new IllegalStateException("현재 접수가 마감되었습니다. 병원에 문의해 주세요.");
        }

        // 당일 병원 지정 휴무일 확인
        LocalDate today = LocalDate.now();
        boolean isTodayHoliday = hospital.getHolidays().stream()
                .anyMatch(h -> h.getHolidayDate().equals(today));
        if (isTodayHoliday) {
            throw new IllegalStateException("오늘은 병원 휴무일입니다. 접수할 수 없습니다.");
        }

        // 공휴일 확인
        if (publicHolidayService.isPublicHoliday(today)) {
            throw new IllegalStateException("오늘은 공휴일입니다. 접수할 수 없습니다.");
        }

        // 마감 30분 전 자동 접수 차단
        LocalTime now = LocalTime.now();
        DayOfWeek todayDow = today.getDayOfWeek();
        hospital.getOperatingHours().stream()
                .filter(h -> h.getDayOfWeek() == todayDow && !h.isDayOff() && h.getCloseTime() != null)
                .findFirst()
                .ifPresent(h -> {
                    LocalTime cutoff = h.getCloseTime().minusMinutes(30);
                    if (!now.isBefore(cutoff)) {
                        throw new IllegalStateException("마감 30분 전이므로 접수가 불가합니다. 내일 다시 이용해 주세요.");
                    }
                });

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        // ✅ 추가: 당일 동일 병원 중복 접수 방지 (WAITING 상태인 접수가 이미 있으면 차단)
        boolean alreadyCheckedIn = checkinRepository.existsTodayActiveCheckin(
                hospital.getId(), patient.getId(), startOfToday, CheckinStatus.WAITING
        );
        if (alreadyCheckedIn) {
            throw new IllegalStateException("이미 해당 병원에 접수 중입니다. 대기 중인 접수가 완료되거나 취소된 후 다시 접수할 수 있습니다.");
        }

        // Redis INCR로 원자적 대기번호 발급 (동시성 안전)
        String redisKey = "checkin:waitingNumber:" + hospital.getId() + ":" + today;

        // Redis 키가 없으면 DB 최대값으로 초기화 (서버 재시작/Redis 초기화 대응)
        if (Boolean.FALSE.equals(checkinRedisTemplate.hasKey(redisKey))) {
            Integer dbMax = checkinRepository.findMaxWaitingNumberToday(hospital.getId(), startOfToday);
            int initValue = (dbMax != null && dbMax > 0) ? dbMax : 0;
            checkinRedisTemplate.opsForValue().setIfAbsent(redisKey, String.valueOf(initValue));
            checkinRedisTemplate.expire(redisKey, Duration.ofHours(24));
        }

        Long nextNumberLong = checkinRedisTemplate.opsForValue().increment(redisKey);
        int nextNumber = nextNumberLong != null ? nextNumberLong.intValue() : 1;

        Checkin checkin = Checkin.builder()
                .patient(patient)
                .hospital(hospital)
                .checkinTime(LocalDateTime.now())
                .waitingNumber(nextNumber)
                .build();

        Checkin saved = checkinRepository.save(checkin);

        // 병원 관리자에게 SSE 알림: 새 접수 등록
        Long hospitalAccountId = hospital.getAccount().getId();
        sseAlarmService.sendMessage(hospitalAccountId,
                "새 접수가 등록되었습니다. " + patient.getName(),
                AlarmType.RECEPTION_CREATED.name(), null);

        CheckinPatientListDto result = CheckinPatientListDto.fromEntity(saved);
        // 새 접수의 현재 대기 순번 계산 (앞에 WAITING 상태인 환자 수 + 1)
        result.setQueuePosition(calculateQueuePosition(saved));
        return result;
    }

    /**
     * 환자가 본인의 접수 목록 조회
     */
    public Page<CheckinPatientListDto> findMyCheckins(Long patientAccountId, Pageable pageable) {
        Patient patient = patientRepository.findByAccountIdAndAccount_DelYn(patientAccountId, "N")
                .orElseThrow(() -> new IllegalArgumentException("환자 정보를 찾을 수 없습니다."));
        return checkinRepository.findByPatientId(patient.getId(), pageable)
                .map(checkin -> {
                    CheckinPatientListDto dto = CheckinPatientListDto.fromEntity(checkin);
                    if (checkin.getStatus() == CheckinStatus.WAITING) {
                        dto.setQueuePosition(calculateQueuePosition(checkin));
                    }
                    return dto;
                });
    }

    /**
     * 환자가 본인 접수 취소
     */
    @Transactional
    public void cancelCheckin(Long patientAccountId, Long checkinId) {
        Patient patient = patientRepository.findByAccountIdAndAccount_DelYn(patientAccountId, "N")
                .orElseThrow(() -> new IllegalArgumentException("환자 정보를 찾을 수 없습니다."));

        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new IllegalArgumentException("접수 정보를 찾을 수 없습니다."));

        if (!checkin.getPatient().getId().equals(patient.getId())) {
            throw new IllegalArgumentException("본인의 접수만 취소할 수 있습니다.");
        }
        if (checkin.getStatus() != CheckinStatus.WAITING) {
            throw new IllegalStateException("대기 중인 접수만 취소할 수 있습니다.");
        }

        checkin.updateStatus(CheckinStatus.CANCELLED);
        sendQueueUpdateNotifications(checkin.getHospital().getId());
    }

    /**
     * 병원 관리자: 당일 자기 병원의 접수 목록 조회
     */
    public Page<CheckinHospitalListDto> findTodayCheckins(Long hospitalAccountId, Pageable pageable) {
        Hospital hospital = hospitalRepository.findByAccount_IdAndAccount_DelYn(hospitalAccountId, "N")
                .orElseThrow(() -> new IllegalArgumentException("병원 정보를 찾을 수 없습니다."));
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        // WAITING 상태 접수 목록으로 대기 순번 맵 생성 (한 번의 쿼리로 전체 계산)
        List<Checkin> waitingCheckins = checkinRepository.findTodayWaitingByHospitalId(
                hospital.getId(), startOfToday, CheckinStatus.WAITING);
        Map<Long, Integer> positionMap = new HashMap<>();
        int pos = 1;
        for (Checkin wc : waitingCheckins) {
            positionMap.put(wc.getId(), pos++);
        }

        return checkinRepository.findTodayByHospitalId(hospital.getId(), startOfToday, pageable)
                .map(checkin -> {
                    CheckinHospitalListDto dto = CheckinHospitalListDto.fromEntity(checkin);
                    if (checkin.getStatus() == CheckinStatus.WAITING) {
                        dto.setQueuePosition(positionMap.get(checkin.getId()));
                    }
                    return dto;
                });
    }

    /**
     * 병원 관리자: 접수 상태 변경 (호출/완료 등)
     */
    @Transactional
    public CheckinHospitalListDto updateCheckinStatus(Long hospitalAccountId, Long checkinId, CheckinStatusUpdateReqDto dto) {
        Hospital hospital = hospitalRepository.findByAccount_IdAndAccount_DelYn(hospitalAccountId, "N")
                .orElseThrow(() -> new IllegalArgumentException("병원 정보를 찾을 수 없습니다."));

        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new IllegalArgumentException("접수 정보를 찾을 수 없습니다."));

        if (!checkin.getHospital().getId().equals(hospital.getId())) {
            throw new IllegalArgumentException("자신의 병원 접수만 수정할 수 있습니다.");
        }

        checkin.updateStatus(dto.getStatus());

        if (dto.getStatus() == CheckinStatus.CALLED) {
            Long patientAccountId = checkin.getPatient().getAccount().getId();
            sseAlarmService.sendMessage(patientAccountId,
                    "진료 차례입니다. " + hospital.getName(),
                    AlarmType.RECEPTION_CALLED.name(), null);
        }

        if (dto.getStatus() == CheckinStatus.CALLED ||
                dto.getStatus() == CheckinStatus.COMPLETED ||
                dto.getStatus() == CheckinStatus.CANCELLED) {
            sendQueueUpdateNotifications(hospital.getId());
        }

        CheckinHospitalListDto result = CheckinHospitalListDto.fromEntity(checkin);
        if (checkin.getStatus() == CheckinStatus.WAITING) {
            result.setQueuePosition(calculateQueuePosition(checkin));
        }
        return result;
    }

    /**
     * 현재 대기 순번 계산 (해당 병원 당일 WAITING 상태 중 내 앞 환자 수 + 1)
     */
    private int calculateQueuePosition(Checkin checkin) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return checkinRepository.countWaitingBefore(
                checkin.getHospital().getId(),
                startOfToday,
                CheckinStatus.WAITING,
                checkin.getWaitingNumber()
        ) + 1;
    }

    /**
     * 대기 순번 업데이트 알림 (해당 병원의 WAITING 상태 환자 전체에게 SSE 발송)
     */
    private void sendQueueUpdateNotifications(Long hospitalId) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        List<Checkin> waitingCheckins = checkinRepository.findTodayWaitingByHospitalId(
                hospitalId, startOfToday, CheckinStatus.WAITING);

        int position = 1;
        for (Checkin c : waitingCheckins) {
            Long patientAccountId = c.getPatient().getAccount().getId();
            sseAlarmService.sendMessage(patientAccountId,
                    "현재 대기 순번: " + position + "번",
                    AlarmType.RECEPTION_QUEUE_UPDATE.name(), null);
            position++;
        }
    }
}