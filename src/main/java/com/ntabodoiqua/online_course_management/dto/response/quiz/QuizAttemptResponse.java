package com.ntabodoiqua.online_course_management.dto.response.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ntabodoiqua.online_course_management.dto.response.user.UserResponse;
import com.ntabodoiqua.online_course_management.dto.response.enrollment.EnrollmentResponse;
import com.ntabodoiqua.online_course_management.enums.AttemptStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizAttemptResponse {
    String id;
    QuizResponse quiz;
    UserResponse student;
    EnrollmentResponse enrollment;
    Integer attemptNumber;
    
    LocalDateTime startedAt;
    LocalDateTime submittedAt;
    LocalDateTime completedAt;
    
    AttemptStatus status;
    
    Double score;
    Double percentage;
    @JsonProperty("isPassed")
    Boolean isPassed;
    
    Integer totalQuestions;
    Integer correctAnswers;
    Integer incorrectAnswers;
    Integer unansweredQuestions;
    
    List<QuizAttemptAnswerResponse> attemptAnswers;
} 