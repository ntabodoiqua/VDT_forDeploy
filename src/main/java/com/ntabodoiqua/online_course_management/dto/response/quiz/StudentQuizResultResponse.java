package com.ntabodoiqua.online_course_management.dto.response.quiz;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentQuizResultResponse {
    String studentId;
    String studentUsername;
    String studentFirstName;
    String studentLastName;
    String studentEmail;
    
    Integer totalAttempts;
    Double bestScore;
    Double averageScore;
    LocalDateTime lastAttemptDate;
    Double percentage;
    
    String status; // "PASSED", "FAILED", "NOT_ATTEMPTED"
} 