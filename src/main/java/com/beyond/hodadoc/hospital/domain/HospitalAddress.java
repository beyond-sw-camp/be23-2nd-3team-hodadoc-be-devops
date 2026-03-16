package com.beyond.hodadoc.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter @ToString
@Builder
@Entity
public class HospitalAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String streetAddress;  // 전체 도로명 주소
    private String detailAddress;  // 상세 주소
    private String zipcode;        // 우편번호

    // 1. 좌표 정보 (Map API 연동 및 거리 계산용)
    private Double latitude;   // 위도 (예: 37.5665)
    private Double longitude;  // 경도 (예: 126.9780)

    // 2. 지역 필터링을 위한 구조화된 데이터 (특정 지역 검색용)
    private String sido;       // 시/도 (예: 서울특별시, 경기도)
    private String sigungu;    // 시/군/구 (예: 강남구, 수원시 팔달구)
    private String emdName;    // 읍/면/동/리 (예: 역삼동, 정자동)

    @OneToOne(mappedBy = "address")
    private Hospital hospital;

    public void updateInfo(String streetAddress, String detailAddress, String zipcode, Double latitude, Double longitude, String sido, String sigungu, String emdName) {
        this.streetAddress = streetAddress;
        this.detailAddress = detailAddress;
        this.zipcode = zipcode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sido = sido;
        this.sigungu = sigungu;
        this.emdName = emdName;
    }
}

