-- 병원 검색 성능 최적화 인덱스

-- hospital: 상태 필터 (모든 쿼리에서 WHERE h.status = 'APPROVED')
CREATE INDEX idx_hospital_status ON hospital (status);

-- hospital_address: 지역 필터 + 통합 검색
CREATE INDEX idx_address_sido ON hospital_address (sido);
CREATE INDEX idx_address_sigungu ON hospital_address (sigungu);
CREATE INDEX idx_address_emd_name ON hospital_address (emd_name);

-- department: 진료과 검색
CREATE INDEX idx_department_name ON department (name);

-- hospital_department: JOIN 성능
CREATE INDEX idx_hosp_dept_hospital_id ON hospital_department (hospital_id);
CREATE INDEX idx_hosp_dept_department_id ON hospital_department (department_id);

-- hospital_operating_time: 영업시간 필터
CREATE INDEX idx_operating_hospital_id ON hospital_operating_time (hospital_id);

-- hospital_filter: 야간/휴일 필터 서브쿼리
CREATE INDEX idx_hosp_filter_hospital_id ON hospital_filter (hospital_id);

-- hospital_holiday: 휴무일 서브쿼리
CREATE INDEX idx_hosp_holiday_hospital_id ON hospital_holiday (hospital_id);

-- hospital_image: 이미지 로딩
CREATE INDEX idx_hosp_image_hospital_id ON hospital_image (hospital_id);

-- 거리 정렬용 좌표 인덱스
CREATE INDEX idx_address_lat_lng ON hospital_address (latitude, longitude);
