package com.ntabodoiqua.online_course_management.service;

import com.ntabodoiqua.online_course_management.dto.request.review.CourseReviewRequest;
import com.ntabodoiqua.online_course_management.dto.response.review.CourseReviewResponse;
import com.ntabodoiqua.online_course_management.entity.Course;
import com.ntabodoiqua.online_course_management.entity.CourseReview;
import com.ntabodoiqua.online_course_management.entity.Enrollment;
import com.ntabodoiqua.online_course_management.entity.User;
import com.ntabodoiqua.online_course_management.exception.ErrorCode;
import com.ntabodoiqua.online_course_management.exception.AppException;
import com.ntabodoiqua.online_course_management.mapper.CourseMapper;
import com.ntabodoiqua.online_course_management.mapper.UserMapper;
import com.ntabodoiqua.online_course_management.repository.CourseRepository;
import com.ntabodoiqua.online_course_management.repository.CourseReviewRepository;
import com.ntabodoiqua.online_course_management.repository.EnrollmentRepository;
import com.ntabodoiqua.online_course_management.repository.UserRepository;
import com.ntabodoiqua.online_course_management.specification.CourseReviewSpecification;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class CourseReviewService {
    CourseReviewRepository courseReviewRepository;
    CourseRepository courseRepository;
    UserRepository userRepository;
    EnrollmentRepository enrollmentRepository;
    UserMapper userMapper;
    CourseMapper courseMapper;
    CourseReviewSpecification courseReviewSpecification;

    public CourseReviewResponse createReview(CourseReviewRequest request, String courseId) {
        // Kiểm tra tồn tại user, course, enrollment
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(student.getId(), course.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_EXISTED));
        if (!enrollment.isCompleted()) {
            throw new AppException(ErrorCode.CANNOT_REVIEW_UNCOMPLETED_COURSE);
        }
        if (courseReviewRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new AppException(ErrorCode.ALREADY_REVIEWED);
        }
        CourseReview review = CourseReview.builder()
                .student(student)
                .course(course)
                .rating(request.getRating())
                .comment(request.getComment())
                .reviewDate(LocalDate.now())
                .isApproved(false) // Chờ duyệt
                .build();
        courseReviewRepository.save(review);
        return toResponse(review);
    }

    private CourseReviewResponse toResponse(CourseReview review) {
        return CourseReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .reviewDate(review.getReviewDate())
                .isApproved(review.isApproved())
                .isRejected(review.isRejected())
                .student(userMapper.toUserResponse(review.getStudent()))
                .course(courseMapper.toCourseResponse(review.getCourse()))
                .build();
    }

    // Lấy tất cả đánh giá của khóa học
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public Page<CourseReviewResponse> getReviewsByCourse(String courseId, Pageable pageable) {
        // Kiểm tra tồn tại khóa học
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        // Lấy thông tin user hiện tại và role
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));
        boolean isInstructor = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("INSTRUCTOR"));
        boolean isCourseInstructor = course.getInstructor().getId().equals(currentUser.getId());

        // Lấy danh sách review dựa trên quyền
        Page<CourseReview> reviews;
        if (isAdmin || (isInstructor && isCourseInstructor)) {
            // Admin và instructor của khóa học có thể xem tất cả review
            reviews = courseReviewRepository.findByCourseId(courseId, pageable);
        } else {
            // User thường chỉ xem được review đã được duyệt và không bị từ chối
            reviews = courseReviewRepository.findByCourseIdAndIsApprovedTrueAndIsRejectedFalse(courseId, pageable);
        }

        return reviews.map(this::toResponse);
    }

    public Page<CourseReviewResponse> getPublicReviewsByCourse(String courseId, Pageable pageable) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        Page<CourseReview> reviews = courseReviewRepository.findByCourseIdAndIsApprovedTrue(courseId, pageable);

        return reviews.map(this::toResponse);
    }

    // Service lấy đánh giá đã được xử lý của khóa học cho admin và instructor
    @PreAuthorize("hasRole('ADMIN') or (hasRole('INSTRUCTOR') and @courseService.isInstructorOfCourse(#courseId))")
    public Page<CourseReviewResponse> getHandledReviewsByCourse(String courseId, Pageable pageable, Boolean isRejected, LocalDate startDate, LocalDate endDate) {
        // Kiểm tra tồn tại khóa học
        courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        Specification<CourseReview> spec = courseReviewSpecification.findHandledReviews(courseId, isRejected, startDate, endDate);

        Page<CourseReview> reviews = courseReviewRepository.findAll(spec, pageable);
        return reviews.map(this::toResponse);
    }

    // Service lấy đánh giá chưa được duyệt của khóa học cho admin và instructor
    @PreAuthorize("hasRole('ADMIN') or (hasRole('INSTRUCTOR') and @courseService.isInstructorOfCourse(#courseId))")
    public Page<CourseReviewResponse> getPendingReviewsByCourse(String courseId, Pageable pageable) {
        // Kiểm tra tồn tại khóa học
        courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        // Lấy tất cả đánh giá chưa được duyệt
        Page<CourseReview> reviews = courseReviewRepository.findByCourseIdAndIsApprovedFalse(courseId, pageable);

        return reviews.map(this::toResponse);
    }

    // Service phê duyệt đánh giá của khóa học
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public CourseReviewResponse approveReview(String reviewId) {
        // Kiểm tra tồn tại đánh giá
        CourseReview review = courseReviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_REVIEW_NOT_EXISTED));

        // Chỉ admin hoặc instructor của khóa học mới có quyền phê duyệt
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!currentUser.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN")) &&
            !review.getCourse().getInstructor().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Phê duyệt đánh giá
        review.setApproved(true);
        review.setRejected(false);
        courseReviewRepository.save(review);

        return toResponse(review);
    }

    // Service từ chối đánh giá của khóa học
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public CourseReviewResponse rejectReview(String reviewId) {
        // Kiểm tra tồn tại đánh giá
        CourseReview review = courseReviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_REVIEW_NOT_EXISTED));

        // Chỉ admin hoặc instructor của khóa học mới có quyền từ chối
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!currentUser.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN")) &&
            !review.getCourse().getInstructor().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Từ chối đánh giá
        review.setRejected(true);
        review.setApproved(true);
        courseReviewRepository.save(review);

        return toResponse(review);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<CourseReviewResponse> getAllReviewsForAdmin(Pageable pageable) {
        Page<CourseReview> reviews = courseReviewRepository.findAll(pageable);
        return reviews.map(this::toResponse);
    }

    // Service lấy tất cả đánh giá đã được xử lý có trạng thái approved = true và rejected = false
    // cho tất cả người dùng (kể cả người dùng chưa đăng nhập)
    public Page<CourseReviewResponse> getAllHandledReviews(Pageable pageable) {
        Specification<CourseReview> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isTrue(root.get("isApproved")));
            predicates.add(criteriaBuilder.isFalse(root.get("isRejected")));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<CourseReview> reviews = courseReviewRepository.findAll(spec, pageable);
        return reviews.map(this::toResponse);
    }

} 