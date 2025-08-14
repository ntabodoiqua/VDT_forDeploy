package com.ntabodoiqua.online_course_management.dto.request.quiz;

import com.ntabodoiqua.online_course_management.enums.QuizType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizFilterRequest {
    String title;
    QuizType type;
    String lessonId;
    String courseId;
    String instructorId;
    Boolean isActive;
    LocalDateTime startTimeFrom;
    LocalDateTime startTimeTo;
    LocalDateTime endTimeFrom;
    LocalDateTime endTimeTo;
    
    // Pagination
    Integer page;
    Integer size;
    String sortBy;
    String sortDir;
} 