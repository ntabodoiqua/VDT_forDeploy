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
public class StudentQuizHistoryResponse {
    String attemptId;
    String quizId;
    String quizTitle;
    String lessonTitle;
    
    Integer attemptNumber;
    Double score;
    Double maxScore;
    Double percentage;
    
    @JsonProperty("isPassed")
    Boolean isPassed;
    
    LocalDateTime startedAt;
    LocalDateTime completedAt;
    Integer duration; // in minutes
    
    Integer totalQuestions;
    Integer correctAnswers;
    Integer incorrectAnswers;
} 