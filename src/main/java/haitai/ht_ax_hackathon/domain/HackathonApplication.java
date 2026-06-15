package haitai.ht_ax_hackathon.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hackathon_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HackathonApplication {

    @Id
    @SequenceGenerator(
            name = "hackathon_applications_seq",
            sequenceName = "seq_hackathon_applications",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hackathon_applications_seq")
    private Long id;

    @Column(nullable = false, length = 100)
    @Nationalized
    private String teamName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Nationalized
    private ApplicationCategory category;

    @Column(nullable = false, length = 200)
    @Nationalized
    private String topic;

    @Nationalized
    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String content;

    @Column(nullable = false, length = 20)
    @Nationalized
    private String representativePhone;

    /** Stored as plain text so admins can look it up for applicants who forgot it. */
    @Column(nullable = false, length = 72)
    @Nationalized
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Nationalized
    private ApplicationStatus status;

    @Column(name = "rejection_reason", length = 1000)
    @Nationalized
    private String rejectionReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<TeamMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<AttachmentFile> files = new ArrayList<>();

    public HackathonApplication(
            String teamName,
            ApplicationCategory category,
            String topic,
            String content,
            String representativePhone,
            String password
    ) {
        this.teamName = teamName;
        this.category = category;
        this.topic = topic;
        this.content = content;
        this.representativePhone = representativePhone;
        this.password = password;
        this.status = ApplicationStatus.SUBMITTED;
    }

    public void changeStatus(ApplicationStatus status) {
        this.status = status;
        if (status != ApplicationStatus.REJECTED) {
            this.rejectionReason = null;
        }
    }

    public void reject(String rejectionReason) {
        this.status = ApplicationStatus.REJECTED;
        this.rejectionReason = normalizeRejectionReason(rejectionReason);
    }

    /** Keeps both sides of the application-member relationship synchronized. */
    public void addMember(TeamMember member) {
        members.add(member);
        member.assignApplication(this);
    }

    /** Keeps both sides of the application-file relationship synchronized. */
    public void addFile(AttachmentFile file) {
        files.add(file);
        file.assignApplication(this);
    }

    public void removeFile(AttachmentFile file) {
        files.remove(file);
    }

    /** Updates editable fields while preserving status, password, and existing attachments. */
    public void update(String teamName, ApplicationCategory category, String topic, String content, String representativePhone) {
        this.teamName = teamName;
        this.category = category;
        this.topic = topic;
        this.content = content;
        this.representativePhone = representativePhone;
    }

    /** Replaces member rows so the submitted edit form becomes the source of truth. */
    public void replaceMembers(List<TeamMember> newMembers) {
        members.clear();
        newMembers.forEach(this::addMember);
    }

    private String normalizeRejectionReason(String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            return null;
        }
        return rejectionReason.strip();
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
