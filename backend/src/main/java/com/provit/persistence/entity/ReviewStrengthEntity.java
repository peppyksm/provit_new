package com.provit.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate
@Table(name = "t_review_strength")
@Getter
@Setter
@NoArgsConstructor
public class ReviewStrengthEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_review_strength")
    @SequenceGenerator(
            name = "seq_t_review_strength",
            sequenceName = "seq_t_review_strength",
            allocationSize = 1)
    @Column(name = "strength_num", nullable = false)
    private Long strengthNum;

    @Column(name = "review_num", nullable = false)
    private Long reviewNum;

    @Column(name = "review_document_num")
    private Long reviewDocumentNum;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "strength_content", nullable = false, columnDefinition = "text")
    private String strengthContent;

    @Column(name = "strength_reason", columnDefinition = "text")
    private String strengthReason;

    @Column(name = "strength_suggestion", columnDefinition = "text")
    private String strengthSuggestion;

    @Column(name = "strength_sources_json", columnDefinition = "text")
    private String strengthSourcesJson;
}
