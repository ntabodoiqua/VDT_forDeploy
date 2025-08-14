package com.ntabodoiqua.online_course_management.dto.request.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * DTO for filtering instructor requests
 * Contains both database-filterable fields and calculated-field filters
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstructorFilterRequest {
    
    // === Database-level filters (handled by InstructorSpecification) ===
    
    /**
     * Search by instructor name (firstName or lastName)
     */
    @Size(max = 100, message = "Tên tìm kiếm không được vượt quá 100 ký tự")
    String name;
    
    /**
     * Search by email
     */
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    String email;
    

    
    /**
     * Filter by account creation date (from)
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdFrom;
    
    /**
     * Filter by account creation date (to)
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdTo;
    
    /**
     * Filter by enabled status
     */
    Boolean enabled;
    
    // === In-memory filters (handled by service layer after database query) ===
    
    /**
     * Minimum average rating filter
     */
    @DecimalMin(value = "0.0", message = "Đánh giá tối thiểu phải từ 0.0")
    @DecimalMax(value = "5.0", message = "Đánh giá tối thiểu không được vượt quá 5.0")
    Double minRating;
    
    /**
     * Maximum average rating filter
     */
    @DecimalMin(value = "0.0", message = "Đánh giá tối đa phải từ 0.0")
    @DecimalMax(value = "5.0", message = "Đánh giá tối đa không được vượt quá 5.0")
    Double maxRating;
    
    /**
     * Minimum years of experience filter (calculated from account creation)
     */
    @Min(value = 0, message = "Kinh nghiệm tối thiểu phải từ 0 năm")
    @Max(value = 50, message = "Kinh nghiệm tối thiểu không được vượt quá 50 năm")
    Integer minExperience;
    
    /**
     * Maximum years of experience filter
     */
    @Min(value = 0, message = "Kinh nghiệm tối đa phải từ 0 năm")
    @Max(value = 50, message = "Kinh nghiệm tối đa không được vượt quá 50 năm")
    Integer maxExperience;
    
    /**
     * Minimum number of students filter
     */
    @Min(value = 0, message = "Số học viên tối thiểu phải từ 0")
    Integer minStudents;
    
    /**
     * Maximum number of students filter
     */
    @Min(value = 0, message = "Số học viên tối đa phải từ 0")
    Integer maxStudents;
    
    /**
     * Minimum number of courses filter
     */
    @Min(value = 0, message = "Số khóa học tối thiểu phải từ 0")
    Integer minCourses;
    
    /**
     * Maximum number of courses filter
     */
    @Min(value = 0, message = "Số khóa học tối đa phải từ 0")
    Integer maxCourses;
    
    /**
     * Minimum number of reviews filter
     */
    @Min(value = 0, message = "Số đánh giá tối thiểu phải từ 0")
    Integer minReviews;
    
    /**
     * Filter by only active courses
     */
    Boolean onlyActiveCourses;
    
    /**
     * Filter by instructors with recent activity (created within last N days)
     */
    @Min(value = 1, message = "Số ngày hoạt động gần đây phải từ 1")
    @Max(value = 365, message = "Số ngày hoạt động gần đây không được vượt quá 365")
    Integer recentActivityDays;
    
    // === Utility methods ===
    
    /**
     * Check if any database-level filter is applied
     */
    public boolean hasDatabaseFilters() {
        return hasText(name) || hasText(email) ||
               createdFrom != null || createdTo != null || enabled != null;
    }
    
    /**
     * Check if any in-memory filter is applied
     */
    public boolean hasInMemoryFilters() {
        return minRating != null || maxRating != null ||
               minExperience != null || maxExperience != null ||
               minStudents != null || maxStudents != null ||
               minCourses != null || maxCourses != null ||
               minReviews != null || onlyActiveCourses != null ||
               recentActivityDays != null;
    }
    
    /**
     * Check if any filter is applied
     */
    public boolean hasAnyFilters() {
        return hasDatabaseFilters() || hasInMemoryFilters();
    }
    
    /**
     * Validate date range consistency
     */
    public boolean isDateRangeValid() {
        if (createdFrom != null && createdTo != null) {
            return !createdFrom.isAfter(createdTo);
        }
        return true;
    }
    
    /**
     * Validate rating range consistency
     */
    public boolean isRatingRangeValid() {
        if (minRating != null && maxRating != null) {
            return minRating <= maxRating;
        }
        return true;
    }
    
    /**
     * Validate experience range consistency
     */
    public boolean isExperienceRangeValid() {
        if (minExperience != null && maxExperience != null) {
            return minExperience <= maxExperience;
        }
        return true;
    }
    
    /**
     * Validate students range consistency
     */
    public boolean isStudentsRangeValid() {
        if (minStudents != null && maxStudents != null) {
            return minStudents <= maxStudents;
        }
        return true;
    }
    
    /**
     * Validate courses range consistency
     */
    public boolean isCoursesRangeValid() {
        if (minCourses != null && maxCourses != null) {
            return minCourses <= maxCourses;
        }
        return true;
    }
    
    /**
     * Get recent activity cutoff date
     */
    public LocalDateTime getRecentActivityCutoff() {
        if (recentActivityDays != null) {
            return LocalDateTime.now().minusDays(recentActivityDays);
        }
        return null;
    }
    
    /**
     * Helper method to check if string has text
     */
    private boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }
} 