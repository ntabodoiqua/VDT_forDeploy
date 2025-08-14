package com.ntabodoiqua.online_course_management.repository;

import com.ntabodoiqua.online_course_management.entity.Quiz;
import com.ntabodoiqua.online_course_management.entity.QuizAttempt;
import com.ntabodoiqua.online_course_management.entity.User;
import com.ntabodoiqua.online_course_management.entity.Enrollment;
import com.ntabodoiqua.online_course_management.enums.AttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, String>, JpaSpecificationExecutor<QuizAttempt> {
    
    List<QuizAttempt> findByQuizAndStudentIn(Quiz quiz, List<User> students);
    
    List<QuizAttempt> findByQuizAndEnrollmentIn(Quiz quiz, List<Enrollment> enrollments);
    
    // Tìm attempt theo quiz và student
    List<QuizAttempt> findByQuizIdAndStudentIdOrderByAttemptNumberDesc(String quizId, String studentId);
    Page<QuizAttempt> findByQuizIdAndStudentId(String quizId, String studentId, Pageable pageable);
    
    // Tìm attempt theo quiz, student và enrollment (NEW - context-aware)
    List<QuizAttempt> findByQuizIdAndStudentIdAndEnrollmentIdOrderByAttemptNumberDesc(String quizId, String studentId, String enrollmentId);
    Page<QuizAttempt> findByQuizIdAndStudentIdAndEnrollmentId(String quizId, String studentId, String enrollmentId, Pageable pageable);
    
    // Tìm attempt theo quiz
    List<QuizAttempt> findByQuizOrderByStartedAtDesc(Quiz quiz);
    List<QuizAttempt> findByQuizIdOrderByStartedAtDesc(String quizId);
    Page<QuizAttempt> findByQuizId(String quizId, Pageable pageable);
    
    // Tìm attempt theo student
    List<QuizAttempt> findByStudentOrderByStartedAtDesc(User student);
    List<QuizAttempt> findByStudentIdOrderByStartedAtDesc(String studentId);
    Page<QuizAttempt> findByStudentId(String studentId, Pageable pageable);
    
    // Tìm attempt theo status
    List<QuizAttempt> findByStatus(AttemptStatus status);
    List<QuizAttempt> findByQuizIdAndStatus(String quizId, AttemptStatus status);
    List<QuizAttempt> findByStudentIdAndStatus(String studentId, AttemptStatus status);
    
    // Tìm attempt latest của student cho quiz
    Optional<QuizAttempt> findFirstByQuizIdAndStudentIdOrderByAttemptNumberDesc(String quizId, String studentId);
    Optional<QuizAttempt> findFirstByQuizIdAndStudentIdAndEnrollmentIdOrderByAttemptNumberDesc(String quizId, String studentId, String enrollmentId);
    
    // Tìm attempt có điểm cao nhất của student cho quiz trong một enrollment cụ thể
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.quiz.id = :quizId AND qa.student.id = :studentId AND qa.enrollment.id = :enrollmentId AND qa.status = 'COMPLETED' ORDER BY qa.score DESC")
    List<QuizAttempt> findBestScoreByQuizAndStudentInEnrollment(@Param("quizId") String quizId, @Param("studentId") String studentId, @Param("enrollmentId") String enrollmentId);
    
    // Đếm số lần attempt
    long countByQuizIdAndStudentId(String quizId, String studentId);
    long countByQuizIdAndStudentIdAndStatus(String quizId, String studentId, AttemptStatus status);
    long countByQuizIdAndStudentIdAndEnrollmentId(String quizId, String studentId, String enrollmentId);
    long countByQuizIdAndStudentIdAndEnrollmentIdAndStatus(String quizId, String studentId, String enrollmentId, AttemptStatus status);
    
    // Tìm attempt đang in progress
    List<QuizAttempt> findByStatusAndStartedAtBefore(AttemptStatus status, LocalDateTime cutoffTime);
    Optional<QuizAttempt> findByQuizIdAndStudentIdAndStatus(String quizId, String studentId, AttemptStatus status);
    Optional<QuizAttempt> findByQuizIdAndStudentIdAndEnrollmentIdAndStatus(String quizId, String studentId, String enrollmentId, AttemptStatus status);
    
    // Thống kê
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.quiz.id = :quizId")
    long countByQuizId(@Param("quizId") String quizId);
    
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.quiz.id = :quizId AND qa.status = 'COMPLETED'")
    long countCompletedByQuizId(@Param("quizId") String quizId);
    
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.quiz.id = :quizId AND qa.isPassed = true")
    long countPassedByQuizId(@Param("quizId") String quizId);
    
    @Query("SELECT AVG(qa.score) FROM QuizAttempt qa WHERE qa.quiz.id = :quizId AND qa.status = 'COMPLETED'")
    Double getAverageScoreByQuizId(@Param("quizId") String quizId);
    
    @Query("SELECT MAX(qa.score) FROM QuizAttempt qa WHERE qa.quiz.id = :quizId AND qa.status = 'COMPLETED'")
    Double getMaxScoreByQuizId(@Param("quizId") String quizId);
    
    @Query("SELECT MIN(qa.score) FROM QuizAttempt qa WHERE qa.quiz.id = :quizId AND qa.status = 'COMPLETED'")
    Double getMinScoreByQuizId(@Param("quizId") String quizId);
    
    // Thống kê cho instructor
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.quiz.createdBy.id = :instructorId")
    long countByInstructorId(@Param("instructorId") String instructorId);
    
    @Query("SELECT COUNT(DISTINCT qa.student.id) FROM QuizAttempt qa WHERE qa.quiz.createdBy.id = :instructorId")
    long countDistinctStudentsByInstructorId(@Param("instructorId") String instructorId);
    
    // Tìm attempt theo course (thông qua quiz)
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.quiz.lesson.id IN (SELECT cl.lesson.id FROM CourseLesson cl WHERE cl.course.id = :courseId)")
    List<QuizAttempt> findByCourseId(@Param("courseId") String courseId);
    
    // Xóa attempt theo quizId
    void deleteByQuizId(String quizId);
    
    // Tìm attempt hết hạn (đang in progress quá lâu)
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.status = 'IN_PROGRESS' AND qa.startedAt < :cutoffTime")
    List<QuizAttempt> findExpiredInProgressAttempts(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Course-level quiz statistics methods
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.quiz.id IN :quizIds AND qa.status = 'COMPLETED'")
    List<QuizAttempt> findCompletedAttemptsByQuizIds(@Param("quizIds") List<String> quizIds);
    
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.student.id = :studentId AND qa.enrollment.id = :enrollmentId AND qa.quiz.id IN :quizIds AND qa.status = :status")
    List<QuizAttempt> findByStudentIdAndEnrollmentIdAndQuizIdInAndStatus(@Param("studentId") String studentId, @Param("enrollmentId") String enrollmentId, @Param("quizIds") List<String> quizIds, @Param("status") AttemptStatus status);
    
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.student.id = :studentId AND qa.enrollment.id = :enrollmentId AND qa.quiz.id IN :quizIds AND qa.status = :status ORDER BY qa.completedAt DESC")
    List<QuizAttempt> findByStudentIdAndEnrollmentIdAndQuizIdInAndStatusOrderByCompletedAtDesc(@Param("studentId") String studentId, @Param("enrollmentId") String enrollmentId, @Param("quizIds") List<String> quizIds, @Param("status") AttemptStatus status);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.enrollment.id IN :enrollmentIds AND qa.quiz.id IN :quizIds AND qa.status = :status")
    List<QuizAttempt> findByEnrollmentIdInAndQuizIdInAndStatus(@Param("enrollmentIds") List<String> enrollmentIds, @Param("quizIds") List<String> quizIds, @Param("status") AttemptStatus status);
} 