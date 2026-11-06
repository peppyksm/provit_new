package com.provit.dao.document.impl;

import static com.provit.persistence.JpaQueries.*;
import static com.provit.persistence.JpaStore.params;

import com.provit.dao.document.DocumentReviewDAO;
import com.provit.dto.document.*;
import com.provit.dto.document.DocumentReviewResultDTO.*;
import com.provit.persistence.*;
import com.provit.persistence.entity.*;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Repository
@Transactional(readOnly = true)
public class DocumentReviewDAOImpl implements DocumentReviewDAO {
    private final JpaStore store;

    public DocumentReviewDAOImpl(JpaStore store) {
        this.store = store;
    }

    @Transactional
    public int insertReview(DocumentReviewDTO value) {
        return store.insert(
                DocumentReviewEntity.class,
                value,
                Map.of("summary", "overallSummary"),
                Set.of(
                        "createdAt",
                        "finishedAt",
                        "errorMessage",
                        "overallSummary",
                        "reviewStatus"));
    }

    @Transactional
    public int insertDocument(Document value) {
        return store.insert(
                ReviewDocumentEntity.class, value, Map.of("summary", "documentSummary"));
    }

    @Transactional
    public int insertStrength(Strength value) {
        return store.insert(
                ReviewStrengthEntity.class,
                value,
                Map.of(
                        "title",
                        "strengthContent",
                        "reason",
                        "strengthReason",
                        "suggestion",
                        "strengthSuggestion",
                        "sourcesJson",
                        "strengthSourcesJson"));
    }

    @Transactional
    public int insertImprovement(Improvement value) {
        return store.insert(
                ReviewImprovementEntity.class,
                value,
                Map.of(
                        "section",
                        "sectionName",
                        "title",
                        "improvementTitle",
                        "issue",
                        "issueContent",
                        "original",
                        "originalContent",
                        "suggestion",
                        "suggestedContent",
                        "reason",
                        "reasonContent"));
    }

    @Transactional
    public int insertConsistency(Consistency value) {
        return store.insert(
                ReviewConsistencyEntity.class,
                value,
                Map.of("type", "issueType", "title", "issueTitle"));
    }

    @Transactional
    public int insertSource(Source value) {
        return store.insert(
                ReviewSourceEntity.class,
                value,
                Map.of("section", "sectionName", "text", "sourceContent"));
    }

    @Transactional
    public int completeReview(DocumentReviewDTO value) {
        return store.modify(
                "update DocumentReviewEntity e set"
                    + " e.overallSummary=:summary,e.careerPreparationJson=:preparation,e.reviewStatus='COMPLETED',e.finishedAt=local"
                    + " datetime where e.reviewNum=:id and e.userNum=:user and"
                    + " e.reviewStatus='PROCESSING'",
                params(
                        "summary",
                        value.getSummary(),
                        "preparation",
                        value.getCareerPreparationJson(),
                        "id",
                        value.getReviewNum(),
                        "user",
                        value.getUserNum()));
    }

    @Transactional
    public int failReview(DocumentReviewDTO value) {
        return store.modify(
                "update DocumentReviewEntity e set"
                    + " e.errorMessage=:message,e.reviewStatus='FAILED',e.finishedAt=local datetime"
                    + " where e.reviewNum=:id and e.userNum=:user and e.reviewStatus='PROCESSING'",
                params(
                        "message",
                        value.getErrorMessage(),
                        "id",
                        value.getReviewNum(),
                        "user",
                        value.getUserNum()));
    }

    @Transactional
    public int updateDocumentSummary(Document value) {
        return store.modify(
                "update ReviewDocumentEntity e set e.documentSummary=:summary where"
                        + " e.reviewNum=:review and e.reviewDocumentNum=:id",
                params(
                        "summary",
                        value.getSummary(),
                        "review",
                        value.getReviewNum(),
                        "id",
                        value.getReviewDocumentNum()));
    }

    @Transactional
    public String lockReview(int user, long id) {
        DocumentReviewEntity value = store.lock(DocumentReviewEntity.class, id);
        return value != null && value.getUserNum() == user ? value.getReviewStatus() : null;
    }

    public DocumentReviewResultDTO selectReview(int user, long id) {
        var rows =
                store.list(
                        DocumentReviewEntity.class,
                        DocumentReviewResultDTO.class,
                        "where e.reviewNum=:id and e.userNum=:user",
                        params("id", id, "user", user),
                        0,
                        1,
                        Map.of("overallSummary", "summary"));
        return rows.isEmpty() ? null : rows.get(0);
    }

    public DocumentReviewDTO selectByRequestId(int user, String request) {
        return store.one(
                DocumentReviewEntity.class,
                DocumentReviewDTO.class,
                "where e.userNum=:user and e.requestId=:request",
                params("user", user, "request", request));
    }

    public List<DocumentReviewDTO> selectReviews(int user, int offset, int size) {
        return store.list(
                DocumentReviewEntity.class,
                DocumentReviewDTO.class,
                "where e.userNum=:user order by e.createdAt desc,e.reviewNum desc",
                params("user", user),
                offset,
                size,
                Map.of());
    }

    public List<Document> selectDocuments(long id) {
        return store.project(
                "select e.reviewDocumentNum as reviewDocumentNum,e.reviewNum as"
                    + " reviewNum,e.documentType as documentType,e.sourceDocumentNum as"
                    + " sourceDocumentNum,e.documentTitle as documentTitle,e.sourceUpdatedAt as"
                    + " sourceUpdatedAt,e.originalFileName as originalFileName,e.documentSummary as"
                    + " summary from ReviewDocumentEntity e where e.reviewNum=:id order by case"
                    + " e.documentType when 'resume' then 1 when 'cover-letter' then 2 else 3 end",
                Document.class,
                params("id", id));
    }

    public List<Strength> selectStrengths(long id) {
        return store.list(
                ReviewStrengthEntity.class,
                Strength.class,
                "where e.reviewNum=:id order by e.reviewDocumentNum asc nulls first,e.displayOrder",
                params("id", id),
                0,
                Integer.MAX_VALUE,
                Map.of(
                        "strengthContent",
                        "title",
                        "strengthReason",
                        "reason",
                        "strengthSuggestion",
                        "suggestion",
                        "strengthSourcesJson",
                        "sourcesJson"));
    }

    public List<Improvement> selectImprovements(long reviewNum) {
        return store.nativeList(
                REVIEW_IMPROVEMENTS, Improvement.class, params("reviewNum", reviewNum));
    }

    public List<Consistency> selectConsistencies(long id) {
        return store.list(
                ReviewConsistencyEntity.class,
                Consistency.class,
                "where e.reviewNum=:id order by e.displayOrder",
                params("id", id),
                0,
                Integer.MAX_VALUE,
                Map.of("issueType", "type", "issueTitle", "title"));
    }

    public List<Source> selectSources(long reviewNum) {
        return store.nativeList(REVIEW_SOURCES, Source.class, params("reviewNum", reviewNum));
    }
}
