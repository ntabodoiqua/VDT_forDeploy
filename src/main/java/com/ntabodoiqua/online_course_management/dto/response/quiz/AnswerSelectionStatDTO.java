package com.ntabodoiqua.online_course_management.dto.response.quiz;

/**
 * DTO for answer selection statistics to replace Object[] returns
 */
public record AnswerSelectionStatDTO(
    String answerId,
    long selectionCount
) {
    public static AnswerSelectionStatDTO from(Object[] result) {
        return new AnswerSelectionStatDTO(
            (String) result[0],
            ((Number) result[1]).longValue()
        );
    }
} 