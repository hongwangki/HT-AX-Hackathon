package haitai.ht_ax_hackathon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationGuideSeedTests {

    @Test
    void seedContainsOnlyFullyEvaluatedTeamsAndSixGuideItems() throws Exception {
        JsonNode root;
        try (InputStream inputStream = new ClassPathResource("evaluation-guides.json").getInputStream()) {
            root = new ObjectMapper().readTree(inputStream);
        }

        assertThat(root.get("guides")).hasSize(27);
        assertThat(root.get("excludedApplicationIds")).hasSize(15);

        Set<Long> applicationIds = new HashSet<>();
        for (JsonNode guide : root.get("guides")) {
            long applicationId = guide.get("applicationId").asLong();
            assertThat(applicationIds.add(applicationId)).isTrue();
            assertThat(guide.get("items")).hasSize(6);

            int itemTotal = 0;
            for (JsonNode item : guide.get("items")) {
                itemTotal += item.get("weightedScore").asInt();
                assertThat(item.get("rawLevel").asInt()).isBetween(1, 5);
            }
            assertThat(itemTotal).isEqualTo(guide.get("sourceTotalScore").asInt());
        }

        Set<Long> excludedIds = new HashSet<>();
        root.get("excludedApplicationIds").forEach(node -> excludedIds.add(node.asLong()));
        assertThat(applicationIds).doesNotContainAnyElementsOf(excludedIds);
    }
}
