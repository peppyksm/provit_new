package com.provit.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@DynamicUpdate
@Table(name = "t_review_document")
@Getter
@Setter
@NoArgsConstructor
public class ReviewDocumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_review_document")
    @SequenceGenerator(
            name = "seq_t_review_document",
            sequenceName = "seq_t_review_document",
            allocationSize = 1)
    @Column(name = "review_document_num", nullable = false)
    private Long reviewDocumentNum;

    @Column(name = "review_num", nullable = false)
    private Long reviewNum;

    @Column(name = "document_type", nullable = false, length = 20)
    private String documentType;

    @Column(name = "source_document_num", nullable = false)
    private Long sourceDocumentNum;

    @Column(name = "document_title", nullable = false, length = 200)
    private String documentTitle;

    @Column(name = "source_updated_at")
    private LocalDateTime sourceUpdatedAt;

    @Column(name = "source_snapshot_json", nullable = false, columnDefinition = "text")
    private String sourceSnapshotJson;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "pdf_snapshot", columnDefinition = "bytea")
    private byte[] pdfSnapshot;

    @Column(name = "document_summary", columnDefinition = "text")
    private String documentSummary;
}
