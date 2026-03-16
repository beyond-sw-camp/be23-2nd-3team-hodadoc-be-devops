package com.beyond.hodadoc.reservation.domain;

import com.beyond.hodadoc.hospital.domain.Hospital;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "hospital_reservation_slot",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"hospital_id", "reservation_date", "reservation_time"}
        )
)
public class ReservationHospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //  슬롯 소속 병원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;


    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;


    //    허용인원
    @Column(nullable = false)
    private int capacity;

    //    현제 예약된 인원
    @Column(nullable = false)
    private int reservedCount;

    //    강제 마감 여부
    @Column(nullable = false)
    private boolean isClosed;


    @Column(name = "reservation_time", nullable = false)
    private LocalTime reservationTime;

}
