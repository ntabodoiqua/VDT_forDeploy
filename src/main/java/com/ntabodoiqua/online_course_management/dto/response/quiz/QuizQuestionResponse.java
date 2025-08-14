package com.ntabodoiqua.online_course_management.dto.response.quiz;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizQuestionResponse {
    String id;
    String questionText;
    Integer orderIndex;
    Double points;
    String explanation;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    List<QuizAnswerResponse> answers;
} 