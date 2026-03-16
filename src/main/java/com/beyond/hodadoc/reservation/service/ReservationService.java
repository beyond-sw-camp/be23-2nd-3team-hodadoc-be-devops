package com.beyond.hodadoc.reservation.service;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.account.domain.Role;
import com.beyond.hodadoc.account.repository.AccountRepository;
import com.beyond.hodadoc.common.domain.AlarmType;
import com.beyond.hodadoc.common.service.SseAlarmService;
import com.beyond.hodadoc.sms.service.SmsService;
import com.beyond.hodadoc.doctor.domain.Doctor;
import com.beyond.hodadoc.doctor.domain.DoctorSchedule;
import com.beyond.hodadoc.doctor.repository.DoctorOffDayRepository;
import com.beyond.hodadoc.doctor.repository.DoctorRepository;
import com.beyond.hodadoc.doctor.repository.DoctorScheduleRepository;
import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.hospital.domain.HospitalStatus;
import com.beyond.hodadoc.hospital.domain.ReservationApprovalMode;
import com.beyond.hodadoc.hospital.repository.HospitalRepository;
import com.beyond.hodadoc.patient.domain.Patient;
import com.beyond.hodadoc.patient.repository.PatientRepository;
import com.beyond.hodadoc.reservation.domain.ReservationPatient;
import com.beyond.hodadoc.reservation.domain.ReservationStatus;
import com.beyond.hodadoc.reservation.dtos.*;
import com.beyond.hodadoc.reservation.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final AccountRepository accountRepository;
    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorOffDayRepository doctorOffDayRepository;
    private final SseAlarmService sseAlarmService;
    private final SmsService smsService;
    private final ReservationLockService reservationLockService;
    private final ReservationCreateService reservationCreateService;

    @Autowired
    public ReservationService(ReservationRepository reservationRepository,
                              AccountRepository accountRepository,
                              PatientRepository patientRepository,
                              HospitalRepository hospitalRepository,
                              DoctorRepository doctorRepository,
                              DoctorScheduleRepository doctorScheduleRepository,
                              DoctorOffDayRepository doctorOffDayRepository,
                              SseAlarmService sseAlarmService,
                              SmsService smsService,
                              ReservationLockService reservationLockService,
                              ReservationCreateService reservationCreateService) {
        this.reservationRepository = reservationRepository;
        this.accountRepository = accountRepository;
        this.patientRepository = patientRepository;
        this.hospitalRepository = hospitalRepository;
        this.doctorRepository = doctorRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.doctorOffDayRepository = doctorOffDayRepository;
        this.sseAlarmService = sseAlarmService;
        this.smsService = smsService;
        this.reservationLockService = reservationLockService;
        this.reservationCreateService = reservationCreateService;
    }

    // ==================== 환자 예약 생성 (분산락 적용) ====================
    /**
     * [핵심 수정] @Transactional(propagation = NOT_SUPPORTED)
     *
     * 기존 문제:
     *   클래스 레벨 @Transactional → createByPatient 호출 시 트랜잭션 시작
     *   → tryLock → doCreate(DB 작업) → unlock() → [트랜잭션 커밋]
     *   → unlock 이후 커밋 전 사이에 다른 요청이 락 획득 가능 → 중복 예약 허용
     *
     * 수정 후:
     *   createByPatient는 트랜잭션 없이 실행 (NOT_SUPPORTED)
     *   → tryLock → doCreate(REQUIRES_NEW로 자체 트랜잭션 시작+커밋 완료) → unlock
     *   → DB 커밋이 unlock 전에 반드시 완료됨 → 동시성 완전 보장
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ReservationPatientListDto createByPatient(Long accountId, ReservationCreateReqDto dto) {
        log.info("[예약생성] 시작 - accountId={}, doctorId={}, date={}, time={}",
                accountId, dto.getDoctorId(), dto.getReservationDate(), dto.getReservationTime());

        // 1. 락 획득 시도
        boolean lockAcquired = reservationLockService.tryLock(
                dto.getDoctorId(), dto.getReservationDate(), dto.getReservationTime());

        if (!lockAcquired) {
            throw new IllegalStateException("현재 해당 시간대에 예약 요청이 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            // 2. 락 획득 성공 → 별도 클래스에서 독립 트랜잭션(REQUIRES_NEW)으로 실행
            return reservationCreateService.doCreateByPatient(accountId, dto);
        } finally {
            // 3. doCreateByPatient의 트랜잭션이 커밋된 후 락 해제
            reservationLockService.unlock(
                    dto.getDoctorId(), dto.getReservationDate(), dto.getReservationTime());
        }
    }

    // ==================== 병원 관리자: 슬롯 수동 블록 ====================
    public void blockSlot(Long accountId, BlockSlotReqDto dto) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("계정을 찾을 수 없습니다."));
        if (account.getRole() != Role.HOSPITAL_ADMIN)
            throw new IllegalStateException("병원 관리자만 슬롯을 블록할 수 있습니다.");

        Doctor doctor = doctorRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new EntityNotFoundException("의사 정보를 찾을 수 없습니다."));

        boolean alreadyBlocked = reservationRepository
                .existsByDoctorIdAndReservationDateAndReservationTimeAndStatus(
                        dto.getDoctorId(), dto.getDate(), dto.getTime(), ReservationStatus.BLOCKED);
        if (alreadyBlocked) return;

        reservationRepository.deleteInactiveByDoctorDateAndTime(
                dto.getDoctorId(), dto.getDate(), dto.getTime());
        reservationRepository.flush();

        ReservationPatient block = ReservationPatient.builder()
                .patient(null).doctor(doctor).hospital(doctor.getHospital())
                .reservationDate(dto.getDate()).reservationTime(dto.getTime())
                .symptoms(null).status(ReservationStatus.BLOCKED).build();

        try {
            reservationRepository.save(block);
            reservationRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("해당 시간은 이미 예약 또는 블록되어 있습니다.");
        }
    }

    public void unblockSlot(Long accountId, Long reservationId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("계정을 찾을 수 없습니다."));
        if (account.getRole() != Role.HOSPITAL_ADMIN)
            throw new IllegalStateException("병원 관리자만 블록을 해제할 수 있습니다.");

        ReservationPatient rp = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다."));
        if (rp.getStatus() != ReservationStatus.BLOCKED)
            throw new IllegalStateException("블록 상태의 슬롯만 해제할 수 있습니다.");

        rp.updateStatus(ReservationStatus.CANCELLED);
    }

    // ==================== 예약 상태 변경 ====================
    public ReservationPatientListDto updateStatus(Long accountId, Long reservationPatientId,
                                                  ReservationStatus targetStatus) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("계정을 찾을 수 없습니다."));
        if ("Y".equals(account.getDelYn())) throw new EntityNotFoundException("삭제된 계정입니다.");

        ReservationPatient rp = reservationRepository.findWithDetailById(reservationPatientId)
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다."));

        if (rp.getStatus() == ReservationStatus.BLOCKED)
            throw new IllegalStateException("블록된 슬롯은 상태를 변경할 수 없습니다.");
        if (rp.getStatus() == ReservationStatus.CANCELLED)
            throw new IllegalStateException("이미 취소된 예약입니다.");
        if (rp.getStatus() == ReservationStatus.COMPLETED)
            throw new IllegalStateException("이미 완료된 예약입니다.");

        String dateTimeInfo = rp.getReservationDate() + " " + rp.getReservationTime();

        if (account.getRole() == Role.PATIENT) {
            if (targetStatus != ReservationStatus.CANCELLED)
                throw new IllegalStateException("환자는 예약 취소(CANCELLED)만 할 수 있습니다.");

            Patient patient = patientRepository.findByAccountIdAndAccount_DelYn(accountId, "N")
                    .orElseThrow(() -> new EntityNotFoundException("환자 정보를 찾을 수 없습니다."));
            if (!rp.getPatient().getId().equals(patient.getId()))
                throw new IllegalStateException("본인 예약만 변경할 수 있습니다.");

            rp.updateStatus(ReservationStatus.CANCELLED);
            sseAlarmService.sendMessage(rp.getHospital().getAccount().getId(),
                    "환자가 예약을 취소했습니다. " + patient.getName() + " / " + dateTimeInfo,
                    AlarmType.RESERVATION_CANCELLED.name(), rp.getId());

        } else if (account.getRole() == Role.HOSPITAL_ADMIN) {
            if (targetStatus != ReservationStatus.APPROVED
                    && targetStatus != ReservationStatus.CANCELLED
                    && targetStatus != ReservationStatus.COMPLETED)
                throw new IllegalStateException("병원은 APPROVED/CANCELLED/COMPLETED만 변경 가능합니다.");

            rp.updateStatus(targetStatus);

            if (targetStatus == ReservationStatus.APPROVED) {
                sseAlarmService.sendMessage(rp.getPatient().getAccount().getId(),
                        "예약이 승인되었습니다. " + rp.getHospital().getName() + " / " + dateTimeInfo,
                        AlarmType.RESERVATION_APPROVED.name(), rp.getId());
            } else if (targetStatus == ReservationStatus.CANCELLED) {
                sseAlarmService.sendMessage(rp.getPatient().getAccount().getId(),
                        "병원에서 예약을 취소했습니다. " + rp.getHospital().getName() + " / " + dateTimeInfo,
                        AlarmType.RESERVATION_CANCELLED.name(), rp.getId());
            }
        } else {
            throw new IllegalStateException("권한이 없습니다.");
        }

        return ReservationPatientListDto.fromEntity(rp);
    }

    public void cancelByPatient(Long accountId, Long reservationPatientId) {
        updateStatus(accountId, reservationPatientId, ReservationStatus.CANCELLED);
    }

    // ==================== 예약 가능 슬롯 조회 ====================
    @Transactional(readOnly = true)
    public List<AvailableSlotDto> getAvailableSlots(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("의사 정보를 찾을 수 없습니다."));

        boolean isHospitalHoliday = doctor.getHospital().getHolidays().stream()
                .anyMatch(h -> h.getHolidayDate().equals(date));
        if (isHospitalHoliday) return List.of();

        DoctorSchedule schedule = doctorScheduleRepository
                .findByDoctorIdAndDayOfWeek(doctorId, date.getDayOfWeek()).orElse(null);
        if (schedule == null || schedule.isDayOff()) return List.of();

        if (doctorOffDayRepository.existsByDoctorIdAndOffDate(doctorId, date)) return List.of();

        List<LocalTime> candidates = generateTimeSlots(
                schedule.getWorkStartTime(), schedule.getWorkEndTime(),
                schedule.getLunchStartTime(), schedule.getLunchEndTime(),
                schedule.getConsultationInterval());

        List<LocalTime> occupied = reservationRepository.findOccupiedTimes(
                doctorId, date,
                List.of(ReservationStatus.CANCELLED, ReservationStatus.REJECTED, ReservationStatus.BLOCKED));

        List<LocalTime> blockedTimes = reservationRepository.findBlockedTimes(doctorId, date);

        return candidates.stream()
                .map(time -> AvailableSlotDto.builder()
                        .time(time)
                        .available(!occupied.contains(time) && !blockedTimes.contains(time))
                        .blocked(blockedTimes.contains(time))
                        .build())
                .collect(Collectors.toList());
    }

    private List<LocalTime> generateTimeSlots(LocalTime workStart, LocalTime workEnd,
                                              LocalTime lunchStart, LocalTime lunchEnd,
                                              int intervalMinutes) {
        List<LocalTime> slots = new ArrayList<>();
        if (workEnd != null && !workEnd.isAfter(workStart)) {
            workEnd = LocalTime.of(23, 59);
        }
        LocalTime current = workStart;
        while (current.isBefore(workEnd)) {
            if (lunchStart != null && lunchEnd != null
                    && !current.isBefore(lunchStart) && current.isBefore(lunchEnd)) {
                current = current.plusMinutes(intervalMinutes);
                continue;
            }
            slots.add(current);
            LocalTime next = current.plusMinutes(intervalMinutes);
            if (!next.isAfter(current)) break; // 자정 넘김 방지
            current = next;
        }
        return slots;
    }

    // ==================== 병원 -> 환자 예약목록 조회 ====================
    public Page<ReservationHospitalListDto> findAllPatientReservation(Long accountId, Pageable pageable,
                                                                      ReservationSearchDto searchDto) {
        Hospital hospital = hospitalRepository.findByAccount_IdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("없는 병원입니다."));

        Specification<ReservationPatient> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("hospital").get("id"), hospital.getId()));
            predicates.add(cb.notEqual(root.get("status"), ReservationStatus.BLOCKED));
            if (searchDto.getStatus() != null)
                predicates.add(cb.equal(root.get("status"), searchDto.getStatus()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return reservationRepository.findAll(spec, pageable).map(ReservationHospitalListDto::fromEntity);
    }

    // ==================== 환자 -> 내 예약목록 조회 ====================
    public Page<ReservationPatientListDto> findAllMyReservation(Long accountId, Pageable pageable) {
        Patient patient = patientRepository.findByAccountIdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("없는 환자입니다."));

        return reservationRepository.findAllMyReservation(patient.getId(), pageable)
                .map(ReservationPatientListDto::fromEntity);
    }

    // ==================== 주간 달력 조회 ====================
    public List<ReservationWeekResDto> findWeeklyByDoctor(Long accountId, Long doctorId,
                                                          LocalDate startDate, LocalDate endDate) {
        Hospital hospital = hospitalRepository.findByAccount_IdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("병원 정보를 찾을 수 없습니다."));

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("의사를 찾을 수 없습니다."));

        if (!doctor.getHospital().getId().equals(hospital.getId()))
            throw new AccessDeniedException("해당 의사에 대한 권한이 없습니다.");

        return reservationRepository.findWeeklyByDoctorId(doctorId, startDate, endDate)
                .stream().map(ReservationWeekResDto::from).collect(Collectors.toList());
    }

    // ==================== 예약 승인 ====================
    public ReservationWeekResDto approve(Long accountId, Long reservationId) {
        ReservationPatient r = getMyHospitalReservation(accountId, reservationId);
        if (r.getStatus() != ReservationStatus.WAITING)
            throw new IllegalStateException("대기 중인 예약만 승인할 수 있습니다.");

        r.updateStatus(ReservationStatus.APPROVED);
        String info = r.getReservationDate() + " " + r.getReservationTime();
        sseAlarmService.sendMessage(r.getPatient().getAccount().getId(),
                "예약이 승인되었습니다. " + r.getHospital().getName() + " / " + info,
                AlarmType.RESERVATION_APPROVED.name(), r.getId());
        smsService.sendSms(r.getPatient().getPhone(),
                "[호다닥] 예약이 승인되었습니다.\n" + r.getHospital().getName() + "\n일시: " + info,
                AlarmType.RESERVATION_APPROVED, r.getId());

        return ReservationWeekResDto.from(r);
    }

    // ==================== 예약 거절 ====================
    public ReservationWeekResDto reject(Long accountId, Long reservationId) {
        ReservationPatient r = getMyHospitalReservation(accountId, reservationId);
        if (r.getStatus() != ReservationStatus.WAITING)
            throw new IllegalStateException("대기 중인 예약만 거절할 수 있습니다.");

        r.updateStatus(ReservationStatus.REJECTED);
        String info = r.getReservationDate() + " " + r.getReservationTime();
        sseAlarmService.sendMessage(r.getPatient().getAccount().getId(),
                "예약이 거절되었습니다. " + r.getHospital().getName() + " / " + info,
                AlarmType.RESERVATION_REJECTED.name(), r.getId());
        smsService.sendSms(r.getPatient().getPhone(),
                "[호다닥] 예약이 병원 사정으로 인해 승인되지 않았습니다.\n" + r.getHospital().getName() + "\n일시: " + info + "\n다른 일정으로 재예약하시거나, 채팅으로 문의해 주세요.",
                AlarmType.RESERVATION_REJECTED, r.getId());

        return ReservationWeekResDto.from(r);
    }

    // ==================== 예약 취소 - 병원 관리자 ====================
    public ReservationWeekResDto cancelByAdmin(Long accountId, Long reservationId) {
        ReservationPatient r = getMyHospitalReservation(accountId, reservationId);
        if (r.getStatus() == ReservationStatus.COMPLETED || r.getStatus() == ReservationStatus.CANCELLED)
            throw new IllegalStateException("이미 완료되었거나 취소된 예약입니다.");

        r.updateStatus(ReservationStatus.CANCELLED);
        String info = r.getReservationDate() + " " + r.getReservationTime();
        sseAlarmService.sendMessage(r.getPatient().getAccount().getId(),
                "병원에서 예약을 취소했습니다. " + r.getHospital().getName() + " / " + info,
                AlarmType.RESERVATION_CANCELLED.name(), r.getId());
        smsService.sendSms(r.getPatient().getPhone(),
                "[호다닥] 예약이 병원 사정으로 인해 취소되었습니다.\n" + r.getHospital().getName() + "\n일시: " + info + "\n다른 일정으로 재예약하시거나, 채팅으로 문의해 주세요.",
                AlarmType.RESERVATION_CANCELLED, r.getId());

        return ReservationWeekResDto.from(r);
    }

    // ==================== 진료 완료 ====================
    public ReservationWeekResDto complete(Long accountId, Long reservationId) {
        ReservationPatient r = getMyHospitalReservation(accountId, reservationId);
        if (r.getStatus() != ReservationStatus.APPROVED)
            throw new IllegalStateException("확정된 예약만 완료 처리할 수 있습니다.");

        r.updateStatus(ReservationStatus.COMPLETED);
        return ReservationWeekResDto.from(r);
    }

    // ==================== 내부 유틸 ====================
    private ReservationPatient getMyHospitalReservation(Long accountId, Long reservationId) {
        Hospital hospital = hospitalRepository.findByAccount_IdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("병원 정보를 찾을 수 없습니다."));

        ReservationPatient r = reservationRepository.findWithDetailById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다."));

        if (!r.getHospital().getId().equals(hospital.getId()))
            throw new AccessDeniedException("해당 예약에 대한 권한이 없습니다.");

        return r;
    }
}