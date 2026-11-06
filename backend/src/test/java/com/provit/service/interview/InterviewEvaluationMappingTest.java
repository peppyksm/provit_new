package com.provit.service.interview;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.provit.dto.interview.InterviewResultDTO;
import com.provit.dto.interview.InterviewResultResponseDTO;
import com.provit.dto.interview.LlmEvaluationResponseDTO;

public class InterviewEvaluationMappingTest {
    private static final List<String> SCORE_FIELDS = List.of(
            "documentConsistencyScore", "expertiseScore", "problemSolvingScore",
            "logicScore", "communicationScore", "totalScore");
    private static final List<String> SCORE_COLUMNS = List.of(
            "DOCUMENT_CONSISTENCY_SCORE", "EXPERTISE_SCORE", "PROBLEM_SOLVING_SCORE",
            "LOGIC_SCORE", "COMMUNICATION_SCORE", "TOTAL_SCORE");
    private static final String NAMESPACE = "com.provit.mapper.interview.InterviewResultMapper.";

    @Test
    public void storedResultsApiResponsesAndAiResponsesExposeExactlyTheSameScores() {
        var mapper = new ObjectMapper();
        for (Object dto : List.of(new InterviewResultDTO(), new InterviewResultResponseDTO(),
                new LlmEvaluationResponseDTO())) {
            var fields = new ArrayList<String>();
            mapper.valueToTree(dto).fieldNames().forEachRemaining(name -> {
                if (name.endsWith("Score")) fields.add(name);
            });
            assertEquals(SCORE_FIELDS, fields);
        }
    }

}
