package com.provit.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@DynamicUpdate
@Table(name = "t_interview_result")
@Getter
@Setter
@NoArgsConstructor
@IdClass(InterviewResultEntityId.class)
public class InterviewResultEntity {
    @Id
    @Column(name = "history_num", nullable = false)
    private Long historyNum;

    @Id
    @Column(name = "user_num", nullable = false)
    private Integer userNum;

    @Column(name = "document_consistency_score", precision = 5, scale = 2)
    private BigDecimal documentConsistencyScore;

    @Column(name = "expertise_score", precision = 5, scale = 2)
    private BigDecimal expertiseScore;

    @Column(name = "problem_solving_score", precision = 5, scale = 2)
    private BigDecimal problemSolvingScore;

    @Column(name = "logic_score", precision = 5, scale = 2)
    private BigDecimal logicScore;

    @Column(name = "communication_score", precision = 5, scale = 2)
    private BigDecimal communicationScore;

    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "strength", length = 250)
    private String strength;

    @Column(name = "weakness", length = 250)
    private String weakness;

    @Column(name = "previous_comparison", length = 250)
    private String previousComparison;

    @Column(name = "improvement_point", length = 250)
    private String improvementPoint;

    @Column(name = "interview_date", nullable = false)
    private LocalDateTime interviewDate;

    @PrePersist
    void initializeDefaults() {
        if (documentConsistencyScore == null) documentConsistencyScore = new BigDecimal("0.00");
        if (expertiseScore == null) expertiseScore = new BigDecimal("0.00");
        if (problemSolvingScore == null) problemSolvingScore = new BigDecimal("0.00");
        if (logicScore == null) logicScore = new BigDecimal("0.00");
        if (communicationScore == null) communicationScore = new BigDecimal("0.00");
        if (totalScore == null) totalScore = new BigDecimal("0.00");
        if (interviewDate == null) interviewDate = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
