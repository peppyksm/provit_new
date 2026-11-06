package com.provit.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@DynamicUpdate
@Table(name = "t_user")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_user")
    @SequenceGenerator(name = "seq_t_user", sequenceName = "seq_t_user", allocationSize = 1)
    @Column(name = "user_num", nullable = false)
    private Integer userNum;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Column(name = "user_nickname", nullable = false, length = 100)
    private String userNickname;

    @Column(name = "user_birth_date")
    private LocalDate userBirthDate;

    @Column(name = "user_email", nullable = false, length = 200)
    private String userEmail;

    @Column(name = "user_pw", nullable = false, length = 255)
    private String userPw;

    @Column(name = "user_register_date", nullable = false)
    private LocalDateTime userRegisterDate;

    @Column(name = "user_type", length = 30)
    private String userType;

    @Column(name = "user_is_deleted", nullable = false)
    private Short userIsDeleted;

    @Column(name = "user_token_version", nullable = false)
    private Integer userTokenVersion;

    @Column(name = "blocked_date")
    private LocalDateTime blockedDate;

    @Column(name = "main_resume_num")
    private Long mainResumeNum;

    @Column(name = "occupation_code", length = 20)
    private String occupationCode;

    @Column(name = "job_code", length = 20)
    private String jobCode;

    @PrePersist
    void initializeDefaults() {
        if (userRegisterDate == null) userRegisterDate = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        if (userType == null) userType = "USER";
        if (userIsDeleted == null) userIsDeleted = (short) 0;
        if (userTokenVersion == null) userTokenVersion = 0;
    }
}
