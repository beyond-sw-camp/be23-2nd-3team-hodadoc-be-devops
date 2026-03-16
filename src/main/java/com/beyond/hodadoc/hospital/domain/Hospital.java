package com.beyond.hodadoc.hospital.domain;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.hospital.dtos.HospitalUpdateDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter @ToString
@Builder
@Entity
public class Hospital {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String businessRegistrationNumber;
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String introduction; // 병원 소개

    @OneToMany(mappedBy = "hospital", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @BatchSize(size = 100)
    @Builder.Default
    private List<HospitalOperatingTime> operatingHours = new ArrayList<>();

    @OneToMany(mappedBy = "hospital", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @BatchSize(size = 100)
    @Builder.Default
    private List<HospitalHoliday> holidays = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private HospitalStatus status = HospitalStatus.PENDING; // 기본값은 대기 상태

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(255) DEFAULT 'AUTO'")
    @Builder.Default
    private ReservationApprovalMode reservationApprovalMode = ReservationApprovalMode.AUTO;

    private LocalDate checkinClosedDate; // null = 접수 가능, 오늘 날짜 = 접수 마감

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private HospitalAddress address;

    @OneToMany(mappedBy = "hospital", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @BatchSize(size = 100)
    @Builder.Default
    private List<HospitalImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "hospital", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @BatchSize(size = 100)
    @Builder.Default
    private List<HospitalDepartment> hospitalDepartments = new ArrayList<>();

    @OneToMany(mappedBy = "hospital", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @BatchSize(size = 100)
    @Builder.Default
    private List<HospitalFilter> hospitalFilters = new ArrayList<>();

    @OneToMany(mappedBy = "hospital", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @BatchSize(size = 100)
    @Builder.Default
    private List<HospitalHashtag> hashtags = new ArrayList<>();

    public void changeStatus(HospitalStatus newStatus) {
        if (newStatus != null) {
            this.status = newStatus;
        }
    }

    public void changeApprovalMode(ReservationApprovalMode mode) {
        if (mode != null) {
            this.reservationApprovalMode = mode;
        }
    }

    public void closeCheckinForToday() {
        this.checkinClosedDate = LocalDate.now();
    }

    public void reopenCheckin() {
        this.checkinClosedDate = null;
    }

    public boolean isCheckinClosedToday() {
        return this.checkinClosedDate != null && this.checkinClosedDate.equals(LocalDate.now());
    }

    public void updateBasicInfo(String name, String phone) {
        if (name != null) this.name = name;
        if (phone != null) this.phone = phone;
    }

    public void updateAddress(HospitalUpdateDto dto) {
        if (this.address == null) {
            this.address = new HospitalAddress();
        }
        this.address.updateInfo(
                dto.getStreetAddress(),
                dto.getDetailAddress(),
                dto.getZipcode(),
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getSido(),
                dto.getSigungu(),
                dto.getEmdName()
        );
    }

    public void replaceDepartments(List<HospitalDepartment> newDepartments) {
        this.hospitalDepartments.clear();
        if (newDepartments != null) {
            this.hospitalDepartments.addAll(newDepartments);
        }
    }

    public void replaceFilters(List<HospitalFilter> newFilters) {
        this.hospitalFilters.clear();
        if (newFilters != null) {
            this.hospitalFilters.addAll(newFilters);
        }
    }

    public void replaceOperatingHours(List<HospitalOperatingTime> newHours) {
        this.operatingHours.clear(); // orphanRemoval로 인해 DB 삭제됨
        if (newHours != null) {
            this.operatingHours.addAll(newHours);
        }
    }

    public void replaceHolidays(List<HospitalHoliday> newHolidays) {
        this.holidays.clear();
        if (newHolidays != null) {
            this.holidays.addAll(newHolidays);
        }
    }
}

