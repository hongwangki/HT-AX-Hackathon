package haitai.ht_ax_hackathon;

import haitai.ht_ax_hackathon.domain.ApplicationCategory;
import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.service.AiUsageInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiUsageInfoServiceTests {

    private final AiUsageInfoService service = new AiUsageInfoService();

    @Test
    void registersAiUsageForAllTwentySevenTasks() {
        Object registered = ReflectionTestUtils.getField(service, "usageByTask");

        assertThat(registered).isInstanceOf(Map.class);
        assertThat((Map<?, ?>) registered).hasSize(27);
    }

    @Test
    void providesTheUpdatedLongToolLists() {
        assertThat(find("Hi-AI", "AI 기반 제조원가 및 수익성 의사결정 플랫폼"))
                .isEqualTo("PANDAS, XGBoost, OR-TOOLs, 로컬 ollama-Gemma (로컬 LLM), Streamlit, Visual Studio Code, Python");
        assertThat(find("H-AI T-AI", "사내 맞춤형 신제품 AI 렌더링 봇"))
                .isEqualTo("OpenAI GPT(챗gpt plus) + Google Gemini(pro), Gemma 3 4B (Ollama)");
    }

    @Test
    void distinguishesSameTeamTasksAndNormalizesTitleWhitespace() {
        assertThat(find(
                "Oh-Yes, No-Loss",
                "AI Loss-Cut Report : 일일 폐기파 손실 원가 분석 및 절감 제안 시스템"
        )).isEqualTo("ChatGPT");
        assertThat(find(
                "Oh-Yes, No-Loss",
                "AI 기반 크림 Just-In-Time 배합 시스템"
        )).isEqualTo("PyTorch");
        assertThat(find(
                "안전보건관리부",
                "안전보건관리 효율성 증진을 위한 AI 챗봇 및\n프롬프트 라이브러리 활용"
        )).isEqualTo("ChatGPT");
    }

    private String find(String teamName, String topic) {
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
