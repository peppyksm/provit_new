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
@Table(name = "t_interview_history")
@Getter
@Setter
@NoArgsConstructor
public class InterviewHistoryEntity {
    @Id
    @Column(name = "history_num", nullable = false)
    private Long historyNum;

    @Column(name = "user_num", nullable = false)
    private Integer userNum;

    @Column(name = "question1", length = 120)
    private String question1;

    @Column(name = "answer1", length = 1000)
    private String answer1;

    @Column(name = "question2", length = 120)
    private String question2;

    @Column(name = "answer2", length = 1000)
    private String answer2;

    @Column(name = "question3", length = 120)
    private String question3;

    @Column(name = "answer3", length = 1000)
    private String answer3;

    @Column(name = "question4", length = 120)
    private String question4;

    @Column(name = "answer4", length = 1000)
    private String answer4;

    @Column(name = "question5", length = 120)
    private String question5;

    @Column(name = "answer5", length = 1000)
    private String answer5;

    @Column(name = "interview_date", nullable = false)
    private LocalDateTime interviewDate;

    @PrePersist
    void initializeDefaults() {
        if (interviewDate == null) interviewDate = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
