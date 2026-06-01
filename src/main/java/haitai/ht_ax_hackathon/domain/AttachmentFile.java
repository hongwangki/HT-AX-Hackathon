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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "attachment_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttachmentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false, length = 1000)
    private String filePath;

    @Column(nullable = false)
    private long fileSize;

    private String contentType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private HackathonApplication application;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AttachmentFile(String originalFileName, String storedFileName, String filePath,
                          long fileSize, String contentType) {
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }

    void assignApplication(HackathonApplication application) {
        this.application = application;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
