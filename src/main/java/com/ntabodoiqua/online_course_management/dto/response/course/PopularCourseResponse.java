package com.ntabodoiqua.online_course_management.dto.response.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularCourseResponse {
    private CourseResponse course;
    private Long enrollmentCount;
    private Double averageRating;
    private Long totalReviews;
} 