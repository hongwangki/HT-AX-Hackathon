package haitai.ht_ax_hackathon.service;

import haitai.ht_ax_hackathon.domain.AttachmentFile;
import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.domain.TeamMember;
import haitai.ht_ax_hackathon.dto.ApplicationForm;
import haitai.ht_ax_hackathon.dto.AttachmentDownload;
import haitai.ht_ax_hackathon.dto.TeamMemberForm;
import haitai.ht_ax_hackathon.exception.AttachmentFileNotFoundException;
import haitai.ht_ax_hackathon.exception.ApplicationNotFoundException;
import haitai.ht_ax_hackathon.repository.HackathonApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HackathonApplicationService {

    private final HackathonApplicationRepository applicationRepository;
    private final FileStorageService fileStorageService;

    /** Persists the application graph and cleans up already stored files if persistence fails. */
    @Transactional
    public HackathonApplication createApplication(ApplicationForm form) {
        HackathonApplication application =
                new HackathonApplication(form.getTeamName(), form.getTopic(), form.getContent());

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
        application.update(form.getTeamName(), form.getTopic(), form.getContent());
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
        List<String> filePaths = application.getFiles().stream()
                .map(AttachmentFile::getFilePath)
                .toList();

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
