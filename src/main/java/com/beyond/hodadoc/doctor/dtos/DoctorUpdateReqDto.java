package com.beyond.hodadoc.doctor.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class DoctorUpdateReqDto {
    private String name;
    private Long departmentId;
    private String imageUrl;
    private MultipartFile profileImage;
    private String university;   // ✅ 대학교
    private String career;       // ✅ 경력사항
}