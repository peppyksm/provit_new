package com.provit.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate
@Table(name = "t_review_source")
@Getter
@Setter
@NoArgsConstructor
@IdClass(ReviewSourceEntityId.class)
public class ReviewSourceEntity {
    @Id
    @Column(name = "consistency_num", nullable = false)
    private Long consistencyNum;

    @Id
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "review_num", nullable = false)
    private Long reviewNum;

    @Column(name = "review_document_num", nullable = false)
    private Long reviewDocumentNum;

    @Column(name = "section_name", nullable = false, length = 500)
    private String sectionName;

    @Column(name = "source_content", nullable = false, columnDefinition = "text")
    private String sourceContent;

    @Column(name = "page_number")
    private Integer pageNumber;
}
