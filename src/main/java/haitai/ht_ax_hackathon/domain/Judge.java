package haitai.ht_ax_hackathon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "judges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Judge {

    @Id
    @SequenceGenerator(name = "judges_seq", sequenceName = "seq_judges", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "judges_seq")
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    @Nationalized
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    @Nationalized
    private String passwordHash;

    @Column(nullable = false, length = 100)
    @Nationalized
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Judge(String username, String passwordHash, String name) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.name = name;
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
