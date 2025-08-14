package com.ntabodoiqua.online_course_management.dto.response.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ntabodoiqua.online_course_management.enums.QuizType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizStudentResponse {
    String id;
    String title;
    String description;
    QuizType type;
    String lessonId;
    String lessonTitle;
    
    LocalDateTime startTime;
    LocalDateTime endTime;
    Integer timeLimitMinutes;
    Integer maxAttempts;
    Double passingScore;
    
    @JsonProperty("isActive")
    Boolean isActive;
    @JsonProperty("shuffleQuestions")
    Boolean shuffleQuestions;
    @JsonProperty("shuffleAnswers")
    Boolean shuffleAnswers;
    
    Integer totalQuestions;
    Integer userAttempts;
    Integer remainingAttempts;
    
    @JsonProperty("canAttempt")
    Boolean canAttempt;
    @JsonProperty("hasAccess")
    Boolean hasAccess;
    
    List<QuizQuestionStudentResponse> questions;
} 