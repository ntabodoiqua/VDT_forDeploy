package com.ntabodoiqua.online_course_management.dto.response.quiz;

import com.ntabodoiqua.online_course_management.enums.QuizType;

/**
 * DTO for quiz type statistics to replace Object[] returns
 */
public record QuizTypeStatDTO(
    QuizType type,
    long count
) {
    public static QuizTypeStatDTO from(Object[] result) {
        return new QuizTypeStatDTO(
            (QuizType) result[0],
            ((Number) result[1]).longValue()
        );
    }
} 