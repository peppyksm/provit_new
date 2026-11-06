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
@Table(name = "t_post")
@Getter
@Setter
@NoArgsConstructor
public class PostEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_post")
    @SequenceGenerator(name = "seq_t_post", sequenceName = "seq_t_post", allocationSize = 1)
    @Column(name = "post_num", nullable = false)
    private Long postNum;

    @Column(name = "category_num", nullable = false)
    private Integer categoryNum;

    @Column(name = "user_num", nullable = false)
    private Integer userNum;

    @Column(name = "post_title", nullable = false, length = 300)
    private String postTitle;

    @Column(name = "post_content", length = 4000)
    private String postContent;

    @Column(name = "post_like_count", nullable = false)
    private Integer postLikeCount;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "post_file", length = 500)
    private String postFile;

    @Column(name = "post_date", nullable = false)
    private LocalDateTime postDate;

    @PrePersist
    void initializeDefaults() {
        if (postLikeCount == null) postLikeCount = 0;
        if (viewCount == null) viewCount = 0;
        if (postDate == null) postDate = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
