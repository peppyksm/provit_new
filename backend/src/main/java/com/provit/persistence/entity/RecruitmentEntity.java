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
@Table(name = "t_recruitment")
@Getter
@Setter
@NoArgsConstructor
public class RecruitmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_recruitment")
    @SequenceGenerator(
            name = "seq_t_recruitment",
            sequenceName = "seq_t_recruitment",
            allocationSize = 1)
    @Column(name = "recruitment_num", nullable = false)
    private Long recruitmentNum;

    @Column(name = "saramin_job_id", nullable = false, length = 50)
    private String saraminJobId;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "title", nullable = false, length = 400)
    private String title;

    @Column(name = "job_url", nullable = false, length = 500)
    private String jobUrl;

    @Column(name = "location_name", length = 200)
    private String locationName;

    @Column(name = "job_name", length = 300)
    private String jobName;

    @Column(name = "experience_level", length = 100)
    private String experienceLevel;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Column(name = "close_type", length = 50)
    private String closeType;

    @Column(name = "is_active", nullable = false)
    private Short isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void initializeDefaults() {
        if (isActive == null) isActive = (short) 1;
        if (createdAt == null) createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
