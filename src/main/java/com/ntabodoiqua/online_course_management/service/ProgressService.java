package com.ntabodoiqua.online_course_management.service;

import com.ntabodoiqua.online_course_management.dto.request.enrollment.ProgressUpdateRequest;
import com.ntabodoiqua.online_course_management.dto.response.enrollment.ProgressResponse;
import com.ntabodoiqua.online_course_management.entity.Enrollment;
import com.ntabodoiqua.online_course_management.entity.Lesson;
import com.ntabodoiqua.online_course_management.entity.Progress;
import com.ntabodoiqua.online_course_management.entity.Quiz;
import com.ntabodoiqua.online_course_management.entity.User;
import com.ntabodoiqua.online_course_management.entity.LessonDocument;
import com.ntabodoiqua.online_course_management.entity.DocumentView;
import com.ntabodoiqua.online_course_management.exception.AppException;
import com.ntabodoiqua.online_course_management.exception.ErrorCode;
import com.ntabodoiqua.online_course_management.repository.EnrollmentRepository;
import com.ntabodoiqua.online_course_management.repository.LessonRepository;
import com.ntabodoiqua.online_course_management.repository.ProgressRepository;
import com.ntabodoiqua.online_course_management.repository.QuizRepository;
import com.ntabodoiqua.online_course_management.repository.UserRepository;
import com.ntabodoiqua.online_course_management.repository.LessonDocumentRepository;
import com.ntabodoiqua.online_course_management.repository.DocumentViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class ProgressService {
    ProgressRepository progressRepository;
    EnrollmentRepository enrollmentRepository;
    LessonRepository lessonRepository;
    QuizRepository quizRepository;
    UserRepository userRepository;
    LessonDocumentRepository lessonDocumentRepository;
    DocumentViewRepository documentViewRepository;

    public List<ProgressResponse> getProgressByEnrollment(String enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_EXISTED));
        
        // Khởi tạo progress records nếu chưa có
        initializeProgressForEnrollment(enrollment);
        
        List<Progress> progresses = progressRepository.findByEnrollmentId(enrollmentId);
        return progresses.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ProgressResponse updateProgress(ProgressUpdateRequest request) {
        Enrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_EXISTED));
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        Progress progress = progressRepository.findByEnrollmentIdAndLessonId(request.getEnrollmentId(), request.getLessonId())
                .orElse(new Progress(null, enrollment, lesson, false, null, null, null));
        progress.setCompleted(request.isCompleted());
        progress.setCompletionDate(request.isCompleted() ? LocalDate.now() : null);
        progressRepository.save(progress);

        recalculateAndSaveEnrollmentProgress(enrollment);

        return toResponse(progress);
    }

    @Transactional
    public void recalculateAndSaveEnrollmentProgress(Enrollment enrollment) {
        // Cập nhật progress tổng thể cho enrollment
        long completed = progressRepository.countByEnrollmentIdAndIsCompletedTrue(enrollment.getId());
        int total = enrollment.getCourse().getTotalLessons();
        enrollment.setProgress(total > 0 ? (double) completed / total : 0.0);
        if (completed == total && total > 0) {
            enrollment.setCompleted(true);
            enrollment.setCompletionDate(LocalDate.now());
        } else {
            enrollment.setCompleted(false);
            enrollment.setCompletionDate(null);
        }
        enrollmentRepository.save(enrollment);
    }

    @Transactional
    public void updateQuizProgress(String studentId, String lessonId, String quizId, Double quizScore, String enrollmentId) {
        // Find the enrollment for this student and the course containing this lesson
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
                
        // Use the specific enrollment context instead of guessing
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_EXISTED));
                
        // Validate that this enrollment belongs to the student
        if (!enrollment.getStudent().getId().equals(studentId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        // Find or create progress record
        Progress progress = progressRepository.findByEnrollmentIdAndLessonId(enrollment.getId(), lessonId)
                .orElse(new Progress(null, enrollment, lesson, false, null, null, null));
        
        // Update quiz completion info
        progress.setCompletedQuiz(quiz);
        progress.setQuizScore(quizScore);
        
        // Mark lesson as completed if quiz is passed
        Double passingScore = quiz.getPassingScore() != null ? quiz.getPassingScore() : 70.0; // Default 70%
        boolean isQuizPassed = quizScore != null && quizScore >= passingScore;
        if (isQuizPassed) {
            progress.setCompleted(true);
            progress.setCompletionDate(LocalDate.now());
        }
        
        progressRepository.save(progress);
        
        // Recalculate overall enrollment progress
        recalculateAndSaveEnrollmentProgress(enrollment);
    }

    /**
     * Track document view và kiểm tra lesson completion
     */
    @Transactional
    public void trackDocumentView(String username, String documentId) {
        System.out.println("=== DEBUG: trackDocumentView called ===");
        System.out.println("Username: " + username);
        System.out.println("DocumentId: " + documentId);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        System.out.println("User found: " + user.getId() + " - " + user.getUsername());
        
        LessonDocument document = lessonDocumentRepository.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        System.out.println("Document found: " + document.getId() + " - " + document.getTitle());
        
        // Kiểm tra đã view chưa
        boolean alreadyViewed = documentViewRepository.existsByUserAndLessonDocument(user, document);
        System.out.println("Already viewed: " + alreadyViewed);
        
        if (!alreadyViewed) {
            try {
                // Tạo record view mới
                DocumentView documentView = DocumentView.builder()
                        .user(user)
                        .lessonDocument(document)
                        .viewedAt(java.time.LocalDateTime.now())
                        .build();
                
                System.out.println("Creating DocumentView record...");
                DocumentView savedView = documentViewRepository.save(documentView);
                System.out.println("DocumentView saved with ID: " + savedView.getId());
                
                // Kiểm tra lesson completion
                checkLessonCompletionByDocuments(user, document.getLesson().getId());
                System.out.println("=== DEBUG: trackDocumentView completed successfully ===");
            } catch (Exception e) {
                System.err.println("=== ERROR: Failed to save DocumentView ===");
                e.printStackTrace();
                throw e; // Re-throw để frontend biết có lỗi
            }
        } else {
            System.out.println("Document already viewed by user - skipping");
        }
    }

    /**
     * Kiểm tra lesson completion dựa trên documents hoặc quiz
     */
    @Transactional
    public void checkLessonCompletionByDocuments(User user, String lessonId) {
        System.out.println("=== DEBUG: checkLessonCompletionByDocuments called ===");
        System.out.println("User: " + user.getId() + " - " + user.getUsername());
        System.out.println("LessonId: " + lessonId);
        
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        System.out.println("Lesson found: " + lesson.getId() + " - " + lesson.getTitle());
        
        // Tìm enrollment của user cho lesson này
        List<Enrollment> enrollments = enrollmentRepository.findByStudent(user).stream()
                .filter(enrollment -> enrollment.getCourse().getCourseLessons().stream()
                        .anyMatch(cl -> cl.getLesson().getId().equals(lessonId)))
                .collect(java.util.stream.Collectors.toList());
        
        if (enrollments.isEmpty()) {
            System.out.println("No enrollments found for user and lesson - returning");
            return; // User không enroll khóa học chứa lesson này
        }
        
        Enrollment enrollment = enrollments.get(0);
        System.out.println("Enrollment found: " + enrollment.getId());
        
        // Kiểm tra progress hiện tại
        Progress progress = progressRepository.findByEnrollmentIdAndLessonId(enrollment.getId(), lessonId)
                .orElse(new Progress(null, enrollment, lesson, false, null, null, null));
        System.out.println("Current progress - ID: " + progress.getId() + ", isCompleted: " + progress.isCompleted());
        
        // Nếu đã completed rồi thì không cần check nữa
        if (progress.isCompleted()) {
            System.out.println("Progress already completed - returning");
            return;
        }
        
        // Logic completion:
        // 1. Nếu lesson có quiz → phải pass quiz mới completed
        // 2. Nếu lesson không có quiz → xem hết documents thì completed
        
        boolean hasQuiz = lesson.getQuiz() != null;
        System.out.println("Lesson has quiz: " + hasQuiz);
        
        if (hasQuiz) {
            // Lesson có quiz → chỉ complete khi pass quiz (logic này đã có trong updateQuizProgress)
            System.out.println("Lesson has quiz - completion handled by quiz logic, returning");
            return;
        } else {
            // Lesson không có quiz → check documents
            long totalDocuments = documentViewRepository.countDocumentsByLessonId(lessonId);
            long viewedDocuments = documentViewRepository.countByUserAndLessonId(user, lessonId);
            
            System.out.println("Total documents in lesson: " + totalDocuments);
            System.out.println("Documents viewed by user: " + viewedDocuments);
            
            if (totalDocuments > 0 && viewedDocuments >= totalDocuments) {
                // Đã xem hết tất cả documents
                System.out.println("All documents viewed - marking lesson as completed");
                progress.setCompleted(true);
                progress.setCompletionDate(java.time.LocalDate.now());
                
                if (progress.getId() == null) {
                    // New progress record - set enrollment and lesson
                    progress.setEnrollment(enrollment);
                    progress.setLesson(lesson);
                }
                
                Progress savedProgress = progressRepository.save(progress);
                System.out.println("Progress saved with ID: " + savedProgress.getId() + ", isCompleted: " + savedProgress.isCompleted());
                
                // Recalculate overall enrollment progress
                recalculateAndSaveEnrollmentProgress(enrollment);
                System.out.println("Enrollment progress recalculated");
            } else {
                System.out.println("Not all documents viewed yet - lesson not completed");
                if (totalDocuments == 0) {
                    System.out.println("NOTE: Lesson has no documents - should be auto-completed by different logic");
                }
            }
        }
        System.out.println("=== DEBUG: checkLessonCompletionByDocuments completed ===");
    }

    private ProgressResponse toResponse(Progress progress) {
        return ProgressResponse.builder()
                .id(progress.getId())
                .lessonId(progress.getLesson() != null ? progress.getLesson().getId() : null)
                .isCompleted(progress.isCompleted())
                .completionDate(progress.getCompletionDate())
                .build();
    }

    /**
     * Khởi tạo progress records cho tất cả lessons trong course khi student enroll
     */
    @Transactional
    public void initializeProgressForEnrollment(Enrollment enrollment) {
        // Lấy tất cả lessons trong course
        List<Lesson> courseLessons = enrollment.getCourse().getCourseLessons()
                .stream()
                .map(cl -> cl.getLesson())
                .collect(Collectors.toList());
        
        for (Lesson lesson : courseLessons) {
            // Kiểm tra xem đã có progress record chưa
            Optional<Progress> existingProgress = progressRepository.findByEnrollmentIdAndLessonId(
                    enrollment.getId(), lesson.getId());
            
            if (existingProgress.isEmpty()) {
                // Tạo progress record mới
                Progress progress = Progress.builder()
                        .enrollment(enrollment)
                        .lesson(lesson)
                        .isCompleted(false)
                        .completionDate(null)
                        .completedQuiz(null)
                        .quizScore(null)
                        .build();
                
                progressRepository.save(progress);
            }
        }
    }

    /**
     * Tự động đánh dấu lesson hoàn thành nếu không có quiz và không có documents
     */
    @Transactional
    public ProgressResponse autoCompleteLessonIfEmpty(String username, String lessonId) {
        System.out.println("=== DEBUG: autoCompleteLessonIfEmpty called ===");
        System.out.println("Username: " + username);
        System.out.println("LessonId: " + lessonId);
        
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        System.out.println("Student found: " + student.getId() + " - " + student.getUsername());
        
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        System.out.println("Lesson found: " + lesson.getId() + " - " + lesson.getTitle());
        
        // Tìm enrollment của student cho lesson này
        List<Enrollment> enrollments = enrollmentRepository.findByStudent(student).stream()
                .filter(enrollment -> enrollment.getCourse().getCourseLessons().stream()
                        .anyMatch(cl -> cl.getLesson().getId().equals(lessonId)))
                .collect(Collectors.toList());
        
        if (enrollments.isEmpty()) {
            System.out.println("No enrollments found - throwing exception");
            throw new AppException(ErrorCode.ENROLLMENT_NOT_EXISTED);
        }
        
        Enrollment enrollment = enrollments.get(0);
        System.out.println("Enrollment found: " + enrollment.getId());
        
        // Kiểm tra progress hiện tại
        Progress progress = progressRepository.findByEnrollmentIdAndLessonId(enrollment.getId(), lessonId)
                .orElse(new Progress(null, enrollment, lesson, false, null, null, null));
        System.out.println("Current progress - ID: " + progress.getId() + ", isCompleted: " + progress.isCompleted());
        
        // Nếu đã completed rồi thì return luôn
        if (progress.isCompleted()) {
            System.out.println("Progress already completed - returning existing progress");
            return toResponse(progress);
        }
        
        // Kiểm tra xem lesson có quiz không
        boolean hasQuiz = lesson.getQuiz() != null;
        System.out.println("Lesson has quiz: " + hasQuiz);
        
        // Kiểm tra xem lesson có documents không
        long totalDocuments = lessonDocumentRepository.countByLessonId(lessonId);
        boolean hasDocuments = totalDocuments > 0;
        System.out.println("Total documents in lesson: " + totalDocuments);
        System.out.println("Lesson has documents: " + hasDocuments);
        
        // Nếu không có cả quiz lẫn documents thì tự động complete
        if (!hasQuiz && !hasDocuments) {
            System.out.println("Lesson has no quiz and no documents - auto-completing");
            progress.setCompleted(true);
            progress.setCompletionDate(LocalDate.now());
            
            if (progress.getId() == null) {
                // New progress record - set enrollment and lesson
                progress.setEnrollment(enrollment);
                progress.setLesson(lesson);
            }
            
            Progress savedProgress = progressRepository.save(progress);
            System.out.println("Progress saved with ID: " + savedProgress.getId() + ", isCompleted: " + savedProgress.isCompleted());
            
            // Recalculate overall enrollment progress
            recalculateAndSaveEnrollmentProgress(enrollment);
            System.out.println("Enrollment progress recalculated");
        } else {
            System.out.println("Lesson has quiz or documents - not auto-completing");
        }
        
        ProgressResponse response = toResponse(progress);
        System.out.println("=== DEBUG: autoCompleteLessonIfEmpty completed, returning isCompleted: " + response.isCompleted() + " ===");
        return response;
    }
} 