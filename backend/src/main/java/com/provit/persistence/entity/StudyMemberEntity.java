package com.provit.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.time.ZoneId;

@Entity
@DynamicUpdate
@Table(name = "t_study_member")
@Getter
@Setter
@NoArgsConstructor
@IdClass(StudyMemberEntityId.class)
public class StudyMemberEntity {
    @Id
    @Column(name = "study_num", nullable = false)
    private Long studyNum;

    @Id
    @Column(name = "user_num", nullable = false)
    private Integer userNum;

    @Column(name = "study_join_date", nullable = false)
    private LocalDate studyJoinDate;

    @PrePersist
    void initializeDefaults() {
        if (studyJoinDate == null) studyJoinDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
    }
}
