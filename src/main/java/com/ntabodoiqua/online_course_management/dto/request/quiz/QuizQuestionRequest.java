package com.ntabodoiqua.online_course_management.dto.request.quiz;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizQuestionRequest {
    String questionText;
    Integer orderIndex;
    Double points;
    String explanation;
    
    List<QuizAnswerRequest> answers;
} 