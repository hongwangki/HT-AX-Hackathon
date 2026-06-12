package haitai.ht_ax_hackathon.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
@Table(name = "task_submissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskSubmission {

    @Id
    @SequenceGenerator(
            name = "task_submissions_seq",
            sequenceName = "seq_task_submissions",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_submissions_seq")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private HackathonApplication application;

    @Column(length = 500)
    @Nationalized
    private String summary;

    @Column(length = 500)
    @Nationalized
    private String demoUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<TaskSubmissionFile> files = new ArrayList<>();

    public TaskSubmission(HackathonApplication application, String summary, String demoUrl) {
        this.application = application;
        this.summary = summary;
        this.demoUrl = demoUrl;
    }

    public void updateDetails(String summary, String demoUrl) {
        this.summary = summary;
        this.demoUrl = demoUrl;
        this.updatedAt = LocalDateTime.now();
    }

    /** Keeps both sides of the submission-file relationship synchronized. */
    public void addFile(TaskSubmissionFile file) {
        files.add(file);
        file.assignSubmission(this);
        this.updatedAt = LocalDateTime.now();
    }

    public void removeFile(TaskSubmissionFile file) {
        files.remove(file);
        this.updatedAt = LocalDateTime.now();
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
