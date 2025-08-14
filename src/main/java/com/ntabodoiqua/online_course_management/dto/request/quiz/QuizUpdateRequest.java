package com.ntabodoiqua.online_course_management.dto.request.quiz;

import com.ntabodoiqua.online_course_management.enums.QuizType;
import com.ntabodoiqua.online_course_management.enums.ScoringMethod;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizUpdateRequest {
    String title;
    String description;
    QuizType type;
    
    // Configuration settings
    LocalDateTime startTime;
    LocalDateTime endTime;
    Integer timeLimitMinutes;
    Integer maxAttempts;
    Double passingScore;
    ScoringMethod scoringMethod;
    
    Boolean isActive;
    Boolean shuffleQuestions;
    Boolean shuffleAnswers;
    Boolean showResults;
    Boolean showCorrectAnswers;
    
    List<QuizQuestionRequest> questions;
} 