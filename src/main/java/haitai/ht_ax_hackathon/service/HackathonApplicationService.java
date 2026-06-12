package haitai.ht_ax_hackathon.service;

import haitai.ht_ax_hackathon.domain.ApplicationStatus;
import haitai.ht_ax_hackathon.domain.AttachmentFile;
import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.domain.TeamMember;
import haitai.ht_ax_hackathon.dto.ApplicationForm;
import haitai.ht_ax_hackathon.dto.AttachmentDownload;
import haitai.ht_ax_hackathon.dto.TeamMemberForm;
import haitai.ht_ax_hackathon.exception.AttachmentFileNotFoundException;
import haitai.ht_ax_hackathon.exception.ApplicationNotFoundException;
import haitai.ht_ax_hackathon.repository.HackathonApplicationRepository;
import haitai.ht_ax_hackathon.repository.TaskSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HackathonApplicationService {

    private final HackathonApplicationRepository applicationRepository;
    private final TaskSubmissionRepository submissionRepository;
    private final FileStorageService fileStorageService;

    /** Persists the application graph and cleans up already stored files if persistence fails. */
    @Transactional
    public HackathonApplication createApplication(ApplicationForm form) {
        HackathonApplication application = new HackathonApplication(
                form.getTeamName(),
                form.getCategory(),
                form.getTopic(),
                form.getContent(),
                normalizePhone(form.getRepresentativePhone()),
                form.getPassword()
        );

        for (TeamMemberForm memberForm : form.getMembers()) {
            application.addMember(new TeamMember(
                    memberForm.getDepartment(),
                    memberForm.getEmployeeNo(),
                    memberForm.getName()
            ));
        }

        List<AttachmentFile> storedFiles = new ArrayList<>();
        try {
            for (MultipartFile multipartFile : form.getAttachments()) {
                if (multipartFile != null && !multipartFile.isEmpty()) {
                    AttachmentFile attachmentFile = fileStorageService.store(multipartFile);
                    storedFiles.add(attachmentFile);
                    application.addFile(attachmentFile);
                }
            }
            return applicationRepository.save(application);
        } catch (RuntimeException exception) {
            storedFiles.forEach(file -> fileStorageService.deleteQuietly(file.getFilePath()));
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ApplicationForm getEditForm(Long id) {
        HackathonApplication application = findApplication(id);
        ApplicationForm form = new ApplicationForm();
        form.setTeamName(application.getTeamName());
        form.setRepresentativePhone(application.getRepresentativePhone());
        form.setCategory(application.getCategory());
        form.setTopic(application.getTopic());
        form.setContent(application.getContent());
        form.setMembers(application.getMembers().stream()
                .map(this::toMemberForm)
                .toList());
        return form;
    }

    /** Updates editable values, replaces member rows, and appends newly uploaded files. */
    @Transactional
    public void updateApplication(Long id, ApplicationForm form) {
        HackathonApplication application = findById(id);
        application.update(
                form.getTeamName(),
                form.getCategory(),
                form.getTopic(),
                form.getContent(),
                normalizePhone(form.getRepresentativePhone())
        );
        application.replaceMembers(form.getMembers().stream()
                .map(member -> new TeamMember(member.getDepartment(), member.getEmployeeNo(), member.getName()))
                .toList());

        List<AttachmentFile> storedFiles = new ArrayList<>();
        try {
            for (MultipartFile multipartFile : form.getAttachments()) {
                if (multipartFile != null && !multipartFile.isEmpty()) {
                    AttachmentFile attachmentFile = fileStorageService.store(multipartFile);
                    storedFiles.add(attachmentFile);
                    application.addFile(attachmentFile);
                }
            }
        } catch (RuntimeException exception) {
            storedFiles.forEach(file -> fileStorageService.deleteQuietly(file.getFilePath()));
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<HackathonApplication> findAllApplications() {
        return applicationRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Returns the applicant's full submission history for the status page, newest first.
     * One matching password unlocks every row for the phone number, because re-submissions
     * are forced to reuse the same password. An empty list means no application exists for
     * the phone number or the password is wrong — the two cases are intentionally
     * indistinguishable to the caller.
     */
    @Transactional(readOnly = true)
    public List<HackathonApplication> findMyApplications(String representativePhone, String rawPassword) {
        List<HackathonApplication> applications = applicationRepository
                .findByRepresentativePhoneInOrderByCreatedAtDesc(phoneLookupKeys(representativePhone));
        boolean authenticated = applications.stream()
                .anyMatch(application -> rawPassword.equals(application.getPassword()));
        return authenticated ? applications : List.of();
    }

    /**
     * A phone number keeps a single password across re-submissions so applicants always see
     * their full history with one credential pair. Rows with a blank password (data predating
     * the password feature) are ignored rather than locking the phone number out forever.
     */
    @Transactional(readOnly = true)
    public boolean passwordConflictsWithExisting(String representativePhone, String rawPassword) {
        List<HackathonApplication> existing = applicationRepository
                .findByRepresentativePhoneInOrderByCreatedAtDesc(phoneLookupKeys(representativePhone))
                .stream()
                .filter(application -> application.getPassword() != null
                        && !application.getPassword().isBlank())
                .toList();
        return !existing.isEmpty() && existing.stream()
                .noneMatch(application -> rawPassword.equals(application.getPassword()));
    }

    @Transactional
    public void changeStatus(Long id, ApplicationStatus status) {
        findById(id).changeStatus(status);
    }

    /**
     * Admin recovery path for applicants who forgot their password: looks the stored
     * password up by phone number so the admin can relay it. Blank passwords
     * (data predating the password feature) are skipped. Empty when the phone is unknown.
     */
    @Transactional(readOnly = true)
    public Optional<String> findPasswordByPhone(String representativePhone) {
        return applicationRepository
                .findByRepresentativePhoneInOrderByCreatedAtDesc(phoneLookupKeys(representativePhone))
                .stream()
                .map(HackathonApplication::getPassword)
                .filter(password -> password != null && !password.isBlank())
                .findFirst();
    }

    @Transactional(readOnly = true)
    public List<HackathonApplication> findAllApplicationsForExport() {
        List<HackathonApplication> applications = applicationRepository.findAllByOrderByCreatedAtDesc();
        applications.forEach(application -> application.getFiles().size());
        return applications;
    }

    @Transactional(readOnly = true)
    public HackathonApplication findApplication(Long id) {
        HackathonApplication application = findById(id);
        // Initialize both collections while the transaction is open for Thymeleaf rendering.
        application.getMembers().size();
        application.getFiles().size();
        return application;
    }

    /** Cascades member/file row deletion and then best-effort deletes physical files. */
    @Transactional
    public void deleteApplication(Long id) {
        HackathonApplication application = findApplication(id);
        List<String> filePaths = new ArrayList<>(application.getFiles().stream()
                .map(AttachmentFile::getFilePath)
                .toList());

        // A task submission (if any) holds an FK to this application and must be removed first.
        submissionRepository.findByApplicationId(id).ifPresent(submission -> {
            submission.getFiles().forEach(file -> filePaths.add(file.getFilePath()));
            submissionRepository.delete(submission);
        });

        applicationRepository.delete(application);
        applicationRepository.flush();
        filePaths.forEach(fileStorageService::deleteQuietly);
    }

    @Transactional(readOnly = true)
    public AttachmentDownload getAttachmentDownload(Long applicationId, Long fileId) {
        HackathonApplication application = findApplication(applicationId);
        AttachmentFile file = findAttachment(application, fileId);
        return new AttachmentDownload(
                fileStorageService.loadAsResource(file.getFilePath()),
                file.getOriginalFileName(),
                file.getContentType()
        );
    }

    @Transactional
    public void deleteAttachment(Long applicationId, Long fileId) {
        HackathonApplication application = findById(applicationId);
        AttachmentFile file = findAttachment(application, fileId);
        String filePath = file.getFilePath();

        application.removeFile(file);
        applicationRepository.flush();
        fileStorageService.deleteQuietly(filePath);
    }

    /** Keeps only digits so mobile numeric keyboards can be used without hyphens. */
    private String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("\\D", "");
    }

    private List<String> phoneLookupKeys(String phone) {
        String normalized = normalizePhone(phone);
        if (normalized == null || normalized.isBlank()) {
            return List.of("");
        }
        if (normalized.length() == 11) {
            return List.of(
                    normalized,
                    normalized.replaceFirst("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3")
            );
        }
        return List.of(normalized);
    }

    private TeamMemberForm toMemberForm(TeamMember member) {
        TeamMemberForm form = new TeamMemberForm();
        form.setDepartment(member.getDepartment());
        form.setEmployeeNo(member.getEmployeeNo());
        form.setName(member.getName());
        return form;
    }

    private HackathonApplication findById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    private AttachmentFile findAttachment(HackathonApplication application, Long fileId) {
        return application.getFiles().stream()
                .filter(item -> item.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new AttachmentFileNotFoundException(fileId));
    }
}
