package com.ntabodoiqua.online_course_management.dto.response.quiz;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseQuizStatisticsResponse {
    Integer totalQuizzes;
    Integer totalAttempts;
    Double averageScore;
    Double passRate;
    
    List<ScoreDistributionItem> scoreDistribution;
    List<GradeDistributionItem> gradeDistribution;
    List<QuizPerformanceItem> quizPerformance;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ScoreDistributionItem {
        String range;
        Integer count;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GradeDistributionItem {
        String name;
        Integer count;
        String color;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class QuizPerformanceItem {
        String quizId;
        String quizTitle;
        Long passedCount;
        Long failedCount;
    }
} 