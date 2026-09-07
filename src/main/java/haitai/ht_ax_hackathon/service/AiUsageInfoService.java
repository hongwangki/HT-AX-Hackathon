package haitai.ht_ax_hackathon.service;

import haitai.ht_ax_hackathon.domain.HackathonApplication;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AiUsageInfoService {

    private final Map<String, String> usageByTask = new HashMap<>();

    public AiUsageInfoService() {
        register("천안공장 AI팀", "AI 품질검사 자동화 시스템 도입", "Chat GPT");
        register("SafeMask AI Agent", "SafeMask AI Agent: 회사자료를 안전하게 활용하는 업무 효율화 AI Agent", "Claude AI, Chat GPT, Manus AI");
        register("캐치캐치", "AI 기반 소셜미디어 콘텐츠 분석을 통한 콘텐츠 기획 업무 효율화", "Claude");
        register("총무팀2", "AI 해태 챗봇", "CHAT GPT, CODEX");
        register("하이퍼루프", "분절된 프로세스의 연결: LLM 에이전트를 활용한 신제품 기획 효율화 과제", "Claude");
        register("마케팅기획부", "유튜브 기반 FMCG 트렌드 조기 포착 자동화 시스템", "Claude");
        register("End-to-End", "사재기 없는 스마트 예산 관리: AI 전산용지 수요 예측 및 자동 배정 시스템", "ChatGPT 및 OpenAI Codex(개발 지원), Prophet(수요예측 모델)");
        register("Hi-AI", "AI 기반 제조원가 및 수익성 의사결정 플랫폼", "PANDAS, XGBoost, OR-TOOLs, 로컬 ollama-Gemma (로컬 LLM), Streamlit, Visual Studio Code, Python");
        register("아)품질관리팀", "에이스 안정운전 조건 도출 시스템 개발", "Chat GPT(주 사용), Claude");
        register("과자로세계통일", "AI 기반 고객점 진단 및 이상징후 자동 알림 시스템", "챗GPT, MAKE");
        register("팀 홍병우", "AI 기반 글로벌 정보 공백 해소 및 바이어 매칭을 통한 해외 영업 가속화 방안", "Claude");
        register("안평 호랑이", "AI 기반 신제품 법규 사전검토 Agent", "Claude AI, Chat GPT");
        register("센서리프로젝트", "AI 기반 감각검사 결과 예측 및 배합 최적화 시스템 모델링을 통한 미래형 제품 개발 시스템 구축", "Claude");
        register("구매 인텔리전스", "MS Copilot을 이용한 구매 인텔리전스 구축", "MS copliot, Chatgpt Plus, Claude Pro");
        register("버터링", "원료 정보 및 식품 법규 자동 로드와 가상 공정 시뮬레이션을 통한 신제품 출시 속도 극대", "Claude");
        register("니맘두 내맘두 고향만두", "영업 데이터 통합 자동화 구축, 다각적인 AI분석을 통한 영업 활용", "Claude");
        register("Oh-Yes, No-Loss", "AI Loss-Cut Report : 일일 폐기파 손실 원가 분석 및 절감 제안 시스템", "ChatGPT");
        register("H-AI T-AI", "사내 맞춤형 신제품 AI 렌더링 봇", "OpenAI GPT(챗gpt plus) + Google Gemini(pro), Gemma 3 4B (Ollama)");
        register("안전보건관리부", "안전보건관리 효율성 증진을 위한 AI 챗봇 및 프롬프트 라이브러리 활용", "ChatGPT");
        register("안평1팀", "표시사항 사전검토 AI 어시스턴트 — 법규 및 식약처 공식 자료 원문 근거로 BW 표시를 1차 점검하는 사내 도구", "Claude");
        register("지름길", "AI 기반의 클레임 원인 추정 및 자동 라우팅을 통한 CS 프로세스 효율화 제안", "Claude");
        register("Oh-Yes, No-Loss", "AI 기반 크림 Just-In-Time 배합 시스템", "PyTorch");
        register("김규섭", "ChatGPT & Gemini 하이브리드 모델을 활용한 신제품 개발 프로세스 효율화", "Chat gpt, Google Gemini");
        register("신준섭", "AI 기반 실적 업로드형 영업실적분석 및 모니터링 표준 대시보드", "Google Antigravity");
        register("기술품질TF", "AI 기반 클레임 관리 플랫폼", "Claude, ChatGPT, Gemini");
        register("회계팀", "AI 기반 전표 증빙 검증 자동화 시스템 구축", "Claude");
        register("분석팀", "분석결과 자동 판정 어시스턴트 — 표시사항·기준규격 적합/부적합 자동 판정", "Claude");
    }

    public Optional<String> findFor(HackathonApplication application) {
        return Optional.ofNullable(usageByTask.get(key(application.getTeamName(), application.getTopic())));
    }

    private void register(String teamName, String topic, String usage) {
        usageByTask.put(key(teamName, topic), usage.strip());
    }

    private String key(String teamName, String topic) {
        return normalize(teamName) + "\u0000" + normalize(topic);
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().replaceAll("\\s+", " ");
    }
}
