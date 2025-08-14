package com.ntabodoiqua.online_course_management.service;

import com.ntabodoiqua.online_course_management.dto.request.course.CourseLessonRequest;
import com.ntabodoiqua.online_course_management.dto.request.course.CourseLessonUpdateRequest;
import com.ntabodoiqua.online_course_management.dto.request.lesson.CourseLessonFilterRequest;
import com.ntabodoiqua.online_course_management.dto.response.course.CourseLessonResponse;
import com.ntabodoiqua.online_course_management.entity.Course;
import com.ntabodoiqua.online_course_management.entity.CourseLesson;
import com.ntabodoiqua.online_course_management.entity.Enrollment;
import com.ntabodoiqua.online_course_management.entity.Lesson;
import com.ntabodoiqua.online_course_management.entity.User;
import com.ntabodoiqua.online_course_management.exception.AppException;
import com.ntabodoiqua.online_course_management.exception.ErrorCode;
import com.ntabodoiqua.online_course_management.mapper.CourseLessonMapper;
import com.ntabodoiqua.online_course_management.repository.CourseLessonRepository;
import com.ntabodoiqua.online_course_management.repository.CourseRepository;
import com.ntabodoiqua.online_course_management.repository.EnrollmentRepository;
import com.ntabodoiqua.online_course_management.repository.LessonRepository;
import com.ntabodoiqua.online_course_management.repository.UserRepository;
import com.ntabodoiqua.online_course_management.specification.CourseLessonSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CourseLessonService {
    CourseRepository courseRepository;
    LessonRepository lessonRepository;
    CourseLessonRepository courseLessonRepository;
    CourseLessonMapper courseLessonMapper;
    UserRepository userRepository;
    EnrollmentRepository enrollmentRepository;
    ProgressService progressService;

    /*** Helper method: kiểm tra quyền instructor/admin hoặc học sinh đã đăng ký courses trên course ***/
    private void checkCoursePermission(Course course) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));
        boolean isInstructor = user.getRoles().stream().anyMatch(r -> r.getName().equals("INSTRUCTOR"));
        boolean isOwner = course.getInstructor() != null && course.getInstructor().getUsername().equals(username);
        // Kiểm tra xem người dùng có phải là học sinh đã đăng ký khóa học không
        boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(user.getId(), course.getId());

        if (!isAdmin && !(isInstructor && isOwner) && !isEnrolled) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    /*** Helper method: kiểm tra quyền instructor/admin trên lesson ***/
    private void checkLessonPermission(Lesson lesson) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));
        boolean isInstructor = user.getRoles().stream().anyMatch(r -> r.getName().equals("INSTRUCTOR"));
        // Assuming Lesson entity has getCreatedBy() returning User who created the lesson
        boolean isCreator = lesson.getCreatedBy() != null && lesson.getCreatedBy().getUsername().equals(username);
        if (!isAdmin && !(isInstructor && isCreator)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * Đồng bộ hóa và cập nhật chính xác totalLessons cho course
     * Sử dụng transaction để đảm bảo consistency
     */
    @Transactional
    private void syncCourseTotalLessons(Course course) {
        // Đếm số lượng bài học thực tế từ database
        long actualLessonCount = courseLessonRepository.countByCourse(course);
        
        // Cập nhật nếu khác biệt
        if (course.getTotalLessons() != (int) actualLessonCount) {
            log.info("Syncing totalLessons for course {}: {} -> {}", 
                course.getId(), course.getTotalLessons(), actualLessonCount);
            course.setTotalLessons((int) actualLessonCount);
            courseRepository.save(course);
        }
    }

    @Transactional
    public CourseLessonResponse addLessonToCourse(String courseId, CourseLessonRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
        checkCoursePermission(course);

        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        checkLessonPermission(lesson);

        // Không cho phép thêm trùng bài học vào cùng một khóa học
        boolean alreadyExists = courseLessonRepository.findByCourseOrderByOrderIndexAsc(course)
                .stream().anyMatch(cl -> cl.getLesson().getId().equals(lesson.getId()));
        if (alreadyExists) throw new AppException(ErrorCode.LESSON_ALREADY_IN_COURSE);

        // Xử lý prerequisite
        CourseLesson prerequisite = null;
        if (request.getPrerequisiteCourseLessonId() != null) {
            prerequisite = courseLessonRepository.findById(request.getPrerequisiteCourseLessonId())
                    .orElseThrow(() -> new AppException(ErrorCode.PREQUISITE_NOT_FOUND));
            if (!prerequisite.getCourse().getId().equals(courseId))
                throw new AppException(ErrorCode.PREQUISITE_MUST_SAME_COURSE);
        }

        // Gán orderIndex
        Integer orderIndex = request.getOrderIndex();
        if (orderIndex == null) {
            // Nếu không truyền, mặc định cuối cùng
            int maxOrder = courseLessonRepository.findByCourseOrderByOrderIndexAsc(course)
                    .stream().mapToInt(CourseLesson::getOrderIndex).max().orElse(0);
            orderIndex = maxOrder + 1;
        }

        CourseLesson courseLesson = CourseLesson.builder()
                .course(course)
                .lesson(lesson)
                .orderIndex(orderIndex)
                .isVisible(request.getIsVisible() != null ? request.getIsVisible() : true)
                .prerequisite(prerequisite)
                .build();

        courseLesson = courseLessonRepository.save(courseLesson);

        // Đồng bộ hóa totalLessons với database thực tế
        syncCourseTotalLessons(course);

        // Cập nhật progress cho tất cả enrollment của course
        updateProgressForAllEnrollmentsInCourse(course);

        return courseLessonMapper.toCourseLessonResponse(courseLesson);
    }

    @Transactional
    public CourseLessonResponse updateCourseLesson(String courseId, String courseLessonId, CourseLessonUpdateRequest request) {
        CourseLesson courseLesson = courseLessonRepository.findById(courseLessonId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND));

        checkCoursePermission(courseLesson.getCourse());
        if (!courseLesson.getCourse().getId().equals(courseId))
            throw new AppException(ErrorCode.COURSE_LESSON_COURSE_MISMATCH);

        // Update orderIndex, isVisible
        if (request.getOrderIndex() != null) {
            courseLesson.setOrderIndex(request.getOrderIndex());
        }
        if (request.getIsVisible() != null) {
            courseLesson.setIsVisible(request.getIsVisible());
        }

        // Update prerequisite
        if (request.getPrerequisiteCourseLessonId() != null) {
            if (request.getPrerequisiteCourseLessonId().equals(courseLessonId))
                throw new AppException(ErrorCode.PREQUISITE_CANNOT_SELF);

            CourseLesson prerequisite = courseLessonRepository.findById(request.getPrerequisiteCourseLessonId())
                    .orElseThrow(() -> new AppException(ErrorCode.PREQUISITE_NOT_FOUND));
            if (!prerequisite.getCourse().getId().equals(courseId))
                throw new AppException(ErrorCode.PREQUISITE_MUST_SAME_COURSE);

            // Kiểm tra vòng lặp prerequisite (optional)
            if (isCircularPrerequisite(courseLesson, prerequisite)) {
                throw new AppException(ErrorCode.PREQUISITE_CIRCULAR);
            }
            courseLesson.setPrerequisite(prerequisite);
        } else {
            courseLesson.setPrerequisite(null);
        }

        courseLesson = courseLessonRepository.save(courseLesson);
        return courseLessonMapper.toCourseLessonResponse(courseLesson);
    }

    @Transactional
    public void removeLessonFromCourse(String courseId, String courseLessonId) {
        CourseLesson courseLessonToRemove = courseLessonRepository.findById(courseLessonId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND));

        checkCoursePermission(courseLessonToRemove.getCourse());
        if (!courseLessonToRemove.getCourse().getId().equals(courseId))
            throw new AppException(ErrorCode.COURSE_LESSON_COURSE_MISMATCH);

        // Kiểm tra xem bài học có phải là điều kiện tiên quyết của bài học khác không
        if (courseLessonRepository.existsByPrerequisiteId(courseLessonId)) {
            throw new AppException(ErrorCode.COURSE_LESSON_HAS_DEPENDENT);
        }

        Course course = courseLessonToRemove.getCourse();
        int removedOrderIndex = courseLessonToRemove.getOrderIndex();

        courseLessonRepository.delete(courseLessonToRemove);

        // Cập nhật orderIndex của các bài học đứng sau
        List<CourseLesson> subsequentLessons = courseLessonRepository
                .findByCourseAndOrderIndexGreaterThanOrderByOrderIndexAsc(course, removedOrderIndex);

        for (CourseLesson lesson : subsequentLessons) {
            lesson.setOrderIndex(lesson.getOrderIndex() - 1);
            courseLessonRepository.save(lesson);
        }

        // Đồng bộ hóa totalLessons với database thực tế
        syncCourseTotalLessons(course);

        // Cập nhật progress cho tất cả enrollment của course
        updateProgressForAllEnrollmentsInCourse(course);
    }

    public Page<CourseLessonResponse> getLessonsOfCourse(String courseId, CourseLessonFilterRequest filter, Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
        // For fetching lessons, we might want a different type of permission check (e.g., public published courses for students)
        // However, sticking to existing strict permission: only admin or instructor-owner can see the lesson list via this service.
        checkCoursePermission(course);

        return courseLessonRepository.findAll(
                CourseLessonSpecification.withFilter(courseId, filter), pageable
        ).map(courseLessonMapper::toCourseLessonResponse);
    }

    /*** Hàm kiểm tra vòng lặp prerequisite ***/
    private boolean isCircularPrerequisite(CourseLesson current, CourseLesson prerequisite) {
        CourseLesson cursor = prerequisite;
        while (cursor != null) {
            if (cursor.getId().equals(current.getId())) return true;
            cursor = cursor.getPrerequisite();
        }
        return false;
    }

    private void updateProgressForAllEnrollmentsInCourse(Course course) {
        List<Enrollment> enrollments = enrollmentRepository.findByCourse(course);
        for (Enrollment enrollment : enrollments) {
            progressService.recalculateAndSaveEnrollmentProgress(enrollment);
        }
    }

    // Lấy thông tin course lesson theo ID
    public CourseLessonResponse getCourseLessonById(String courseLessonId) {
        // Kiểm tra xem người dùng có quyền truy cập vào bài học này không

        CourseLesson courseLesson = courseLessonRepository.findById(courseLessonId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND));
        return courseLessonMapper.toCourseLessonResponse(courseLesson);
    }

    /**
     * Lấy danh sách bài học cho student
     * - Nếu student đã enrolled: trả về full content
     * - Nếu chưa enrolled: chỉ trả về thông tin cơ bản
     */
    public Page<CourseLessonResponse> getPublicLessonsOfCourse(String courseId, CourseLessonFilterRequest filter, Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
        
        // Chỉ cho phép xem khóa học active
        if (!course.isActive()) {
            throw new AppException(ErrorCode.COURSE_NOT_ACTIVE);
        }

        // Lấy tất cả bài học visible của khóa học, sắp xếp theo orderIndex
        List<CourseLesson> allCourseLessons = courseLessonRepository.findByCourseAndIsVisibleTrueOrderByOrderIndexAsc(course);

        // Kiểm tra enrollment của user hiện tại

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        final boolean isEnrolled;
        
        if (!"anonymousUser".equals(username)) {
            User currentUser = userRepository.findByUsername(username).orElse(null);
            if (currentUser != null) {
                isEnrolled = enrollmentRepository.findByStudent(currentUser)
                        .stream()
                        .anyMatch(enrollment -> enrollment.getCourse().getId().equals(courseId));
            } else {
                isEnrolled = false;
            }
        } else {
            isEnrolled = false;
        }

        // Convert sang response dựa vào enrollment status
        List<CourseLessonResponse> responses = allCourseLessons.stream()
                .map(courseLesson -> isEnrolled ? 
                        courseLessonMapper.toCourseLessonResponse(courseLesson) : 
                        mapToPublicCourseLessonResponse(courseLesson))
                .collect(Collectors.toList());

        // Tạo Page manually để phù hợp với Pageable
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        
        List<CourseLessonResponse> pagedContent = start < responses.size() ? 
                responses.subList(start, end) : 
                new ArrayList<>();

        return new PageImpl<>(pagedContent, pageable, responses.size());
    }

    /**
     * Map CourseLesson sang response với thông tin hạn chế cho public
     */
    private CourseLessonResponse mapToPublicCourseLessonResponse(CourseLesson courseLesson) {
        Lesson lesson = courseLesson.getLesson();
        
        return CourseLessonResponse.builder()
                .id(courseLesson.getId())
                .lesson(com.ntabodoiqua.online_course_management.dto.response.lesson.LessonResponse.builder()
                        .id(lesson.getId())
                        .title(lesson.getTitle())
                        .description(lesson.getDescription())
                        // Không hiển thị content, duration, hoặc thông tin chi tiết khác
                        .build())
                .orderIndex(courseLesson.getOrderIndex())
                .isVisible(courseLesson.getIsVisible())
                // Không hiển thị prerequisite để tránh lộ cấu trúc khóa học
                .build();
    }

    /**
     * Batch sync totalLessons cho tất cả courses
     * Admin utility method để fix inconsistency
     */
    @Transactional
    public void batchSyncAllCoursesTotalLessons() {
        log.info("Starting batch sync for all courses totalLessons");
        List<Course> allCourses = courseRepository.findAll();
        
        int updatedCount = 0;
        for (Course course : allCourses) {
            long actualLessonCount = courseLessonRepository.countByCourse(course);
            if (course.getTotalLessons() != (int) actualLessonCount) {
                log.info("Syncing course {}: {} -> {}", 
                    course.getId(), course.getTotalLessons(), actualLessonCount);
                course.setTotalLessons((int) actualLessonCount);
                courseRepository.save(course);
                updatedCount++;
            }
        }
        
        log.info("Batch sync completed. Updated {} out of {} courses", updatedCount, allCourses.size());
    }

    /**
     * Sync totalLessons cho một course cụ thể - public method
     */
    @Transactional
    public void syncSpecificCourseTotalLessons(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
        
        checkCoursePermission(course);
        syncCourseTotalLessons(course);
        
        log.info("Synced totalLessons for course: {}", courseId);
    }
}
