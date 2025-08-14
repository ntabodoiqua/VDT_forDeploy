package com.ntabodoiqua.online_course_management.service;

import com.ntabodoiqua.online_course_management.dto.request.quiz.*;
import com.ntabodoiqua.online_course_management.dto.response.quiz.*;
import com.ntabodoiqua.online_course_management.entity.*;
import com.ntabodoiqua.online_course_management.enums.QuizType;
import com.ntabodoiqua.online_course_management.enums.ScoringMethod;
import com.ntabodoiqua.online_course_management.exception.AppException;
import com.ntabodoiqua.online_course_management.exception.ErrorCode;
import com.ntabodoiqua.online_course_management.mapper.quiz.QuizMapper;
import com.ntabodoiqua.online_course_management.mapper.quiz.QuizMapperFacade;
import com.ntabodoiqua.online_course_management.mapper.quiz.QuizQuestionMapper;
import com.ntabodoiqua.online_course_management.mapper.quiz.QuizAnswerMapper;
import com.ntabodoiqua.online_course_management.repository.*;
import com.ntabodoiqua.online_course_management.specification.QuizSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizService {
    
    QuizRepository quizRepository;
    QuizQuestionRepository quizQuestionRepository;
    QuizAnswerRepository quizAnswerRepository;
    QuizAttemptRepository quizAttemptRepository;
    QuizAttemptAnswerRepository quizAttemptAnswerRepository;
    LessonRepository lessonRepository;
    UserRepository userRepository;
    EnrollmentRepository enrollmentRepository;
    CourseLessonRepository courseLessonRepository;
    
    QuizMapper quizMapper;
    QuizMapperFacade quizMapperFacade;
    QuizQuestionMapper quizQuestionMapper;
    QuizAnswerMapper quizAnswerMapper;
    
    /**
     * Lấy danh sách quiz với phân trang và lọc
     * - Admin: Xem tất cả quiz (kể cả inactive)
     * - Instructor: CHỈ xem quiz của chính mình (kể cả inactive)
     * - Student/Guest: Chỉ xem quiz active và available
     */
    public Page<QuizResponse> getQuizzes(QuizFilterRequest filter, Pageable pageable) {
        log.info("Getting quizzes with filter: {}, page: {}", filter, pageable);
        
        // Kiểm tra quyền truy cập
        QuizAccessInfo accessInfo = getCurrentUserAccessInfo();
        
        // Tạo specification với phân quyền và auto-filter cho instructor
        Specification<Quiz> spec;
        
        if ("INSTRUCTOR".equals(accessInfo.accessLevel())) {
            // Instructor chỉ thấy quiz của mình trong trang quản lý
            String currentUserId = getCurrentUser().getId();
            spec = QuizSpecification.withFilter(filter)
                    .and(QuizSpecification.byInstructorId(currentUserId));
        } else {
            // Admin hoặc Student/Guest sử dụng logic cũ
            spec = QuizSpecification.withFilterAndPermission(
                    filter,
                    accessInfo.canViewInactive(),
                    accessInfo.instructorUsername()
            );
        }
        
        // Lấy danh sách quiz
        Page<Quiz> quizPage = quizRepository.findAll(spec, pageable);
        
        log.info("Found {} quizzes for user with access level: {}",
                quizPage.getTotalElements(), accessInfo.accessLevel());
        
        // Convert sang response
        return quizPage.map(quizMapper::toQuizResponse);
    }
    
    /**
     * Lấy quiz theo course với phân quyền
     */
    public List<QuizResponse> getQuizzesByCourse(String courseId) {
        log.info("Getting quizzes for course: {}", courseId);
        
        QuizAccessInfo accessInfo = getCurrentUserAccessInfo();
        
        Specification<Quiz> spec = Specification.where(QuizSpecification.byCourseId(courseId));
        
        // Apply permission filters
        if (!accessInfo.canViewInactive()) {
            if (accessInfo.instructorUsername() != null) {
                // Instructor: active quizzes + own quizzes
                Specification<Quiz> permissionSpec = (root, query, cb) -> {
                    return cb.or(
                        cb.equal(root.get("isActive"), true),
                        cb.equal(root.get("createdBy").get("username"), accessInfo.instructorUsername())
                    );
                };
                spec = spec.and(permissionSpec);
            } else {
                // Student: only active and available quizzes
                spec = spec.and(QuizSpecification.isAvailable());
            }
        }
        
        List<Quiz> quizzes = quizRepository.findAll(spec);
        return quizzes.stream()
                .map(quizMapper::toQuizResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy quiz available cho student
     */
    public List<QuizResponse> getAvailableQuizzes(String courseId) {
        log.info("Getting available quizzes for course: {}", courseId);
        
        Specification<Quiz> spec = Specification
                .where(QuizSpecification.byCourseId(courseId))
                .and(QuizSpecification.isAvailable());
        
        List<Quiz> quizzes = quizRepository.findAll(spec);
        return quizzes.stream()
                .map(quizMapper::toQuizResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Tìm kiếm quiz với nhiều tiêu chí
     */
    public Page<QuizResponse> searchQuizzes(QuizFilterRequest filter) {
        // Tạo pageable với sort mặc định
        Pageable pageable = createPageable(filter);
        
        return getQuizzes(filter, pageable);
    }
    
    /**
     * Lấy quiz của instructor
     */
    public Page<QuizResponse> getInstructorQuizzes(String instructorId, Pageable pageable) {
        log.info("Getting quizzes for instructor: {}", instructorId);
        
        Specification<Quiz> spec = QuizSpecification.byInstructorId(instructorId);
        Page<Quiz> quizPage = quizRepository.findAll(spec, pageable);
        
        return quizPage.map(quizMapper::toQuizResponse);
    }
    
    /**
     * Lấy quiz hết hạn cần xử lý
     */
    public List<QuizResponse> getExpiredQuizzes() {
        log.info("Getting expired quizzes");
        
        Specification<Quiz> spec = QuizSpecification.isExpired();
        List<Quiz> expiredQuizzes = quizRepository.findAll(spec);
        
        return expiredQuizzes.stream()
                .map(quizMapper::toQuizResponse)
                .collect(Collectors.toList());
    }
    
    // Helper methods
    private QuizAccessInfo getCurrentUserAccessInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return new QuizAccessInfo("GUEST", false, null);
        }
        
        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
        boolean isAdmin = roles.contains("ROLE_ADMIN");
        boolean isInstructor = roles.contains("ROLE_INSTRUCTOR");
        
        String accessLevel = isAdmin ? "ADMIN" : (isInstructor ? "INSTRUCTOR" : "STUDENT");
        boolean canViewInactive = isAdmin;
        String instructorUsername = isInstructor ? username : null;
        
        return new QuizAccessInfo(accessLevel, canViewInactive, instructorUsername);
    }
    
    private Pageable createPageable(QuizFilterRequest filter) {
        int page = filter.getPage() != null ? filter.getPage() : 0;
        int size = filter.getSize() != null ? filter.getSize() : 10;
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "createdAt";
        String sortDir = filter.getSortDir() != null ? filter.getSortDir() : "desc";
        
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
        
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
    
    // ================= CRUD OPERATIONS =================
    
    /**
     * Tạo quiz mới
     * Chỉ instructor hoặc admin mới có thể tạo quiz
     * Mỗi lesson chỉ có tối đa 1 quiz (quan hệ 1:1)
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Transactional
    public QuizResponse createQuiz(QuizCreationRequest request) {
        log.info("Creating quiz: {} for lesson: {}", request.getTitle(), request.getLessonId());
        
        // Validate lesson exists
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        
        // Check permission to create quiz on this lesson
        checkLessonPermission(lesson);
        
        // Check if lesson already has a quiz (1:1 relationship)
        if (quizRepository.existsByLessonId(request.getLessonId())) {
            throw new AppException(ErrorCode.QUIZ_ALREADY_EXISTS_FOR_LESSON);
        }
        
        // Validate quiz title uniqueness
        if (quizRepository.existsByTitleIgnoreCase(request.getTitle())) {
            throw new AppException(ErrorCode.QUIZ_TITLE_ALREADY_EXISTS);
        }
        
        // Get current user
        User currentUser = getCurrentUser();
        
        // Create quiz entity
        Quiz quiz = quizMapper.toQuiz(request);
        quiz.setLesson(lesson);
        quiz.setCreatedBy(currentUser);
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setUpdatedAt(LocalDateTime.now());
        
        // Set default values if not provided
        if (quiz.getPassingScore() == null) {
            quiz.setPassingScore(70.0); // Default 70%
        }
        if (quiz.getScoringMethod() == null) {
            quiz.setScoringMethod(ScoringMethod.HIGHEST);
        }
        if (quiz.getMaxAttempts() == null) {
            quiz.setMaxAttempts(-1); // Unlimited by default
        }
        
        quiz = quizRepository.save(quiz);
        
        // Create questions if provided
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            createQuizQuestions(quiz, request.getQuestions());
        }
        
        // Fetch the quiz again to ensure all associations are loaded
        var finalQuiz = quizRepository.findById(quiz.getId()).orElse(quiz);

        log.info("Quiz created successfully with ID: {}", finalQuiz.getId());
        return quizMapperFacade.toQuizResponseWithDetails(finalQuiz);
    }
    
    /**
     * Lấy chi tiết quiz theo ID với phân quyền
     */
    public QuizResponse getQuizById(String quizId) {
        log.info("Getting quiz by ID: {}", quizId);
        
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        
        // Check permission
        QuizAccessInfo accessInfo = getCurrentUserAccessInfo();
        
        // Admin: Full access
        if ("ADMIN".equals(accessInfo.accessLevel())) {
            return quizMapperFacade.toQuizResponseWithDetails(quiz);
        }
        
        // Instructor: Own quizzes + active quizzes
        if ("INSTRUCTOR".equals(accessInfo.accessLevel())) {
            boolean isOwner = quiz.getCreatedBy().getUsername().equals(accessInfo.instructorUsername());
            if (isOwner || Boolean.TRUE.equals(quiz.getIsActive())) {
                return quizMapperFacade.toQuizResponseWithDetails(quiz);
            } else {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }
        
        // Student: Only active and available quizzes they have access to
        if (!Boolean.TRUE.equals(quiz.getIsActive()) || !isQuizAvailable(quiz)) {
            throw new AppException(ErrorCode.QUIZ_NOT_AVAILABLE);
        }
        
        // Check if student has access to the lesson/course
        if (!hasAccessToQuiz(quiz)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        return quizMapperFacade.toQuizResponseWithDetails(quiz);
    }
    
    /**
     * Lấy quiz dành cho student với thông tin về attempts
     */
    public QuizStudentResponse getQuizForStudent(String quizId) {
        log.info("Getting quiz for student: {}", quizId);
        
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        
        // Check if quiz is available for students
        if (!Boolean.TRUE.equals(quiz.getIsActive()) || !isQuizAvailable(quiz)) {
            throw new AppException(ErrorCode.QUIZ_NOT_AVAILABLE);
        }
        
        // Check if student has access
        if (!hasAccessToQuiz(quiz)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        User currentUser = getCurrentUser();
        
        // Get student's attempt info
        int userAttempts = (int) quizAttemptRepository.countByQuizIdAndStudentId(quizId, currentUser.getId());
        int remainingAttempts = calculateRemainingAttempts(quiz, userAttempts);
        boolean canAttempt = canStudentAttemptQuiz(quiz, userAttempts);
        
        return quizMapperFacade.toQuizStudentResponseWithDetails(
                quiz, userAttempts, remainingAttempts, canAttempt, true);
    }
    
    /**
     * Cập nhật quiz
     * Chỉ creator hoặc admin mới có thể cập nhật
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Transactional
    public QuizResponse updateQuiz(String quizId, QuizUpdateRequest request) {
        log.info("Updating quiz: {}", quizId);
        
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        
        // Check permission
        checkQuizPermission(quiz);
        
        // Validate title uniqueness if changed
        if (request.getTitle() != null && !request.getTitle().equalsIgnoreCase(quiz.getTitle())) {
            if (quizRepository.existsByTitleIgnoreCase(request.getTitle())) {
                throw new AppException(ErrorCode.QUIZ_TITLE_ALREADY_EXISTS);
            }
        }
        
        // Update quiz
        quizMapper.updateQuiz(quiz, request);
        quiz.setUpdatedAt(LocalDateTime.now());
        
        quiz = quizRepository.save(quiz);
        
        log.info("Quiz updated successfully: {}", quizId);
        return quizMapperFacade.toQuizResponseWithDetails(quiz);
    }
    
    /**
     * Xóa quiz
     * Chỉ creator hoặc admin mới có thể xóa.
     * Khi xóa quiz, mọi attempt liên quan cũng sẽ bị xóa.
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Transactional
    public void deleteQuiz(String quizId) {
        log.info("Deleting quiz: {}", quizId);
        
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        
        // Check permission
        checkQuizPermission(quiz);
        
        // Xóa tất cả các attempt liên quan đến quiz này.
        // Điều này giả định rằng việc xóa QuizAttempt sẽ xóa theo tầng (cascade) các QuizAttemptAnswer.
        quizAttemptRepository.deleteByQuizId(quizId);
        
        quizRepository.deleteById(quizId);
        log.info("Quiz deleted successfully: {}", quizId);
    }
    
    /**
     * Activate/Deactivate quiz
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Transactional
    public QuizResponse toggleQuizStatus(String quizId) {
        log.info("Toggling quiz status: {}", quizId);
        
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        
        checkQuizPermission(quiz);
        
        quiz.setIsActive(!Boolean.TRUE.equals(quiz.getIsActive()));
        quiz.setUpdatedAt(LocalDateTime.now());
        
        quiz = quizRepository.save(quiz);
        
        log.info("Quiz status toggled to: {} for quiz: {}", quiz.getIsActive(), quizId);
        return quizMapperFacade.toQuizResponseWithDetails(quiz);
    }
    
    // ================= QUIZ QUESTION MANAGEMENT =================
    
    /**
     * Thêm câu hỏi vào quiz
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Transactional
    public QuizQuestionResponse addQuestionToQuiz(String quizId, QuizQuestionRequest request) {
        log.info("Adding question to quiz: {}", quizId);
        
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        
        checkQuizPermission(quiz);
        
        // Validate answers
        validateQuestionAnswers(request.getAnswers());
        
        // Create question
        QuizQuestion question = quizQuestionMapper.toQuizQuestion(request);
        question.setQuiz(quiz);
        question.setCreatedAt(LocalDateTime.now());
        question.setUpdatedAt(LocalDateTime.now());
        
        // Validate and set order index
        if (question.getOrderIndex() == null) {
            Integer maxOrder = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quizId)
                    .stream()
                    .mapToInt(QuizQuestion::getOrderIndex)
                    .max()
                    .orElse(0);
            question.setOrderIndex(maxOrder + 1);
        } else {
            // Check if orderIndex already exists
            if (quizQuestionRepository.existsByQuizIdAndOrderIndex(quizId, question.getOrderIndex())) {
                throw new AppException(ErrorCode.QUESTION_ORDER_INDEX_ALREADY_EXISTS);
            }
        }
        
        question = quizQuestionRepository.save(question);
        
        // Create answers
        if (request.getAnswers() != null) {
            createQuestionAnswers(question, request.getAnswers());
        }
        
        log.info("Question added to quiz successfully");
        return getQuestionWithAnswers(question);
    }
    
    /**
     * Cập nhật câu hỏi
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Transactional
    public QuizQuestionResponse updateQuestion(String questionId, QuizQuestionRequest request) {
        log.info("Updating question: {}", questionId);
        
        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        
        checkQuizPermission(question.getQuiz());
        
        // Validate answers if provided
        if (request.getAnswers() != null) {
            validateQuestionAnswers(request.getAnswers());
        }
        
        // Validate orderIndex if provided and different from current
        if (request.getOrderIndex() != null && !request.getOrderIndex().equals(question.getOrderIndex())) {
            if (quizQuestionRepository.existsByQuizIdAndOrderIndex(question.getQuiz().getId(), request.getOrderIndex())) {
                throw new AppException(ErrorCode.QUESTION_ORDER_INDEX_ALREADY_EXISTS);
            }
        }
        
        // Update question
        quizQuestionMapper.updateQuizQuestion(question, request);
        question.setUpdatedAt(LocalDateTime.now());
        
        question = quizQuestionRepository.save(question);
        
        // Update answers if provided
        if (request.getAnswers() != null) {
            // Delete existing answers and create new ones
            quizAnswerRepository.deleteByQuestionId(question.getId());
            quizAnswerRepository.flush(); // Force immediate deletion
            createQuestionAnswers(question, request.getAnswers());
        }
        
        log.info("Question updated successfully");
        return getQuestionWithAnswers(question);
    }
    
    /**
     * Xóa câu hỏi
     * Khi xóa câu hỏi, mọi câu trả lời trong các attempt cũng sẽ bị xóa.
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Transactional
    public void deleteQuestion(String questionId) {
        log.info("Deleting question: {}", questionId);
        
        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        
        checkQuizPermission(question.getQuiz());
        
        // Xóa tất cả các câu trả lời trong các lần thử (attempts) cho câu hỏi này.
        quizAttemptAnswerRepository.deleteByQuestionId(questionId);
        
        quizQuestionRepository.deleteById(questionId);
        log.info("Question deleted successfully");
    }
    
    /**
     * Sắp xếp lại thứ tự câu hỏi trong quiz
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Transactional
    public List<QuizQuestionResponse> reorderQuestions(String quizId, List<QuestionOrderRequest> orderRequests) {
        log.info("Reordering questions for quiz: {}", quizId);
        
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        
        checkQuizPermission(quiz);
        
        // Validate that all questions belong to this quiz
        List<String> questionIds = orderRequests.stream()
                .map(QuestionOrderRequest::getQuestionId)
                .toList();
        
        List<QuizQuestion> questions = quizQuestionRepository.findAllById(questionIds);
        
        // Verify all questions belong to the quiz
        boolean allBelongToQuiz = questions.stream()
                .allMatch(q -> q.getQuiz().getId().equals(quizId));
        
        if (!allBelongToQuiz || questions.size() != questionIds.size()) {
            throw new AppException(ErrorCode.QUESTION_NOT_FOUND);
        }
        
        try {
            // Get all existing questions for this quiz
            List<QuizQuestion> allQuizQuestions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quizId);
            
            // Create a map for quick lookup
            Map<String, QuizQuestion> questionMap = allQuizQuestions.stream()
                    .collect(Collectors.toMap(QuizQuestion::getId, q -> q));
            
            // Use a higher temporary base to avoid conflicts with existing order indices
            int tempBase = 100000; // Much higher to avoid any possible conflicts
            
            // First phase: Move all questions to temporary indices to clear conflicts
            for (int i = 0; i < allQuizQuestions.size(); i++) {
                QuizQuestion question = allQuizQuestions.get(i);
                question.setOrderIndex(tempBase + i);
                quizQuestionRepository.save(question);
            }
            
            // Flush to ensure temporary values are persisted
            quizQuestionRepository.flush();
            
            // Second phase: Apply the new order indices
            for (QuestionOrderRequest orderRequest : orderRequests) {
                QuizQuestion question = questionMap.get(orderRequest.getQuestionId());
                if (question != null) {
                    question.setOrderIndex(orderRequest.getOrderIndex());
                    question.setUpdatedAt(LocalDateTime.now());
                    quizQuestionRepository.save(question);
                }
            }
            
            log.info("Successfully reordered {} questions for quiz {}", orderRequests.size(), quizId);
            
            // Fetch and return the updated questions
            List<QuizQuestion> updatedQuestions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quizId);
            
            return updatedQuestions.stream()
                    .map(this::getQuestionWithAnswers)
                    .sorted((q1, q2) -> Integer.compare(q1.getOrderIndex(), q2.getOrderIndex()))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error reordering questions for quiz {}: {}", quizId, e.getMessage(), e);
            throw new AppException(ErrorCode.DATA_INTEGRITY_VIOLATION);
        }
    }
    
    // ================= QUIZ STATISTICS =================
    
    /**
     * Thống kê quiz cho instructor
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public QuizSummaryResponse getQuizSummary(String quizId) {
        log.info("Getting quiz summary: {}", quizId);
        
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        
        checkQuizPermission(quiz);
        
        QuizSummaryResponse summary = quizMapper.toQuizSummaryResponse(quiz);
        
        // Calculate statistics
        long totalAttempts = quizAttemptRepository.countByQuizId(quizId);
        long passedAttempts = quizAttemptRepository.countPassedByQuizId(quizId);
        
        Double averageScore = quizAttemptRepository.getAverageScoreByQuizId(quizId);
        Double highestScore = quizAttemptRepository.getMaxScoreByQuizId(quizId);
        
        summary.setTotalAttempts((int) totalAttempts);
        summary.setPassedAttempts((int) passedAttempts);
        // Additional statistics would be set here
        
        return summary;
    }
    
    // ================= HELPER METHODS =================
    
    private void checkLessonPermission(Lesson lesson) {
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));
        boolean isOwner = lesson.getCreatedBy().equals(currentUser);
        
        if (!isAdmin && !isOwner) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
    
    private void checkQuizPermission(Quiz quiz) {
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));
        boolean isOwner = quiz.getCreatedBy().equals(currentUser);
        
        if (!isAdmin && !isOwner) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
    
    private boolean hasAccessToQuiz(Quiz quiz) {
        User currentUser = getCurrentUser();
        String lessonId = quiz.getLesson().getId();
        
        // Check if student is enrolled in any course that contains this lesson
        return enrollmentRepository.findByStudent(currentUser)
                .stream()
                .anyMatch(enrollment -> 
                    enrollment.getCourse().getCourseLessons()
                            .stream()
                            .anyMatch(cl -> cl.getLesson().getId().equals(lessonId))
                );
    }
    
    private boolean isQuizAvailable(Quiz quiz) {
        LocalDateTime now = LocalDateTime.now();
        
        // Check start time
        if (quiz.getStartTime() != null && now.isBefore(quiz.getStartTime())) {
            return false;
        }
        
        // Check end time
        if (quiz.getEndTime() != null && now.isAfter(quiz.getEndTime())) {
            return false;
        }
        
        return true;
    }
    
    private boolean canStudentAttemptQuiz(Quiz quiz, int currentAttempts) {
        if (!Boolean.TRUE.equals(quiz.getIsActive()) || !isQuizAvailable(quiz)) {
            return false;
        }
        
        // Check max attempts
        if (quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0) {
            return currentAttempts < quiz.getMaxAttempts();
        }
        
        return true; // Unlimited attempts
    }
    
    private int calculateRemainingAttempts(Quiz quiz, int currentAttempts) {
        if (quiz.getMaxAttempts() == null || quiz.getMaxAttempts() <= 0) {
            return -1; // Unlimited
        }
        
        return Math.max(0, quiz.getMaxAttempts() - currentAttempts);
    }
    
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
    
    private void createQuizQuestions(Quiz quiz, List<QuizQuestionRequest> questionRequests) {
        for (int i = 0; i < questionRequests.size(); i++) {
            QuizQuestionRequest request = questionRequests.get(i);
            
            // Validate answers
            validateQuestionAnswers(request.getAnswers());
            
            QuizQuestion question = quizQuestionMapper.toQuizQuestion(request);
            question.setQuiz(quiz);
            question.setOrderIndex(i + 1);
            question.setCreatedAt(LocalDateTime.now());
            question.setUpdatedAt(LocalDateTime.now());
            
            question = quizQuestionRepository.save(question);
            
            if (request.getAnswers() != null) {
                createQuestionAnswers(question, request.getAnswers());
            }
        }
    }
    
    private void createQuestionAnswers(QuizQuestion question, List<QuizAnswerRequest> answerRequests) {
        for (int i = 0; i < answerRequests.size(); i++) {
            QuizAnswerRequest request = answerRequests.get(i);
            
            QuizAnswer answer = quizAnswerMapper.toQuizAnswer(request);
            answer.setQuestion(question);
            
            // Use orderIndex from request or auto-increment
            if (answer.getOrderIndex() == null) {
                answer.setOrderIndex(i + 1);
            }
            
            answer.setCreatedAt(LocalDateTime.now());
            answer.setUpdatedAt(LocalDateTime.now());
            
            quizAnswerRepository.save(answer);
        }
    }
    
    private void validateQuestionAnswers(List<QuizAnswerRequest> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new AppException(ErrorCode.QUESTION_MUST_HAVE_ANSWERS);
        }
        
        if (answers.size() < 2) {
            throw new AppException(ErrorCode.QUESTION_MUST_HAVE_AT_LEAST_TWO_ANSWERS);
        }
        
        // Check that exactly one answer is correct for multiple choice
        long correctAnswersCount = answers.stream()
                .mapToLong(answer -> Boolean.TRUE.equals(answer.getIsCorrect()) ? 1 : 0)
                .sum();
        
        if (correctAnswersCount != 1) {
            throw new AppException(ErrorCode.QUESTION_MUST_HAVE_EXACTLY_ONE_CORRECT_ANSWER);
        }
    }
    
    private QuizQuestionResponse getQuestionWithAnswers(QuizQuestion question) {
        QuizQuestionResponse response = quizQuestionMapper.toQuizQuestionResponse(question);
        List<QuizAnswer> answers = quizAnswerRepository.findByQuestionIdOrderByOrderIndexAsc(question.getId());
        response.setAnswers(answers.stream()
                .map(quizAnswerMapper::toQuizAnswerResponse)
                .collect(Collectors.toList()));
        return response;
    }

    // Inner class for access info
    private record QuizAccessInfo(
            String accessLevel,
            boolean canViewInactive,
            String instructorUsername
    ) {}

    /**
     * Lấy quiz status cho student theo enrollment context
     */
    @PreAuthorize("hasRole('STUDENT')")
    public QuizStudentResponse getQuizStatusForStudent(String quizId, String courseId) {
        log.info("Getting quiz status for student - quiz: {}, course: {}", quizId, courseId);
        
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        
        User student = getCurrentUser();
        
        // Get enrollment for this specific course
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(student.getId(), courseId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_EXISTED));
        
        // Validate quiz belongs to this course
        boolean quizBelongsToCourse = enrollment.getCourse().getCourseLessons()
                .stream()
                .anyMatch(cl -> cl.getLesson().getId().equals(quiz.getLesson().getId()));
        
        if (!quizBelongsToCourse) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        // Get enrollment-specific attempt statistics
        int userAttempts = (int) quizAttemptRepository.countByQuizIdAndStudentIdAndEnrollmentId(
                quizId, student.getId(), enrollment.getId());
        
        int remainingAttempts = calculateRemainingAttempts(quiz, userAttempts);
        boolean canAttempt = canStudentAttempt(quiz, userAttempts);
        
        QuizStudentResponse response = quizMapper.toQuizStudentResponse(quiz);
        response.setUserAttempts(userAttempts);
        response.setRemainingAttempts(remainingAttempts);
        response.setCanAttempt(canAttempt);
        response.setHasAccess(true); // Already validated above
        
        return response;
    }
    
    private boolean canStudentAttempt(Quiz quiz, int userAttempts) {
        if (!Boolean.TRUE.equals(quiz.getIsActive())) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (quiz.getStartTime() != null && now.isBefore(quiz.getStartTime())) {
            return false;
        }
        
        if (quiz.getEndTime() != null && now.isAfter(quiz.getEndTime())) {
            return false;
        }
        
        if (quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0) {
            return userAttempts < quiz.getMaxAttempts();
        }
        
        return true;
    }
} 