package com.ntabodoiqua.online_course_management.dto.response.user;

import com.ntabodoiqua.online_course_management.enums.Gender;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstructorResponse {
    // Basic user information
    String id;
    String username;
    String firstName;
    String lastName;
    LocalDate dob;
    String avatarUrl;
    String email;
    String phone;
    String bio;
    Gender gender;
    LocalDateTime createdAt;
    boolean enabled;

    // Instructor-specific statistics
    Double averageRating; // Average rating from course reviews
    Integer totalReviews; // Total number of reviews received
    Long totalStudents; // Total number of enrolled students
    Long totalCourses; // Total number of courses created
    Long activeCourses; // Number of active courses
    Integer experienceYears; // Years of experience (calculated from createdAt)
    
    // Additional instructor data
    String specialty; // Main teaching area/expertise
    List<String> achievements; // Achievements or certifications
    Double completionRate; // Average course completion rate
    Long totalLessons; // Total lessons across all courses

    // Course category distribution (for charts/statistics)
    List<CategoryStats> categoryDistribution;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryStats {
        String categoryName;
        Long courseCount;
        Long studentCount;
    }
} 