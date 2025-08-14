package com.ntabodoiqua.online_course_management.dto.response.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ntabodoiqua.online_course_management.dto.response.user.UserResponse;
import com.ntabodoiqua.online_course_management.enums.QuizType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizSummaryResponse {
    String id;
    String title;
    String description;
    QuizType type;
    String lessonId;
    String lessonTitle;
    UserResponse createdBy;
    
    LocalDateTime startTime;
    LocalDateTime endTime;
    Integer timeLimitMinutes;
    Integer maxAttempts;
    Double passingScore;
    
    @JsonProperty("isActive")
    Boolean isActive;
    
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    Integer totalQuestions;
    Integer totalAttempts;
    Integer passedAttempts;
} 