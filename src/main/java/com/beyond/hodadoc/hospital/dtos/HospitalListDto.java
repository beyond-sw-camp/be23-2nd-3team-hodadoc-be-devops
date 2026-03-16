package com.beyond.hodadoc.hospital.dtos;

import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.hospital.domain.HospitalOperatingTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class HospitalListDto {
    private Long id;
    private String name;
    private Double distance;
    private String address;
    private LocalTime todayCloseTime;
    private List<String> departments;
    private String imageUrl;
    private Boolean isOpenNow;

    // Entity를 DTO로 변환하는 정적 팩토리 메서드
    public static HospitalListDto fromEntity(Hospital hospital, Double userLat, Double userLng, LocalDate today, boolean isOpenNow) {

        // 1. 오늘 요일의 종료 시간 찾기
        LocalTime closeTime = null;
        if (hospital.getOperatingHours() != null) {
            closeTime = hospital.getOperatingHours().stream()
                    .filter(op -> op.getDayOfWeek() == today.getDayOfWeek())
                    .map(HospitalOperatingTime::getCloseTime)
                    .filter(Objects::nonNull) //null이 아닌것만 걸러내는 부분
                    .findFirst()
                    .orElse(null);
        }

        // 2. 진료과 이름 리스트 추출
        List<String> departmentNames = List.of();
        if (hospital.getHospitalDepartments() != null) {
            departmentNames = hospital.getHospitalDepartments().stream()
                    .filter(dept -> dept.getDepartment() != null)
                    .map(dept -> dept.getDepartment().getName())
                    .collect(Collectors.toList());
        }

        // 3. 첫 번째 이미지 추출
        String firstImageUrl = null;
        if (hospital.getImages() != null && !hospital.getImages().isEmpty()) {
            firstImageUrl = hospital.getImages().get(0).getImageUrl();
        }

        // 4. 거리 계산 (안전망 포함)
        double calculatedDistance = 0.0;
        if (hospital.getAddress() != null && hospital.getAddress().getLatitude() != null && hospital.getAddress().getLongitude() != null) {
            calculatedDistance = calculateDistance(
                    userLat, userLng,
                    hospital.getAddress().getLatitude(), hospital.getAddress().getLongitude()
            );
        }

        // 5. DTO 조립 및 반환
        return HospitalListDto.builder()
                .id(hospital.getId())
                .name(hospital.getName())
                .distance(Math.round(calculatedDistance * 10) / 10.0)
                .address(hospital.getAddress() != null ? hospital.getAddress().getStreetAddress() : "")
                .todayCloseTime(closeTime)
                .departments(departmentNames)
                .imageUrl(firstImageUrl)
                .isOpenNow(isOpenNow)
                .build();
    }

    // 거리 계산 유틸리티
    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구의 반지름 (km)
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000;
    }
}