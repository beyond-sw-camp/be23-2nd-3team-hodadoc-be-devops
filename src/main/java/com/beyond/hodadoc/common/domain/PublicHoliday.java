package com.beyond.hodadoc.common.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
@Table(name = "public_holiday",
        uniqueConstraints = @UniqueConstraint(columnNames = "holidayDate"))
public class PublicHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(nullable = false)
    private String dateName;

    @Column(nullable = false)
    private int year;
}
