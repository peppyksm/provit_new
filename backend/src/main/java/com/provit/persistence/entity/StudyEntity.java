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
@Table(name = "t_study")
@Getter
@Setter
@NoArgsConstructor
public class StudyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_study")
    @SequenceGenerator(name = "seq_t_study", sequenceName = "seq_t_study", allocationSize = 1)
    @Column(name = "study_num", nullable = false)
    private Long studyNum;

    @Column(name = "user_num", nullable = false)
    private Integer userNum;

    @Column(name = "study_name", nullable = false, length = 200)
    private String studyName;

    @Column(name = "study_explain", length = 2000)
    private String studyExplain;

    @Column(name = "max_members", nullable = false)
    private Short maxMembers;

    @Column(name = "study_create_date", nullable = false)
    private LocalDateTime studyCreateDate;

    @PrePersist
    void initializeDefaults() {
        if (maxMembers == null) maxMembers = (short) 10;
        if (studyCreateDate == null) studyCreateDate = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
