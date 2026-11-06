package com.provit.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate
@Table(name = "t_review_improvement")
@Getter
@Setter
@NoArgsConstructor
public class ReviewImprovementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_review_improvement")
    @SequenceGenerator(
            name = "seq_t_review_improvement",
            sequenceName = "seq_t_review_improvement",
            allocationSize = 1)
    @Column(name = "improvement_num", nullable = false)
    private Long improvementNum;

    @Column(name = "review_document_num", nullable = false)
    private Long reviewDocumentNum;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "section_name", nullable = false, length = 500)
    private String sectionName;

    @Column(name = "improvement_title", nullable = false, length = 500)
    private String improvementTitle;

    @Column(name = "issue_content", nullable = false, columnDefinition = "text")
    private String issueContent;

    @Column(name = "original_content", nullable = false, columnDefinition = "text")
    private String originalContent;

    @Column(name = "suggested_content", nullable = false, columnDefinition = "text")
    private String suggestedContent;

    @Column(name = "reason_content", nullable = false, columnDefinition = "text")
    private String reasonContent;
}
