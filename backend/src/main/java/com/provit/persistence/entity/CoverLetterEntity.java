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
@Table(name = "t_cover_letter")
@Getter
@Setter
@NoArgsConstructor
public class CoverLetterEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_cover_letter")
    @SequenceGenerator(
            name = "seq_t_cover_letter",
            sequenceName = "seq_t_cover_letter",
            allocationSize = 1)
    @Column(name = "letter_num", nullable = false)
    private Long letterNum;

    @Column(name = "user_num", nullable = false)
    private Integer userNum;

    @Column(name = "cover_letter_title", nullable = false, length = 200)
    private String coverLetterTitle;

    @Column(name = "growth_process", length = 3000)
    private String growthProcess;

    @Column(name = "personality_strengths_weaknesses", length = 3000)
    private String personalityStrengthsWeaknesses;

    @Column(name = "problem_solving_experience", length = 3000)
    private String problemSolvingExperience;

    @Column(name = "post_joining_aspiration", length = 3000)
    private String postJoiningAspiration;

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
