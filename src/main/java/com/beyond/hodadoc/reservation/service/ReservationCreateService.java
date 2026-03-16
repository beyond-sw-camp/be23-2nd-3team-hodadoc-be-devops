package com.beyond.hodadoc.reservation.service;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.account.repository.AccountRepository;
import com.beyond.hodadoc.common.domain.AlarmType;
import com.beyond.hodadoc.common.service.SseAlarmService;
import com.beyond.hodadoc.sms.service.SmsService;
import com.beyond.hodadoc.doctor.domain.Doctor;
import com.beyond.hodadoc.doctor.repository.DoctorRepository;
import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.hospital.domain.HospitalStatus;
import com.beyond.hodadoc.hospital.domain.ReservationApprovalMode;
import com.beyond.hodadoc.patient.domain.Patient;
import com.beyond.hodadoc.patient.repository.PatientRepository;
import com.beyond.hodadoc.reservation.domain.ReservationPatient;
import com.beyond.hodadoc.reservation.domain.ReservationStatus;
import com.beyond.hodadoc.reservation.dtos.ReservationCreateReqDto;
import com.beyond.hodadoc.reservation.dtos.ReservationPatientListDto;
import com.beyond.hodadoc.reservation.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 생성 전용 서비스 - 별도 Bean으로 분리 (Spring AOP 프록시 우회 방지)
 *
 * 동시성 3중 방어:
 *   1. Redis 분산락 (ReservationService) → 동시 진입 자체를 직렬화
 *   2. existsActiveByDoctorDateAndTime 체크 (여기) → 락 해제 후 순차 진입한 요청 차단
 *   3. DB 유니크 제약 (ReservationPatient @UniqueConstraint) → 최후 방어선
 */
@Slf4j
@Service
public class ReservationCreateService {

    private final ReservationRepository reservationRepository;
    private final AccountRepository accountRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final SseAlarmService sseAlarmService;
    private final SmsService smsService;

    public ReservationCreateService(ReservationRepository reservationRepository,
                                    AccountRepository accountRepository,
                                    PatientRepository patientRepository,
                                    DoctorRepository doctorRepository,
                                    SseAlarmService sseAlarmService,
                                    SmsService smsService) {
        this.reservationRepository = reservationRepository;
        this.accountRepository = accountRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.sseAlarmService = sseAlarmService;
        this.smsService = smsService;
    }

    /**
     * 실제 예약 생성 로직
     * REQUIRES_NEW: 항상 새 트랜잭션으로 실행 → 이 메서드 리턴 시점에 DB 커밋 완료
     *               → finally의 unlock()은 커밋 후에 실행됨이 보장됨
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReservationPatientListDto doCreateByPatient(Long accountId, ReservationCreateReqDto dto) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("계정을 찾을 수 없습니다."));
        if ("Y".equals(account.getDelYn())) throw new EntityNotFoundException("삭제된 계정입니다.");

        Patient patient = patientRepository.findByAccountIdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("환자 정보를 찾을 수 없습니다."));

        Doctor doctor = doctorRepository.findByIdWithHospital(dto.getDoctorId())
                .orElseThrow(() -> new EntityNotFoundException("의사 정보를 찾을 수 없습니다."));
        Hospital hospital = doctor.getHospital();

        if (hospital.getStatus() == HospitalStatus.DELETED)
            throw new IllegalStateException("서비스 종료된 병원에는 예약할 수 없습니다.");

        boolean isHoliday = hospital.getHolidays().stream()
                .anyMatch(h -> h.getHolidayDate().equals(dto.getReservationDate()));
        if (isHoliday)
            throw new IllegalStateException("해당 날짜는 병원 휴무일입니다. 다른 날짜를 선택해 주세요.");

        // [2중 방어] 활성 예약(WAITING/APPROVED/BLOCKED) 존재 여부 체크
        // - 락이 직렬화를 보장하므로, 1번째 요청 커밋 후 2~4번째 요청이 순차 진입할 때 여기서 막힘
        // - existsActiveByDoctorDateAndTime은 이미 ReservationRepository에 존재하는 쿼리
        boolean alreadyReserved = reservationRepository.existsActiveByDoctorDateAndTime(
                dto.getDoctorId(),
                dto.getReservationDate(),
                dto.getReservationTime()
        );
        if (alreadyReserved) {
            throw new IllegalStateException("이미 예약된 시간입니다. 다른 시간을 선택해 주세요.");
        }

        ReservationApprovalMode approvalMode = hospital.getReservationApprovalMode();
        boolean isAutoApproval = (approvalMode == null || approvalMode == ReservationApprovalMode.AUTO);
        ReservationStatus initialStatus = isAutoApproval ? ReservationStatus.APPROVED : ReservationStatus.WAITING;

        // CANCELLED, REJECTED 된 이전 기록 정리
        reservationRepository.deleteInactiveByDoctorDateAndTime(
                dto.getDoctorId(), dto.getReservationDate(), dto.getReservationTime());
        reservationRepository.flush();

        ReservationPatient rp = ReservationPatient.builder()
                .patient(patient)
                .doctor(doctor)
                .hospital(hospital)
                .reservationDate(dto.getReservationDate())
                .reservationTime(dto.getReservationTime())
                .symptoms(dto.getSymptoms())
                .status(initialStatus)
                .build();

        try {
            reservationRepository.save(rp);
            reservationRepository.flush();
            log.info("[예약생성] 저장 완료 - reservationId={}", rp.getId());
        } catch (DataIntegrityViolationException e) {
            // [3중 방어] DB 유니크 제약 (ReservationPatient @UniqueConstraint) 최후 방어
            throw new IllegalStateException("이미 예약된 시간입니다. 다른 시간을 선택해 주세요.");
        }

        String dateTimeInfo = rp.getReservationDate() + " " + rp.getReservationTime();
        Long hospitalAccountId = hospital.getAccount().getId();

        if (isAutoApproval) {
            sseAlarmService.sendMessage(hospitalAccountId,
                    "새 예약이 들어왔습니다. " + patient.getName() + " / " + dateTimeInfo,
                    AlarmType.RESERVATION_AUTO_APPROVED.name(), rp.getId());
            smsService.sendSms(patient.getPhone(),
                    "[호다닥] 예약이 확정되었습니다.\n" + hospital.getName() + "\n일시: " + dateTimeInfo,
                    AlarmType.RESERVATION_AUTO_APPROVED, rp.getId());
        } else {
            sseAlarmService.sendMessage(hospitalAccountId,
                    "새 예약 요청이 있습니다. 승인 대기 중. " + patient.getName() + " / " + dateTimeInfo,
                    AlarmType.RESERVATION_WAITING.name(), rp.getId());
        }

        return ReservationPatientListDto.fromEntity(
                reservationRepository.findWithDetailById(rp.getId()).orElse(rp));
    }
}