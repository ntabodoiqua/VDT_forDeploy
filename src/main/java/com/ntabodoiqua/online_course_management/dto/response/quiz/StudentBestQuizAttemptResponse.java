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
public class StudentBestQuizAttemptResponse {
    // Student Info
    String studentId;
    String studentUsername;
    String studentFirstName;
    String studentLastName;
    String studentEmail;

    // Quiz Info
    String quizId;
    String quizTitle;

    // Best Attempt Info
    String attemptId;
    Integer attemptNumber;
    Double score;
    Double maxScore;
    Double percentage;
    @JsonProperty("isPassed")
    Boolean isPassed;
    LocalDateTime completedAt;
} 