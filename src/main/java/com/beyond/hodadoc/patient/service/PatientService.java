package com.beyond.hodadoc.patient.service;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.account.domain.Role;
import com.beyond.hodadoc.account.repository.AccountRepository;
import com.beyond.hodadoc.patient.domain.Patient;
import com.beyond.hodadoc.patient.dtos.PatientCreateDto;
import com.beyond.hodadoc.patient.dtos.PatientDetailDto;
import com.beyond.hodadoc.patient.dtos.PatientUpdateDto;
import com.beyond.hodadoc.patient.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class PatientService {
    private final PatientRepository patientRepository;
    private final AccountRepository accountRepository;
    @Autowired
    public PatientService(PatientRepository patientRepository,AccountRepository accountRepository) {
        this.patientRepository = patientRepository;
        this.accountRepository = accountRepository;
    }

    public void register(PatientCreateDto dto, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(()-> new EntityNotFoundException("없는 계정입니다."));
        // email이 dto에 없으면 account email 사용 (카카오 재가입 시 프론트에서 email을 보내지 않아도 됨)
        String email = (dto.getEmail() != null && !dto.getEmail().isBlank()) ? dto.getEmail() : account.getEmail();
        Patient patient = Patient.builder()
                .account(account)
                .name(dto.getName())
                .phone(dto.getPhone())
                .email(email)
                .address(dto.getAddress())
                .build();
        patientRepository.save(patient);
    }

    @Transactional(readOnly = true)
    public PatientDetailDto myinfo(){
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Patient patient = patientRepository.findByAccountIdAndAccount_DelYn(accountId, "N")
                .orElseThrow(()-> new EntityNotFoundException("없는 환자입니다."));
        PatientDetailDto dto = PatientDetailDto.builder()
                .patientId(patient.getId())
                .name(patient.getName())
                .email(patient.getEmail())
                .phone(patient.getPhone())
                .address(patient.getAddress())
                .build();
        return dto;
    }

    public void updateProfile(PatientUpdateDto dto){
        Long accountId= (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Patient patient = patientRepository.findByAccountIdAndAccount_DelYn(accountId, "N")
                .orElseThrow(()-> new EntityNotFoundException("없는 환자입니다."));
        patient.updateProfile(dto); //변경감지(dirty checking)
    }



    public void delete(){
        Long accountId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Patient patient = patientRepository.findByAccountIdAndAccount_DelYn(accountId, "N")
                .orElseThrow(()-> new EntityNotFoundException("없는 환자입니다."));
        patient.clearPersonalInfo(); // 개인정보 초기화 후 soft delete
        patient.delete();            // account.delYn = "Y"
    }







}
