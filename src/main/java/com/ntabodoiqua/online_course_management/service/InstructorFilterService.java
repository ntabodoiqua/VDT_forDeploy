package com.ntabodoiqua.online_course_management.service;

import com.ntabodoiqua.online_course_management.dto.request.user.InstructorFilterRequest;
import com.ntabodoiqua.online_course_management.dto.response.user.InstructorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling in-memory filtering of instructor responses
 * Used for calculated fields that cannot be filtered at database level
 */
@Service
@Slf4j
public class InstructorFilterService {

    /**
     * Apply in-memory filters to instructor list
     * Filters based on calculated fields like rating, experience, student count, etc.
     */
    public List<InstructorResponse> applyInMemoryFilters(List<InstructorResponse> instructors, 
                                                       InstructorFilterRequest filter) {
        if (!filter.hasInMemoryFilters()) {
            log.debug("No in-memory filters to apply");
            return instructors;
        }

        log.debug("Applying in-memory filters to {} instructors", instructors.size());
        
        List<InstructorResponse> filtered = instructors.stream()
                .filter(instructor -> applyRatingFilters(instructor, filter))
                .filter(instructor -> applyExperienceFilters(instructor, filter))
                .filter(instructor -> applyStudentFilters(instructor, filter))
                .filter(instructor -> applyCourseFilters(instructor, filter))
                .filter(instructor -> applyReviewFilters(instructor, filter))
                .filter(instructor -> applyActivityFilters(instructor, filter))
                .collect(Collectors.toList());
                
        log.debug("After in-memory filtering: {} instructors remaining", filtered.size());
        return filtered;
    }

    /**
     * Apply rating-based filters
     */
    private boolean applyRatingFilters(InstructorResponse instructor, InstructorFilterRequest filter) {
        Double rating = instructor.getAverageRating();
        if (rating == null) rating = 0.0;

        // Min rating filter
        if (filter.getMinRating() != null && rating < filter.getMinRating()) {
            return false;
        }

        // Max rating filter
        if (filter.getMaxRating() != null && rating > filter.getMaxRating()) {
            return false;
        }

        return true;
    }

    /**
     * Apply experience-based filters
     */
    private boolean applyExperienceFilters(InstructorResponse instructor, InstructorFilterRequest filter) {
        Integer experience = instructor.getExperienceYears();
        if (experience == null) experience = 0;

        // Min experience filter
        if (filter.getMinExperience() != null && experience < filter.getMinExperience()) {
            return false;
        }

        // Max experience filter
        if (filter.getMaxExperience() != null && experience > filter.getMaxExperience()) {
            return false;
        }

        return true;
    }

    /**
     * Apply student count filters
     */
    private boolean applyStudentFilters(InstructorResponse instructor, InstructorFilterRequest filter) {
        Long students = instructor.getTotalStudents();
        if (students == null) students = 0L;

        // Min students filter
        if (filter.getMinStudents() != null && students < filter.getMinStudents()) {
            return false;
        }

        // Max students filter
        if (filter.getMaxStudents() != null && students > filter.getMaxStudents()) {
            return false;
        }

        return true;
    }

    /**
     * Apply course count filters
     */
    private boolean applyCourseFilters(InstructorResponse instructor, InstructorFilterRequest filter) {
        Long courses = instructor.getTotalCourses();
        if (courses == null) courses = 0L;

        // Use active courses if filter is specified
        if (filter.getOnlyActiveCourses() != null && filter.getOnlyActiveCourses()) {
            courses = instructor.getActiveCourses();
            if (courses == null) courses = 0L;
        }

        // Min courses filter
        if (filter.getMinCourses() != null && courses < filter.getMinCourses()) {
            return false;
        }

        // Max courses filter
        if (filter.getMaxCourses() != null && courses > filter.getMaxCourses()) {
            return false;
        }

        return true;
    }

    /**
     * Apply review count filters
     */
    private boolean applyReviewFilters(InstructorResponse instructor, InstructorFilterRequest filter) {
        Integer reviews = instructor.getTotalReviews();
        if (reviews == null) reviews = 0;

        // Min reviews filter
        if (filter.getMinReviews() != null && reviews < filter.getMinReviews()) {
            return false;
        }

        return true;
    }

    /**
     * Apply activity-based filters
     */
    private boolean applyActivityFilters(InstructorResponse instructor, InstructorFilterRequest filter) {
        LocalDateTime recentActivityCutoff = filter.getRecentActivityCutoff();
        
        if (recentActivityCutoff != null) {
            LocalDateTime createdAt = instructor.getCreatedAt();
            if (createdAt == null || createdAt.isBefore(recentActivityCutoff)) {
                return false;
            }
        }

        return true;
    }
} 