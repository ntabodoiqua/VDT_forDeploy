package com.ntabodoiqua.online_course_management.controller;

import com.ntabodoiqua.online_course_management.dto.request.ApiResponse;
import com.ntabodoiqua.online_course_management.dto.request.course.CourseLessonRequest;
import com.ntabodoiqua.online_course_management.dto.request.course.CourseLessonUpdateRequest;
import com.ntabodoiqua.online_course_management.dto.request.lesson.CourseLessonFilterRequest;
import com.ntabodoiqua.online_course_management.dto.response.course.CourseLessonResponse;
import com.ntabodoiqua.online_course_management.service.CourseLessonService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses/{courseId}/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CourseLessonController {
    CourseLessonService courseLessonService;

    // Controller thêm bài học vào khóa học
    @PostMapping
    public ApiResponse<CourseLessonResponse> addLessonToCourse(
            @PathVariable String courseId,
            @RequestBody CourseLessonRequest request
    ) {
        return ApiResponse.<CourseLessonResponse>builder()
                .result(courseLessonService.addLessonToCourse(courseId, request))
                .message("Lesson added to course successfully")
                .build();
    }

    @PatchMapping("/{courseLessonId}")
    public ApiResponse<CourseLessonResponse> updateCourseLesson(
            @PathVariable String courseId,
            @PathVariable String courseLessonId,
            @RequestBody CourseLessonUpdateRequest request
    ) {
        return ApiResponse.<CourseLessonResponse>builder()
                .result(courseLessonService.updateCourseLesson(courseId, courseLessonId, request))
                .message("Course lesson updated successfully")
                .build();
    }

    @DeleteMapping("/{courseLessonId}")
    public ApiResponse<String> removeLessonFromCourse(
            @PathVariable String courseId,
            @PathVariable String courseLessonId
    ) {
        courseLessonService.removeLessonFromCourse(courseId, courseLessonId);
        return ApiResponse.<String>builder()
                .result("Lesson removed from course successfully")
                .build();
    }

    @GetMapping
    public ApiResponse<Page<CourseLessonResponse>> getLessonsOfCourse(
            @PathVariable String courseId,
            @ModelAttribute CourseLessonFilterRequest filter,
            Pageable pageable
    ) {
        return ApiResponse.<Page<CourseLessonResponse>>builder()
                .result(courseLessonService.getLessonsOfCourse(courseId, filter, pageable))
                .build();
    }

    // Lấy thông tin chi tiết của một course lesson theo ID
    @GetMapping("/{courseLessonId}")
    public ApiResponse<CourseLessonResponse> getCourseLessonById(
            @PathVariable String courseId,
            @PathVariable String courseLessonId
    ) {
        return ApiResponse.<CourseLessonResponse>builder()
                .result(courseLessonService.getCourseLessonById(courseLessonId))
                .message("Course lesson fetched successfully")
                .build();
    }

    // Endpoint public cho student xem danh sách bài học với thông tin hạn chế
    @GetMapping("/public")
    public ApiResponse<Page<CourseLessonResponse>> getPublicLessonsOfCourse(
            @PathVariable String courseId,
            @ModelAttribute CourseLessonFilterRequest filter,
            Pageable pageable
    ) {
        return ApiResponse.<Page<CourseLessonResponse>>builder()
                .result(courseLessonService.getPublicLessonsOfCourse(courseId, filter, pageable))
                .message("Public course lessons fetched successfully")
                .build();
    }

}
