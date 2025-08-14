package com.ntabodoiqua.online_course_management.service;

import com.ntabodoiqua.online_course_management.dto.request.course.CourseCreationRequest;
import com.ntabodoiqua.online_course_management.dto.request.course.CourseUpdateRequest;
import com.ntabodoiqua.online_course_management.dto.response.course.CourseResponse;
import com.ntabodoiqua.online_course_management.dto.response.enrollment.EnrollmentResponse;
import com.ntabodoiqua.online_course_management.entity.Category;
import com.ntabodoiqua.online_course_management.entity.Course;
import com.ntabodoiqua.online_course_management.entity.Enrollment;
import com.ntabodoiqua.online_course_management.entity.User;
import com.ntabodoiqua.online_course_management.enums.DefaultUrl;
import com.ntabodoiqua.online_course_management.enums.EnrollmentStatus;
import com.ntabodoiqua.online_course_management.exception.AppException;
import com.ntabodoiqua.online_course_management.exception.ErrorCode;
import com.ntabodoiqua.online_course_management.mapper.CourseMapper;
import com.ntabodoiqua.online_course_management.mapper.EnrollmentMapper;
import com.ntabodoiqua.online_course_management.repository.CategoryRepository;
import com.ntabodoiqua.online_course_management.repository.CourseRepository;
import com.ntabodoiqua.online_course_management.repository.CourseReviewRepository;
import com.ntabodoiqua.online_course_management.repository.EnrollmentRepository;
import com.ntabodoiqua.online_course_management.repository.UserRepository;
import com.ntabodoiqua.online_course_management.service.file.FileStorageService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EnrollmentService {

    CourseRepository courseRepository;
    UserRepository userRepository;
    EnrollmentRepository enrollmentRepository;
    EnrollmentMapper enrollmentMapper;
    CourseReviewRepository courseReviewRepository;

    // Lấy người dùng hiện tại
    User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public String enroll(String courseId) {
        User student = getCurrentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        if (!course.isActive() || course.getStartDate().isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.COURSE_NOT_AVAILABLE);
        }

        enrollmentRepository.findByStudentIdAndCourseId(student.getId(), courseId).ifPresent(
                e -> {
                    throw new AppException(ErrorCode.ALREADY_ENROLLED);
                }
        );

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .enrollmentDate(LocalDate.now())
                .isCompleted(false)
                .progress(0.0)
                .approvalStatus(course.isRequiresApproval() ?
                        EnrollmentStatus.PENDING : EnrollmentStatus.APPROVED)
                .build();
        enrollmentRepository.save(enrollment);
        return "Enrollment successful for course: " + course.getTitle();
    }

    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public String cancelEnrollment(String courseId) {
        User student = getCurrentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(student.getId(), courseId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_EXISTED));
        if (enrollment.isCompleted()) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_COMPLETED_ENROLLMENT);
        }
        enrollmentRepository.delete(enrollment);
        return "Enrollment cancelled for course: " + course.getTitle();
    }

    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public Page<EnrollmentResponse> getMyEnrollments(Pageable pageable) {
        User student = getCurrentUser();
        return enrollmentRepository.findByStudent(student, pageable)
                .map(enrollment -> {
                    EnrollmentResponse response = enrollmentMapper.toEnrollmentResponse(enrollment);
                    // Enrich course response with rating data
                    if (response.getCourse() != null) {
                        enrichCourseWithRatingData(response.getCourse());
                    }
                    return response;
                });
    }

    /**
     * Enrich CourseResponse with rating data
     */
    private void enrichCourseWithRatingData(com.ntabodoiqua.online_course_management.dto.response.course.CourseResponse courseResponse) {
        try {
            String courseId = courseResponse.getId();
            Double averageRating = courseReviewRepository.findAverageRatingByCourseId(courseId);
            Integer totalReviews = courseReviewRepository.countByCourseIdAndIsApprovedTrue(courseId);
            
            courseResponse.setAverageRating(averageRating);
            courseResponse.setTotalReviews(totalReviews);
            
            log.debug("Enriched course {} with rating data - Average: {}, Total reviews: {}", 
                     courseId, averageRating, totalReviews);
        } catch (Exception e) {
            log.warn("Failed to enrich course {} with rating data: {}", courseResponse.getId(), e.getMessage());
            courseResponse.setAverageRating(null);
            courseResponse.setTotalReviews(0);
        }
    }

    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public String approveEnrollment(String enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_EXISTED));

        if (enrollment.getApprovalStatus() != EnrollmentStatus.PENDING) {
            throw new AppException(ErrorCode.ENROLLMENT_ALREADY_PROCESSED);
        }

        enrollment.setApprovalStatus(EnrollmentStatus.APPROVED);
        enrollmentRepository.save(enrollment);
        return "Enrollment approved for course: " + enrollment.getCourse().getTitle();
    }

    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public String rejectEnrollment(String enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_EXISTED));

        if (enrollment.getApprovalStatus() != EnrollmentStatus.PENDING) {
            throw new AppException(ErrorCode.ENROLLMENT_ALREADY_PROCESSED);
        }

        enrollment.setApprovalStatus(EnrollmentStatus.REJECTED);
        enrollmentRepository.save(enrollment);
        return "Enrollment rejected for course: " + enrollment.getCourse().getTitle();
    }

    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public Page<EnrollmentResponse> getPendingEnrollmentsByCourse(String courseId, Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        return enrollmentRepository.findByCourseAndApprovalStatus(course, EnrollmentStatus.PENDING, pageable)
                .map(enrollmentMapper::toEnrollmentResponse);
    }

    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public Page<EnrollmentResponse> getApprovedEnrollmentsByCourse(String courseId, Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        return enrollmentRepository.findByCourseAndApprovalStatus(course, EnrollmentStatus.APPROVED, pageable)
                .map(enrollmentMapper::toEnrollmentResponse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<EnrollmentResponse> getAllEnrollmentsForAdmin(Pageable pageable) {
        return enrollmentRepository.findAll(pageable)
                .map(enrollmentMapper::toEnrollmentResponse);
    }

    @PreAuthorize("hasRole('STUDENT')")
    public EnrollmentResponse getMyEnrollmentForCourse(String courseId) {
        User currentUser = getCurrentUser();
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(currentUser.getId(), courseId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_EXISTED));
        return enrollmentMapper.toEnrollmentResponse(enrollment);
    }

}
