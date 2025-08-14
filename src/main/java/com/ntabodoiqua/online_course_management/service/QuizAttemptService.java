package com.ntabodoiqua.online_course_management.service;

import com.ntabodoiqua.online_course_management.dto.request.quiz.*;
import com.ntabodoiqua.online_course_management.dto.response.quiz.*;
import com.ntabodoiqua.online_course_management.entity.*;
import com.ntabodoiqua.online_course_management.enums.AttemptStatus;
import com.ntabodoiqua.online_course_management.exception.AppException;
import com.ntabodoiqua.online_course_management.exception.ErrorCode;
import com.ntabodoiqua.online_course_management.mapper.quiz.QuizMapperFacade;
import com.ntabodoiqua.online_course_management.mapper.quiz.QuizAttemptMapper;
import com.ntabodoiqua.online_course_management.mapper.quiz.QuizQuestionMapper;
import com.ntabodoiqua.online_course_management.mapper.quiz.QuizAnswerMapper;
import com.ntabodoiqua.online_course_management.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizAttemptService {
    
    QuizRepository quizRepository;
    QuizQuestionRepository quizQuestionRepository;
    QuizAnswerRepository quizAnswerRepository;
    QuizAttemptRepository quizAttemptRepository;
    QuizAttemptAnswerRepository quizAttemptAnswerRepository;
    UserRepository userRepository;
    EnrollmentRepository enrollmentRepository;
    CourseRepository courseRepository;
    CourseLessonRepository courseLessonRepository;
    ProgressService progressService;
    
    QuizMapperFacade quizMapperFacade;
    QuizAttemptMapper quizAttemptMapper;
    QuizQuestionMapper quizQuestionMapper;
    QuizAnswerMapper quizAnswerMapper;
    
    // In-memory storage for preview sessions (In production, use Redis or similar)
    private final Map<String, QuizPreviewSession> previewSessions = new ConcurrentHashMap<>();
    
    /**
     * Bắt đầu làm quiz - tạo attempt mới
     */
    @PreAuthorize("hasRole('STUDENT')")
    @Transactional
    public QuizAttemptResponse startQuizAttempt(String quizId, String courseId) {
        log.info("Starting quiz attempt for quiz: {} in course: {}", quizId, courseId);
        
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        
        User student = getCurrentUser();
        
        // Validate quiz availability and course context
        validateQuizAvailability(quiz, student);
        validateQuizBelongsToCourse(quiz, courseId);
        
        // Get current enrollment for this specific course
        Enrollment enrollment = getEnrollmentForCourse(courseId, student);
        
        // Check if student has any in-progress attempt for this enrollment
        Optional<QuizAttempt> inProgressAttempt = quizAttemptRepository
                .findByQuizIdAndStudentIdAndEnrollmentIdAndStatus(quizId, student.getId(), enrollment.getId(), AttemptStatus.IN_PROGRESS);
        
        if (inProgressAttempt.isPresent()) {
            // Return existing in-progress attempt
            return quizMapperFacade.toQuizAttemptResponseWithDetails(inProgressAttempt.get());
        }
        
        // Check attempt limits for this enrollment
        int currentAttempts = (int) quizAttemptRepository.countByQuizIdAndStudentIdAndEnrollmentId(quizId, student.getId(), enrollment.getId());
        if (quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0 && currentAttempts >= quiz.getMaxAttempts()) {
            throw new AppException(ErrorCode.QUIZ_MAX_ATTEMPTS_EXCEEDED);
        }
        
        // Create new attempt
        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .student(student)
                .enrollment(enrollment)
                .attemptNumber(currentAttempts + 1)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .totalQuestions(quiz.getQuestions().size())
                .correctAnswers(0)
                .incorrectAnswers(0)
                .unansweredQuestions(quiz.getQuestions().size())
                .score(0.0)
                .percentage(0.0)
                .isPassed(false)
                .build();
        
        attempt = quizAttemptRepository.save(attempt);
        
        // Initialize attempt answers for all questions
        initializeAttemptAnswers(attempt, quiz.getQuestions());
        
        log.info("Quiz attempt started successfully: {}", attempt.getId());
        return quizMapperFacade.toQuizAttemptResponseWithDetails(attempt);
    }
    
    /**
     * Trả lời câu hỏi trong quiz
     */
    @PreAuthorize("hasRole('STUDENT')")
    @Transactional
    public QuizAttemptAnswerResponse answerQuestion(String attemptId, String questionId, QuizAttemptAnswerRequest request) {
        log.info("Answering question {} in attempt {}", questionId, attemptId);
        
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_ATTEMPT_NOT_FOUND));
        
        // Validate attempt
        validateAttemptAccess(attempt);
        
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.QUIZ_ATTEMPT_NOT_IN_PROGRESS);
        }
        
        // Check time limit
        if (isAttemptExpired(attempt)) {
            expireAttempt(attempt);
            throw new AppException(ErrorCode.QUIZ_ATTEMPT_EXPIRED);
        }
        
        // Find attempt answer
        QuizAttemptAnswer attemptAnswer = quizAttemptAnswerRepository
                .findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseThrow(() -> new AppException(ErrorCode.ATTEMPT_ANSWER_NOT_FOUND));
        
        // Find selected answer
        QuizAnswer selectedAnswer = quizAnswerRepository.findById(request.getSelectedAnswerId())
                .orElseThrow(() -> new AppException(ErrorCode.ANSWER_NOT_FOUND));
        
        // Validate answer belongs to question
        if (!selectedAnswer.getQuestion().getId().equals(questionId)) {
            throw new AppException(ErrorCode.INVALID_ANSWER_FOR_QUESTION);
        }
        
        // Update attempt answer
        boolean wasAnswered = attemptAnswer.getSelectedAnswer() != null;
        attemptAnswer.setSelectedAnswer(selectedAnswer);
        attemptAnswer.setIsCorrect(selectedAnswer.getIsCorrect());
        attemptAnswer.setPointsEarned(selectedAnswer.getIsCorrect() ? attemptAnswer.getQuestion().getPoints() : 0.0);
        attemptAnswer.setAnsweredAt(LocalDateTime.now());
        
        attemptAnswer = quizAttemptAnswerRepository.save(attemptAnswer);
        
        // Update attempt statistics if this is a new answer
        if (!wasAnswered) {
            updateAttemptStatistics(attempt);
        }
        
        log.info("Question answered successfully");
        return quizMapperFacade.toQuizAttemptAnswerResponseWithDetails(attemptAnswer);
    }
    
    /**
     * Nộp bài quiz
     */
    @PreAuthorize("hasRole('STUDENT')")
    @Transactional
    public QuizResultResponse submitQuiz(String attemptId) {
        log.info("Submitting quiz attempt: {}", attemptId);
        
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_ATTEMPT_NOT_FOUND));
        
        validateAttemptAccess(attempt);
        
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.QUIZ_ATTEMPT_NOT_IN_PROGRESS);
        }
        
        // Calculate final scores
        calculateFinalScores(attempt);
        
        // Mark as completed
        attempt.setStatus(AttemptStatus.COMPLETED);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setCompletedAt(LocalDateTime.now());
        
        attempt = quizAttemptRepository.save(attempt);
        
        // Update progress if quiz is passed
        if (attempt.getIsPassed()) {
            updateLessonProgress(attempt);
        }
        
        log.info("Quiz submitted successfully with score: {}%", attempt.getPercentage());
        
        // Prepare result response
        QuizResultResponse result = quizAttemptMapper.toQuizResultResponse(attempt);
        
        // Add additional info
        int userAttempts = (int) quizAttemptRepository.countByQuizIdAndStudentIdAndEnrollmentId(
                attempt.getQuiz().getId(), attempt.getStudent().getId(), attempt.getEnrollment().getId());
        int remainingAttempts = calculateRemainingAttempts(attempt.getQuiz(), userAttempts);
        boolean canRetake = canStudentRetakeQuiz(attempt.getQuiz(), userAttempts);
        
        result.setCanRetake(canRetake);
        result.setRemainingAttempts(remainingAttempts);
        result.setFeedback(generateFeedback(attempt));
        
        return result;
    }
    
    /**
     * Lấy attempt hiện tại của student
     */
    @PreAuthorize("hasRole('STUDENT')")
    public QuizAttemptResponse getCurrentAttempt(String quizId, String courseId) {
        log.info("Getting current attempt for quiz: {} in course: {}", quizId, courseId);
        
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        
        User student = getCurrentUser();
        Enrollment enrollment = getEnrollmentForCourse(courseId, student);
        
        Optional<QuizAttempt> attempt = quizAttemptRepository
                .findByQuizIdAndStudentIdAndEnrollmentIdAndStatus(quizId, student.getId(), enrollment.getId(), AttemptStatus.IN_PROGRESS);
        
        if (attempt.isEmpty()) {
            throw new AppException(ErrorCode.NO_ACTIVE_ATTEMPT_FOUND);
        }
        
        // Check if attempt has expired
        if (isAttemptExpired(attempt.get())) {
            expireAttempt(attempt.get());
            throw new AppException(ErrorCode.QUIZ_ATTEMPT_EXPIRED);
        }
        
        return quizMapperFacade.toQuizAttemptResponseWithDetails(attempt.get());
    }
    
        /**
     * Lấy lịch sử attempts của student
     */
    @PreAuthorize("hasRole('STUDENT')")
    public List<QuizResultResponse> getStudentAttemptHistory(String quizId, String courseId) {
        log.info("Getting attempt history for quiz: {} in course: {}", quizId, courseId);
        
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        
        User student = getCurrentUser();
        Enrollment enrollment = getEnrollmentForCourse(courseId, student);
        
        List<QuizAttempt> attempts = quizAttemptRepository
                .findByQuizIdAndStudentIdAndEnrollmentIdOrderByAttemptNumberDesc(quizId, student.getId(), enrollment.getId());
        
        return attempts.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.COMPLETED)
                .map(quizAttemptMapper::toQuizResultResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy kết quả tốt nhất của student
     */
    @PreAuthorize("hasRole('STUDENT')")
    public QuizResultResponse getBestScore(String quizId, String courseId) {
        log.info("Getting best score for quiz: {} in course: {}", quizId, courseId);
        
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        
        User student = getCurrentUser();
        Enrollment enrollment = getEnrollmentForCourse(courseId, student);
        
        List<QuizAttempt> attempts = quizAttemptRepository
                .findBestScoreByQuizAndStudentInEnrollment(quizId, student.getId(), enrollment.getId());
        
        if (attempts.isEmpty()) {
            throw new AppException(ErrorCode.NO_ATTEMPTS_FOUND);
        }
        
        return quizAttemptMapper.toQuizResultResponse(attempts.get(0));
    }
    
    // ================= HELPER METHODS =================
    
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private Enrollment getEnrollmentForCourse(String courseId, User student) {
        // Tìm enrollment cụ thể của student cho course này
        return enrollmentRepository.findByStudentIdAndCourseId(student.getId(), courseId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_EXISTED));
    }
    
    private Enrollment getCurrentEnrollmentForQuiz(Quiz quiz, User student) {
        // Tìm enrollment của student cho course chứa quiz này
        List<Enrollment> enrollments = enrollmentRepository.findByStudent(student);
        
        for (Enrollment enrollment : enrollments) {
            // Kiểm tra xem quiz có thuộc course này không
            boolean hasQuizInCourse = enrollment.getCourse().getCourseLessons()
                    .stream()
                    .anyMatch(cl -> cl.getLesson().getId().equals(quiz.getLesson().getId()));
            
            if (hasQuizInCourse) {
                return enrollment;
            }
        }
        
        throw new AppException(ErrorCode.ENROLLMENT_NOT_EXISTED);
    }
    
    private void validateQuizAvailability(Quiz quiz, User student) {
        // Check if quiz is active
        if (!Boolean.TRUE.equals(quiz.getIsActive())) {
            throw new AppException(ErrorCode.QUIZ_NOT_AVAILABLE);
        }
        
        // Check time availability
        LocalDateTime now = LocalDateTime.now();
        if (quiz.getStartTime() != null && now.isBefore(quiz.getStartTime())) {
            throw new AppException(ErrorCode.QUIZ_NOT_STARTED);
        }
        
        if (quiz.getEndTime() != null && now.isAfter(quiz.getEndTime())) {
            throw new AppException(ErrorCode.QUIZ_EXPIRED);
        }
        
        // Check if student has access to the quiz (enrolled in course)
        boolean hasAccess = enrollmentRepository.findByStudent(student)
                .stream()
                .anyMatch(enrollment -> 
                    enrollment.getCourse().getCourseLessons()
                            .stream()
                            .anyMatch(cl -> cl.getLesson().getId().equals(quiz.getLesson().getId()))
                );
        
        if (!hasAccess) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
    
    private void validateQuizBelongsToCourse(Quiz quiz, String courseId) {
        // Verify that the quiz's lesson belongs to the specified course
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
        
        boolean quizBelongsToCourse = course.getCourseLessons()
                .stream()
                .anyMatch(cl -> cl.getLesson().getId().equals(quiz.getLesson().getId()));
        
        if (!quizBelongsToCourse) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
    
    private void validateAttemptAccess(QuizAttempt attempt) {
        User currentUser = getCurrentUser();
        if (!attempt.getStudent().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
    
    private boolean isAttemptExpired(QuizAttempt attempt) {
        if (attempt.getQuiz().getTimeLimitMinutes() == null) {
            return false; // No time limit
        }
        
        LocalDateTime expirationTime = attempt.getStartedAt()
                .plusMinutes(attempt.getQuiz().getTimeLimitMinutes());
        
        return LocalDateTime.now().isAfter(expirationTime);
    }
    
    private void expireAttempt(QuizAttempt attempt) {
        attempt.setStatus(AttemptStatus.EXPIRED);
        attempt.setCompletedAt(LocalDateTime.now());
        quizAttemptRepository.save(attempt);
    }
    
    private void initializeAttemptAnswers(QuizAttempt attempt, Set<QuizQuestion> questions) {
        for (QuizQuestion question : questions) {
            QuizAttemptAnswer attemptAnswer = QuizAttemptAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .isCorrect(false)
                    .pointsEarned(0.0)
                    .build();
            
            quizAttemptAnswerRepository.save(attemptAnswer);
        }
    }
    
    private void updateAttemptStatistics(QuizAttempt attempt) {
        List<QuizAttemptAnswer> attemptAnswers = quizAttemptAnswerRepository.findByAttemptId(attempt.getId());
        
        int answered = 0;
        int correct = 0;
        int incorrect = 0;
        
        for (QuizAttemptAnswer answer : attemptAnswers) {
            if (answer.getSelectedAnswer() != null) {
                answered++;
                if (answer.getIsCorrect()) {
                    correct++;
                } else {
                    incorrect++;
                }
            }
        }
        
        attempt.setCorrectAnswers(correct);
        attempt.setIncorrectAnswers(incorrect);
        attempt.setUnansweredQuestions(attempt.getTotalQuestions() - answered);
        
        // Calculate current percentage
        if (attempt.getTotalQuestions() > 0) {
            attempt.setPercentage((double) correct / attempt.getTotalQuestions() * 100);
        }
        
        quizAttemptRepository.save(attempt);
    }
    
    private void calculateFinalScores(QuizAttempt attempt) {
        List<QuizAttemptAnswer> attemptAnswers = quizAttemptAnswerRepository.findByAttemptId(attempt.getId());
        
        double totalPossiblePoints = attemptAnswers.stream()
                .mapToDouble(answer -> answer.getQuestion().getPoints())
                .sum();
        
        double earnedPoints = attemptAnswers.stream()
                .mapToDouble(answer -> answer.getPointsEarned() != null ? answer.getPointsEarned() : 0.0)
                .sum();
        
        attempt.setScore(earnedPoints);
        
        if (totalPossiblePoints > 0) {
            attempt.setPercentage(earnedPoints / totalPossiblePoints * 100);
        } else {
            attempt.setPercentage(0.0);
        }
        
        // Check if passed
        Double passingScore = attempt.getQuiz().getPassingScore();
        if (passingScore != null) {
            attempt.setIsPassed(attempt.getPercentage() >= passingScore);
        } else {
            attempt.setIsPassed(attempt.getPercentage() >= 70.0); // Default 70%
        }
    }
    
    private void updateLessonProgress(QuizAttempt attempt) {
        try {
            progressService.updateQuizProgress(
                    attempt.getStudent().getId(),
                    attempt.getQuiz().getLesson().getId(),
                    attempt.getQuiz().getId(),
                    attempt.getPercentage(),
                    attempt.getEnrollment().getId()
            );
        } catch (Exception e) {
            log.warn("Failed to update lesson progress for attempt: {}", attempt.getId(), e);
            // Don't fail the submission if progress update fails
        }
    }
    
    private int calculateRemainingAttempts(Quiz quiz, int currentAttempts) {
        if (quiz.getMaxAttempts() == null || quiz.getMaxAttempts() <= 0) {
            return -1; // Unlimited
        }
        
        return Math.max(0, quiz.getMaxAttempts() - currentAttempts);
    }
    
    private boolean canStudentRetakeQuiz(Quiz quiz, int currentAttempts) {
        if (!Boolean.TRUE.equals(quiz.getIsActive())) {
            return false;
        }
        
        // Check time availability
        LocalDateTime now = LocalDateTime.now();
        if (quiz.getEndTime() != null && now.isAfter(quiz.getEndTime())) {
            return false;
        }
        
        // Check attempt limits
        if (quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0) {
            return currentAttempts < quiz.getMaxAttempts();
        }
        
        return true; // Unlimited attempts
    }
    
    private String generateFeedback(QuizAttempt attempt) {
        double percentage = attempt.getPercentage();
        
        if (percentage >= 90) {
            return "Excellent work! You have mastered this topic.";
        } else if (percentage >= 80) {
            return "Great job! You have a solid understanding of the material.";
        } else if (percentage >= 70) {
            return "Good work! You passed, but consider reviewing the material.";
        } else if (percentage >= 60) {
            return "You're getting there. Review the material and try again.";
        } else {
            return "Keep studying and practicing. Don't give up!";
        }
    }

    // ================= COURSE-LEVEL QUIZ STATISTICS =================
    
    /**
     * Lấy thống kê quiz tổng quan của course
     * Chỉ Instructor hoặc Admin có thể xem
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public CourseQuizStatisticsResponse getCourseQuizStatistics(String courseId) {
        log.info("Getting quiz statistics for course: {}", courseId);
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
        
        // Validate permission
        validateCourseAccess(course);
        
        // Get all quizzes in this course
        List<Quiz> courseQuizzes = getCourseQuizzes(course);
        
        if (courseQuizzes.isEmpty()) {
            return CourseQuizStatisticsResponse.builder()
                    .totalQuizzes(0)
                    .totalAttempts(0)
                    .averageScore(0.0)
                    .passRate(0.0)
                    .scoreDistribution(Collections.emptyList())
                    .gradeDistribution(Collections.emptyList())
                    .build();
        }
        
        // Get all completed attempts for these quizzes
        List<String> quizIds = courseQuizzes.stream()
                .map(Quiz::getId)
                .collect(Collectors.toList());
        
        List<QuizAttempt> completedAttempts = quizAttemptRepository.findCompletedAttemptsByQuizIds(quizIds);
        
        // Calculate statistics
        return buildCourseQuizStatistics(courseQuizzes, completedAttempts);
    }
    
    /**
     * Lấy kết quả quiz của tất cả students trong course
     * Chỉ Instructor hoặc Admin có thể xem
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public Page<StudentBestQuizAttemptResponse> getCourseStudentQuizResults(String courseId, Pageable pageable) {
        log.info("Getting best student quiz results for each quiz in course: {}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        validateCourseAccess(course);

        List<Enrollment> enrollments = enrollmentRepository.findByCourse(course);
        if (enrollments.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Quiz> courseQuizzes = getCourseQuizzes(course);
        if (courseQuizzes.isEmpty()) {
            return Page.empty(pageable);
        }
        List<String> quizIds = courseQuizzes.stream().map(Quiz::getId).collect(Collectors.toList());
        Map<String, Quiz> quizMap = courseQuizzes.stream().collect(Collectors.toMap(Quiz::getId, q -> q));

        List<String> enrollmentIds = enrollments.stream().map(Enrollment::getId).collect(Collectors.toList());
        Map<String, User> studentMap = enrollments.stream()
                .collect(Collectors.toMap(e -> e.getStudent().getId(), Enrollment::getStudent, (existing, replacement) -> existing));

        // 1. Fetch all completed attempts for all students IN THIS COURSE for the relevant quizzes
        List<QuizAttempt> allAttempts = quizAttemptRepository.findByEnrollmentIdInAndQuizIdInAndStatus(enrollmentIds, quizIds, AttemptStatus.COMPLETED);

        // 2. Group attempts by student, then by quiz, and find the one with the max score
        Map<String, Map<String, Optional<QuizAttempt>>> bestAttemptsByStudentAndQuiz = allAttempts.stream()
                .collect(Collectors.groupingBy(
                        attempt -> attempt.getStudent().getId(),
                        Collectors.groupingBy(
                                attempt -> attempt.getQuiz().getId(),
                                Collectors.maxBy(Comparator.comparing(QuizAttempt::getScore))
                        )
                ));

        List<StudentBestQuizAttemptResponse> results = new ArrayList<>();

        bestAttemptsByStudentAndQuiz.forEach((sId, quizBestAttemptMap) -> {
            User student = studentMap.get(sId);
            if (student == null) return; // Skip if student not found in current course enrollments
            
            quizBestAttemptMap.forEach((qId, bestAttemptOpt) -> {
                bestAttemptOpt.ifPresent(bestAttempt -> {
                    Quiz quiz = quizMap.get(qId);
                    double maxScore = quiz.getQuestions().stream()
                            .mapToDouble(q -> q.getPoints() != null ? q.getPoints() : 0.0)
                            .sum();
                    results.add(StudentBestQuizAttemptResponse.builder()
                            .studentId(student.getId())
                            .studentUsername(student.getUsername())
                            .studentFirstName(student.getFirstName())
                            .studentLastName(student.getLastName())
                            .studentEmail(student.getEmail())
                            .quizId(quiz.getId())
                            .quizTitle(quiz.getTitle())
                            .attemptId(bestAttempt.getId())
                            .attemptNumber(bestAttempt.getAttemptNumber())
                            .score(bestAttempt.getScore())
                            .maxScore(maxScore)
                            .percentage(bestAttempt.getPercentage())
                            .isPassed(bestAttempt.getIsPassed())
                            .completedAt(bestAttempt.getCompletedAt())
                            .build());
                });
            });
        });

        // 3. Paginate the final list
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), results.size());
        List<StudentBestQuizAttemptResponse> pageContent = (start > results.size()) ? Collections.emptyList() : results.subList(start, end);

        return new PageImpl<>(pageContent, pageable, results.size());
    }
    
    /**
     * Lấy lịch sử quiz của 1 student trong course
     * Chỉ Instructor hoặc Admin có thể xem
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public List<StudentQuizHistoryResponse> getStudentQuizHistoryInCourse(String courseId, String studentId) {
        log.info("Getting quiz history for student {} in course {}", studentId, courseId);
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
        
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Validate permission
        validateCourseAccess(course);
        
        // Check if student is enrolled in this course
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_EXISTED));
        
        // Get all quizzes in this course
        List<Quiz> courseQuizzes = getCourseQuizzes(course);
        
        if (courseQuizzes.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> quizIds = courseQuizzes.stream()
                .map(Quiz::getId)
                .collect(Collectors.toList());
        
        // Get all completed attempts for this student in this course
        List<QuizAttempt> attempts = quizAttemptRepository
                .findByStudentIdAndEnrollmentIdAndQuizIdInAndStatusOrderByCompletedAtDesc(
                        studentId, enrollment.getId(), quizIds, AttemptStatus.COMPLETED);
        
        return attempts.stream()
                .map(this::buildStudentQuizHistory)
                .collect(Collectors.toList());
    }
    
    // ================= QUIZ PREVIEW METHODS =================
    
    /**
     * Bắt đầu quiz preview cho instructor/admin
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public QuizAttemptResponse startQuizPreview(String quizId) {
        log.info("Starting quiz preview for quiz: {}", quizId);
        
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        
        User currentUser = getCurrentUser();
        
        // Validate permission
        validateQuizPreviewPermission(quiz, currentUser);
        
        // Generate session ID
        String sessionId = UUID.randomUUID().toString();
        
        // Create preview session
        QuizPreviewSession session = QuizPreviewSession.create(sessionId, quizId, currentUser.getId());
        previewSessions.put(sessionId, session);
        
        // Create mock QuizAttemptResponse for preview
        QuizAttemptResponse response = QuizAttemptResponse.builder()
                .id(sessionId) // Use sessionId as attemptId
                .quiz(quizMapperFacade.toQuizResponseWithDetails(quiz))
                .student(null) // No student in preview
                .attemptNumber(1)
                .startedAt(session.getStartedAt())
                .status(AttemptStatus.IN_PROGRESS)
                .totalQuestions(quiz.getQuestions().size())
                .correctAnswers(0)
                .incorrectAnswers(0)
                .unansweredQuestions(quiz.getQuestions().size())
                .score(0.0)
                .percentage(0.0)
                .isPassed(false)
                .attemptAnswers(new ArrayList<>())
                .build();
        
        log.info("Quiz preview started successfully with session: {}", sessionId);
        return response;
    }
    
    /**
     * Trả lời câu hỏi trong preview mode
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public QuizAttemptAnswerResponse answerQuestionPreview(String sessionId, String questionId, QuizAttemptAnswerRequest request) {
        log.info("Answering question {} in preview session {}", questionId, sessionId);
        
        // Get preview session
        QuizPreviewSession session = previewSessions.get(sessionId);
        if (session == null) {
            throw new AppException(ErrorCode.QUIZ_ATTEMPT_NOT_FOUND);
        }
        
        // Validate user
        User currentUser = getCurrentUser();
        if (!session.getUserId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        if (session.isCompleted()) {
            throw new AppException(ErrorCode.QUIZ_ATTEMPT_NOT_IN_PROGRESS);
        }
        
        // Find question
        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        
        // Find selected answer
        QuizAnswer selectedAnswer = quizAnswerRepository.findById(request.getSelectedAnswerId())
                .orElseThrow(() -> new AppException(ErrorCode.ANSWER_NOT_FOUND));
        
        // Validate answer belongs to question
        if (!selectedAnswer.getQuestion().getId().equals(questionId)) {
            throw new AppException(ErrorCode.INVALID_ANSWER_FOR_QUESTION);
        }
        
        // Store answer in session
        session.answerQuestion(questionId, selectedAnswer);
        
        // Create response
        QuizAttemptAnswerResponse response = QuizAttemptAnswerResponse.builder()
                .id(sessionId + "-" + questionId)
                .question(quizQuestionMapper.toQuizQuestionResponse(question))
                .selectedAnswer(quizAnswerMapper.toQuizAnswerResponse(selectedAnswer))
                .isCorrect(selectedAnswer.getIsCorrect())
                .pointsEarned(selectedAnswer.getIsCorrect() ? question.getPoints() : 0.0)
                .answeredAt(LocalDateTime.now())
                .build();
        
        log.info("Question answered successfully in preview");
        return response;
    }
    
    /**
     * Nộp bài preview quiz
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public QuizResultResponse submitQuizPreview(String sessionId) {
        log.info("Submitting quiz preview session: {}", sessionId);
        
        // Get preview session
        QuizPreviewSession session = previewSessions.get(sessionId);
        if (session == null) {
            throw new AppException(ErrorCode.QUIZ_ATTEMPT_NOT_FOUND);
        }
        
        // Validate user
        User currentUser = getCurrentUser();
        if (!session.getUserId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        if (session.isCompleted()) {
            throw new AppException(ErrorCode.QUIZ_ATTEMPT_NOT_IN_PROGRESS);
        }
        
        // Get quiz
        Quiz quiz = quizRepository.findById(session.getQuizId())
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        
        // Calculate results
        QuizResultResponse result = calculatePreviewResults(session, quiz);
        
        // Mark session as completed
        session.complete();
        
        // Clean up session after some time (optional)
        // In production, you might want to use a scheduled task to clean up old sessions
        
        log.info("Quiz preview submitted successfully with score: {}%", result.getPercentage());
        return result;
    }
    
    /**
     * Lấy trạng thái preview session hiện tại
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public QuizAttemptResponse getPreviewStatus(String sessionId) {
        log.info("Getting preview session status: {}", sessionId);
        
        // Get preview session
        QuizPreviewSession session = previewSessions.get(sessionId);
        if (session == null) {
            throw new AppException(ErrorCode.QUIZ_ATTEMPT_NOT_FOUND);
        }
        
        // Validate user
        User currentUser = getCurrentUser();
        if (!session.getUserId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        // Get quiz
        Quiz quiz = quizRepository.findById(session.getQuizId())
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
        
        // Build response
        AttemptStatus status = session.isCompleted() ? AttemptStatus.COMPLETED : AttemptStatus.IN_PROGRESS;
        int answeredCount = session.getAnswers().size();
        
        QuizAttemptResponse response = QuizAttemptResponse.builder()
                .id(sessionId)
                .quiz(quizMapperFacade.toQuizResponseWithDetails(quiz))
                .student(null) // No student in preview
                .attemptNumber(1)
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .status(status)
                .totalQuestions(quiz.getQuestions().size())
                .unansweredQuestions(quiz.getQuestions().size() - answeredCount)
                .build();
        
        return response;
    }
    
    // Preview helper methods
    private void validateQuizPreviewPermission(Quiz quiz, User user) {
        // Admin can preview any quiz
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));
        
        if (isAdmin) {
            return;
        }
        
        // Instructor can preview their own quizzes
        boolean isOwner = quiz.getCreatedBy().getId().equals(user.getId());
        if (!isOwner) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
    
    private QuizResultResponse calculatePreviewResults(QuizPreviewSession session, Quiz quiz) {
        int totalQuestions = quiz.getQuestions().size();
        int answeredQuestions = session.getAnswers().size();
        int correctAnswers = 0;
        double totalPoints = 0.0;
        double earnedPoints = 0.0;
        
        for (QuizQuestion question : quiz.getQuestions()) {
            totalPoints += question.getPoints();
            
            QuizAnswer selectedAnswer = session.getAnswer(question.getId());
            if (selectedAnswer != null && selectedAnswer.getIsCorrect()) {
                correctAnswers++;
                earnedPoints += question.getPoints();
            }
        }
        
        double percentage = totalPoints > 0 ? (earnedPoints / totalPoints * 100) : 0.0;
        boolean isPassed = quiz.getPassingScore() != null ? 
                percentage >= quiz.getPassingScore() : percentage >= 70.0;
        
        // Calculate duration
        LocalDateTime completedAt = LocalDateTime.now();
        Long durationMinutes = null;
        if (session.getStartedAt() != null) {
            durationMinutes = java.time.Duration.between(session.getStartedAt(), completedAt).toMinutes();
        }
        
        return QuizResultResponse.builder()
                .attemptId(session.getSessionId())
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .startedAt(session.getStartedAt())
                .completedAt(completedAt)
                .durationMinutes(durationMinutes)
                .score(earnedPoints)
                .percentage(percentage)
                .isPassed(isPassed)
                .totalQuestions(totalQuestions)
                .correctAnswers(correctAnswers)
                .incorrectAnswers(answeredQuestions - correctAnswers)
                .unansweredQuestions(totalQuestions - answeredQuestions)
                .attemptNumber(1)
                .passingScore(quiz.getPassingScore())
                .feedback("Preview completed - " + generatePreviewFeedback(percentage))
                .canRetake(true) // Always true for preview
                .remainingAttempts(-1) // Unlimited for preview
                .build();
    }
    
    // ================= COURSE-LEVEL STATISTICS HELPER METHODS =================
    
    private void validateCourseAccess(Course course) {
        User currentUser = getCurrentUser();
        
        // Admin can access any course
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));
        
        if (isAdmin) {
            return;
        }
        
        // Instructor can access their own courses
        boolean isOwner = course.getInstructor().getId().equals(currentUser.getId());
        if (!isOwner) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
    
    private List<Quiz> getCourseQuizzes(Course course) {
        List<CourseLesson> courseLessons = courseLessonRepository.findByCourseOrderByOrderIndexAsc(course);
        
        return courseLessons.stream()
                .map(CourseLesson::getLesson)
                .map(Lesson::getQuiz)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    private CourseQuizStatisticsResponse buildCourseQuizStatistics(List<Quiz> courseQuizzes, List<QuizAttempt> completedAttempts) {
        int totalQuizzes = courseQuizzes.size();
        int totalAttempts = completedAttempts.size();
        
        if (totalAttempts == 0) {
            return CourseQuizStatisticsResponse.builder()
                    .totalQuizzes(totalQuizzes)
                    .totalAttempts(0)
                    .averageScore(0.0)
                    .passRate(0.0)
                    .scoreDistribution(Collections.emptyList())
                    .gradeDistribution(Collections.emptyList())
                    .build();
        }
        
        // Calculate average score and pass rate
        double totalScore = completedAttempts.stream()
                .mapToDouble(QuizAttempt::getScore)
                .sum();
        double averageScore = totalScore / totalAttempts;
        
        long passedAttempts = completedAttempts.stream()
                .mapToLong(attempt -> attempt.getIsPassed() ? 1 : 0)
                .sum();
        double passRate = (double) passedAttempts / totalAttempts * 100;
        
        // Build score distribution
        List<CourseQuizStatisticsResponse.ScoreDistributionItem> scoreDistribution = buildScoreDistribution(completedAttempts);
        
        // Build grade distribution
        List<CourseQuizStatisticsResponse.GradeDistributionItem> gradeDistribution = buildGradeDistribution(completedAttempts);

        // Build quiz performance distribution
        List<CourseQuizStatisticsResponse.QuizPerformanceItem> quizPerformance = buildQuizPerformance(completedAttempts);
        
        return CourseQuizStatisticsResponse.builder()
                .totalQuizzes(totalQuizzes)
                .totalAttempts(totalAttempts)
                .averageScore(averageScore)
                .passRate(passRate)
                .scoreDistribution(scoreDistribution)
                .gradeDistribution(gradeDistribution)
                .quizPerformance(quizPerformance)
                .build();
    }
    
    private List<CourseQuizStatisticsResponse.ScoreDistributionItem> buildScoreDistribution(List<QuizAttempt> attempts) {
        Map<String, Long> distribution = attempts.stream()
                .collect(Collectors.groupingBy(
                        attempt -> {
                            double score = attempt.getScore();
                            if (score >= 9) return "9-10";
                            else if (score >= 8) return "8-9";
                            else if (score >= 7) return "7-8";
                            else if (score >= 6) return "6-7";
                            else if (score >= 5) return "5-6";
                            else if (score >= 4) return "4-5";
                            else if (score >= 3) return "3-4";
                            else if (score >= 2) return "2-3";
                            else if (score >= 1) return "1-2";
                            else return "0-1";
                        },
                        Collectors.counting()
                ));
        
        return distribution.entrySet().stream()
                .map(entry -> CourseQuizStatisticsResponse.ScoreDistributionItem.builder()
                        .range(entry.getKey())
                        .count(entry.getValue().intValue())
                        .build())
                .sorted((a, b) -> Float.compare(Float.parseFloat(b.getRange().split("-")[0]), 
                                               Float.parseFloat(a.getRange().split("-")[0])))
                .collect(Collectors.toList());
    }
    
    private List<CourseQuizStatisticsResponse.QuizPerformanceItem> buildQuizPerformance(List<QuizAttempt> completedAttempts) {
        if (completedAttempts.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Find the best attempt for each student for each quiz
        Map<String, Optional<QuizAttempt>> bestAttemptsByStudentAndQuiz = completedAttempts.stream()
                .collect(Collectors.groupingBy(
                        attempt -> attempt.getStudent().getId() + "_" + attempt.getQuiz().getId(),
                        Collectors.maxBy(Comparator.comparing(QuizAttempt::getScore))
                ));

        // 2. Group these best attempts by quiz
        Map<Quiz, List<QuizAttempt>> bestAttemptsByQuiz = bestAttemptsByStudentAndQuiz.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.groupingBy(QuizAttempt::getQuiz));


        // 3. For each quiz, count passed and failed
        return bestAttemptsByQuiz.entrySet().stream()
                .map(entry -> {
                    Quiz quiz = entry.getKey();
                    List<QuizAttempt> bestAttempts = entry.getValue();

                    long passedCount = bestAttempts.stream().filter(QuizAttempt::getIsPassed).count();
                    long failedCount = bestAttempts.size() - passedCount;

                    return CourseQuizStatisticsResponse.QuizPerformanceItem.builder()
                            .quizId(quiz.getId())
                            .quizTitle(quiz.getTitle())
                            .passedCount(passedCount)
                            .failedCount(failedCount)
                            .build();
                })
                .sorted(Comparator.comparing(CourseQuizStatisticsResponse.QuizPerformanceItem::getQuizTitle))
                .collect(Collectors.toList());
    }
    
    private List<CourseQuizStatisticsResponse.GradeDistributionItem> buildGradeDistribution(List<QuizAttempt> attempts) {
        Map<String, Long> distribution = attempts.stream()
                .collect(Collectors.groupingBy(
                        attempt -> {
                            double percentage = attempt.getPercentage();
                            if (percentage >= 90) return "Xuất sắc";
                            else if (percentage >= 80) return "Giỏi";
                            else if (percentage >= 70) return "Khá";
                            else if (percentage >= 50) return "Trung bình";
                            else return "Yếu";
                        },
                        Collectors.counting()
                ));
        
        String[] grades = {"Xuất sắc", "Giỏi", "Khá", "Trung bình", "Yếu"};
        String[] colors = {"#52c41a", "#1890ff", "#faad14", "#fa8c16", "#ff4d4f"};
        
        List<CourseQuizStatisticsResponse.GradeDistributionItem> result = new ArrayList<>();
        for (int i = 0; i < grades.length; i++) {
            long count = distribution.getOrDefault(grades[i], 0L);
            result.add(CourseQuizStatisticsResponse.GradeDistributionItem.builder()
                    .name(grades[i])
                    .count((int) count)
                    .color(colors[i])
                    .build());
        }
        
        return result;
    }
    
    private StudentQuizHistoryResponse buildStudentQuizHistory(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        Lesson lesson = quiz.getLesson();
        
        Integer duration = null;
        if (attempt.getStartedAt() != null && attempt.getCompletedAt() != null) {
            duration = (int) java.time.Duration.between(attempt.getStartedAt(), attempt.getCompletedAt()).toMinutes();
        }

        double maxScore = quiz.getQuestions().stream()
                .mapToDouble(q -> q.getPoints() != null ? q.getPoints() : 0.0)
                .sum();
        
        return StudentQuizHistoryResponse.builder()
                .attemptId(attempt.getId())
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .lessonTitle(lesson.getTitle())
                .attemptNumber(attempt.getAttemptNumber())
                .score(attempt.getScore())
                .maxScore(maxScore)
                .percentage(attempt.getPercentage())
                .isPassed(attempt.getIsPassed())
                .startedAt(attempt.getStartedAt())
                .completedAt(attempt.getCompletedAt())
                .duration(duration)
                .totalQuestions(attempt.getTotalQuestions())
                .correctAnswers(attempt.getCorrectAnswers())
                .incorrectAnswers(attempt.getIncorrectAnswers())
                .build();
    }

    private String generatePreviewFeedback(double percentage) {
        String feedback;
        if (percentage >= 90) {
            feedback = "Excellent work! You have mastered this topic.";
        } else if (percentage >= 80) {
            feedback = "Great job! You have a solid understanding of the material.";
        } else if (percentage >= 70) {
            feedback = "Good work! You passed, but consider reviewing the material.";
        } else if (percentage >= 60) {
            feedback = "You're getting there. Review the material and try again.";
        } else {
            feedback = "Keep studying and practicing. Don't give up!";
        }
        return "This is a preview result. " + feedback;
    }

    /**
     * Lấy số lượt làm quiz theo thời gian
     * Chỉ Instructor hoặc Admin có thể xem
     */
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public List<Map<String, Object>> getQuizAttemptsOverTime(String quizId) {
        log.info("Getting attempt count over time for quiz: {}", quizId);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        // Instructor can only access their own course quizzes
        User currentUser = getCurrentUser();
        if (currentUser.getRoles().stream().anyMatch(role -> role.getName().equals("INSTRUCTOR"))) {
            List<CourseLesson> courseLessons = courseLessonRepository.findByLesson(quiz.getLesson());
            if (courseLessons.isEmpty()) {
                throw new AppException(ErrorCode.UNAUTHORIZED); // This quiz/lesson is not in any course
            }
            boolean isInstructorForCourse = courseLessons.stream()
                    .anyMatch(cl -> cl.getCourse().getInstructor().getId().equals(currentUser.getId()));
            if (!isInstructorForCourse) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }
        
        List<QuizAttempt> attempts = quizAttemptRepository.findByQuizIdAndStatus(quizId, AttemptStatus.COMPLETED);

        // Group by completed date and count
        Map<String, Long> attemptsByDate = attempts.stream()
                .filter(attempt -> attempt.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                        attempt -> attempt.getCompletedAt().toLocalDate().toString(),
                        Collectors.counting()
                ));

        return attemptsByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("date", entry.getKey());
                    map.put("attemptCount", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }
}