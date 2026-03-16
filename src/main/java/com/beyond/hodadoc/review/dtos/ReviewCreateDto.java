package com.beyond.hodadoc.review.dtos;


import com.beyond.hodadoc.checkin.domain.Checkin;
import com.beyond.hodadoc.hospital.domain.Hospital;
import com.beyond.hodadoc.patient.domain.Patient;
import com.beyond.hodadoc.reservation.domain.ReservationPatient;
import com.beyond.hodadoc.review.domain.Review;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReviewCreateDto {

    @NotBlank(message = "내용을 작성해주세요")
    @Size(max = 255, message = "리뷰는 255자 이내로 작성해주세요")
    private String contents;
    private int rating;
    private Long reservationId;
    private Long checkinId;

    public Review toEntity(Hospital hospital, Patient patient, ReservationPatient reservation, Checkin checkin) {
        return Review.builder()
                .hospital(hospital)
                .patient(patient)
                .reservation(reservation)
                .checkin(checkin)
                .contents(this.contents)
                .rating(this.rating)
                .build();
    }
}

