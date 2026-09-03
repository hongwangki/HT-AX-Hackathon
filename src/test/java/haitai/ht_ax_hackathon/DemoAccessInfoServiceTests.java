package haitai.ht_ax_hackathon;

import haitai.ht_ax_hackathon.domain.ApplicationCategory;
import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.dto.DemoAccessInfo;
import haitai.ht_ax_hackathon.service.DemoAccessInfoService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemoAccessInfoServiceTests {

    private final DemoAccessInfoService service = new DemoAccessInfoService();

    @Test
    void providesCredentialsAndSupportsPasswordOnlyAccounts() {
        DemoAccessInfo safeMask = find(
                "SafeMask AI Agent",
                "SafeMask AI Agent: 회사자료를 안전하게 활용하는 업무 효율화 AI Agent"
        );
        assertThat(safeMask.isCredentials()).isTrue();
        assertThat(safeMask.username()).isEqualTo("2670090");
        assertThat(safeMask.password()).isEqualTo("@a13688631");
        assertThat(safeMask.hasAdminAccount()).isTrue();
        assertThat(safeMask.adminUsername()).isEqualTo("999999");
        assertThat(safeMask.adminPassword()).isEqualTo("haitaiai1");

        DemoAccessInfo passwordOnly = find(
                "팀 홍병우",
                "AI 기반 글로벌 정보 공백 해소 및 바이어 매칭을 통한 해외 영업 가속화 방안"
        );
        assertThat(passwordOnly.hasUsername()).isFalse();
        assertThat(passwordOnly.password()).isEqualTo("1001");
        assertThat(passwordOnly.hasAdminAccount()).isFalse();
    }

    @Test
    void distinguishesTasksWithTheSameTeamName() {
        DemoAccessInfo first = find(
                "Oh-Yes, No-Loss",
                "AI Loss-Cut Report : 일일 폐기파 손실 원가 분석 및 절감 제안 시스템"
        );
        DemoAccessInfo second = find(
                "Oh-Yes, No-Loss",
                "AI 기반 크림 Just-In-Time 배합 시스템"
        );

        assertThat(first.type()).isEqualTo(DemoAccessInfo.Type.NO_DEMO);
        assertThat(second.type()).isEqualTo(DemoAccessInfo.Type.NO_DEMO);
    }

    @Test
    void normalizesWhitespaceAndSeparatesNoLoginFromNoDemo() {
        DemoAccessInfo noLogin = find(
                "캐치캐치",
                "AI 기반 소셜미디어 콘텐츠 분석을 통한 콘텐츠 기획 업무 효율화"
        );
        DemoAccessInfo noDemo = find(
                "안전보건관리부",
                "안전보건관리 효율성 증진을 위한 AI 챗봇 및\n프롬프트 라이브러리 활용"
        );

        assertThat(noLogin.isNoLoginRequired()).isTrue();
        assertThat(noDemo.type()).isEqualTo(DemoAccessInfo.Type.NO_DEMO);
        assertThat(noDemo.message()).contains("시연 영상");
    }

    private DemoAccessInfo find(String teamName, String topic) {
        HackathonApplication application = new HackathonApplication(
                teamName,
                ApplicationCategory.MARKETING,
                topic,
                "내용",
                "010-0000-0000",
                "password"
        );
        return service.findFor(application).orElseThrow();
    }
}
