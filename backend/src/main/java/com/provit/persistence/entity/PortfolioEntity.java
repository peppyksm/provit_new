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
@Table(name = "t_portfolio")
@Getter
@Setter
@NoArgsConstructor
public class PortfolioEntity {
    @Id
    @Column(name = "portfolio_num", nullable = false)
    private Long portfolioNum;

    @Column(name = "user_num", nullable = false)
    private Integer userNum;

    @Column(name = "portfolio_title", nullable = false, length = 200)
    private String portfolioTitle;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "saved_file_name", length = 255)
    private String savedFileName;

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
