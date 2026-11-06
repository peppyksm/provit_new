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
@Table(name = "t_report")
@Getter
@Setter
@NoArgsConstructor
public class ReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_report")
    @SequenceGenerator(name = "seq_t_report", sequenceName = "seq_t_report", allocationSize = 1)
    @Column(name = "report_num", nullable = false)
    private Long reportNum;

    @Column(name = "reporter_num")
    private Integer reporterNum;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_num", nullable = false)
    private Long targetNum;

    @Column(name = "report_reason", nullable = false, length = 100)
    private String reportReason;

    @Column(name = "report_status", nullable = false, length = 20)
    private String reportStatus;

    @Column(name = "report_date", nullable = false)
    private LocalDateTime reportDate;

    @PrePersist
    void initializeDefaults() {
        if (reportStatus == null) reportStatus = "PENDING";
        if (reportDate == null) reportDate = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
