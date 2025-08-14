package com.ntabodoiqua.online_course_management.service;

import com.ntabodoiqua.online_course_management.dto.request.user.InstructorFilterRequest;
import com.ntabodoiqua.online_course_management.dto.response.course.CourseResponse;
import com.ntabodoiqua.online_course_management.dto.response.user.InstructorResponse;
import com.ntabodoiqua.online_course_management.entity.User;
import com.ntabodoiqua.online_course_management.exception.AppException;
import com.ntabodoiqua.online_course_management.exception.ErrorCode;
import com.ntabodoiqua.online_course_management.mapper.CourseMapper;
import com.ntabodoiqua.online_course_management.mapper.UserMapper;
import com.ntabodoiqua.online_course_management.repository.CourseRepository;
import com.ntabodoiqua.online_course_management.repository.CourseReviewRepository;
import com.ntabodoiqua.online_course_management.repository.EnrollmentRepository;
import com.ntabodoiqua.online_course_management.repository.UserRepository;
import com.ntabodoiqua.online_course_management.specification.InstructorSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling public instructor operations
 * Provides functionality for fetching and filtering instructors without authentication
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class InstructorPublicService {

    UserRepository userRepository;
    CourseRepository courseRepository;
    EnrollmentRepository enrollmentRepository;
    CourseReviewRepository courseReviewRepository;
    UserMapper userMapper;
    CourseMapper courseMapper;
    InstructorFilterService instructorFilterService;

    /**
     * Get top instructors ordered by average rating
     * Uses specification pattern for database filtering
     */
    public List<InstructorResponse> getTopInstructors(int limit) {
        log.info("Fetching top {} instructors and admins", limit);
        
        // Use specification to get all instructors and admins
        Specification<User> spec = InstructorSpecification.getTopInstructors();
        List<User> users = userRepository.findAll(spec);
        
        // Convert to InstructorResponse and filter by rating
        List<InstructorResponse> instructorResponses = users.stream()
                .map(this::mapToInstructorResponse)
                .filter(instructor -> instructor.getAverageRating() != null && instructor.getAverageRating() > 0)
                .sorted((a, b) -> Double.compare(b.getAverageRating(), a.getAverageRating()))
                .limit(limit)
                .collect(Collectors.toList());

        log.info("Found {} top instructors and admins", instructorResponses.size());
        return instructorResponses;
    }

    /**
     * Get all instructors with filtering and pagination
     * Uses specification pattern for database filtering and service for in-memory filtering
     */
    public Page<InstructorResponse> getAllInstructors(InstructorFilterRequest filter, Pageable pageable) {
        log.info("Fetching all instructors and admins with filter: {}", filter);
        
        // Validate filter parameters
        validateFilterRequest(filter);
        
        // Build specification for database-level filtering
        Specification<User> spec = InstructorSpecification.filterInstructors(filter);
        
        // Get paginated instructors from database
        Page<User> instructorPage = userRepository.findAll(spec, pageable);
        
        // Convert to InstructorResponse
        List<InstructorResponse> instructorResponses = instructorPage.getContent().stream()
                .map(this::mapToInstructorResponse)
                .collect(Collectors.toList());

        // Apply in-memory filtering for calculated fields
        List<InstructorResponse> filteredInstructors = instructorFilterService
                .applyInMemoryFilters(instructorResponses, filter);

        log.info("Found {} instructors and admins on page {} of {} (after filtering: {} remaining)", 
                instructorPage.getContent().size(), pageable.getPageNumber(), 
                instructorPage.getTotalPages(), filteredInstructors.size());
        
        // Note: Total elements count may not be accurate due to in-memory filtering
        // For exact pagination with in-memory filters, consider fetching all records first
        return new PageImpl<>(filteredInstructors, pageable, instructorPage.getTotalElements());
    }

    /**
     * Get instructor by ID with detailed information
     */
    public InstructorResponse getInstructorById(String instructorId) {
        log.info("Fetching instructor details for ID: {}", instructorId);
        
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Verify user is an instructor or admin
        boolean isInstructorOrAdmin = instructor.getRoles().stream()
                .anyMatch(role -> "INSTRUCTOR".equals(role.getName()) || "ADMIN".equals(role.getName()));
        
        if (!isInstructorOrAdmin) {
            log.warn("User {} is not an instructor or admin", instructorId);
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        InstructorResponse response = mapToInstructorResponse(instructor);
        log.info("Successfully retrieved instructor details for: {}", instructor.getUsername());
        return response;
    }

    /**
     * Get instructor's courses with pagination
     */
    public Page<CourseResponse> getInstructorCourses(String instructorId, Pageable pageable) {
        log.info("Fetching courses for instructor ID: {}", instructorId);
        
        // Verify instructor exists and has proper role
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        boolean isInstructorOrAdmin = instructor.getRoles().stream()
                .anyMatch(role -> "INSTRUCTOR".equals(role.getName()) || "ADMIN".equals(role.getName()));
        
        if (!isInstructorOrAdmin) {
            log.warn("User {} is not an instructor or admin", instructorId);
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }
        
        // Get instructor's courses
        var coursePage = courseRepository.findByInstructorIdAndIsActiveTrue(instructorId, pageable);
        
        // Convert to CourseResponse and enrich with statistics
        List<CourseResponse> courseResponses = coursePage.getContent().stream()
                .map(course -> {
                    CourseResponse response = courseMapper.toCourseResponse(course);
                    Double avgRating = courseReviewRepository.findAverageRatingByCourseId(course.getId());
                    Integer totalReviews = courseReviewRepository.countByCourseIdAndIsApprovedTrue(course.getId());
                    response.setAverageRating(avgRating);
                    response.setTotalReviews(totalReviews);
                    return response;
                })
                .collect(Collectors.toList());

        log.info("Found {} courses for instructor: {}", courseResponses.size(), instructor.getUsername());
        return new PageImpl<>(courseResponses, pageable, coursePage.getTotalElements());
    }

    /**
     * Validate filter request parameters
     */
    private void validateFilterRequest(InstructorFilterRequest filter) {
        if (!filter.isDateRangeValid()) {
            throw new AppException(ErrorCode.INVALID_KEY); // Consider creating specific error for invalid date range
        }
        
        if (!filter.isRatingRangeValid()) {
            throw new AppException(ErrorCode.INVALID_KEY); // Consider creating specific error for invalid rating range
        }
        
        if (!filter.isExperienceRangeValid()) {
            throw new AppException(ErrorCode.INVALID_KEY); // Consider creating specific error for invalid experience range
        }
        
        if (!filter.isStudentsRangeValid()) {
            throw new AppException(ErrorCode.INVALID_KEY); // Consider creating specific error for invalid students range
        }
        
        if (!filter.isCoursesRangeValid()) {
            throw new AppException(ErrorCode.INVALID_KEY); // Consider creating specific error for invalid courses range
        }
    }

    /**
     * Convert User entity to InstructorResponse with statistics
     * Maps basic user information and enriches with calculated statistics
     */
    private InstructorResponse mapToInstructorResponse(User user) {
        // Map basic user info using builder pattern
        InstructorResponse response = InstructorResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dob(user.getDob())
                .avatarUrl(user.getAvatarUrl())
                .email(user.getEmail())
                .phone(user.getPhone())
                .bio(user.getBio())
                .gender(user.getGender())
                .createdAt(user.getCreatedAt())
                .enabled(user.isEnabled())
                .build();

        // Calculate and enrich with statistics
        enrichWithStatistics(response, user.getId());
        
        return response;
    }

    /**
     * Enrich instructor response with calculated statistics
     * Fetches and calculates various metrics for the instructor
     */
    private void enrichWithStatistics(InstructorResponse response, String instructorId) {
        try {
            // Get course statistics
            Long totalCourses = courseRepository.countByInstructorId(instructorId);
            Long activeCourses = courseRepository.countByInstructorIdAndIsActiveTrue(instructorId);
            response.setTotalCourses(totalCourses != null ? totalCourses : 0L);
            response.setActiveCourses(activeCourses != null ? activeCourses : 0L);
            
            // Get student statistics
            Long totalStudents = enrollmentRepository.countDistinctStudentsByInstructorId(instructorId);
            response.setTotalStudents(totalStudents);
            
            // Get rating and review statistics
            Double averageRating = courseReviewRepository.findAverageRatingByInstructorId(instructorId);
            Integer totalReviews = courseReviewRepository.countApprovedReviewsByInstructorId(instructorId);
            response.setAverageRating(averageRating != null ? averageRating : 0.0);
            response.setTotalReviews(totalReviews != null ? totalReviews : 0);
            
            // Calculate experience years from account creation
            calculateExperienceYears(response);
            
            // Set specialty based on bio analysis

            
            log.debug("Enriched statistics for instructor {}: {} courses, {} students, {:.1f} rating", 
                    instructorId, totalCourses, totalStudents, averageRating);
            
        } catch (Exception e) {
            log.warn("Error enriching statistics for instructor {}: {}", instructorId, e.getMessage());
            setDefaultStatistics(response);
        }
    }
    
    /**
     * Calculate experience years based on account creation date
     */
    private void calculateExperienceYears(InstructorResponse response) {
        if (response.getCreatedAt() != null) {
            long years = ChronoUnit.YEARS.between(response.getCreatedAt(), LocalDateTime.now());
            response.setExperienceYears((int) Math.max(1, years));
        } else {
            response.setExperienceYears(1);
        }
    }
    

    
    /**
     * Set default statistics when enrichment fails
     */
    private void setDefaultStatistics(InstructorResponse response) {
        response.setTotalCourses(0L);
        response.setActiveCourses(0L);
        response.setTotalStudents(0L);
        response.setAverageRating(0.0);
        response.setTotalReviews(0);
        response.setExperienceYears(1);
    }


} 