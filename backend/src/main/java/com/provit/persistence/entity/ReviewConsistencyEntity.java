package com.provit.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate
@Table(name = "t_review_consistency")
@Getter
@Setter
@NoArgsConstructor
public class ReviewConsistencyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_review_consistency")
    @SequenceGenerator(
            name = "seq_t_review_consistency",
            sequenceName = "seq_t_review_consistency",
            allocationSize = 1)
    @Column(name = "consistency_num", nullable = false)
    private Long consistencyNum;

    @Column(name = "review_num", nullable = false)
    private Long reviewNum;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "issue_type", nullable = false, length = 30)
    private String issueType;

    @Column(name = "issue_title", nullable = false, length = 500)
    private String issueTitle;

    @Column(name = "recommendation", nullable = false, columnDefinition = "text")
    private String recommendation;
}
