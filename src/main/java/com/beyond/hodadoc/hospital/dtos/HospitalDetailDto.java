package com.beyond.hodadoc.hospital.dtos;

import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.hospital.domain.HospitalImage;
import com.beyond.hodadoc.hospital.domain.HospitalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class HospitalDetailDto {
    private Long id;
    private String name;
    private String phone;
    private String introduction;
    private String businessRegistrationNumber;
    private HospitalStatus status;             // 승인 상태 확인용

    // 주소
    private String streetAddress;
    private String detailAddress;
    private String zipcode;
    private Double latitude;
    private Double longitude;

    // 이미지
    private List<String> imageUrls;

    // 진료과 & 필터 (이름 리스트)
    private List<String> departments;
    private List<String> filters;

    // 해시태그
    private List<HashtagDto> hashtags;

    @lombok.Data
    @AllArgsConstructor
    public static class HashtagDto {
        private Long id;
        private String tag;
    }

    // 운영시간 & 휴무일
    private List<OperatingTimeDto> operatingHours;
    private List<HolidayDto> holidays;

    // 접수 마감 상태
    private boolean checkinClosed;

    public static HospitalDetailDto fromEntity(Hospital hospital) {
        return fromEntity(hospital, !hospital.isCheckinClosedToday());
    }

    public static HospitalDetailDto fromEntity(Hospital hospital, boolean isAcceptingCheckin) {
        return HospitalDetailDto.builder()
                .id(hospital.getId())
                .name(hospital.getName())
                .phone(hospital.getPhone())
                .introduction(hospital.getIntroduction())
                .businessRegistrationNumber(hospital.getBusinessRegistrationNumber())
                .status(hospital.getStatus())

                .streetAddress(hospital.getAddress() != null ? hospital.getAddress().getStreetAddress() : null)
                .detailAddress(hospital.getAddress() != null ? hospital.getAddress().getDetailAddress() : null)
                .zipcode(hospital.getAddress() != null ? hospital.getAddress().getZipcode() : null)
                .latitude(hospital.getAddress() != null ? hospital.getAddress().getLatitude() : null)
                .longitude(hospital.getAddress() != null ? hospital.getAddress().getLongitude() : null)

                .imageUrls(hospital.getImages() != null
                        ? hospital.getImages().stream().map(HospitalImage::getImageUrl).collect(Collectors.toList())
                        : List.of())

                .departments(hospital.getHospitalDepartments() != null
                        ? hospital.getHospitalDepartments().stream().map(hd -> hd.getDepartment().getName()).collect(Collectors.toList())
                        : List.of())

                .filters(hospital.getHospitalFilters() != null
                        ? hospital.getHospitalFilters().stream().map(hf -> hf.getFilter().getName()).collect(Collectors.toList())
                        : List.of())

                .hashtags(hospital.getHashtags().stream()
                        .map(h -> new HashtagDto(h.getId(), h.getTag()))
                        .collect(Collectors.toList()))

                // Entity -> OperatingTimeDto 변환
                .operatingHours(hospital.getOperatingHours() != null
                        ? hospital.getOperatingHours().stream().map(h -> new OperatingTimeDto(
                                h.getDayOfWeek().toString(),
                                h.getOpenTime() != null ? h.getOpenTime().toString() : null,
                                h.getCloseTime() != null ? h.getCloseTime().toString() : null,
                                h.getBreakStartTime() != null ? h.getBreakStartTime().toString() : null,
                                h.getBreakEndTime() != null ? h.getBreakEndTime().toString() : null,
                                h.isDayOff()
                        )).collect(Collectors.toList())
                        : List.of())

                // Entity -> HolidayDto 변환
                .holidays(hospital.getHolidays() != null
                        ? hospital.getHolidays().stream().map(h -> new HolidayDto(h.getHolidayDate(), h.getReason())).collect(Collectors.toList())
                        : List.of())

                .checkinClosed(!isAcceptingCheckin)
                .build();
    }
}
