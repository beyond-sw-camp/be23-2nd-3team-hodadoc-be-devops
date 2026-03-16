# HodaDoc(호다닥) 백엔드 기술 명세서

> 비대면 병원 예약 및 접수 플랫폼

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 프로젝트명 | HodaDoc (호다닥) |
| 설명 | 환자-병원 간 비대면 예약·접수·리뷰 플랫폼 |
| 기술 스택 | Spring Boot 3.5.10, Java 17, MariaDB, Redis, AWS S3 |
| 인증 방식 | JWT (Access + Refresh Token), Kakao OAuth 2.0 |
| 실시간 통신 | SSE (Server-Sent Events) + Redis Pub/Sub |
| 서버 포트 | 8081 |

---

## 2. 사용자 역할 (Role)

| 역할 | 설명 | 인증 방식 |
|------|------|----------|
| `PATIENT` | 환자 | Kakao OAuth 로그인 |
| `HOSPITAL_ADMIN` | 병원 관리자 | 이메일/비밀번호 로그인 |
| `ADMIN` | 시스템 관리자 | 이메일/비밀번호 로그인 |

---

## 3. 도메인 모델 (ERD 요약)

### 3.1 엔티티 관계도

```
Account (1:1) ──── Patient
   │
   └──(1:1)──── Hospital ──(1:N)── HospitalOperatingTime
                    │        ├──(1:N)── HospitalHoliday
                    │        ├──(1:N)── HospitalImage
                    │        ├──(N:M)── Department  (via HospitalDepartment)
                    │        ├──(N:M)── Filter      (via HospitalFilter)
                    │        └──(1:1)── HospitalAddress
                    │
                    ├──(1:N)── Doctor ──(1:N)── DoctorSchedule
                    │             └──(1:N)── DoctorOffDay
                    │
                    ├──(1:N)── Checkin ──────── Review ──(1:N)── ReviewReport
                    │                             │
                    └──(1:N)── ReservationPatient ─┘
```

### 3.2 핵심 엔티티

#### Account (계정)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| email | String | 이메일 (unique) |
| password | String | BCrypt 암호화 비밀번호 |
| role | Enum | ADMIN / PATIENT / HOSPITAL_ADMIN |
| socialType | Enum | KAKAO (nullable) |
| socialId | String | 소셜 로그인 ID |
| delYn | String | 탈퇴 여부 ("N"/"Y") |
| createdTime | LocalDateTime | 생성일시 (자동) |
| updatedTime | LocalDateTime | 수정일시 (자동) |

#### Patient (환자)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| name | String | 환자명 |
| email | String | 이메일 (unique) |
| phone | String | 전화번호 |
| address | String | 주소 |
| rrn | String | 주민번호 (13자리, unique) |
| account | Account | FK (1:1) |

#### Hospital (병원)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| name | String | 병원명 |
| phone | String | 전화번호 |
| introduction | String (TEXT) | 병원 소개글 |
| businessRegistrationNumber | String | 사업자등록번호 |
| status | Enum | PENDING / APPROVED / REJECTED / DELETED |
| reservationApprovalMode | Enum | AUTO / MANUAL |
| account | Account | FK (1:1) |
| address | HospitalAddress | FK (1:1, cascade) |

#### HospitalAddress (병원 주소)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| streetAddress | String | 도로명 주소 |
| detailAddress | String | 상세 주소 |
| zipcode | String | 우편번호 |
| latitude | Double | 위도 |
| longitude | Double | 경도 |
| sido | String | 시/도 |
| sigungu | String | 시/군/구 |
| emdName | String | 읍/면/동 |

#### HospitalOperatingTime (운영시간)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| hospital | Hospital | FK |
| dayOfWeek | DayOfWeek | MONDAY ~ SUNDAY |
| openTime | LocalTime | 개원 시간 |
| closeTime | LocalTime | 폐원 시간 |
| breakStartTime | LocalTime | 점심시간 시작 |
| breakEndTime | LocalTime | 점심시간 종료 |
| isDayOff | boolean | 휴무 여부 |

#### HospitalHoliday (지정 휴무일)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| hospital | Hospital | FK |
| holidayDate | LocalDate | 휴무 날짜 |
| reason | String | 휴무 사유 |

#### Doctor (의사)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| name | String | 의사명 |
| department | Department | FK (진료과) |
| imageUrl | String | 프로필 이미지 URL |
| university | String | 출신 대학 |
| career | String (TEXT) | 경력 |
| hospital | Hospital | FK |

#### DoctorSchedule (의사 근무 스케줄)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| doctor | Doctor | FK |
| dayOfWeek | DayOfWeek | 요일 |
| workStartTime | LocalTime | 근무 시작 |
| workEndTime | LocalTime | 근무 종료 |
| lunchStartTime | LocalTime | 점심 시작 |
| lunchEndTime | LocalTime | 점심 종료 |
| consultationInterval | int | 진료 간격 (분) |
| dayOff | boolean | 휴무 여부 |

#### DoctorOffDay (의사 휴무/연차)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| doctor | Doctor | FK |
| offDate | LocalDate | 휴무 날짜 |
| type | Enum | OFF (휴무) / BLOCKED (예약 차단) |

#### ReservationPatient (예약)
| 필드 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| id | Long | PK | |
| patient | Patient | FK (nullable) | BLOCKED 상태일 때 null |
| doctor | Doctor | FK | |
| hospital | Hospital | FK | |
| reservationDate | LocalDate | not null | 예약 날짜 |
| reservationTime | LocalTime | not null | 예약 시간 |
| symptoms | String (TEXT) | | 증상 메모 |
| status | Enum | | WAITING / APPROVED / REJECTED / CANCELLED / COMPLETED / BLOCKED |

- UNIQUE 제약: (doctor_id, reservation_date, reservation_time)

#### Checkin (접수)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| patient | Patient | FK |
| hospital | Hospital | FK |
| checkinTime | LocalDateTime | 접수 시각 (자동) |
| waitingNumber | Integer | 대기 번호 |
| status | Enum | WAITING / CALLED / COMPLETED / CANCELLED |

#### Review (리뷰)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| hospital | Hospital | FK |
| patient | Patient | FK |
| reservation | ReservationPatient | FK (optional) |
| checkin | Checkin | FK (optional) |
| contents | String (255) | 리뷰 내용 |
| rating | int | 평점 (1~5) |
| status | Enum | NORMAL / REPORTED / DELETED |
| reportCount | int | 신고 횟수 |
| createdTime | LocalDateTime | 작성일 (자동) |
| updatedTime | LocalDateTime | 수정일 (자동) |

#### ReviewReport (리뷰 신고)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| review | Review | FK |
| patient | Patient | FK (신고자, nullable) |
| hospital | Hospital | FK |
| reason | Enum | FALSE_INFORMATION / INAPPROPRIATE_LANGUAGE / PRIVACY_VIOLATION / SPAM / OTHER |

#### 마스터 데이터

**Department (진료과)**: id, name (unique)
**Filter (검색 필터)**: id, name (unique)
**BadWord (금지어)**: id, word (unique)

---

## 4. API 명세

### 4.1 인증 (Account)

**Base Path**: `/account`

| Method | Endpoint | Auth | Request | Response | 설명 |
|--------|----------|------|---------|----------|------|
| POST | `/create` | - | `AccountCreateDto` | `AccountDetailDto` | 회원가입 |
| POST | `/doLogin` | - | `AccountLoginDto` | `{accessToken, refreshToken}` | 일반 로그인 |
| POST | `/kakao/doLogin` | - | `{code}` | `{accessToken, refreshToken}` | Kakao OAuth 로그인 |
| POST | `/refresh` | - | `{refreshToken}` | `{accessToken, refreshToken}` | 토큰 재발급 |
| POST | `/logout` | JWT | - | `"로그아웃 완료"` | 로그아웃 |
| PATCH | `/update/password` | HOSPITAL_ADMIN | `AccountUpdatePwDto` | `"비밀번호 변경 완료"` | 비밀번호 변경 |

<details>
<summary>Request/Response DTO 상세</summary>

**AccountCreateDto**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "role": "PATIENT"
}
```

**AccountLoginDto**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**AccountUpdatePwDto**
```json
{
  "currentPassword": "oldPw123",
  "newPassword": "newPw456"
}
```

**로그인 응답**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```
</details>

---

### 4.2 환자 (Patient)

**Base Path**: `/patient`

| Method | Endpoint | Auth | Request | Response | 설명 |
|--------|----------|------|---------|----------|------|
| POST | `/create` | JWT | `PatientCreateDto` | `"환자 회원가입 완료"` | 환자 정보 등록 |
| GET | `/detail` | JWT | - | `PatientDetailDto` | 내 정보 조회 |
| PUT | `/update` | JWT | `PatientUpdateDto` | `"환자 정보 수정 완료"` | 내 정보 수정 |
| DELETE | `/delete` | JWT | - | `"회원 탈퇴 완료"` | 회원 탈퇴 (soft delete) |

<details>
<summary>Request/Response DTO 상세</summary>

**PatientCreateDto**
```json
{
  "name": "홍길동",
  "phone": "010-1234-5678",
  "address": "서울시 강남구",
  "rrn": "9901011234567"
}
```

**PatientDetailDto**
```json
{
  "id": 1,
  "name": "홍길동",
  "email": "hong@kakao.com",
  "phone": "010-1234-5678",
  "address": "서울시 강남구",
  "rrn": "9901011234567"
}
```
</details>

---

### 4.3 병원 (Hospital)

**Base Path**: `/hospital`

| Method | Endpoint | Auth | Request | Response | 설명 |
|--------|----------|------|---------|----------|------|
| POST | `/create` | HOSPITAL_ADMIN | `HospitalCreateDto` (multipart) | `Long` (hospitalId) | 병원 등록 |
| PUT | `/{id}` | HOSPITAL_ADMIN | `HospitalUpdateDto` (multipart) | `Long` (hospitalId) | 병원 정보 수정 |
| DELETE | `/{id}` | HOSPITAL_ADMIN | - | `"병원 삭제 완료"` | 병원 삭제 (soft delete) |
| GET | `/my` | HOSPITAL_ADMIN | - | `HospitalDetailDto` | 내 병원 조회 |
| GET | `/{id}` | 공개 | - | `HospitalPublicDetailDto` | 병원 상세 조회 |
| GET | `/list` | 공개 | `HospitalSearchDto` (query params) | `Page<HospitalListDto>` | 병원 목록 검색 |
| PATCH | `/{hospitalId}/approval-mode` | HOSPITAL_ADMIN | `ApprovalModeUpdateReqDto` | `"승인 방식 변경 완료"` | 예약 승인 모드 변경 |
| GET | `/mapApi` | 공개 | - | `List<HospitalMapResponseDto>` | 지도 마커용 데이터 |
| GET | `/nearby` | 공개 | `userLat, userLng, radius` | `List<HospitalNearbyResponseDto>` | 근처 병원 조회 |

<details>
<summary>Request/Response DTO 상세</summary>

**HospitalCreateDto** (multipart/form-data)
```
name: "호다닥 병원"
phone: "02-1234-5678"
introduction: "친절한 진료를 약속합니다"
businessRegistrationNumber: "123-45-67890"
streetAddress: "서울시 강남구 테헤란로 123"
detailAddress: "4층"
zipcode: "06234"
latitude: 37.5012
longitude: 127.0396
sido: "서울특별시"
sigungu: "강남구"
emdName: "역삼동"
departmentIds: [1, 3, 5]
filterIds: [1, 2]
operatingHours: [
  {"dayOfWeek": "MONDAY", "openTime": "09:00", "closeTime": "18:00",
   "breakStartTime": "12:00", "breakEndTime": "13:00", "dayOff": false},
  ...
]
holidays: [
  {"holidayDate": "2026-03-01", "reason": "삼일절"},
  {"holidayDate": "2026-05-05", "reason": "어린이날"}
]
images: [파일1, 파일2]
```

**HospitalUpdateDto** (multipart/form-data)
```
name: "호다닥 병원 (수정)"
phone: "02-1234-5678"
keepImageUrls: ["https://s3.../1_1.jpg"]    # 유지할 이미지, null=이미지 안 건드림, []=전부 삭제
images: [새 파일]                             # 추가 업로드할 이미지
departmentIds: [1, 3]
filterIds: [1]
operatingHours: [...]
holidays: [...]                               # 전체 교체
status: "APPROVED"                            # 상태 변경 (optional)
```

**HospitalPublicDetailDto** (병원 상세 조회 응답)
```json
{
  "id": 1,
  "name": "호다닥 병원",
  "phone": "02-1234-5678",
  "introduction": "친절한 진료를 약속합니다",
  "fullAddress": "서울시 강남구 테헤란로 123 4층",
  "latitude": 37.5012,
  "longitude": 127.0396,
  "imageUrls": ["https://s3.../1_1.jpg"],
  "departments": ["내과", "외과"],
  "filters": ["평일야간진료"],
  "isOpenNow": true,
  "isTerminated": false,
  "operatingHours": [
    {
      "dayOfWeek": "MONDAY",
      "openTime": "09:00",
      "closeTime": "18:00",
      "breakStartTime": "12:00",
      "breakEndTime": "13:00",
      "dayOff": false
    }
  ],
  "holidays": [
    {"holidayDate": "2026-03-01", "reason": "삼일절"},
    {"holidayDate": "2026-05-05", "reason": "어린이날"}
  ]
}
```

**HospitalSearchDto** (검색 쿼리 파라미터)
```
GET /hospital/list?name=호다닥&sido=서울특별시&sigungu=강남구&emdName=역삼동
    &departmentName=내과&nightFilter=평일야간진료&holidayFilter=토요일진료
    &isCurrentlyOpen=true&userLat=37.5012&userLng=127.0396
    &page=0&size=10
```

**HospitalListDto** (목록 응답)
```json
{
  "id": 1,
  "name": "호다닥 병원",
  "distance": 1.2,
  "address": "서울시 강남구 테헤란로 123",
  "todayCloseTime": "18:00",
  "departments": ["내과", "외과"],
  "imageUrl": "https://s3.../1_1.jpg"
}
```
</details>

---

### 4.4 의사 (Doctor)

**Base Path**: `/doctor`

| Method | Endpoint | Auth | Request | Response | 설명 |
|--------|----------|------|---------|----------|------|
| GET | `/hospital/{hospitalId}` | 공개 | - | `List<DoctorResDto>` | 병원별 의사 목록 (환자용) |
| GET | `/list` | JWT | `?departmentId=` (optional) | `List<DoctorResDto>` | 내 병원 의사 목록 |
| POST | `/create` | JWT | `DoctorCreateReqDto` (multipart) | `DoctorResDto` | 의사 등록 |
| PUT | `/{doctorId}` | JWT | `DoctorUpdateReqDto` (multipart) | `DoctorResDto` | 의사 수정 |
| DELETE | `/{doctorId}` | JWT | - | `"삭제 완료"` | 의사 삭제 |
| GET | `/{doctorId}/schedule` | JWT | - | `List<DoctorScheduleResDto>` | 근무 스케줄 조회 |
| PUT | `/{doctorId}/schedule` | JWT | `List<DoctorScheduleReqDto>` | `List<DoctorScheduleResDto>` | 근무 스케줄 저장 |
| GET | `/{doctorId}/offdays` | JWT | `?startDate=&endDate=` | `List<DoctorOffDayResDto>` | 휴무/연차 조회 |
| POST | `/offday` | JWT | `DoctorOffDayReqDto` | `DoctorOffDayResDto` | 휴무/연차 등록 |

<details>
<summary>Request/Response DTO 상세</summary>

**DoctorCreateReqDto** (multipart/form-data)
```
name: "김닥터"
departmentId: 1
university: "서울대학교"
career: "경력 10년\n대한외과학회 정회원"
image: [프로필 이미지 파일]
```

**DoctorScheduleReqDto**
```json
[
  {
    "dayOfWeek": "MONDAY",
    "workStartTime": "09:00",
    "workEndTime": "18:00",
    "lunchStartTime": "12:00",
    "lunchEndTime": "13:00",
    "consultationInterval": 30,
    "dayOff": false
  }
]
```

**DoctorOffDayReqDto**
```json
{
  "doctorId": 1,
  "offDate": "2026-03-15",
  "type": "OFF"
}
```
</details>

---

### 4.5 예약 (Reservation)

**Base Path**: `/reservation`

| Method | Endpoint | Auth | Request | Response | 설명 |
|--------|----------|------|---------|----------|------|
| POST | `/patient` | JWT(환자) | `ReservationCreateReqDto` | `ReservationPatientListDto` | 예약 생성 |
| DELETE | `/patient/{id}` | JWT(환자) | - | `"예약 취소 완료"` | 예약 취소 |
| GET | `/patient` | JWT(환자) | Pageable | `Page<ReservationPatientListDto>` | 내 예약 목록 |
| PATCH | `/{id}/status` | JWT | `ReservationStatusUpdateReqDto` | `ReservationPatientListDto` | 상태 변경 (공통) |
| GET | `/slots` | 공개 | `?doctorId=&date=` | `List<AvailableSlotDto>` | 예약 가능 슬롯 |
| POST | `/hospital/block` | JWT(병원) | `BlockSlotReqDto` | `"슬롯 블록 완료"` | 슬롯 블록 |
| DELETE | `/hospital/block/{id}` | JWT(병원) | - | `"블록 해제 완료"` | 블록 해제 |
| GET | `/hospital` | JWT(병원) | Pageable, `ReservationSearchDto` | `Page<ReservationHospitalListDto>` | 병원 예약 목록 |
| GET | `/list` | JWT(병원) | `?doctorId=&startDate=&endDate=` | `List<ReservationWeekResDto>` | 주간 캘린더 |
| PATCH | `/{id}/approve` | JWT(병원) | - | `ReservationWeekResDto` | 예약 승인 |
| PATCH | `/{id}/reject` | JWT(병원) | - | `ReservationWeekResDto` | 예약 거절 |
| PATCH | `/{id}/cancel` | JWT(병원) | - | `ReservationWeekResDto` | 예약 취소 (관리자) |
| PATCH | `/{id}/complete` | JWT(병원) | - | `ReservationWeekResDto` | 진료 완료 |

<details>
<summary>Request/Response DTO 상세</summary>

**ReservationCreateReqDto**
```json
{
  "doctorId": 1,
  "reservationDate": "2026-03-10",
  "reservationTime": "14:00",
  "symptoms": "두통이 심합니다"
}
```

**예약 상태 흐름**
```
[자동 승인 모드]
  APPROVED ──→ COMPLETED
     │
     └──→ CANCELLED

[수동 승인 모드]
  WAITING ──→ APPROVED ──→ COMPLETED
     │           │
     └──→ REJECTED  └──→ CANCELLED
```

**AvailableSlotDto**
```json
[
  {"time": "09:00"},
  {"time": "09:30"},
  {"time": "10:00"}
]
```

**BlockSlotReqDto**
```json
{
  "doctorId": 1,
  "date": "2026-03-10",
  "time": "14:00"
}
```
</details>

---

### 4.6 접수 (Checkin)

**Base Path**: `/checkin`

| Method | Endpoint | Auth | Request | Response | 설명 |
|--------|----------|------|---------|----------|------|
| POST | `/patient` | JWT(환자) | `CheckinCreateReqDto` | `CheckinPatientListDto` | 온라인 접수 |
| GET | `/patient` | JWT(환자) | Pageable | `Page<CheckinPatientListDto>` | 내 접수 목록 |
| DELETE | `/patient/{id}` | JWT(환자) | - | - | 접수 취소 |
| GET | `/hospital` | JWT(병원) | Pageable | `Page<CheckinHospitalListDto>` | 당일 접수 목록 |
| PATCH | `/{id}/status` | JWT(병원) | `CheckinStatusUpdateReqDto` | `CheckinHospitalListDto` | 상태 변경 |

<details>
<summary>상태 흐름 및 DTO</summary>

**접수 상태 흐름**
```
WAITING ──→ CALLED ──→ COMPLETED
   │
   └──→ CANCELLED
```

**CheckinCreateReqDto**
```json
{
  "hospitalId": 1
}
```
</details>

---

### 4.7 리뷰 (Review)

**Base Path**: `/reviews`

| Method | Endpoint | Auth | Request | Response | 설명 |
|--------|----------|------|---------|----------|------|
| POST | `/create` | JWT(환자) | `ReviewCreateDto` | - | 리뷰 작성 |
| GET | `/{hospitalId}/hospitalist` | JWT | Pageable | `Page<ReviewListDto>` | 병원별 리뷰 목록 |
| GET | `/myreviews` | JWT(환자) | Pageable | `Page<ReviewListDto>` | 내 리뷰 목록 |
| PUT | `/{reviewId}/update` | JWT(환자) | `ReviewUpdateDto` | `"리뷰 수정 완료"` | 리뷰 수정 |
| DELETE | `/{reviewId}/delete` | JWT(환자) | - | `"리뷰 삭제 완료"` | 리뷰 삭제 |
| GET | `/exists/reservation/{id}` | JWT | - | `Boolean` | 예약 리뷰 작성 여부 |
| GET | `/exists/checkin/{id}` | JWT | - | `Boolean` | 접수 리뷰 작성 여부 |

<details>
<summary>Request/Response DTO 상세</summary>

**ReviewCreateDto**
```json
{
  "hospitalId": 1,
  "reservationId": 5,
  "contents": "친절한 진료 감사합니다",
  "rating": 5
}
```
</details>

---

### 4.8 리뷰 신고 (ReviewReport)

**Base Path**: `/reviewreports`

| Method | Endpoint | Auth | Request | Response | 설명 |
|--------|----------|------|---------|----------|------|
| POST | `/{reviewId}` | JWT | `ReviewReportCreateDto` | `"신고 완료"` | 리뷰 신고 |
| GET | `/reviews` | ADMIN | Pageable | `Page<ReviewReportAdminListDto>` | 신고된 리뷰 목록 |
| POST | `/{reviewId}/delete` | ADMIN | - | `"리뷰 삭제 완료"` | 신고 승인 (삭제) |
| POST | `/{reviewId}/reject` | ADMIN | - | `"신고 반려 완료"` | 신고 반려 (복구) |

<details>
<summary>신고 사유</summary>

```
FALSE_INFORMATION      허위 정보
INAPPROPRIATE_LANGUAGE 부적절한 언어
PRIVACY_VIOLATION      개인정보 침해
SPAM                   스팸
OTHER                  기타
```
</details>

---

### 4.9 관리자 (Admin)

**Base Path**: `/admin`

| Method | Endpoint | Auth | Request | Response | 설명 |
|--------|----------|------|---------|----------|------|
| GET | `/summary` | ADMIN | - | `AdminSummaryDto` | 대시보드 요약 |
| GET | `/hospital/pending` | ADMIN | - | `List<HospitalDetailDto>` | 승인 대기 병원 목록 |
| PATCH | `/hospital/{id}/approve` | ADMIN | - | `"병원 승인 완료"` | 병원 승인 |
| PATCH | `/hospital/{id}/reject` | ADMIN | - | `"병원 반려"` | 병원 반려 |
| POST | `/department` | ADMIN | `NameDto` | `Department` | 진료과 생성 |
| GET | `/department/list` | 공개 | - | `List<Department>` | 진료과 목록 |
| DELETE | `/department/{id}` | ADMIN | - | `"삭제 완료"` | 진료과 삭제 |
| POST | `/filter` | ADMIN | - | `Filter` | 필터 생성 |
| GET | `/filter/list` | 공개 | - | `List<Filter>` | 필터 목록 |
| DELETE | `/filter/{id}` | ADMIN | - | `"삭제 완료"` | 필터 삭제 |

---

### 4.10 실시간 알림 (SSE)

**Base Path**: `/sse`

| Method | Endpoint | Auth | Content-Type | 설명 |
|--------|----------|------|-------------|------|
| GET | `/connect` | JWT | `text/event-stream` | SSE 연결 (타임아웃: 1시간) |
| GET | `/disconnect` | JWT | - | SSE 연결 해제 |

<details>
<summary>SSE 알림 타입</summary>

| 알림 타입 | 수신자 | 설명 |
|----------|--------|------|
| `RESERVATION_AUTO_APPROVED` | 병원 | 자동 승인 예약 접수 알림 |
| `RESERVATION_WAITING` | 병원 | 새 예약 대기 알림 |
| `RESERVATION_APPROVED` | 환자 | 예약 승인 알림 |
| `RESERVATION_REJECTED` | 환자 | 예약 거절 알림 |
| `RESERVATION_CANCELLED` | 양쪽 | 예약 취소 알림 |
| `RECEPTION_CREATED` | 병원 | 새 접수 알림 |
| `RECEPTION_CALLED` | 환자 | 호출 알림 |
| `RECEPTION_QUEUE_UPDATE` | 환자 | 대기 순번 변경 알림 |

**SSE 메시지 형식**
```json
{
  "receiverId": 1,
  "message": "새 예약이 들어왔습니다. 홍길동 / 2026-03-10 14:00",
  "type": "RESERVATION_AUTO_APPROVED",
  "reservationId": 5
}
```
</details>

---

## 5. 인증 및 보안

### 5.1 JWT 토큰

| 항목 | 값 |
|------|---|
| 알고리즘 | HS512 |
| Access Token 만료 | 3000분 |
| Refresh Token 만료 | 10080분 (7일) |
| Refresh Token 저장소 | Redis |
| 헤더 형식 | `Authorization: Bearer {token}` |

### 5.2 토큰 Claims

```json
{
  "sub": "1",         // accountId
  "role": "PATIENT",  // 사용자 역할
  "iat": 1234567890,
  "exp": 1234567890
}
```

### 5.3 접근 제어

| 경로 패턴 | 접근 권한 |
|----------|---------|
| `POST /account/create` | 공개 |
| `POST /account/doLogin` | 공개 |
| `POST /account/kakao/doLogin` | 공개 |
| `POST /account/refresh` | 공개 |
| `GET /hospital/**` | 공개 |
| `GET /doctor/hospital/**` | 공개 |
| `GET /reservation/slots` | 공개 |
| `GET /admin/department/list` | 공개 |
| `GET /admin/filter/list` | 공개 |
| `GET /sse/**` | 공개 |
| `PATCH /account/update/password` | HOSPITAL_ADMIN |
| `POST,PUT,DELETE /hospital/**` | HOSPITAL_ADMIN |
| `GET /hospital/my` | HOSPITAL_ADMIN |
| `/admin/**` (CRUD) | ADMIN |
| 그 외 모든 경로 | 인증 필요 |

### 5.4 CORS 설정

```
허용 Origin: http://localhost:*, http://127.0.0.1:*, https://www.hodadoc.com
허용 Method: GET, POST, PUT, PATCH, DELETE, OPTIONS
허용 Header: *
Credentials: 허용
```

---

## 6. 비즈니스 로직 상세

### 6.1 병원 영업 여부 판단 (`isOpenNow`)

```
1. 오늘이 지정 휴무일(HospitalHoliday)인가? → false
2. 오늘 요일의 운영시간 데이터가 있는가? → 없으면 false
3. 해당 요일이 휴무(isDayOff)인가? → true면 false
4. 현재 시간 >= 개원시간인가? → 아니면 false
5. 현재 시간 < 폐원시간인가? → 아니면 false
6. 현재 점심시간인가? → 맞으면 false
7. 위 조건 모두 통과 → true (영업 중)
```

### 6.2 예약 가능 슬롯 계산

```
1. 의사의 해당 요일 스케줄 조회 (DoctorSchedule)
2. 휴무 요일이거나, 해당 날짜가 DoctorOffDay이면 → 빈 리스트
3. 근무 시작~종료 사이를 consultationInterval 간격으로 슬롯 생성
4. 점심시간 슬롯 제외
5. 이미 예약된(CANCELLED 제외) 슬롯 제외
6. 남은 슬롯 반환
```

### 6.3 예약 생성 시 검증

```
1. 계정 존재 + 삭제되지 않음 확인
2. 환자 역할 확인
3. 의사 존재 확인
4. 병원 상태가 DELETED가 아닌지 확인
5. 예약 날짜가 병원 지정 휴무일인지 확인 → 휴무일이면 거절
6. 예약 승인 모드 확인 (AUTO → APPROVED, MANUAL → WAITING)
7. DB 저장 (중복 시간 → DataIntegrityViolationException → "이미 예약된 시간입니다")
8. SSE 알림 발송 (병원에게)
```

### 6.4 접수 생성 시 검증

```
1. 환자 + 병원 존재 확인
2. 병원 상태가 DELETED가 아닌지 확인
3. 오늘이 병원 지정 휴무일인지 확인 → 휴무일이면 거절
4. 대기번호 발급 (당일 최대번호 + 1)
5. 접수 저장 + SSE 알림 발송 (병원에게)
```

### 6.5 이미지 관리 (keepImageUrls 방식)

```
keepImageUrls가 null → 이미지 처리 건너뜀 (기존 유지)
keepImageUrls가 [] (빈 배열) → 기존 이미지 전부 삭제
keepImageUrls에 URL → 해당 URL만 유지, 나머지 S3에서 삭제
+ images 필드에 새 파일 → 추가 업로드
```

### 6.6 리뷰 금지어 필터링

- `BadWord` 테이블에 등록된 금지어가 리뷰 내용에 포함되면 작성/수정 거부
- 한글 + 영문 욕설 30+개 사전 등록

---

## 7. 외부 연동

### 7.1 AWS S3

| 항목 | 값 |
|------|---|
| 리전 | ap-northeast-2 (서울) |
| 버킷 | abilitytony-board-profile-image |
| 용도 | 병원 이미지, 의사 프로필 이미지 |
| 파일명 규칙 | `{hospitalId}_{순서}.{확장자}` |

### 7.2 Kakao OAuth

| 항목 | 값 |
|------|---|
| Provider | Kakao |
| Redirect URI | `http://localhost:3002/oauth/kakao/callback` |
| 수집 정보 | 이메일, 프로필 |

### 7.3 Redis

| 용도 | Key 패턴 | TTL |
|------|---------|-----|
| Refresh Token | `RT:{accountId}` | 7일 |
| SSE Pub/Sub | `notification-channel` | - |

---

## 8. 에러 응답 형식

```json
{
  "status_code": 400,
  "error_message": "에러 메시지"
}
```

| HTTP 상태 | 예외 | 설명 |
|----------|------|------|
| 400 | IllegalArgumentException | 잘못된 요청 |
| 400 | MethodArgumentNotValidException | 유효성 검증 실패 |
| 401 | AuthenticationException | 인증 실패 (토큰 없음/만료) |
| 403 | AuthorizationDeniedException | 권한 없음 |
| 404 | EntityNotFoundException | 리소스 없음 |

---

## 9. 주요 비즈니스 시나리오

### 시나리오 1: 병원 등록 → 승인

```
1. 병원 관리자 회원가입         POST /account/create
2. 로그인                     POST /account/doLogin
3. 병원 정보 등록              POST /hospital/create
   → 상태: PENDING
4. 시스템 관리자 승인           PATCH /admin/hospital/{id}/approve
   → 상태: APPROVED
5. 의사 등록                   POST /doctor/create
6. 의사 스케줄 설정             PUT /doctor/{id}/schedule
```

### 시나리오 2: 환자 예약

```
1. 카카오 로그인               POST /account/kakao/doLogin
2. 환자 정보 등록              POST /patient/create
3. 병원 검색                   GET /hospital/list?isCurrentlyOpen=true
4. 병원 상세 조회              GET /hospital/{id}
   → 휴무일 목록 확인 (holidays)
5. 의사 목록 조회              GET /doctor/hospital/{hospitalId}
6. 예약 슬롯 확인              GET /reservation/slots?doctorId=1&date=2026-03-10
7. 예약 생성                   POST /reservation/patient
   → 휴무일이면 400 "해당 날짜는 병원 휴무일입니다."
   → 성공 시 SSE로 병원에 알림
8. 진료 후 리뷰 작성           POST /reviews/create
```

### 시나리오 3: 환자 접수 (당일)

```
1. 카카오 로그인               POST /account/kakao/doLogin
2. 병원 상세 조회              GET /hospital/{id}
   → isOpenNow: true 확인
3. 접수 생성                   POST /checkin/patient
   → 오늘 휴무일이면 400 "오늘은 병원 휴무일입니다."
   → 성공 시 대기번호 발급 + SSE 알림
4. 병원에서 호출               PATCH /checkin/{id}/status (CALLED)
   → 환자에게 SSE 알림
5. 진료 완료                   PATCH /checkin/{id}/status (COMPLETED)
```

### 시나리오 4: 병원 휴무일 관리

```
1. 내 병원 조회                GET /hospital/my
   → 기존 holidays 목록 확인
2. 병원 정보 수정              PUT /hospital/{id}
   → holidays에 새 휴무일 추가 (전체 교체)
3. 환자가 휴무일에 예약 시도    POST /reservation/patient
   → 400 "해당 날짜는 병원 휴무일입니다."
4. 환자가 휴무일에 접수 시도    POST /checkin/patient
   → 400 "오늘은 병원 휴무일입니다."
5. 병원 목록에서 영업 중 필터   GET /hospital/list?isCurrentlyOpen=true
   → 오늘 휴무인 병원은 결과에서 제외
```

### 시나리오 5: 리뷰 신고 처리

```
1. 리뷰 작성                   POST /reviews/create
   → 금지어 포함 시 거부
2. 리뷰 신고                   POST /reviewreports/{reviewId}
   → 신고 횟수 증가, 상태 REPORTED
3. 관리자 신고 목록 조회        GET /reviewreports/reviews
4-A. 신고 승인 (삭제)          POST /reviewreports/{reviewId}/delete
4-B. 신고 반려 (복구)          POST /reviewreports/{reviewId}/reject
```
