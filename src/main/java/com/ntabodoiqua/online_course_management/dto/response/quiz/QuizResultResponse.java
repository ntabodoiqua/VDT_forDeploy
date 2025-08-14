package com.ntabodoiqua.online_course_management.dto.response.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizResultResponse {
    String attemptId;
    String quizId;
    String quizTitle;
    Integer attemptNumber;
    
    LocalDateTime startedAt;
    LocalDateTime completedAt;
    Long durationMinutes;
    
    Double score;
    Double percentage;
    @JsonProperty("isPassed")
    Boolean isPassed;
    
    Integer totalQuestions;
    Integer correctAnswers;
    Integer incorrectAnswers;
    Integer unansweredQuestions;
    
    Double passingScore;
    String feedback;
    
    @JsonProperty("canRetake")
    Boolean canRetake;
    Integer remainingAttempts;
} 