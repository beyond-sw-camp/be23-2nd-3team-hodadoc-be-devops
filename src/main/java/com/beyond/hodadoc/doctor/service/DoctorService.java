package com.beyond.hodadoc.doctor.service;

import com.beyond.hodadoc.admin.domain.Department;
import com.beyond.hodadoc.admin.repository.DepartmentRepository;
import com.beyond.hodadoc.doctor.domain.Doctor;
import com.beyond.hodadoc.doctor.domain.DoctorOffDay;
import com.beyond.hodadoc.doctor.domain.DoctorSchedule;
import com.beyond.hodadoc.doctor.domain.OffDayType;
import com.beyond.hodadoc.doctor.dtos.*;
import com.beyond.hodadoc.doctor.repository.DoctorOffDayRepository;
import com.beyond.hodadoc.doctor.repository.DoctorRepository;
import com.beyond.hodadoc.doctor.repository.DoctorScheduleRepository;
import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.hospital.domain.HospitalOperatingTime;
import com.beyond.hodadoc.hospital.repository.HospitalRepository;
import com.beyond.hodadoc.reservation.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class DoctorService {

    private final DoctorRepository          doctorRepository;
    private final DoctorScheduleRepository  doctorScheduleRepository;
    private final DoctorOffDayRepository    doctorOffDayRepository;
    private final HospitalRepository        hospitalRepository;
    private final DepartmentRepository      departmentRepository;
    private final DoctorS3Service           doctorS3Service;
    private final ReservationRepository     reservationRepository;

    public DoctorService(DoctorRepository doctorRepository,
                         DoctorScheduleRepository doctorScheduleRepository,
                         DoctorOffDayRepository doctorOffDayRepository,
                         HospitalRepository hospitalRepository,
                         DepartmentRepository departmentRepository,
                         DoctorS3Service doctorS3Service,
                         ReservationRepository reservationRepository) {
        this.doctorRepository         = doctorRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.doctorOffDayRepository   = doctorOffDayRepository;
        this.hospitalRepository       = hospitalRepository;
        this.departmentRepository     = departmentRepository;
        this.doctorS3Service          = doctorS3Service;
        this.reservationRepository    = reservationRepository;
    }

    // ── 의사 CRUD ────────────────────────────────────────────────────────────

    // 환자용: hospitalId로 직접 조회 (인증 불필요)
    @Transactional(readOnly = true)
    public List<DoctorResDto> findByHospitalId(Long hospitalId) {
        return doctorRepository.findByHospitalId(hospitalId)
                .stream().map(DoctorResDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DoctorResDto> findAllByHospital(Long accountId, Long departmentId) {
        Hospital hospital = getMyHospital(accountId);
        List<Doctor> doctors;
        if (departmentId != null) {
            doctors = doctorRepository.findByHospitalIdAndDepartmentId(hospital.getId(), departmentId);
        } else {
            doctors = doctorRepository.findByHospitalId(hospital.getId());
        }
        return doctors.stream().map(DoctorResDto::from).collect(Collectors.toList());
    }

    public DoctorResDto create(Long accountId, DoctorCreateReqDto dto) {
        Hospital   hospital   = getMyHospital(accountId);
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("진료과를 찾을 수 없습니다."));

        String imageUrl = dto.getImageUrl();
        if (dto.getProfileImage() != null && !dto.getProfileImage().isEmpty()) {
            imageUrl = doctorS3Service.upload(dto.getProfileImage());
        }

        Doctor doctor = Doctor.builder()
                .name(dto.getName())
                .department(department)
                .imageUrl(imageUrl)
                .university(dto.getUniversity())
                .career(dto.getCareer())
                .hospital(hospital)
                .build();
        doctorRepository.save(doctor);

        // 병원 운영시간을 기본 근무규칙으로 자동 생성
        createDefaultSchedules(doctor, hospital);

        return DoctorResDto.from(doctor);
    }

    public DoctorResDto update(Long accountId, Long doctorId, DoctorUpdateReqDto dto) {
        Doctor doctor = getMyDoctor(accountId, doctorId);

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new EntityNotFoundException("진료과를 찾을 수 없습니다."));
        }

        String imageUrl = dto.getImageUrl() != null ? dto.getImageUrl() : doctor.getImageUrl();
        if (dto.getProfileImage() != null && !dto.getProfileImage().isEmpty()) {
            doctorS3Service.deleteFile(doctor.getImageUrl());
            imageUrl = doctorS3Service.upload(dto.getProfileImage());
        }

        doctor.update(dto.getName(), department, imageUrl, dto.getUniversity(), dto.getCareer());
        return DoctorResDto.from(doctor);
    }

    public void delete(Long accountId, Long doctorId) {
        Doctor doctor = getMyDoctor(accountId, doctorId);
        if (doctor.getImageUrl() != null) {
            doctorS3Service.deleteFile(doctor.getImageUrl());
        }
        doctorRepository.delete(doctor);
    }

    // ── 근무규칙(스케줄) ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DoctorScheduleResDto> getSchedules(Long accountId, Long doctorId) {
        getMyDoctor(accountId, doctorId);
        return doctorScheduleRepository.findByDoctorId(doctorId)
                .stream().map(DoctorScheduleResDto::from).collect(Collectors.toList());
    }

    /**
     * 근무규칙 저장
     * 1. 기존 스케줄 전체 삭제 후 새로 저장
     * 2. 근무일(isDayOff=false)로 변경된 요일의 오늘 ~ 1년치 BLOCKED 슬롯 삭제
     */
    public List<DoctorScheduleResDto> saveSchedules(Long accountId, Long doctorId, List<DoctorScheduleReqDto> dtos) {
        Doctor doctor = getMyDoctor(accountId, doctorId);

        doctorScheduleRepository.deleteByDoctorId(doctorId);
        doctorScheduleRepository.flush();

        List<DoctorSchedule> result = new ArrayList<>();
        for (DoctorScheduleReqDto dto : dtos) {
            DoctorSchedule schedule = DoctorSchedule.builder()
                    .doctor(doctor)
                    .dayOfWeek(dto.getDayOfWeek())
                    .build();
            schedule.update(
                    dto.getWorkStartTime()  != null ? LocalTime.parse(dto.getWorkStartTime())  : null,
                    dto.getWorkEndTime()    != null ? LocalTime.parse(dto.getWorkEndTime())    : null,
                    dto.getLunchStartTime() != null ? LocalTime.parse(dto.getLunchStartTime()) : null,
                    dto.getLunchEndTime()   != null ? LocalTime.parse(dto.getLunchEndTime())   : null,
                    dto.getConsultationInterval(),
                    dto.isDayOff()
            );
            result.add(doctorScheduleRepository.save(schedule));
        }

        // ✅ 근무일로 변경된 요일의 BLOCKED 슬롯 삭제 (오늘 ~ 1년 후)
        List<LocalDate> datesToClearBlocked = new ArrayList<>();
        LocalDate today        = LocalDate.now();
        LocalDate oneYearLater = today.plusYears(1);
        for (DoctorScheduleReqDto dto : dtos) {
            if (!dto.isDayOff()) {
                LocalDate cursor = today;
                while (!cursor.isAfter(oneYearLater)) {
                    if (cursor.getDayOfWeek().name().equals(dto.getDayOfWeek())) {
                        datesToClearBlocked.add(cursor);
                    }
                    cursor = cursor.plusDays(1);
                }
            }
        }
        if (!datesToClearBlocked.isEmpty()) {
            reservationRepository.deleteBlockedByDoctorIdAndDates(doctorId, datesToClearBlocked);
        }

        return result.stream().map(DoctorScheduleResDto::from).collect(Collectors.toList());
    }

    // ── 휴무/연차 ─────────────────────────────────────────────────────────────

    // 환자용: 병원 소유자 검증 없이 의사 휴무일 조회
    @Transactional(readOnly = true)
    public List<DoctorOffDayResDto> getOffDaysPublic(Long doctorId, LocalDate startDate, LocalDate endDate) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("의사를 찾을 수 없습니다."));

        List<DoctorOffDayResDto> result = new ArrayList<>(
                doctorOffDayRepository.findByDoctorIdAndOffDateBetween(doctorId, startDate, endDate)
                        .stream().map(DoctorOffDayResDto::from).collect(Collectors.toList())
        );

        doctor.getHospital().getHolidays().stream()
                .filter(h -> !h.getHolidayDate().isBefore(startDate) && !h.getHolidayDate().isAfter(endDate))
                .forEach(h -> result.add(DoctorOffDayResDto.fromHospitalHoliday(h.getHolidayDate(), h.getReason())));

        return result;
    }

    @Transactional(readOnly = true)
    public List<DoctorOffDayResDto> getOffDays(Long accountId, Long doctorId,
                                               LocalDate startDate, LocalDate endDate) {
        Doctor doctor = getMyDoctor(accountId, doctorId);

        // 의사 개인 휴무/연차
        List<DoctorOffDayResDto> result = new ArrayList<>(
                doctorOffDayRepository.findByDoctorIdAndOffDateBetween(doctorId, startDate, endDate)
                        .stream().map(DoctorOffDayResDto::from).collect(Collectors.toList())
        );

        // 병원 휴무일도 포함
        doctor.getHospital().getHolidays().stream()
                .filter(h -> !h.getHolidayDate().isBefore(startDate) && !h.getHolidayDate().isAfter(endDate))
                .forEach(h -> result.add(DoctorOffDayResDto.fromHospitalHoliday(h.getHolidayDate(), h.getReason())));

        return result;
    }

    public DoctorOffDayResDto createOffDay(Long accountId, DoctorOffDayReqDto dto) {
        Doctor doctor = getMyDoctor(accountId, dto.getDoctorId());

        if (doctorOffDayRepository.existsByDoctorIdAndOffDate(doctor.getId(), dto.getOffDate())) {
            throw new IllegalStateException("해당 날짜에 이미 휴무가 등록되어 있습니다.");
        }

        DoctorOffDay offDay = DoctorOffDay.builder()
                .doctor(doctor)
                .offDate(dto.getOffDate())
                .type(dto.getType() != null ? dto.getType() : OffDayType.OFF)
                .build();
        return DoctorOffDayResDto.from(doctorOffDayRepository.save(offDay));
    }

    // ✅ 추가: 여러 날짜 일괄 OFF / BLOCKED 등록 (upsert)
    @Transactional
    public void batchSetOffDays(Long accountId, Long doctorId, List<String> dates, String type) {
        Doctor doctor = getMyDoctor(accountId, doctorId);
        OffDayType offType = OffDayType.valueOf(type);

        for (String dateStr : dates) {
            LocalDate date = LocalDate.parse(dateStr);
            Optional<DoctorOffDay> existing =
                    doctorOffDayRepository.findByDoctorIdAndOffDate(doctorId, date);
            if (existing.isPresent()) {
                existing.get().updateType(offType); // 타입만 변경
            } else {
                doctorOffDayRepository.save(
                        DoctorOffDay.builder()
                                .doctor(doctor)
                                .offDate(date)
                                .type(offType)
                                .build()
                );
            }
        }
    }

    // ✅ 추가: 선택 날짜 일괄 초기화 (삭제)
    @Transactional
    public void batchResetOffDays(Long accountId, Long doctorId, List<String> dates) {
        getMyDoctor(accountId, doctorId);
        for (String dateStr : dates) {
            doctorOffDayRepository.deleteByDoctorIdAndOffDate(doctorId, LocalDate.parse(dateStr));
        }
    }

    // ✅ 추가: 해당 월 전체 초기화
    @Transactional
    public void resetMonthOffDays(Long accountId, Long doctorId, int year, int month) {
        getMyDoctor(accountId, doctorId);
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());
        doctorOffDayRepository.deleteByDoctorIdAndOffDateBetween(doctorId, start, end);
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────────────

    private static final int DEFAULT_CONSULTATION_INTERVAL = 15;

    private void createDefaultSchedules(Doctor doctor, Hospital hospital) {
        List<HospitalOperatingTime> opTimes = hospital.getOperatingHours();
        if (opTimes == null || opTimes.isEmpty()) return;

        for (HospitalOperatingTime ot : opTimes) {
            DoctorSchedule schedule = DoctorSchedule.builder()
                    .doctor(doctor)
                    .dayOfWeek(ot.getDayOfWeek())
                    .build();

            // 휴무일이어도 시간 필드에 기본값을 넣어 프론트 null 에러 방지
            LocalTime startTime = ot.getOpenTime() != null ? ot.getOpenTime() : LocalTime.of(9, 0);
            LocalTime endTime = ot.getCloseTime() != null ? ot.getCloseTime() : LocalTime.of(18, 0);

            schedule.update(
                    startTime,
                    endTime,
                    ot.getBreakStartTime(),
                    ot.getBreakEndTime(),
                    DEFAULT_CONSULTATION_INTERVAL,
                    ot.isDayOff()
            );
            doctorScheduleRepository.save(schedule);
        }
    }

    private Hospital getMyHospital(Long accountId) {
        return hospitalRepository.findByAccount_IdAndAccount_DelYn(accountId, "N")
                .orElseThrow(() -> new EntityNotFoundException("병원 정보를 찾을 수 없습니다."));
    }

    private Doctor getMyDoctor(Long accountId, Long doctorId) {
        Hospital hospital = getMyHospital(accountId);
        Doctor   doctor   = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("의사를 찾을 수 없습니다."));
        if (!doctor.getHospital().getId().equals(hospital.getId())) {
            throw new IllegalStateException("해당 의사에 대한 권한이 없습니다.");
        }
        return doctor;
    }
}