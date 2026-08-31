package haitai.ht_ax_hackathon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * 마감 판정에 쓰는 시계입니다. 빈으로 두면 테스트가 고정 시각을 주입할 수 있어,
     * 마감일이 지났다는 이유로 테스트가 깨지지 않습니다.
     */
    @Bean
    public Clock clock() {
        return Clock.system(SERVICE_ZONE);
    }
}
