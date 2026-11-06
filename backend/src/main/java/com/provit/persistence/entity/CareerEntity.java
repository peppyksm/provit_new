package com.provit.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;

@Entity
@DynamicUpdate
@Table(name = "t_career")
@Getter
@Setter
@NoArgsConstructor
public class CareerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_career")
    @SequenceGenerator(name = "seq_t_career", sequenceName = "seq_t_career", allocationSize = 1)
    @Column(name = "career_num", nullable = false)
    private Long careerNum;

    @Column(name = "resume_num", nullable = false)
    private Long resumeNum;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Column(name = "resign_date")
    private LocalDate resignDate;

    @Column(name = "main_duty", length = 2000)
    private String mainDuty;
}
