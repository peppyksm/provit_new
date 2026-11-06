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
@Table(name = "t_document_review")
@Getter
@Setter
@NoArgsConstructor
public class DocumentReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_document_review")
    @SequenceGenerator(
            name = "seq_t_document_review",
            sequenceName = "seq_t_document_review",
            allocationSize = 1)
    @Column(name = "review_num", nullable = false)
    private Long reviewNum;

    @Column(name = "user_num", nullable = false)
    private Integer userNum;

    @Column(name = "request_id", length = 36)
    private String requestId;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "review_title", nullable = false, length = 200)
    private String reviewTitle;

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus;

    @Column(name = "review_mode", nullable = false, length = 20)
    private String reviewMode;

    @Column(name = "custom_criteria", length = 200)
    private String customCriteria;

    @Column(name = "instructions", length = 200)
    private String instructions;

    @Column(name = "overall_summary", columnDefinition = "text")
    private String overallSummary;

    @Column(name = "career_preparation_json", columnDefinition = "text")
    private String careerPreparationJson;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Column(name = "response_version", nullable = false)
    private Short responseVersion;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @PrePersist
    void initializeDefaults() {
        if (reviewStatus == null) reviewStatus = "PROCESSING";
        if (reviewMode == null) reviewMode = "comprehensive";
        if (responseVersion == null) responseVersion = (short) 1;
        if (createdAt == null) createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
