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
public class QuizAttemptAnswerResponse {
    String id;
    QuizQuestionResponse question;
    QuizAnswerResponse selectedAnswer;
    
    @JsonProperty("isCorrect")
    Boolean isCorrect;
    Double pointsEarned;
    
    LocalDateTime answeredAt;
} 