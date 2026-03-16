package com.beyond.hodadoc.hospital.dtos;

import com.beyond.hodadoc.common.domain.PublicHoliday;
import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.hospital.domain.HospitalHashtag;
import com.beyond.hodadoc.hospital.domain.HospitalHoliday;
import com.beyond.hodadoc.hospital.domain.HospitalImage;
import com.beyond.hodadoc.hospital.domain.HospitalOperatingTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class HospitalPublicDetailDto {
    private Long id;
    private String name;
    private String phone;
    // 사업자 번호, 상태값 등 민감 정보 제외

    private String fullAddress; // 주소를 합쳐서 보여줌
    private Double latitude;
    private Double longitude;

    private String introduction; // 병원 소개글

    private List<String> imageUrls;
    private List<String> departments;
    private List<String> filters;
    private List<String> hashtags;

    // ★ 현재 진료 중 여부
    private boolean isOpenNow;

    // ★ 서비스 종료 여부 (DELETED 상태)
    private boolean isTerminated;

    // ★ 접수 마감 여부 (병원 관리자 수동 마감)
    private boolean checkinClosed;

    // ★ 요일별 운영시간
    private List<OperatingTimeInfo> operatingHours;

    // ★ 지정 휴무일 목록
    private List<HolidayInfo> holidays;

    // ★ 공휴일 목록
    private List<PublicHolidayInfo> publicHolidays;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OperatingTimeInfo {
        private String dayOfWeek;      // "MONDAY", "TUESDAY", ...
        private String openTime;       // "09:00"
        private String closeTime;      // "18:00"
        private String breakStartTime; // "12:00" (점심 시작)
        private String breakEndTime;   // "13:00" (점심 끝)
        private boolean dayOff;        // 휴무 여부

        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

        public static OperatingTimeInfo from(HospitalOperatingTime ot) {
            return OperatingTimeInfo.builder()
                    .dayOfWeek(ot.getDayOfWeek() != null ? ot.getDayOfWeek().name() : null)
                    .openTime(formatTime(ot.getOpenTime()))
                    .closeTime(formatTime(ot.getCloseTime()))
                    .breakStartTime(formatTime(ot.getBreakStartTime()))
                    .breakEndTime(formatTime(ot.getBreakEndTime()))
                    .dayOff(ot.isDayOff())
                    .build();
        }

        private static String formatTime(LocalTime time) {
            return time != null ? time.format(FMT) : null;
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HolidayInfo {
        private String holidayDate; // "2024-12-25"
        private String reason;      // "크리스마스"

        public static HolidayInfo from(HospitalHoliday h) {
            return HolidayInfo.builder()
                    .holidayDate(h.getHolidayDate() != null ? h.getHolidayDate().toString() : null)
                    .reason(h.getReason())
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PublicHolidayInfo {
        private String holidayDate; // "2026-01-01"
        private String dateName;    // "신정"

        public static PublicHolidayInfo from(PublicHoliday ph) {
            return PublicHolidayInfo.builder()
                    .holidayDate(ph.getHolidayDate().toString())
                    .dateName(ph.getDateName())
                    .build();
        }
    }

    public static HospitalPublicDetailDto fromTerminatedEntity(Hospital hospital) {
        return HospitalPublicDetailDto.builder()
                .id(hospital.getId())
                .name(hospital.getName())
                .isTerminated(true)
                .build();
    }

    public static HospitalPublicDetailDto fromEntity(Hospital hospital, boolean isOpenNow,
                                                       List<PublicHoliday> publicHolidays) {
        return HospitalPublicDetailDto.builder()
                .id(hospital.getId())
                .name(hospital.getName())
                .phone(hospital.getPhone())
                .introduction(hospital.getIntroduction())
                .fullAddress(hospital.getAddress() != null
                        ? hospital.getAddress().getStreetAddress() + " " + hospital.getAddress().getDetailAddress()
                        : "")
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
                        .map(HospitalHashtag::getTag)
                        .collect(Collectors.toList()))

                .isOpenNow(isOpenNow)
                .checkinClosed(hospital.isCheckinClosedToday())

                .operatingHours(hospital.getOperatingHours() != null
                        ? hospital.getOperatingHours().stream().map(OperatingTimeInfo::from).collect(Collectors.toList())
                        : List.of())

                .holidays(hospital.getHolidays() != null
                        ? hospital.getHolidays().stream().map(HolidayInfo::from).collect(Collectors.toList())
                        : List.of())

                .publicHolidays(publicHolidays != null
                        ? publicHolidays.stream().map(PublicHolidayInfo::from).collect(Collectors.toList())
                        : List.of())

                .build();
    }
}
