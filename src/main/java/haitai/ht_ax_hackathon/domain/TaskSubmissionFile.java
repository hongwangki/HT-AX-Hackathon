package haitai.ht_ax_hackathon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_submission_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskSubmissionFile {

    @Id
    @SequenceGenerator(
            name = "task_submission_files_seq",
            sequenceName = "seq_task_submission_files",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_submission_files_seq")
    private Long id;

    @Column(nullable = false)
    @Nationalized
    private String originalFileName;

    @Column(nullable = false)
    @Nationalized
    private String storedFileName;

    @Column(nullable = false, length = 1000)
    @Nationalized
    private String filePath;

    @Column(nullable = false)
    private long fileSize;

    @Nationalized
    private String contentType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private TaskSubmission submission;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TaskSubmissionFile(String originalFileName, String storedFileName, String filePath,
                              long fileSize, String contentType) {
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }

    void assignSubmission(TaskSubmission submission) {
        this.submission = submission;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
