package com.ntabodoiqua.online_course_management.dto.response.quiz;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizAnswerStudentResponse {
    String id;
    String answerText;
    Integer orderIndex;
    // Note: isCorrect field is intentionally omitted for student view
} 