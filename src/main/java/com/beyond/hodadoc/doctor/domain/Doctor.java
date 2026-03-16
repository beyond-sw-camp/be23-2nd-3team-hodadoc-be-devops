package com.beyond.hodadoc.doctor.domain;

import com.beyond.hodadoc.admin.domain.Department;
import com.beyond.hodadoc.hospital.domain.Hospital;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@Builder
@Entity
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private Department department;

    private String imageUrl;

    private String university;   // ✅ 대학교

    @Column(columnDefinition = "TEXT")
    private String career;       // ✅ 경력사항

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private Hospital hospital;

    public void update(String name, Department department, String imageUrl, String university, String career) {
        if (name != null) this.name = name;
        if (department != null) this.department = department;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (university != null) this.university = university;
        if (career != null) this.career = career;
    }
}