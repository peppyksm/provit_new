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
@Table(name = "t_job_scrap")
@Getter
@Setter
@NoArgsConstructor
@IdClass(JobScrapEntityId.class)
public class JobScrapEntity {
    @Id
    @Column(name = "recruitment_num", nullable = false)
    private Long recruitmentNum;

    @Id
    @Column(name = "user_num", nullable = false)
    private Integer userNum;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void initializeDefaults() {
        if (createdAt == null) createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
