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
@Table(name = "t_comment")
@Getter
@Setter
@NoArgsConstructor
public class CommentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_comment")
    @SequenceGenerator(name = "seq_t_comment", sequenceName = "seq_t_comment", allocationSize = 1)
    @Column(name = "comment_num", nullable = false)
    private Long commentNum;

    @Column(name = "post_num", nullable = false)
    private Long postNum;

    @Column(name = "user_num", nullable = false)
    private Integer userNum;

    @Column(name = "comment_content", nullable = false, length = 2000)
    private String commentContent;

    @Column(name = "comment_date", nullable = false)
    private LocalDateTime commentDate;

    @Column(name = "parent_comment_num")
    private Long parentCommentNum;

    @Column(name = "is_deleted", nullable = false)
    private Short isDeleted;

    @PrePersist
    void initializeDefaults() {
        if (commentDate == null) commentDate = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        if (isDeleted == null) isDeleted = (short) 0;
    }
}
