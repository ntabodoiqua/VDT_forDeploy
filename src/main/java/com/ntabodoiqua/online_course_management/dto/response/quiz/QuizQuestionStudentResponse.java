package com.ntabodoiqua.online_course_management.dto.response.quiz;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizQuestionStudentResponse {
    String id;
    String questionText;
    Integer orderIndex;
    Double points;
    
    List<QuizAnswerStudentResponse> answers;
} 