package com.ntabodoiqua.online_course_management.dto.response.quiz;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizAttemptsOverTimeResponse {
    String date;
    Long attemptCount;
} 