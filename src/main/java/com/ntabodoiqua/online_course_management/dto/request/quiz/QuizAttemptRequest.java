package com.ntabodoiqua.online_course_management.dto.request.quiz;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizAttemptRequest {
    String quizId;
    List<QuizAttemptAnswerRequest> answers;
} 