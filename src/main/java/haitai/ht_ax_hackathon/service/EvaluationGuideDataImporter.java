package haitai.ht_ax_hackathon.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import haitai.ht_ax_hackathon.domain.EvaluationGuideItem;
import haitai.ht_ax_hackathon.domain.EvaluationGuideOverview;
import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.repository.EvaluationGuideItemRepository;
import haitai.ht_ax_hackathon.repository.EvaluationGuideOverviewRepository;
import haitai.ht_ax_hackathon.repository.HackathonApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationGuideDataImporter implements ApplicationRunner {

    private static final String GUIDE_RESOURCE = "evaluation-guides.json";

    private final ObjectMapper objectMapper;
    private final HackathonApplicationRepository applicationRepository;
    private final EvaluationGuideOverviewRepository overviewRepository;
    private final EvaluationGuideItemRepository itemRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        GuideSeedData seedData = readSeedData();
        removeUnevaluatedApplications(seedData.excludedApplicationIds());

        int importedCount = 0;
        for (GuideSeed guide : seedData.guides()) {
            HackathonApplication application = applicationRepository.findById(guide.applicationId())
                    .orElse(null);
            if (application == null) {
                continue;
            }
            upsertOverview(application, guide);
            guide.items().forEach(item -> upsertItem(application, item));
            importedCount++;
        }
        log.info("평가 가이드 초기 적재 완료: {}팀", importedCount);
    }

    private GuideSeedData readSeedData() throws IOException {
        ClassPathResource resource = new ClassPathResource(GUIDE_RESOURCE);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() { });
        }
    }

    private void removeUnevaluatedApplications(List<Long> applicationIds) {
        for (Long applicationId : applicationIds) {
            itemRepository.deleteByApplicationId(applicationId);
            if (overviewRepository.existsById(applicationId)) {
                overviewRepository.deleteById(applicationId);
            }
        }
    }

    private void upsertOverview(HackathonApplication application, GuideSeed guide) {
        EvaluationGuideOverview overview = overviewRepository.findById(application.getId())
                .orElseGet(() -> new EvaluationGuideOverview(
                        application,
                        guide.oneLineSummary(),
                        guide.sourceTotalScore()
                ));
        overview.update(guide.oneLineSummary(), guide.sourceTotalScore());
        overviewRepository.save(overview);
    }

    private void upsertItem(HackathonApplication application, GuideItemSeed seed) {
        EvaluationGuideItem item = itemRepository
                .findByApplicationIdAndCriterionCode(application.getId(), seed.criterionCode())
                .orElseGet(() -> new EvaluationGuideItem(
                        application,
                        seed.criterionCode(),
                        seed.criterionName(),
                        seed.maxScore(),
                        seed.rawLevel(),
                        seed.weightedScore(),
                        seed.criterionText(),
                        seed.detailComment(),
                        seed.displayOrder()
                ));
        item.update(
                seed.criterionName(),
                seed.maxScore(),
                seed.rawLevel(),
                seed.weightedScore(),
                seed.criterionText(),
                seed.detailComment(),
                seed.displayOrder()
        );
        itemRepository.save(item);
    }

    private record GuideSeedData(
            List<Long> excludedApplicationIds,
            List<GuideSeed> guides
    ) {
    }

    private record GuideSeed(
            Long applicationId,
            String teamName,
            String oneLineSummary,
            Integer sourceTotalScore,
            List<GuideItemSeed> items
    ) {
    }

    private record GuideItemSeed(
            String criterionCode,
            String criterionName,
            int maxScore,
            Integer rawLevel,
            Integer weightedScore,
            String criterionText,
            String detailComment,
            int displayOrder
    ) {
    }
}
