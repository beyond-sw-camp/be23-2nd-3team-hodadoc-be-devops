package com.beyond.hodadoc.common.scheduler;

import com.beyond.hodadoc.common.service.PublicHolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicHolidayScheduler implements CommandLineRunner {

    private final PublicHolidayService publicHolidayService;

    // 앱 시작 시 올해 + 내년 공휴일 동기화
    @Override
    public void run(String... args) {
        int currentYear = LocalDate.now().getYear();
        try {
            publicHolidayService.syncYear(currentYear);
            publicHolidayService.syncYear(currentYear + 1);
            log.info("[공휴일] 앱 시작 시 {}년, {}년 동기화 완료", currentYear, currentYear + 1);
        } catch (Exception e) {
            log.error("[공휴일] 앱 시작 시 동기화 실패 - 서비스 시작은 계속됩니다.", e);
        }
    }

    // 매년 1월 1일 03시 연간 동기화
    @Scheduled(cron = "0 0 3 1 1 *")
    public void yearlySync() {
        int year = LocalDate.now().getYear();
        try {
            publicHolidayService.syncYear(year);
            publicHolidayService.syncYear(year + 1);
            log.info("[공휴일] 연간 스케줄 동기화 완료: {}년, {}년", year, year + 1);
        } catch (Exception e) {
            log.error("[공휴일] 연간 스케줄 동기화 실패", e);
        }
    }
}
