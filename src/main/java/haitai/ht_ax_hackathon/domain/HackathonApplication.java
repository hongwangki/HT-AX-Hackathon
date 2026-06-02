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

    @Column(nullable = false, length = 200)
    @Nationalized
    private String topic;

    @Nationalized
    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Nationalized
    private ApplicationStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<TeamMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<AttachmentFile> files = new ArrayList<>();

    public HackathonApplication(String teamName, String topic, String content) {
        this.teamName = teamName;
        this.topic = topic;
        this.content = content;
        this.status = ApplicationStatus.SUBMITTED;
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

    /** Updates editable fields while preserving status and existing attachments. */
    public void update(String teamName, String topic, String content) {
        this.teamName = teamName;
        this.topic = topic;
        this.content = content;
    }

    /** Replaces member rows so the submitted edit form becomes the source of truth. */
    public void replaceMembers(List<TeamMember> newMembers) {
        members.clear();
        newMembers.forEach(this::addMember);
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
