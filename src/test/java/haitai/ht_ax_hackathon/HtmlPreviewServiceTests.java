package haitai.ht_ax_hackathon;

import haitai.ht_ax_hackathon.config.FileUploadProperties;
import haitai.ht_ax_hackathon.domain.ApplicationCategory;
import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.domain.TaskSubmission;
import haitai.ht_ax_hackathon.domain.TaskSubmissionFile;
import haitai.ht_ax_hackathon.dto.DemoAccessInfo;
import haitai.ht_ax_hackathon.repository.TaskSubmissionRepository;
import haitai.ht_ax_hackathon.service.FileStorageService;
import haitai.ht_ax_hackathon.service.HtmlPreviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HtmlPreviewServiceTests {

    @TempDir
    Path tempDirectory;

    @Test
    void findsAnArbitrarilyNamedHtmlInsideZipAndServesItsAssets() throws IOException {
        Path zipPath = tempDirectory.resolve("demo-package.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath), StandardCharsets.UTF_8)) {
            writeEntry(zip, "web/showcase-page.html", "<link rel=\"stylesheet\" href=\"css/site.css\"><h1>시연</h1>");
            writeEntry(zip, "web/css/site.css", "h1 { color: red; }");
        }

        TestFixture fixture = fixture("demo-package.zip", zipPath);
        var preview = fixture.service().findPreview(fixture.submission(), DemoAccessInfo.noLoginRequired())
                .orElseThrow();

        assertThat(preview.previewUrl()).endsWith("/preview/web/showcase-page.html");
        assertThat(preview.displayFileName()).isEqualTo("showcase-page.html");
        assertThat(preview.archive()).isTrue();
        assertThat(fixture.service().loadPreviewContent(1L, 10L, "web/showcase-page.html").contentType())
                .startsWith("text/html");
        assertThat(new String(
                fixture.service().loadPreviewContent(1L, 10L, "web/css/site.css").content(),
                StandardCharsets.UTF_8
        )).contains("color: red");
    }

    @Test
    void exposesEveryHtmlWhenZipContainsDifferentResults() throws IOException {
        Path zipPath = tempDirectory.resolve("ambiguous.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath), StandardCharsets.UTF_8)) {
            writeEntry(zip, "first.html", "first");
            writeEntry(zip, "second.html", "second");
        }

        TestFixture fixture = fixture("ambiguous.zip", zipPath);

        assertThat(fixture.service().findPreviews(fixture.submission(), DemoAccessInfo.noLoginRequired()))
                .extracting(preview -> preview.displayFileName())
                .containsExactly("first.html", "second.html");
    }

    @Test
    void keepsOnlyTheLatestDatedHtmlForTheSameResult() throws IOException {
        Path zipPath = tempDirectory.resolve("trend-results.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath), StandardCharsets.UTF_8)) {
            writeEntry(zip, "results/keyword_lifecycle_20260803.html", "old");
            writeEntry(zip, "results/keyword_lifecycle_20260804.html", "older");
            writeEntry(zip, "results/keyword_lifecycle_20260805.html", "latest");
            writeEntry(zip, "results/market_overview_20260805.html", "different result");
        }

        TestFixture fixture = fixture("trend-results.zip", zipPath);

        assertThat(fixture.service().findPreviews(fixture.submission(), DemoAccessInfo.noLoginRequired()))
                .extracting(preview -> preview.displayFileName())
                .containsExactly("keyword_lifecycle_20260805.html", "market_overview_20260805.html");
    }

    @Test
    void keepsOnlyTheLatestDatedHtmlWhenVersionsAreSeparateAttachments() throws IOException {
        Path oldPath = tempDirectory.resolve("keyword_lifecycle_20260804.html");
        Path latestPath = tempDirectory.resolve("keyword_lifecycle_20260805.html");
        Files.writeString(oldPath, "old");
        Files.writeString(latestPath, "latest");
        TestFixture fixture = fixture(oldPath.getFileName().toString(), oldPath);
        TaskSubmissionFile latestFile = new TaskSubmissionFile(
                latestPath.getFileName().toString(),
                latestPath.getFileName().toString(),
                latestPath.toString(),
                Files.size(latestPath),
                "text/html"
        );
        ReflectionTestUtils.setField(latestFile, "id", 11L);
        fixture.submission().addFile(latestFile);

        assertThat(fixture.service().findPreviews(fixture.submission(), DemoAccessInfo.noLoginRequired()))
                .extracting(preview -> preview.displayFileName())
                .containsExactly("keyword_lifecycle_20260805.html");
    }

    @Test
    void exposesOnlyTheDesignatedMarketingTrendHtml() throws IOException {
        Path zipPath = tempDirectory.resolve("marketing-trend-results.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath), StandardCharsets.UTF_8)) {
            writeEntry(zip, "results/keyword_lifecycle_20260804.html", "old");
            writeEntry(zip, "results/keyword_lifecycle_20260805.html", "selected");
            writeEntry(zip, "backup/results/keyword_lifecycle_20260805.html", "duplicate");
            writeEntry(zip, "results/market_overview_20260805.html", "not selected");
        }
        TestFixture fixture = fixture(
                "marketing-trend-results.zip",
                zipPath,
                "마케팅기획부",
                "유튜브 기반 FMCG 트렌드 조기 포착 자동화 시스템"
        );

        assertThat(fixture.service().findPreviews(fixture.submission(), DemoAccessInfo.noLoginRequired()))
                .extracting(preview -> preview.displayFileName())
                .containsExactly("keyword_lifecycle_20260805.html");
        assertThat(fixture.service().findPreviews(fixture.submission(), DemoAccessInfo.noLoginRequired()))
                .singleElement()
                .satisfies(preview -> assertThat(preview.previewUrl())
                        .endsWith("/preview/results/keyword_lifecycle_20260805.html"));
    }

    @Test
    void doesNotPromoteFilesForTasksMarkedAsHavingNoDemo() throws IOException {
        Path htmlPath = tempDirectory.resolve("demo.html");
        Files.writeString(htmlPath, "<h1>demo</h1>");
        TestFixture fixture = fixture("demo.html", htmlPath);

        assertThat(fixture.service().findPreview(
                fixture.submission(),
                DemoAccessInfo.noDemo("시연 페이지가 없습니다.")
        )).isEmpty();
    }

    @Test
    void detectsHtmlFromTheActualFileEvenWhenTaskMetadataDoesNotMatch() throws IOException {
        Path zipPath = tempDirectory.resolve("데모파일.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath), StandardCharsets.UTF_8)) {
            writeEntry(zip, "결과물/시연화면.html", "<h1>demo</h1>");
        }
        TestFixture fixture = fixture("데모파일.zip", zipPath);

        var preview = fixture.service().findPreview(fixture.submission(), null).orElseThrow();

        assertThat(preview.displayFileName()).isEqualTo("시연화면.html");
        assertThat(preview.archive()).isTrue();
    }

    private TestFixture fixture(String originalFileName, Path filePath) {
        return fixture(originalFileName, filePath, "테스트팀", "테스트 과제");
    }

    private TestFixture fixture(String originalFileName, Path filePath, String teamName, String topic) {
        HackathonApplication application = new HackathonApplication(
                teamName,
                ApplicationCategory.MARKETING,
                topic,
                "내용",
                "010-0000-0000",
                "password"
        );
        ReflectionTestUtils.setField(application, "id", 1L);
        TaskSubmission submission = new TaskSubmission(application, null, null);
        TaskSubmissionFile file = new TaskSubmissionFile(
                originalFileName,
                filePath.getFileName().toString(),
                filePath.toString(),
                Files.exists(filePath) ? filePath.toFile().length() : 0,
                "application/octet-stream"
        );
        ReflectionTestUtils.setField(file, "id", 10L);
        submission.addFile(file);

        TaskSubmissionRepository repository = mock(TaskSubmissionRepository.class);
        when(repository.findDetailByApplicationId(1L)).thenReturn(Optional.of(submission));
        FileUploadProperties properties = new FileUploadProperties();
        properties.setUploadDir(tempDirectory.toString());
        HtmlPreviewService service = new HtmlPreviewService(repository, new FileStorageService(properties));
        return new TestFixture(service, submission);
    }

    private void writeEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private record TestFixture(HtmlPreviewService service, TaskSubmission submission) {
    }
}
