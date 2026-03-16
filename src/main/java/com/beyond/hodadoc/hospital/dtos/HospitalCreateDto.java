package com.beyond.hodadoc.hospital.dtos;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.hospital.domain.HospitalAddress;
import com.beyond.hodadoc.hospital.domain.HospitalStatus;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class HospitalCreateDto {

    // --- 1. 병원 기본 정보 ---
    private String name;
    private String businessRegistrationNumber;
    private String phone;
    private String introduction;

    // --- 2. 주소 정보 (Address) ---
    private String streetAddress;
    private String detailAddress;
    private String zipcode;
    private Double latitude;
    private Double longitude;
    private String sido;
    private String sigungu;
    private String emdName;

    // --- 3. 연관 데이터 ID 및 파일 ---
    private List<Long> departmentIds;
    private List<Long> filterIds;
    private List<MultipartFile> images;

    // --- 4. 운영시간 및 휴무일 ---
    private List<OperatingTimeDto> operatingHours;
    private List<HolidayDto> holidays;

    public Hospital toEntity(Account account) {

        // 1. 주소 엔티티 생성
        HospitalAddress address = HospitalAddress.builder()
                .streetAddress(this.streetAddress)
                .detailAddress(this.detailAddress)
                .zipcode(this.zipcode)
                .latitude(this.latitude)
                .longitude(this.longitude)
                .sido(this.sido)
                .sigungu(this.sigungu)
                .emdName(this.emdName)
                .build();

        // 2. 병원 엔티티 생성 (아직 운영시간/휴무일은 비어있음)
        Hospital hospital = Hospital.builder()
                .name(this.name)
                .businessRegistrationNumber(this.businessRegistrationNumber)
                .phone(this.phone)
                .introduction(this.introduction)
                .status(HospitalStatus.PENDING)
                .account(account)
                .address(address)
                .operatingHours(new ArrayList<>())
                .holidays(new ArrayList<>())
                .build();

        // 3. 운영 시간 매핑 (각 DTO의 toEntity 호출)
        if (this.operatingHours != null) {
            for (OperatingTimeDto dto : this.operatingHours) {
                // dto에게 hospital을 넘겨주며 엔티티 생성을 위임
                hospital.getOperatingHours().add(dto.toEntity(hospital));
            }
        }

        // 4. 휴무일 매핑 (각 DTO의 toEntity 호출)
        if (this.holidays != null) {
            for (HolidayDto dto : this.holidays) {
                hospital.getHolidays().add(dto.toEntity(hospital));
            }
        }

        return hospital;
    }
}