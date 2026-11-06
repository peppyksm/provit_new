package com.provit.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@DynamicUpdate
@Table(name = "t_resume")
@Getter
@Setter
@NoArgsConstructor
public class ResumeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_resume")
    @SequenceGenerator(name = "seq_t_resume", sequenceName = "seq_t_resume", allocationSize = 1)
    @Column(name = "resume_num", nullable = false)
    private Long resumeNum;

    @Column(name = "user_num", nullable = false)
    private Integer userNum;

    @Column(name = "resume_title", length = 200)
    private String resumeTitle;

    @Column(name = "occupation_code", length = 20)
    private String occupationCode;

    @Column(name = "job_code", length = 20)
    private String jobCode;

    @Column(name = "highest_level", nullable = false, length = 20)
    private String highestLevel;

    @Column(name = "motivation", columnDefinition = "text")
    private String motivation;

    @Column(name = "desired_location", length = 200)
    private String desiredLocation;

    @Column(name = "desired_work_type", length = 100)
    private String desiredWorkType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void initializeDefaults() {
        if (createdAt == null) createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        if (updatedAt == null) updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
