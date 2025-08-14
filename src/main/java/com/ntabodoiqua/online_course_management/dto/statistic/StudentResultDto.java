package com.ntabodoiqua.online_course_management.dto.statistic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResultDto {
    private String studentId;
    private String studentName;
    private String quizId;
    private String quizTitle;
    private Double score;
    private Double maxScore;
    private Double percentage;
    private Boolean isPassed;
} 