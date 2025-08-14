package com.ntabodoiqua.online_course_management.dto.response.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ntabodoiqua.online_course_management.dto.response.lesson.LessonResponse;
import com.ntabodoiqua.online_course_management.dto.response.user.UserResponse;
import com.ntabodoiqua.online_course_management.enums.QuizType;
import com.ntabodoiqua.online_course_management.enums.ScoringMethod;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizResponse {
    String id;
    String title;
    String description;
    QuizType type;
    LessonResponse lesson;
    UserResponse createdBy;
    
    // Configuration settings
    LocalDateTime startTime;
    LocalDateTime endTime;
    Integer timeLimitMinutes;
    Integer maxAttempts;
    Double passingScore;
    ScoringMethod scoringMethod;
    
    @JsonProperty("isActive")
    Boolean isActive;
    @JsonProperty("shuffleQuestions")
    Boolean shuffleQuestions;
    @JsonProperty("shuffleAnswers")
    Boolean shuffleAnswers;
    @JsonProperty("showResults")
    Boolean showResults;
    @JsonProperty("showCorrectAnswers")
    Boolean showCorrectAnswers;
    
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    List<QuizQuestionResponse> questions;
    Integer totalQuestions;
    Integer totalAttempts;
} 