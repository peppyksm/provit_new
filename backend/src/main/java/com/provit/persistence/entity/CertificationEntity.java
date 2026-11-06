package com.provit.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;

@Entity
@DynamicUpdate
@Table(name = "t_certification")
@Getter
@Setter
@NoArgsConstructor
public class CertificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_certification")
    @SequenceGenerator(
            name = "seq_t_certification",
            sequenceName = "seq_t_certification",
            allocationSize = 1)
    @Column(name = "cert_num", nullable = false)
    private Long certNum;

    @Column(name = "resume_num", nullable = false)
    private Long resumeNum;

    @Column(name = "cert_name", nullable = false, length = 200)
    private String certName;

    @Column(name = "cert_grade", length = 100)
    private String certGrade;

    @Column(name = "issue_date")
    private LocalDate issueDate;
}
