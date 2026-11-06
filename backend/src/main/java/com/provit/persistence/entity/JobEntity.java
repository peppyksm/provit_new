package com.provit.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate
@Table(name = "t_job")
@Getter
@Setter
@NoArgsConstructor
public class JobEntity {
    @Id
    @Column(name = "job_code", nullable = false, length = 20)
    private String jobCode;

    @Column(name = "occupation_code", nullable = false, length = 20)
    private String occupationCode;

    @Column(name = "job_name", nullable = false, length = 200)
    private String jobName;
}
