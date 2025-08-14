package com.ntabodoiqua.online_course_management.dto.response.quiz;

import com.ntabodoiqua.online_course_management.entity.QuizAnswer;
import com.ntabodoiqua.online_course_management.entity.QuizQuestion;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizPreviewSession {
    String sessionId;
    String quizId;
    String userId; // instructor/admin user ID
    LocalDateTime startedAt;
    Map<String, QuizAnswer> answers; // questionId -> selected answer
    boolean isCompleted;
    LocalDateTime completedAt;
    
    // Initialize with empty answers map
    public static QuizPreviewSession create(String sessionId, String quizId, String userId) {
        return QuizPreviewSession.builder()
                .sessionId(sessionId)
                .quizId(quizId)
                .userId(userId)
                .startedAt(LocalDateTime.now())
                .answers(new HashMap<>())
                .isCompleted(false)
                .build();
    }
    
    public void answerQuestion(String questionId, QuizAnswer selectedAnswer) {
        this.answers.put(questionId, selectedAnswer);
    }
    
    public void complete() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
    }
    
    public boolean hasAnswered(String questionId) {
        return answers.containsKey(questionId);
    }
    
    public QuizAnswer getAnswer(String questionId) {
        return answers.get(questionId);
    }
} 