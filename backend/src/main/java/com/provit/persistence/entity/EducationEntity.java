package com.provit.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;

@Entity
@DynamicUpdate
@Table(name = "t_education")
@Getter
@Setter
@NoArgsConstructor
public class EducationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_education")
    @SequenceGenerator(
            name = "seq_t_education",
            sequenceName = "seq_t_education",
            allocationSize = 1)
    @Column(name = "edu_num", nullable = false)
    private Long eduNum;

    @Column(name = "resume_num", nullable = false)
    private Long resumeNum;

    @Column(name = "school_name", nullable = false, length = 200)
    private String schoolName;

    @Column(name = "admission_date")
    private LocalDate admissionDate;

    @Column(name = "graduation_date")
    private LocalDate graduationDate;

    @Column(name = "major", length = 200)
    private String major;

    @Column(name = "education_status", nullable = false, length = 20)
    private String educationStatus;
}
