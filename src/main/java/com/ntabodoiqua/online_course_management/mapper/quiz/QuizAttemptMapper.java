package com.ntabodoiqua.online_course_management.mapper.quiz;

import com.ntabodoiqua.online_course_management.dto.response.quiz.QuizResultResponse;
import com.ntabodoiqua.online_course_management.entity.QuizAttempt;
import org.mapstruct.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface QuizAttemptMapper {

    @Mapping(source = "id", target = "attemptId")
    @Mapping(source = "quiz.id", target = "quizId")
    @Mapping(source = "quiz.title", target = "quizTitle")
    @Mapping(source = "quiz.passingScore", target = "passingScore")
    @Mapping(target = "durationMinutes", expression = "java(calculateDurationMinutes(attempt.getStartedAt(), attempt.getCompletedAt()))")
    @Mapping(target = "feedback", ignore = true)
    @Mapping(target = "canRetake", ignore = true)
    @Mapping(target = "remainingAttempts", ignore = true)
    QuizResultResponse toQuizResultResponse(QuizAttempt attempt);

    default Long calculateDurationMinutes(LocalDateTime startedAt, LocalDateTime completedAt) {
        if (startedAt != null && completedAt != null) {
            return Duration.between(startedAt, completedAt).toMinutes();
        }
        return null;
    }
} 