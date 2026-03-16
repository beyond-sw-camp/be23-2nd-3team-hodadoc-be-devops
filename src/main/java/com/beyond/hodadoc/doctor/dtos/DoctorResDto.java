package com.beyond.hodadoc.doctor.dtos;

import com.beyond.hodadoc.doctor.domain.Doctor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DoctorResDto {
    private Long id;
    private String name;
    private Long departmentId;
    private String departmentName;
    private String imageUrl;
    private String university;   // ✅ 대학교
    private String career;       // ✅ 경력사항

    public static DoctorResDto from(Doctor doctor) {
        return DoctorResDto.builder()
                .id(doctor.getId())
                .name(doctor.getName())
                .departmentId(doctor.getDepartment() != null ? doctor.getDepartment().getId() : null)
                .departmentName(doctor.getDepartment() != null ? doctor.getDepartment().getName() : null)
                .imageUrl(doctor.getImageUrl() != null ? doctor.getImageUrl() : "https://abilitytony-board-profile-image.s3.ap-northeast-2.amazonaws.com/doctor/daa67d16-4899-44c5-b337-b53e5206ff42_docktor_default.jpeg")
                .university(doctor.getUniversity())
                .career(doctor.getCareer())
                .build();
    }
}