package com.beyond.hodadoc.patient.controller;

import com.beyond.hodadoc.patient.dtos.PatientCreateDto;
import com.beyond.hodadoc.patient.dtos.PatientDetailDto;
import com.beyond.hodadoc.patient.dtos.PatientUpdateDto;
import com.beyond.hodadoc.patient.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/patient")
public class PatientController {
    private final PatientService patientService;
    @Autowired
    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

//    회원가입
    @PostMapping("/create")
    public ResponseEntity<?> register(@RequestBody @Valid PatientCreateDto dto,
                                      @AuthenticationPrincipal Long principal){
        patientService.register(dto, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body("ok");
    }

//    환자 정보조회(개인정보조회)
    @GetMapping("/detail")
    public ResponseEntity<?> myinfo(){
        PatientDetailDto dto = patientService.myinfo();
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }

//    회원 개인정보수정(연락처, 이름, 주소 등의 개인정보수정)
    @PutMapping ("/update")
    public ResponseEntity<?> updateProfile(@RequestBody @Valid PatientUpdateDto dto){
        patientService.updateProfile(dto);
        return ResponseEntity.status(HttpStatus.OK).body("수정 완료");
    }


//    회원탈퇴(soft delete)
    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(){
        patientService.delete();
        return ResponseEntity.status(HttpStatus.OK).body("회원탈퇴 완료");
    }
    

}
