package com.ntabodoiqua.online_course_management.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntabodoiqua.online_course_management.dto.request.ApiResponse;
import com.ntabodoiqua.online_course_management.dto.request.course.CourseCreationRequest;
import com.ntabodoiqua.online_course_management.dto.request.course.CourseFilterRequest;
import com.ntabodoiqua.online_course_management.dto.request.course.CourseUpdateRequest;
import com.ntabodoiqua.online_course_management.dto.request.lesson.LessonFilterRequest;
import com.ntabodoiqua.online_course_management.dto.response.course.CourseResponse;
import com.ntabodoiqua.online_course_management.dto.response.enrollment.EnrollmentResponse;
import com.ntabodoiqua.online_course_management.dto.response.lesson.LessonResponse;
import com.ntabodoiqua.online_course_management.exception.AppException;
import com.ntabodoiqua.online_course_management.exception.ErrorCode;
import com.ntabodoiqua.online_course_management.service.CourseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.ntabodoiqua.online_course_management.dto.response.course.PopularCourseResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CourseController {
    CourseService courseService;
    @Autowired
    private final ObjectMapper objectMapper;
    // API tạo khóa học mới với thông tin và hình ảnh thumbnail
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CourseResponse> createCourse(
            @RequestPart("course") String courseJson,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
        try {
            CourseCreationRequest request = objectMapper.readValue(courseJson, CourseCreationRequest.class);
            CourseResponse courseResponse = courseService.createCourse(request, thumbnail);

            return ApiResponse.<CourseResponse>builder()
                    .message("Course created successfully")
                    .result(courseResponse)
                    .build();
        } catch (IOException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

//    // API lấy toàn bộ bài học với các tham số lọc
//    @GetMapping
//    public ApiResponse<Page<LessonResponse>> getAllLessons(@ModelAttribute LessonFilterRequest filter, Pageable pageable) {
//        return ApiResponse.<Page<LessonResponse>>builder()
//                .result(lessonService.getAllLessons(filter, pageable))
//                .build();
//    }

    // API lấy danh sách khóa học với các tham số lọc và phân trang
    @GetMapping
    public ApiResponse<Page<CourseResponse>> getAllCourses(
            @ModelAttribute CourseFilterRequest filter,
            Pageable pageable) {
        Page<CourseResponse> courses = courseService.getCourses(filter, pageable);
        return ApiResponse.<Page<CourseResponse>>builder()
                .result(courses)
                .build();
    }

    @GetMapping("/public")
    public ApiResponse<Page<CourseResponse>> getAllPublicCourses(
            @ModelAttribute CourseFilterRequest filter,
            Pageable pageable) {
        Page<CourseResponse> courses = courseService.getCourses(filter, pageable);
        return ApiResponse.<Page<CourseResponse>>builder()
                .result(courses)
                .build();
    }

    // API lấy danh sách các khóa học của tôi
    @GetMapping("/my")
    public ApiResponse<Page<EnrollmentResponse>> getMyCourses(Pageable pageable) {
        Page<EnrollmentResponse> courses = courseService.getMyCourses(pageable);
        return ApiResponse.<Page<EnrollmentResponse>>builder()
                .result(courses)
                .build();
    }

    // API lấy thông tin chi tiết của một khóa học theo ID
    @GetMapping("/{courseId}")
    public ApiResponse<CourseResponse> getCourseById(@PathVariable String courseId) {
        return ApiResponse.<CourseResponse>builder().result(courseService.getCourseById(courseId)).build();
    }

    @GetMapping("/public/{courseId}")
    public ApiResponse<CourseResponse> getPublicCourseById(@PathVariable String courseId) {
        return ApiResponse.<CourseResponse>builder().result(courseService.getCourseById(courseId)).build();
    }

    @GetMapping("/public/popular")
    public ApiResponse<List<PopularCourseResponse>> getPopularCourses(@RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.<List<PopularCourseResponse>>builder()
                .result(courseService.getPopularCourses(limit))
                .build();
    }

    // API xóa khóa học theo ID
    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ApiResponse<String> deleteCourse(@PathVariable String courseId) {
        courseService.deleteCourse(courseId);
        return ApiResponse.<String>builder()
                .message("Course deleted successfully")
                .result("Course has been deleted")
                .build();
    }

    // API cập nhật thông tin khóa học với thông tin và hình ảnh thumbnail
    @PutMapping(value = "/{courseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CourseResponse> updateCourse(
            @PathVariable String courseId,
            @RequestPart("course") String courseJson,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
        try {
            CourseUpdateRequest request = objectMapper.readValue(courseJson, CourseUpdateRequest.class);
            CourseResponse courseResponse = courseService.updateCourse(courseId, request, thumbnail);

            return ApiResponse.<CourseResponse>builder()
                    .message("Course updated successfully")
                    .result(courseResponse)
                    .build();
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    // API toggle trạng thái khóa học (đơn giản)
    @PatchMapping("/{courseId}/toggle-status")
    public ApiResponse<CourseResponse> toggleCourseStatus(
            @PathVariable String courseId,
            @RequestBody Map<String, Boolean> statusRequest) {
        Boolean isActive = statusRequest.get("isActive");
        if (isActive == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        
        CourseResponse courseResponse = courseService.toggleCourseStatus(courseId, isActive);
        return ApiResponse.<CourseResponse>builder()
                .message("Course status updated successfully")
                .result(courseResponse)
                .build();
    }

    // API đồng bộ totalLessons cho tất cả khóa học (Admin only)
    @PostMapping("/admin/sync-all-total-lessons")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> syncAllCoursesTotalLessons() {
        courseService.syncAllCoursesTotalLessons();
        return ApiResponse.<String>builder()
                .message("Successfully synced totalLessons for all courses")
                .result("Sync completed")
                .build();
    }

    // API đồng bộ totalLessons cho một khóa học cụ thể
    @PostMapping("/{courseId}/sync-total-lessons")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ApiResponse<String> syncCourseTotalLessons(@PathVariable String courseId) {
        courseService.syncCourseTotalLessons(courseId);
        return ApiResponse.<String>builder()
                .message("Successfully synced totalLessons for course")
                .result("Sync completed for course: " + courseId)
                .build();
    }

}
