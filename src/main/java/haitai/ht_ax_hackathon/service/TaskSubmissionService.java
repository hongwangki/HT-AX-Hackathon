package haitai.ht_ax_hackathon.service;

import haitai.ht_ax_hackathon.domain.ApplicationStatus;
import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.domain.TaskSubmission;
import haitai.ht_ax_hackathon.domain.TaskSubmissionFile;
import haitai.ht_ax_hackathon.dto.AttachmentDownload;
import haitai.ht_ax_hackathon.exception.AttachmentFileNotFoundException;
import haitai.ht_ax_hackathon.repository.HackathonApplicationRepository;
import haitai.ht_ax_hackathon.repository.TaskSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskSubmissionService {

    private final TaskSubmissionRepository submissionRepository;
    private final HackathonApplicationRepository applicationRepository;
    private final HackathonApplicationService applicationService;
    private final FileStorageService fileStorageService;

    /**
     * Re-verifies the applicant's phone+password on every request (the submission flow is
     * stateless) and returns the selected approved application, if any. Empty means wrong
     * credentials, nothing approved, or a mismatched application id — indistinguishable on
     * purpose, like the status page.
     */
    @Transactional(readOnly = true)
    public Optional<HackathonApplication> findApprovedApplication(
            Long applicationId, String representativePhone, String rawPassword) {
        return findApprovedApplications(representativePhone, rawPassword).stream()
                .filter(application -> application.getId().equals(applicationId))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public List<HackathonApplication> findApprovedApplications(String representativePhone, String rawPassword) {
        return applicationService.findMyApplications(representativePhone, rawPassword).stream()
                .filter(application -> application.getStatus() == ApplicationStatus.APPROVED)
                .toList();
    }

    /** Returns the submission with its files initialized for rendering. */
    @Transactional(readOnly = true)
    public Optional<TaskSubmission> findByApplication(Long applicationId) {
        Optional<TaskSubmission> submission = submissionRepository.findByApplicationId(applicationId);
        submission.ifPresent(item -> item.getFiles().size());
        return submission;
    }

    /**
     * Creates the submission on first save and updates it afterwards. Newly stored files are
     * cleaned up from disk if persistence fails, mirroring the application save path.
     */
    @Transactional
    public TaskSubmission saveSubmission(Long applicationId, String summary, String demoUrl,
                                         List<MultipartFile> attachments) {
        HackathonApplication application = applicationRepository.getReferenceById(applicationId);
        TaskSubmission submission = submissionRepository.findByApplicationId(applicationId)
                .orElseGet(() -> new TaskSubmission(application, null, null));
        submission.updateDetails(blankToNull(summary), blankToNull(demoUrl));

        List<TaskSubmissionFile> storedFiles = new ArrayList<>();
        try {
            for (MultipartFile multipartFile : attachments) {
                if (multipartFile != null && !multipartFile.isEmpty()) {
                    TaskSubmissionFile file = fileStorageService.storeSubmissionFile(multipartFile);
                    storedFiles.add(file);
                    submission.addFile(file);
                }
            }
            return submissionRepository.save(submission);
        } catch (RuntimeException exception) {
            storedFiles.forEach(file -> fileStorageService.deleteQuietly(file.getFilePath()));
            throw exception;
        }
    }

    /** Deletes one submitted file; the submission row itself stays so the summary survives. */
    @Transactional
    public void deleteFile(Long applicationId, Long fileId) {
        TaskSubmission submission = submissionRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new AttachmentFileNotFoundException(fileId));
        TaskSubmissionFile file = findFile(submission, fileId);
        String filePath = file.getFilePath();

        submission.removeFile(file);
        submissionRepository.flush();
        fileStorageService.deleteQuietly(filePath);
    }

    /** Submissions keyed by application id for the admin overview of approved teams. */
    @Transactional(readOnly = true)
    public Map<Long, TaskSubmission> findSubmissionsFor(List<HackathonApplication> applications) {
        List<Long> applicationIds = applications.stream().map(HackathonApplication::getId).toList();
        if (applicationIds.isEmpty()) {
            return Map.of();
        }
        return submissionRepository.findWithFilesByApplicationIdIn(applicationIds).stream()
                .collect(Collectors.toMap(item -> item.getApplication().getId(), Function.identity()));
    }

    @Transactional(readOnly = true)
    public List<HackathonApplication> findApprovedApplications() {
        return applicationRepository.findByStatusOrderByCreatedAtDesc(ApplicationStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public AttachmentDownload getFileDownload(Long applicationId, Long fileId) {
        TaskSubmission submission = submissionRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new AttachmentFileNotFoundException(fileId));
        TaskSubmissionFile file = findFile(submission, fileId);
        return new AttachmentDownload(
                fileStorageService.loadAsResource(file.getFilePath()),
                file.getOriginalFileName(),
                file.getContentType()
        );
    }

    private TaskSubmissionFile findFile(TaskSubmission submission, Long fileId) {
        return submission.getFiles().stream()
                .filter(item -> item.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new AttachmentFileNotFoundException(fileId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
