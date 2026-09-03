package haitai.ht_ax_hackathon.service;

import haitai.ht_ax_hackathon.domain.TaskSubmission;
import haitai.ht_ax_hackathon.domain.TaskSubmissionFile;
import haitai.ht_ax_hackathon.dto.DemoAccessInfo;
import haitai.ht_ax_hackathon.dto.HtmlPreview;
import haitai.ht_ax_hackathon.dto.HtmlPreviewContent;
import haitai.ht_ax_hackathon.exception.AttachmentFileNotFoundException;
import haitai.ht_ax_hackathon.repository.TaskSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@RequiredArgsConstructor
public class HtmlPreviewService {

    private static final int MAX_ZIP_ENTRIES = 10_000;
    private static final long MAX_PREVIEW_ENTRY_SIZE = 50L * 1024 * 1024;
    private static final String MARKETING_TREND_TEAM = "마케팅기획부";
    private static final String MARKETING_TREND_TOPIC = "유튜브 기반 FMCG 트렌드 조기 포착 자동화 시스템";
    private static final String MARKETING_TREND_PREVIEW = "keyword_lifecycle_20260805.html";
    private static final Pattern DATED_HTML_FILE = Pattern.compile(
            "^(.*?)(?:[_-])(\\d{8})(\\.html?)$",
            Pattern.CASE_INSENSITIVE
    );

    private final TaskSubmissionRepository submissionRepository;
    private final FileStorageService fileStorageService;

    public List<HtmlPreview> findPreviews(TaskSubmission submission, DemoAccessInfo accessInfo) {
        if (accessInfo != null && accessInfo.type() == DemoAccessInfo.Type.NO_DEMO) {
            return List.of();
        }

        String requiredPreviewFileName = isMarketingTrendTask(submission)
                ? MARKETING_TREND_PREVIEW
                : null;
        List<PreviewCandidate> candidates = new ArrayList<>(submission.getFiles().stream()
                .filter(file -> hasExtension(file.getOriginalFileName(), ".html", ".htm"))
                .filter(file -> requiredPreviewFileName == null
                        || requiredPreviewFileName.equalsIgnoreCase(file.getOriginalFileName()))
                .map(file -> new PreviewCandidate(file, sanitizeEntryPath(file.getOriginalFileName())))
                .toList());

        for (TaskSubmissionFile file : submission.getFiles()) {
            if (!hasExtension(file.getOriginalFileName(), ".zip")) continue;
            findZipHtmlPages(file, requiredPreviewFileName).stream()
                    .map(entry -> new PreviewCandidate(file, entry))
                    .forEach(candidates::add);
        }
        if (requiredPreviewFileName != null) {
            return candidates.stream()
                    .min(Comparator.comparingInt(candidate -> pathDepth(candidate.entryPath())))
                    .map(candidate -> List.of(toPreview(submission, candidate)))
                    .orElseGet(List::of);
        }
        return keepLatestDatedCandidates(candidates).stream()
                .map(candidate -> toPreview(submission, candidate))
                .toList();
    }

    public Optional<HtmlPreview> findPreview(TaskSubmission submission, DemoAccessInfo accessInfo) {
        return findPreviews(submission, accessInfo).stream().findFirst();
    }

    public boolean usesDesignatedPreview(TaskSubmission submission) {
        return isMarketingTrendTask(submission);
    }

    @Transactional(readOnly = true)
    public HtmlPreviewContent loadPreviewContent(
            Long applicationId,
            Long fileId,
            String requestedEntryPath
    ) {
        TaskSubmission submission = submissionRepository.findDetailByApplicationId(applicationId)
                .orElseThrow(() -> new AttachmentFileNotFoundException(fileId));
        TaskSubmissionFile file = submission.getFiles().stream()
                .filter(item -> item.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new AttachmentFileNotFoundException(fileId));

        if (hasExtension(file.getOriginalFileName(), ".html", ".htm")) {
            String expectedName = sanitizeEntryPath(file.getOriginalFileName());
            if (!expectedName.equals(sanitizeEntryPath(requestedEntryPath))) {
                throw new AttachmentFileNotFoundException(fileId);
            }
            Resource resource = fileStorageService.loadAsResource(file.getFilePath());
            try (InputStream inputStream = resource.getInputStream()) {
                return new HtmlPreviewContent(
                        readLimited(inputStream),
                        expectedName,
                        "text/html;charset=UTF-8"
                );
            } catch (IOException exception) {
                throw new AttachmentFileNotFoundException("HTML 미리보기 파일을 읽을 수 없습니다.");
            }
        }

        if (!hasExtension(file.getOriginalFileName(), ".zip")) {
            throw new AttachmentFileNotFoundException(fileId);
        }
        String safeEntryPath = sanitizeEntryPath(requestedEntryPath);
        try (ZipFile zipFile = openZip(file.getFilePath())) {
            ZipEntry entry = zipFile.getEntry(safeEntryPath);
            if (entry == null || entry.isDirectory() || !isSafeEntry(entry.getName())) {
                throw new AttachmentFileNotFoundException(fileId);
            }
            if (entry.getSize() > MAX_PREVIEW_ENTRY_SIZE) {
                throw new AttachmentFileNotFoundException("미리보기 파일의 크기가 너무 큽니다.");
            }
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                return new HtmlPreviewContent(
                        readLimited(inputStream),
                        fileName(entry.getName()),
                        contentType(entry.getName())
                );
            }
        } catch (AttachmentFileNotFoundException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new AttachmentFileNotFoundException("ZIP 미리보기 파일을 읽을 수 없습니다.");
        }
    }

    private List<String> findZipHtmlPages(TaskSubmissionFile file, String requiredFileName) {
        try (ZipFile zipFile = openZip(file.getFilePath())) {
            List<String> htmlEntries = new ArrayList<>();
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            int entryCount = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (++entryCount > MAX_ZIP_ENTRIES) return List.of();
                if (!entry.isDirectory()
                        && isSafeEntry(entry.getName())
                        && hasExtension(entry.getName(), ".html", ".htm")
                        && (requiredFileName == null
                        || requiredFileName.equalsIgnoreCase(fileName(entry.getName())))) {
                    htmlEntries.add(entry.getName());
                }
            }
            Optional<String> startPage = selectStartPage(htmlEntries, baseName(file.getOriginalFileName()));
            return htmlEntries.stream()
                    .sorted((left, right) -> {
                        if (startPage.isPresent()) {
                            if (left.equals(startPage.get())) return -1;
                            if (right.equals(startPage.get())) return 1;
                        }
                        int depthComparison = Integer.compare(pathDepth(left), pathDepth(right));
                        return depthComparison != 0 ? depthComparison : left.compareToIgnoreCase(right);
                    })
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private boolean isMarketingTrendTask(TaskSubmission submission) {
        return MARKETING_TREND_TEAM.equals(submission.getApplication().getTeamName().strip())
                && MARKETING_TREND_TOPIC.equals(submission.getApplication().getTopic().strip());
    }

    private List<PreviewCandidate> keepLatestDatedCandidates(List<PreviewCandidate> candidates) {
        Map<String, PreviewCandidate> latestBySeries = new LinkedHashMap<>();
        List<PreviewCandidate> undatedCandidates = new ArrayList<>();

        for (PreviewCandidate candidate : candidates) {
            String name = fileName(candidate.entryPath());
            Matcher matcher = DATED_HTML_FILE.matcher(name);
            if (!matcher.matches()) {
                undatedCandidates.add(candidate);
                continue;
            }

            String directory = candidate.entryPath().substring(0, candidate.entryPath().length() - name.length());
            String seriesKey = (directory + matcher.group(1) + matcher.group(3)).toLowerCase(Locale.ROOT);
            latestBySeries.merge(seriesKey, candidate, (current, replacement) ->
                    datedFileVersion(replacement.entryPath()).compareTo(datedFileVersion(current.entryPath())) > 0
                            ? replacement
                            : current
            );
        }

        List<PreviewCandidate> result = new ArrayList<>(undatedCandidates);
        result.addAll(latestBySeries.values());
        return result;
    }

    private String datedFileVersion(String entry) {
        Matcher matcher = DATED_HTML_FILE.matcher(fileName(entry));
        return matcher.matches() ? matcher.group(2) : "";
    }

    private Optional<String> selectStartPage(List<String> entries, String archiveBaseName) {
        if (entries.size() == 1) return Optional.of(entries.get(0));
        if (entries.isEmpty()) return Optional.empty();

        Optional<String> index = uniqueShallowest(entries.stream()
                .filter(entry -> fileName(entry).equalsIgnoreCase("index.html")
                        || fileName(entry).equalsIgnoreCase("index.htm"))
                .toList());
        if (index.isPresent()) return index;

        Optional<String> matchingArchive = uniqueShallowest(entries.stream()
                .filter(entry -> baseName(fileName(entry)).equalsIgnoreCase(archiveBaseName))
                .toList());
        if (matchingArchive.isPresent()) return matchingArchive;

        List<String> commonStartPages = entries.stream()
                .filter(entry -> {
                    String name = baseName(fileName(entry)).toLowerCase(Locale.ROOT);
                    return name.equals("main") || name.equals("home") || name.equals("demo")
                            || name.equals("app") || name.equals("start");
                })
                .toList();
        Optional<String> commonStartPage = uniqueShallowest(commonStartPages);
        if (commonStartPage.isPresent()) return commonStartPage;

        int minimumDepth = entries.stream().mapToInt(this::pathDepth).min().orElse(Integer.MAX_VALUE);
        List<String> shallowest = entries.stream()
                .filter(entry -> pathDepth(entry) == minimumDepth)
                .toList();
        return shallowest.stream().sorted(String::compareToIgnoreCase).findFirst();
    }

    private Optional<String> uniqueShallowest(List<String> entries) {
        if (entries.isEmpty()) return Optional.empty();
        int minimumDepth = entries.stream().mapToInt(this::pathDepth).min().orElse(Integer.MAX_VALUE);
        List<String> shallowest = entries.stream()
                .filter(entry -> pathDepth(entry) == minimumDepth)
                .sorted(String::compareToIgnoreCase)
                .toList();
        return shallowest.size() == 1 ? Optional.of(shallowest.get(0)) : Optional.empty();
    }

    private HtmlPreview toPreview(TaskSubmission submission, PreviewCandidate candidate) {
        String encodedEntry = List.of(candidate.entryPath().split("/", -1)).stream()
                .map(segment -> UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8))
                .reduce((left, right) -> left + "/" + right)
                .orElse("");
        String previewUrl = "/judge/evaluations/" + submission.getApplication().getId()
                + "/files/" + candidate.file().getId() + "/preview/" + encodedEntry;
        return new HtmlPreview(
                candidate.file().getId(),
                fileName(candidate.entryPath()),
                previewUrl,
                hasExtension(candidate.file().getOriginalFileName(), ".zip")
        );
    }

    private byte[] readLimited(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > MAX_PREVIEW_ENTRY_SIZE) {
                throw new IOException("Preview entry exceeds size limit");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private ZipFile openZip(String filePath) throws IOException {
        try {
            return new ZipFile(filePath, StandardCharsets.UTF_8);
        } catch (java.util.zip.ZipException utf8Exception) {
            return new ZipFile(filePath, Charset.forName("MS949"));
        }
    }

    private String contentType(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html;charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css;charset=UTF-8";
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "text/javascript;charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json;charset=UTF-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".ttf")) return "font/ttf";
        if (lower.endsWith(".mp4")) return "video/mp4";
        return "application/octet-stream";
    }

    private boolean hasExtension(String fileName, String... extensions) {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (lower.endsWith(extension)) return true;
        }
        return false;
    }

    private boolean isSafeEntry(String entryPath) {
        if (entryPath == null || entryPath.isBlank()) return false;
        String normalized = entryPath.replace('\\', '/');
        return !normalized.startsWith("/")
                && !normalized.contains("../")
                && !normalized.equals("..")
                && !normalized.contains("\u0000")
                && !normalized.startsWith("__MACOSX/");
    }

    private String sanitizeEntryPath(String entryPath) {
        if (!isSafeEntry(entryPath)) {
            throw new AttachmentFileNotFoundException("잘못된 미리보기 경로입니다.");
        }
        return entryPath.replace('\\', '/');
    }

    private int pathDepth(String path) {
        return (int) path.chars().filter(character -> character == '/').count();
    }

    private String fileName(String path) {
        String normalized = path.replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        return separator < 0 ? normalized : normalized.substring(separator + 1);
    }

    private String baseName(String fileName) {
        int extension = fileName.lastIndexOf('.');
        return extension <= 0 ? fileName : fileName.substring(0, extension);
    }

    private record PreviewCandidate(TaskSubmissionFile file, String entryPath) {
    }
}
