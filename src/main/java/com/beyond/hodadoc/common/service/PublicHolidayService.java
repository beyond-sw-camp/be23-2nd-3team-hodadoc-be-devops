package com.beyond.hodadoc.common.service;

import com.beyond.hodadoc.common.domain.PublicHoliday;
import com.beyond.hodadoc.common.dtos.PublicHolidayApiResponseDto;
import com.beyond.hodadoc.common.dtos.PublicHolidayApiResponseDto.Item;
import com.beyond.hodadoc.common.repository.PublicHolidayRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PublicHolidayService {

    private final PublicHolidayRepository publicHolidayRepository;
    private final ObjectMapper objectMapper;

    @Value("${public-holiday.api.service-key}")
    private String serviceKey;

    @Value("${public-holiday.api.base-url}")
    private String baseUrl;

    private static final DateTimeFormatter LOCDATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public PublicHolidayService(PublicHolidayRepository publicHolidayRepository,
                                ObjectMapper objectMapper) {
        this.publicHolidayRepository = publicHolidayRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void syncYear(int year) {
        log.info("[공휴일 동기화] year={} 시작", year);

        RestClient restClient = RestClient.create();

        String uri = baseUrl
                + "?solYear=" + year
                + "&ServiceKey=" + serviceKey
                + "&_type=json"
                + "&numOfRows=50";

        ResponseEntity<PublicHolidayApiResponseDto> response = restClient.get()
                .uri(uri)
                .retrieve()
                .toEntity(PublicHolidayApiResponseDto.class);

        PublicHolidayApiResponseDto body = response.getBody();
        if (body == null || body.getResponse() == null
                || body.getResponse().getBody() == null
                || body.getResponse().getBody().getItems() == null) {
            log.warn("[공휴일 동기화] API 응답이 비어있음 year={}", year);
            return;
        }

        List<Item> items = normalizeItems(body.getResponse().getBody().getItems().getItem());

        // 해당 연도 기존 데이터 삭제 후 재저장
        publicHolidayRepository.deleteByYear(year);

        for (Item item : items) {
            if (!"Y".equals(item.getIsHoliday())) continue;

            LocalDate date = LocalDate.parse(String.valueOf(item.getLocdate()), LOCDATE_FMT);

            PublicHoliday holiday = PublicHoliday.builder()
                    .holidayDate(date)
                    .dateName(item.getDateName())
                    .year(year)
                    .build();
            publicHolidayRepository.save(holiday);
        }

        log.info("[공휴일 동기화] year={} 완료", year);
    }

    // data.go.kr 응답 특이케이스 처리: 단건이면 Object, 다건이면 List
    private List<Item> normalizeItems(Object itemObj) {
        if (itemObj == null) return Collections.emptyList();
        if (itemObj instanceof List) {
            return objectMapper.convertValue(itemObj, new TypeReference<List<Item>>() {});
        }
        Item single = objectMapper.convertValue(itemObj, Item.class);
        return List.of(single);
    }

    public boolean isPublicHoliday(LocalDate date) {
        return publicHolidayRepository.existsByHolidayDate(date);
    }

    public List<PublicHoliday> getHolidaysForYear(int year) {
        return publicHolidayRepository.findByYear(year);
    }
}
