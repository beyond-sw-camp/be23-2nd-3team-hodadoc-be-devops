package com.beyond.hodadoc.common.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync      // 비동기 작업 활성화 ( 예약 응답문자가 느려지면 안 되니까 설정한 것)
@EnableScheduling //스케줄러(2시간 전 리마인더) 활성화
public class AsyncConfig {

    @Bean("smsExecutor")
    public Executor smsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // 평소 대기 일꾼 2명
        executor.setMaxPoolSize(5);   // 바쁘면 최대 5명까지 증가
        executor.setQueueCapacity(50);  // 일꾼 다 바쁘면 최대 50개까지 대기열에 줄 세움
        executor.setThreadNamePrefix("sms-");
        executor.initialize();
        return executor;
    }
}
