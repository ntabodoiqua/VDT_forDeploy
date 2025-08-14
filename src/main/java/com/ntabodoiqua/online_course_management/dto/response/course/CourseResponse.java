package com.ntabodoiqua.online_course_management.dto.response.course;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ntabodoiqua.online_course_management.dto.response.user.UserResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseResponse {
    String id;
    String title;
    String description;
    String detailedDescription;
    String thumbnailUrl;
    @JsonProperty("isActive")
    boolean isActive;
    int totalLessons;
    LocalDate startDate;
    LocalDate endDate;
    CategoryResponse category;
    
    // Rating information
    Double averageRating; // Average rating from reviews (1-5 scale)
    Integer totalReviews; // Total number of approved reviews

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    // Thông tin về giảng viên
    UserResponse instructor;
    boolean requiresApproval;
}
