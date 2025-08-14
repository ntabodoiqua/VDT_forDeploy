package com.ntabodoiqua.online_course_management.service;

import com.ntabodoiqua.online_course_management.dto.request.course.CourseCreationRequest;
import com.ntabodoiqua.online_course_management.dto.request.course.CourseFilterRequest;
import com.ntabodoiqua.online_course_management.dto.request.course.CourseUpdateRequest;
import com.ntabodoiqua.online_course_management.dto.response.course.CourseResponse;
import com.ntabodoiqua.online_course_management.entity.*;
import com.ntabodoiqua.online_course_management.enums.DefaultUrl;
import com.ntabodoiqua.online_course_management.exception.AppException;
import com.ntabodoiqua.online_course_management.exception.ErrorCode;
import com.ntabodoiqua.online_course_management.mapper.CategoryMapper;
import com.ntabodoiqua.online_course_management.mapper.CourseMapper;
import com.ntabodoiqua.online_course_management.mapper.EnrollmentMapper;
import com.ntabodoiqua.online_course_management.mapper.UserMapper;
import com.ntabodoiqua.online_course_management.repository.*;
import com.ntabodoiqua.online_course_management.service.file.FileStorageService;
import com.ntabodoiqua.online_course_management.specification.CourseSpecification;
import com.ntabodoiqua.online_course_management.configuration.properties.DigitalOceanSpacesProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.ntabodoiqua.online_course_management.dto.response.course.PopularCourseResponse;
import org.springframework.data.domain.PageRequest;
import com.ntabodoiqua.online_course_management.dto.response.enrollment.EnrollmentResponse;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CourseService {
    // Service này sẽ chứa các phương thức liên quan đến quản lý khóa học
    // Bao gồm:
    // - Tạo khóa học mới
    // - Cập nhật thông tin khóa học
    // - Xóa khóa học
    // - Lấy danh sách khóa học
    // - Lấy thông tin chi tiết khóa học
    // - Đăng ký khóa học
    // - Hủy đăng ký khóa học

    CourseRepository courseRepository;
    CategoryRepository categoryRepository;
    UserRepository userRepository;
    CourseMapper courseMapper;
    UserMapper userMapper;
    FileStorageService fileStorageService;
    CourseLessonRepository courseLessonRepository;
    EnrollmentRepository enrollmentRepository;
    CourseReviewRepository courseReviewRepository;
    CategoryMapper categoryMapper;
    EnrollmentMapper enrollmentMapper;
    DigitalOceanSpacesProperties spacesProperties;

    // Forward declaration to avoid circular dependency
    @Lazy
    CourseLessonService courseLessonService;

    // Service tạo khóa học mới
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public CourseResponse createCourse(CourseCreationRequest request, MultipartFile thumbnail) {
        // Kiểm tra xem danh mục có tồn tại không
        var category = categoryRepository.findFirstByNameContainingIgnoreCase(request.getCategoryName())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));
        // Kiểm tra xem title khóa học đã tồn tại chưa
        if (courseRepository.existsByTitleIgnoreCase(request.getTitle())) {
            throw new AppException(ErrorCode.COURSE_EXISTED);
        }
        // Lấy thông tin người dùng hiện tại
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User instructor = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Xử lý thumbnail (nếu có)
        String thumbnailUrl;
        if (thumbnail != null && !thumbnail.isEmpty()) {
            String fileName = fileStorageService.storeFile(thumbnail, true).getFileName();
            thumbnailUrl = spacesProperties.getBaseUrl() + "/" + fileName;
        } else {
            thumbnailUrl = DefaultUrl.COURSE_THUMBNAIL.getURL();
        }

        Course course = courseMapper.toCourse(request);
        course.setInstructor(instructor);
        course.setCategory(category);
        course.setTotalLessons(0);
        // Thiết lập active nếu ngày hiện tại nằm trong khoảng thời gian khóa học
        LocalDateTime now = LocalDateTime.now();
        course.setActive(now.isAfter(request.getStartDate().atStartOfDay())
                && now.isBefore(request.getEndDate().atTime(23, 59, 59)));
        course.setThumbnailUrl(thumbnailUrl);
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());
        course.setRequiresApproval(request.isRequiresApproval());

        // Lưu khóa học vào cơ sở dữ liệu
        courseRepository.save(course);
        return courseMapper.toCourseResponse(course);
    }

    // Logic kiểm tra quyền truy cập
    private void checkCoursePermission(Course course) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        boolean isOwner = course.getInstructor() != null && course.getInstructor().getUsername().equals(username);

        if (!isAdmin && !isOwner) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    // Service cập nhật khóa học
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public CourseResponse updateCourse(String courseId, CourseUpdateRequest request, MultipartFile thumbnail) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        checkCoursePermission(course);

        // Category
        if (request.getCategoryName() != null) {
            Category category = categoryRepository.findFirstByNameContainingIgnoreCase(request.getCategoryName())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));
            course.setCategory(category);
        }

        // Title
        if (request.getTitle() != null && !request.getTitle().isEmpty()) {
            if (courseRepository.existsByTitleIgnoreCase(request.getTitle())
                    && !course.getTitle().equalsIgnoreCase(request.getTitle())) {
                throw new AppException(ErrorCode.COURSE_EXISTED);
            }
            course.setTitle(request.getTitle());
        }

        // Thumbnail
        if (thumbnail != null && !thumbnail.isEmpty()) {
            String fileName = fileStorageService.storeFile(thumbnail, true).getFileName();
            course.setThumbnailUrl(spacesProperties.getBaseUrl() + "/" + fileName);
        }

        // Description
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }

        // Detailed Description
        if (request.getDetailedDescription() != null) {
            course.setDetailedDescription(request.getDetailedDescription());
        }

        // Start Date
        if (request.getStartDate() != null) {
            course.setStartDate(request.getStartDate());
        }

        // End Date
        if (request.getEndDate() != null) {
            course.setEndDate(request.getEndDate());
        }

        // isActive
        if (request.getIsActive() != null) {
            course.setActive(request.getIsActive());
        }

        // requiresApproval
        if (request.getRequiresApproval() != null) {
            course.setRequiresApproval(request.getRequiresApproval());
        }

        course.setUpdatedAt(LocalDateTime.now());
        courseRepository.save(course);

        return courseMapper.toCourseResponse(course);
    }

    // Service xóa khóa học
    @Transactional
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public void deleteCourse(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        checkCoursePermission(course);

        // Xóa các liên kết
        courseLessonRepository.deleteByCourseId(courseId);
        enrollmentRepository.deleteByCourseId(courseId);
        courseReviewRepository.deleteByCourseId(courseId);

        // Xóa khóa học
        courseRepository.delete(course);

        log.info("Deleted course {} and all related records", courseId);
    }

    /**
     * Lấy thông tin khóa học theo ID với phân quyền
     * - Admin: Xem toàn bộ thông tin tất cả khóa học (kể cả inactive)
     * - Instructor: Xem toàn bộ thông tin khóa học của mình (kể cả inactive), thông tin cơ bản khóa học active khác
     * - Student/Guest: Chỉ xem thông tin cơ bản của khóa học active
     */
    public CourseResponse getCourseById(String courseId) {
        log.info("Getting course by ID: {}", courseId);

        // 1. Tìm course theo ID
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        // 2. Kiểm tra quyền truy cập
        AccessLevel accessLevel = checkAccessLevel(course);

        // 3. Kiểm tra khóa học có active không (đối với user thường)
        if (accessLevel == AccessLevel.NO_ACCESS) {
            log.warn("User attempted to access inactive course: {}", courseId);
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        boolean hasFullAccess = (accessLevel == AccessLevel.FULL_ACCESS);

        log.info("Accessing course {} with access level: {}", courseId, accessLevel);

        return mapCourseToResponse(course, hasFullAccess);
    }

    /**
     * Enum để định nghĩa mức độ truy cập
     */
    private enum AccessLevel {
        FULL_ACCESS,    // Xem toàn bộ thông tin
        BASIC_ACCESS,   // Chỉ xem thông tin cơ bản
        NO_ACCESS       // Không được xem
    }

    /**
     * Kiểm tra mức độ truy cập của user đối với course
     */
    private AccessLevel checkAccessLevel(Course course) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // Anonymous user hoặc chưa đăng nhập
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            log.debug("Anonymous user accessing course {}", course.getId());
            return course.isActive() ? AccessLevel.BASIC_ACCESS : AccessLevel.NO_ACCESS;
        }

        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        boolean isAdmin = roles.contains("ROLE_ADMIN");
        boolean isInstructor = roles.contains("ROLE_INSTRUCTOR");

        log.debug("User {} roles: {}, course active: {}", username, roles, course.isActive());

        // Admin có quyền xem tất cả (kể cả inactive)
        if (isAdmin) {
            log.debug("User {} has ADMIN role - full access", username);
            return AccessLevel.FULL_ACCESS;
        }

        // Instructor có quyền xem tất cả khóa học của mình (kể cả inactive)
        if (isInstructor) {
            boolean isOwner = course.getInstructor().getUsername().equals(username);
            if (isOwner) {
                log.debug("User {} is course owner - full access", username);
                return AccessLevel.FULL_ACCESS;
            } else {
                // Instructor xem khóa học của người khác
                log.debug("User {} is instructor but not owner, course active: {}", username, course.isActive());
                return course.isActive() ? AccessLevel.BASIC_ACCESS : AccessLevel.NO_ACCESS;
            }
        }

        // Student và các role khác chỉ xem được khóa học active
        log.debug("User {} has limited access, course active: {}", username, course.isActive());
        return course.isActive() ? AccessLevel.BASIC_ACCESS : AccessLevel.NO_ACCESS;
    }

    /**
     * Định nghĩa Course Response tùy quyền
     */
    private CourseResponse mapCourseToResponse(Course course, boolean full) {
        CourseResponse response;
        if (full) {
            log.debug("Returning full course information for course: {}", course.getId());
            response = courseMapper.toCourseResponse(course);
        } else {
            log.debug("Returning basic course information for course: {}", course.getId());
            // Trả về thông tin cơ bản mà student cần để quyết định đăng ký khóa học
            response = CourseResponse.builder()
                    .id(course.getId())
                    .title(course.getTitle())
                    .description(course.getDescription())
                    .createdAt(course.getCreatedAt())
                    .instructor(userMapper.toUserResponse(course.getInstructor()))
                    .thumbnailUrl(course.getThumbnailUrl())
                    .category(categoryMapper.toCategoryResponse(course.getCategory()))
                    .totalLessons(course.getTotalLessons())
                    .isActive(course.isActive())
                    .startDate(course.getStartDate())
                    .endDate(course.getEndDate())
                    .requiresApproval(course.isRequiresApproval())
                    .build();
        }
        
        // Add rating information for both full and basic responses
        enrichWithRatingData(response, course.getId());
        return response;
    }

    /**
     * Enrich CourseResponse with rating data
     */
    private void enrichWithRatingData(CourseResponse response, String courseId) {
        try {
            Double avgRating = courseReviewRepository.findAverageRatingByCourseId(courseId);
            Integer totalReviews = courseReviewRepository.countByCourseIdAndIsApprovedTrue(courseId);
            
            response.setAverageRating(avgRating);
            response.setTotalReviews(totalReviews);
            
            log.debug("Enriched course {} with rating data - Average: {}, Total reviews: {}", 
                     courseId, avgRating, totalReviews);
        } catch (Exception e) {
            log.warn("Failed to enrich course {} with rating data: {}", courseId, e.getMessage());
            response.setAverageRating(null);
            response.setTotalReviews(0);
        }
    }

    /**
     * Alternative method: Sử dụng khi cần kiểm tra quyền với logic đặc biệt
     */
    public CourseResponse getCourseByIdWithCustomAuth(String courseId, String username, List<String> roles) {
        log.info("Getting course by ID: {} for user: {}", courseId, username);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        AccessLevel accessLevel = checkAccessLevelWithRoles(course, username, roles);

        if (accessLevel == AccessLevel.NO_ACCESS) {
            log.warn("User {} attempted to access unauthorized course: {}", username, courseId);
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        boolean hasFullAccess = (accessLevel == AccessLevel.FULL_ACCESS);

        return mapCourseToResponse(course, hasFullAccess);
    }

    /**
     * Kiểm tra mức độ truy cập với roles được truyền vào
     */
    private AccessLevel checkAccessLevelWithRoles(Course course, String username, List<String> roles) {
        boolean isAdmin = roles.contains("ROLE_ADMIN");
        boolean isInstructor = roles.contains("ROLE_INSTRUCTOR");

        // Admin có quyền xem tất cả (kể cả inactive)
        if (isAdmin) {
            return AccessLevel.FULL_ACCESS;
        }

        // Instructor có quyền xem tất cả khóa học của mình (kể cả inactive)
        if (isInstructor) {
            boolean isOwner = course.getInstructor().getUsername().equals(username);
            if (isOwner) {
                return AccessLevel.FULL_ACCESS;
            } else {
                return course.isActive() ? AccessLevel.BASIC_ACCESS : AccessLevel.NO_ACCESS;
            }
        }

        // Student và các role khác chỉ xem được khóa học active
        return course.isActive() ? AccessLevel.BASIC_ACCESS : AccessLevel.NO_ACCESS;
    }
    /**
     * Lấy danh sách khóa học với phân trang và lọc
     * - Admin: Xem tất cả khóa học (kể cả inactive)
     * - Instructor: Xem tất cả khóa học active + khóa học của mình (kể cả inactive)
     * - Student/Guest: Chỉ xem khóa học active
     */
    public Page<CourseResponse> getCourses(CourseFilterRequest filter, Pageable pageable) {
        log.info("Getting courses with filter: {}, page: {}", filter, pageable);

        // Kiểm tra quyền truy cập
        CourseAccessInfo accessInfo = getCurrentUserAccessInfo();

        // Tạo specification với phân quyền
        Specification<Course> spec = CourseSpecification.withFilterAndPermission(
                filter,
                accessInfo.canViewInactive(),
                accessInfo.instructorUsername()
        );

        // Lấy danh sách khóa học
        Page<Course> coursePage = courseRepository.findAll(spec, pageable);

        log.info("Found {} courses for user with access level: {}",
                coursePage.getTotalElements(), accessInfo.accessLevel());

        // Convert sang response với phân quyền
        return coursePage.map(course -> {
            boolean hasFullAccess = determineFullAccess(course, accessInfo);
            return mapCourseToResponse(course, hasFullAccess);
        });
    }

    /**
     * Alternative method với custom authentication
     */
    public Page<CourseResponse> getCoursesWithCustomAuth(CourseFilterRequest filter,
                                                         Pageable pageable,
                                                         String username,
                                                         List<String> roles) {
        log.info("Getting courses with custom auth for user: {}, roles: {}", username, roles);

        CourseAccessInfo accessInfo = buildAccessInfo(username, roles);

        Specification<Course> spec = CourseSpecification.withFilterAndPermission(
                filter,
                accessInfo.canViewInactive(),
                accessInfo.instructorUsername()
        );

        Page<Course> coursePage = courseRepository.findAll(spec, pageable);

        return coursePage.map(course -> {
            boolean hasFullAccess = determineFullAccess(course, accessInfo);
            return mapCourseToResponse(course, hasFullAccess);
        });
    }

    /**
     * Record để lưu thông tin truy cập của user
     */
    private record CourseAccessInfo(
            String username,
            List<String> roles,
            boolean canViewInactive,
            String instructorUsername,
            String accessLevel
    ) {}

    /**
     * Lấy thông tin truy cập của user hiện tại
     */
    private CourseAccessInfo getCurrentUserAccessInfo() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            log.debug("Anonymous user accessing course list");
            return new CourseAccessInfo(null, List.of(), false, null, "ANONYMOUS");
        }

        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return buildAccessInfo(username, roles);
    }

    /**
     * Xây dựng thông tin truy cập từ username và roles
     */
    private CourseAccessInfo buildAccessInfo(String username, List<String> roles) {
        boolean isAdmin = roles.contains("ROLE_ADMIN");
        boolean isInstructor = roles.contains("ROLE_INSTRUCTOR");

        if (isAdmin) {
            return new CourseAccessInfo(username, roles, true, null, "ADMIN");
        } else if (isInstructor) {
            return new CourseAccessInfo(username, roles, false, username, "INSTRUCTOR");
        } else {
            return new CourseAccessInfo(username, roles, false, null, "STUDENT");
        }
    }

    /**
     * Xác định user có quyền xem full thông tin course không
     */
    private boolean determineFullAccess(Course course, CourseAccessInfo accessInfo) {
        // Admin có full access đến tất cả
        if (accessInfo.roles().contains("ROLE_ADMIN")) {
            return true;
        }

        // Instructor có full access đến khóa học của mình
        if (accessInfo.roles().contains("ROLE_INSTRUCTOR")) {
            return course.getInstructor().getUsername().equals(accessInfo.username());
        }

        // User khác chỉ có basic access
        return false;
    }

    // Service toggle trạng thái khóa học (đơn giản)
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public CourseResponse toggleCourseStatus(String courseId, Boolean isActive) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        checkCoursePermission(course);

        // Cập nhật trạng thái
        course.setActive(isActive);
        course.setUpdatedAt(LocalDateTime.now());
        Course savedCourse = courseRepository.save(course);

        log.info("Toggled course {} status to: {}. Entity isActive before mapping: {}", courseId, isActive, savedCourse.isActive());
        return courseMapper.toCourseResponse(savedCourse);
    }

    public List<PopularCourseResponse> getPopularCourses(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Object[]> popularCourseData = enrollmentRepository.findPopularCourseIds(pageable);

        return popularCourseData.getContent().stream()
                .map(result -> {
                    String courseId = (String) result[0];
                    Long enrollmentCount = (Long) result[1];
                    return courseRepository.findById(courseId)
                            .map(course -> {
                                PopularCourseResponse response = new PopularCourseResponse();
                                CourseResponse courseResponse = courseMapper.toCourseResponse(course);

                                // Enrich with rating data
                                enrichWithRatingData(courseResponse, course.getId());

                                response.setCourse(courseResponse);
                                response.setEnrollmentCount(enrollmentCount);
                                response.setAverageRating(courseResponse.getAverageRating());
                                response.setTotalReviews(courseResponse.getTotalReviews() != null ? courseResponse.getTotalReviews().longValue() : null);

                                return response;
                            })
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Đồng bộ lại trường totalLessons cho tất cả courses dựa trên số lượng bài học thực tế
     * Phương thức này nên được gọi khi có sự không nhất quán trong dữ liệu
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void syncAllCoursesTotalLessons() {
        log.info("Admin triggered sync all courses totalLessons");
        courseLessonService.batchSyncAllCoursesTotalLessons();
    }

    /**
     * Đồng bộ lại trường totalLessons cho một course cụ thể
     */
    @Transactional
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public void syncCourseTotalLessons(String courseId) {
        log.info("User triggered sync for course: {}", courseId);
        courseLessonService.syncSpecificCourseTotalLessons(courseId);
    }

    /**
     * Kiểm tra xem instructor hiện tại có phải là chủ sở hữu của khóa học hay không
     * Method này được sử dụng cho @PreAuthorize annotation
     */
    public boolean isInstructorOfCourse(String courseId) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElse(null);
            
            if (currentUser == null) {
                return false;
            }

            Course course = courseRepository.findById(courseId)
                    .orElse(null);
                    
            if (course == null) {
                return false;
            }

            return course.getInstructor() != null && 
                   course.getInstructor().getUsername().equals(username);
        } catch (Exception e) {
            log.error("Error checking instructor permission for course {}: {}", courseId, e.getMessage());
            return false;
        }
    }

    public Page<EnrollmentResponse> getMyCourses(Pageable pageable) {
        // Lấy thông tin người dùng hiện tại
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Lấy danh sách các khóa học đã đăng ký (có phân trang)
        Page<Enrollment> enrollments = enrollmentRepository.findByStudent(user, pageable);

        // Chuyển đổi Page<Enrollment> sang Page<EnrollmentResponse>
        return enrollments.map(enrollmentMapper::toEnrollmentResponse);
    }
}
