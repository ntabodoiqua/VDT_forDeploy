package com.ntabodoiqua.online_course_management.dto.request.quiz;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizAnswerRequest {
    String answerText;
    Boolean isCorrect;
    Integer orderIndex;
} 