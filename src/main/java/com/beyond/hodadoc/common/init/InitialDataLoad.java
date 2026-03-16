package com.beyond.hodadoc.common.init;

import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.account.domain.Role;
import com.beyond.hodadoc.account.repository.AccountRepository;
import com.beyond.hodadoc.admin.domain.Department;
import com.beyond.hodadoc.admin.domain.Filter;
import com.beyond.hodadoc.admin.repository.DepartmentRepository;
import com.beyond.hodadoc.admin.repository.FilterRepository;
import com.beyond.hodadoc.hospital.domain.*;
import com.beyond.hodadoc.hospital.repository.HospitalRepository;
import com.beyond.hodadoc.patient.domain.Patient;
import com.beyond.hodadoc.patient.repository.PatientRepository;
import com.beyond.hodadoc.review.domain.BadWord;
import com.beyond.hodadoc.review.repository.BadWordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;


//CommandLineRunner를 구현함으로서 아래 run메서드가 스프링 빈으로 등록되는 시점에 자동 실행
@Component
@Transactional
public class InitialDataLoad implements CommandLineRunner {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;
    private final FilterRepository filterRepository;

    private final PatientRepository patientRepository;
    private final BadWordRepository badWordRepository;
    private final RedisConnectionFactory redisConnectionFactory;

    @Value("${spring.jpa.hibernate.ddl-auto:none}")
    private String ddlAuto;

    @Autowired
    public InitialDataLoad(AccountRepository accountRepository, PasswordEncoder passwordEncoder, HospitalRepository hospitalRepository, DepartmentRepository departmentRepository, FilterRepository filterRepository, PatientRepository patientRepository, BadWordRepository badWordRepository, RedisConnectionFactory redisConnectionFactory) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.hospitalRepository = hospitalRepository;
        this.departmentRepository = departmentRepository;
        this.filterRepository = filterRepository;
        this.patientRepository = patientRepository;
        this.badWordRepository = badWordRepository;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public void run(String... args) throws Exception {
        // ddl-auto가 create일 때 Redis 캐시 초기화 (DB와 불일치 방지)
        if ("create".equals(ddlAuto) || "create-drop".equals(ddlAuto)) {
            redisConnectionFactory.getConnection().serverCommands().flushAll();
        }

        // admin 계정이 비어있다면 admin 생성
        if (accountRepository.findByEmail("admin@naver.com").isEmpty()) {
            accountRepository.save(Account.builder()
                    .email("admin@naver.com")
                    .role(Role.ADMIN)
                    .password(passwordEncoder.encode("12341234"))
                    .build());
        }

        // 1. 진료과(Department)가 비어있을 때만 생성
        if (departmentRepository.count() == 0) {
            List<String> departments = Arrays.asList(
                    "내과", "소아청소년과", "피부과", "치과", "이비인후과", "안과", "산부인과", "정신의학과",
                    "성형외과", "정형외과", "한의과", "비뇨기과", "가정의학과", "신경외과", "외과", "흉부외과",
                    "마취통증과", "영상의학과", "신경과", "재활의학과"
            );

            departments.forEach(name -> {
                departmentRepository.save(Department.builder().name(name).build());
            });
            System.out.println(">>> Initial Departments Loaded.");
        }

        // 2. 필터(Filter)가 비어있을 때만 생성
        if (filterRepository.count() == 0) {
            List<String> filters = Arrays.asList(
                    "평일야간", "주말야간", "평일24시", "주말24시", "토요일", "일요일", "공휴일"
            );

            filters.forEach(name -> {
                filterRepository.save(Filter.builder().name(name).build());
            });
            System.out.println(">>> Initial Filters Loaded.");
        }

        // 3. 욕설(BadWord)가 비어있을 때만 생성
        if (badWordRepository.count() == 0) {
            List<String> badWords = Arrays.asList(
                    "씨발", "시발", "ㅅㅂ", "씨바", "씨팔",
                    "개새끼", "개새", "ㄱㅅㄲ",
                    "병신", "ㅂㅅ",
                    "지랄", "ㅈㄹ",
                    "꺼져", "뒤져",
                    "미친", "ㅁㅊ",
                    "존나", "ㅈㄴ",
                    "느금마", "니애미",
                    "새끼", "ㅅㄲ",
                    "죽어", "죽여",
                    "fuck", "shit", "bitch", "asshole"
            );
            badWords.forEach(word ->
                    badWordRepository.save(BadWord.builder().word(word).build())
            );
//            System.out.println(">>> Initial BadWords Loaded.");
        }

        // ====================================================================
        // [더미 데이터 설정 3] 환자 계정 및 환자 생성
        // ====================================================================
        // 환자 데이터가 없다면 환자계정 + 환자 데이터 생성
        if (patientRepository.count() == 0) {

            Account patientAccount1 = accountRepository.save(Account.builder()
                    .email("patient1@naver.com")
                    .role(Role.PATIENT)
                    .password(passwordEncoder.encode("12341234"))
                    .build());

            Account patientAccount2 = accountRepository.save(Account.builder()
                    .email("patient2@naver.com")
                    .role(Role.PATIENT)
                    .password(passwordEncoder.encode("12341234"))
                    .build());

            Account patientAccount3 = accountRepository.save(Account.builder()
                    .email("patient3@naver.com")
                    .role(Role.PATIENT)
                    .password(passwordEncoder.encode("12341234"))
                    .build());

            Account patientAccount4 = accountRepository.save(Account.builder()
                    .email("patient4@naver.com")
                    .role(Role.PATIENT)
                    .password(passwordEncoder.encode("12341234"))
                    .build());

            patientRepository.saveAll(List.of(
                    Patient.builder()
                            .account(patientAccount1)
                            .name("신짱구")
                            .email("patient1@naver.com")
                            .phone("010-1111-1111")
                            .address("서울특별시 강남구 역삼동 123")
                            .build(),
                    Patient.builder()
                            .account(patientAccount2)
                            .name("김철수")
                            .email("patient2@naver.com")
                            .phone("010-2222-2222")
                            .address("서울특별시 마포구 합정동 456")
                            .build(),
                    Patient.builder()
                            .account(patientAccount3)
                            .name("최유리")
                            .email("patient3@naver.com")
                            .phone("010-3333-3333")
                            .address("경기도 성남시 분당구 정자동 789")
                            .build(),
                    Patient.builder()
                            .account(patientAccount4)
                            .name("박맹구")
                            .email("patient4@naver.com")
                            .phone("010-4444-4444")
                            .address("서울특별시 송파구 잠실동 321")
                            .build()
            ));
        }
    }
}

// 추후에 더미데이터 추가할 시 아래 방법 사용 추천
// if (accountRepository.findByEmail("patient5@naver.com").isEmpty()) {
//    // patient5 계정 + 환자 생성
//}