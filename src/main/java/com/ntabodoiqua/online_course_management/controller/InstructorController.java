package com.ntabodoiqua.online_course_management.controller;

import com.ntabodoiqua.online_course_management.dto.request.ApiResponse;
import com.ntabodoiqua.online_course_management.dto.request.user.InstructorFilterRequest;
import com.ntabodoiqua.online_course_management.dto.response.course.CourseResponse;
import com.ntabodoiqua.online_course_management.dto.response.user.InstructorResponse;
import com.ntabodoiqua.online_course_management.service.InstructorPublicService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * Controller for handling public instructor operations
 * Provides endpoints for students and admins to view instructor information
 */
@RestController
@RequestMapping("/instructors")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Validated
public class InstructorController {
    
    InstructorPublicService instructorPublicService;

    /**
     * Get top-rated instructors (public for everyone)
     * @param limit Number of top instructors to return (1-50)
     * @return Top instructors ordered by rating
     */
    @GetMapping("/public/top")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<InstructorResponse>> getTopInstructors(
            @RequestParam(defaultValue = "10") 
            @Min(value = 1, message = "Limit phải ít nhất là 1")
            @Max(value = 50, message = "Limit không được vượt quá 50") 
            int limit) {
        
        log.info("Getting top {} instructors", limit);
        List<InstructorResponse> topInstructors = instructorPublicService.getTopInstructors(limit);
        
        return ApiResponse.<List<InstructorResponse>>builder()
                .code(1000)
                .message("Successfully retrieved top instructors")
                .result(topInstructors)
                .build();
    }

    /**
     * Get all instructors with filtering and pagination (public for everyone)
     * @param filter Filter criteria with validation
     * @param pageable Pagination parameters
     * @return Paginated list of instructors
     */
    @GetMapping("/public")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Page<InstructorResponse>> getAllInstructors(
            @Valid @ModelAttribute InstructorFilterRequest filter,
            Pageable pageable) {
        
        log.info("Getting all instructors with filter: {} and pageable: {}", filter, pageable);
        Page<InstructorResponse> instructors = instructorPublicService.getAllInstructors(filter, pageable);
        
        return ApiResponse.<Page<InstructorResponse>>builder()
                .code(1000)
                .message("Successfully retrieved instructors")
                .result(instructors)
                .build();
    }

    /**
     * Get instructor by ID with detailed information (public for everyone)
     * @param instructorId Instructor ID
     * @return Detailed instructor information
     */
    @GetMapping("/public/{instructorId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<InstructorResponse> getInstructorById(
            @PathVariable String instructorId) {
        
        log.info("Getting instructor details for ID: {}", instructorId);
        InstructorResponse instructor = instructorPublicService.getInstructorById(instructorId);
        
        return ApiResponse.<InstructorResponse>builder()
                .code(1000)
                .message("Successfully retrieved instructor details")
                .result(instructor)
                .build();
    }

    /**
     * Get instructor courses (public for everyone)
     * @param instructorId Instructor ID
     * @param pageable Pagination parameters
     * @return Paginated list of instructor's courses
     */
    @GetMapping("/public/{instructorId}/courses")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Page<CourseResponse>> getInstructorCourses(
            @PathVariable String instructorId,
            Pageable pageable) {
        
        log.info("Getting courses for instructor ID: {} with pageable: {}", instructorId, pageable);
        Page<CourseResponse> courses = instructorPublicService.getInstructorCourses(instructorId, pageable);
        
        return ApiResponse.<Page<CourseResponse>>builder()
                .code(1000)
                .message("Successfully retrieved instructor courses")
                .result(courses)
                .build();
    }
} 