package haitai.ht_ax_hackathon.service;

import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.dto.DemoAccessInfo;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class DemoAccessInfoService {

    private final Map<String, DemoAccessInfo> accessInfoByTask = new HashMap<>();

    public DemoAccessInfoService() {
        register(
                "SafeMask AI Agent",
                "SafeMask AI Agent: 회사자료를 안전하게 활용하는 업무 효율화 AI Agent",
                DemoAccessInfo.credentialsWithAdmin("2670090", "@a13688631", "999999", "haitaiai1")
        );
        registerCredentials("총무팀2", "AI 해태 챗봇", "admin", "1234");
        registerCredentials("Hi-AI", "AI 기반 제조원가 및 수익성 의사결정 플랫폼", "0000000", "0000");
        registerCredentials("아)품질관리팀", "에이스 안정운전 조건 도출 시스템 개발", "2470594", "ddong9437!");
        registerCredentials("팀 홍병우", "AI 기반 글로벌 정보 공백 해소 및 바이어 매칭을 통한 해외 영업 가속화 방안", null, "1001");
        registerCredentials("안평1팀", "표시사항 사전검토 AI 어시스턴트 — 법규 및 식약처 공식 자료 원문 근거로 BW 표시를 1차 점검하는 사내 도구", "0000000", "haitai0000");
        registerCredentials("기술품질TF", "AI 기반 클레임 관리 플랫폼", "admin", "admin1234");

        registerNoLogin("캐치캐치", "AI 기반 소셜미디어 콘텐츠 분석을 통한 콘텐츠 기획 업무 효율화");
        registerNoLogin("마케팅기획부", "유튜브 기반 FMCG 트렌드 조기 포착 자동화 시스템");
        registerNoLogin("End-to-End", "사재기 없는 스마트 예산 관리: AI 전산용지 수요 예측 및 자동 배정 시스템");
        registerNoLogin("안평 호랑이", "AI 기반 신제품 법규 사전검토 Agent");
        registerNoLogin("센서리프로젝트", "AI 기반 감각검사 결과 예측 및 배합 최적화 시스템 모델링을 통한 미래형 제품 개발 시스템 구축");
        registerNoLogin("니맘두 내맘두 고향만두", "영업 데이터 통합 자동화 구축, 다각적인 AI분석을 통한 영업 활용");
        registerNoLogin("버터링", "원료 정보 및 식품 법규 자동 로드와 가상 공정 시뮬레이션을 통한 신제품 출시 속도 극대");
        registerNoLogin("신준섭", "AI 기반 실적 업로드형 영업실적분석 및 모니터링 표준 대시보드");
        registerNoLogin("분석팀", "분석결과 자동 판정 어시스턴트 — 표시사항·기준규격 적합/부적합 자동 판정");

        registerNoDemo("천안공장 AI팀", "AI 품질검사 자동화 시스템 도입", "시연 페이지가 없습니다. 별도 어플로 시연합니다.");
        registerNoDemo("하이퍼루프", "분절된 프로세스의 연결: LLM 에이전트를 활용한 신제품 기획 효율화 과제", "시연 페이지가 없습니다. 제출된 시연 영상을 확인해 주세요.");
        registerNoDemo("과자로세계통일", "AI 기반 고객점 진단 및 이상징후 자동 알림 시스템", "시연 페이지가 없습니다. 제출된 시연 영상을 확인해 주세요.");
        registerNoDemo("구매 인텔리전스", "MS Copilot을 이용한 구매 인텔리전스 구축", "시연 페이지가 없습니다.");
        registerNoDemo("Oh-Yes, No-Loss", "AI Loss-Cut Report : 일일 폐기파 손실 원가 분석 및 절감 제안 시스템", "시연 페이지가 없습니다.");
        registerNoDemo("H-AI T-AI", "사내 맞춤형 신제품 AI 렌더링 봇", "시연 페이지가 없습니다. 제출된 시연 영상을 확인해 주세요.");
        registerNoDemo("안전보건관리부", "안전보건관리 효율성 증진을 위한 AI 챗봇 및 프롬프트 라이브러리 활용", "시연 페이지가 없습니다. 제출된 시연 영상을 확인해 주세요.");
        registerNoDemo("지름길", "AI 기반의 클레임 원인 추정 및 자동 라우팅을 통한 CS 프로세스 효율화 제안", "시연 페이지가 없습니다. 제출된 시연 영상을 확인해 주세요.");
        registerNoDemo("Oh-Yes, No-Loss", "AI 기반 크림 Just-In-Time 배합 시스템", "시연 페이지가 없습니다.");
        registerNoDemo("김규섭", "ChatGPT & Gemini 하이브리드 모델을 활용한 신제품 개발 프로세스 효율화", "시연 페이지가 없습니다.");
        registerNoDemo("회계팀", "AI 기반 전표 증빙 검증 자동화 시스템 구축", "시연 페이지가 없습니다.");
    }

    public Optional<DemoAccessInfo> findFor(HackathonApplication application) {
        return Optional.ofNullable(accessInfoByTask.get(key(application.getTeamName(), application.getTopic())));
    }

    private void registerCredentials(String teamName, String topic, String username, String password) {
        register(teamName, topic, DemoAccessInfo.credentials(username, password));
    }

    private void registerNoLogin(String teamName, String topic) {
        register(teamName, topic, DemoAccessInfo.noLoginRequired());
    }

    private void registerNoDemo(String teamName, String topic, String message) {
        register(teamName, topic, DemoAccessInfo.noDemo(message));
    }

    private void register(String teamName, String topic, DemoAccessInfo accessInfo) {
        accessInfoByTask.put(key(teamName, topic), accessInfo);
    }

    private String key(String teamName, String topic) {
        return normalize(teamName) + "\u0000" + normalize(topic);
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().replaceAll("\\s+", " ");
    }
}
