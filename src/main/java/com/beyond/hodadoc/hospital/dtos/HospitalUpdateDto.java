package com.beyond.hodadoc.hospital.dtos;

import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.hospital.domain.HospitalStatus;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class HospitalUpdateDto {
    // 1. 기본 정보
    private String name;
    private String phone;
    private String introduce;

    // 2. 주소 정보
    private String streetAddress;
    private String detailAddress;
    private String zipcode;
    private Double latitude; // 프론트에서 재검색 필요
    private Double longitude;
    private String sido;
    private String sigungu;
    private String emdName;

    // 3. 연관 데이터 (새로 선택한 ID들)
    private List<Long> departmentIds;
    private List<Long> filterIds;

    // 4. 리스트 데이터 (운영시간, 휴무일 - 전체 교체)
    private List<OperatingTimeDto> operatingHours;
    private List<HolidayDto> holidays;

    // 5. 이미지
    // keepImageUrls: 유지할 기존 이미지 URL 목록
    //   - null이면 이미지 처리 로직 전체 건너뜀 (이미지 안 건드림)
    //   - 빈 리스트이면 기존 이미지 전부 삭제
    //   - URL이 있으면 해당 이미지만 유지하고 나머지는 삭제
    private List<String> keepImageUrls;

    // images: 새로 업로드할 파일 목록
    @Builder.Default
    private List<MultipartFile> images = new ArrayList<>();

    // 프론트에서 명시적으로 상태 변경을 요청할 때 사용 (예: REJECTED → PENDING 재승인 요청)
    // null이면 상태 변경 없음
    private HospitalStatus status;

}

