package com.ntabodoiqua.online_course_management.repository;

import com.ntabodoiqua.online_course_management.entity.QuizAttempt;
import com.ntabodoiqua.online_course_management.entity.QuizAttemptAnswer;
import com.ntabodoiqua.online_course_management.entity.QuizQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswer, String>, JpaSpecificationExecutor<QuizAttemptAnswer> {
    
    // Tìm answer theo attempt
    List<QuizAttemptAnswer> findByAttempt(QuizAttempt attempt);
    List<QuizAttemptAnswer> findByAttemptId(String attemptId);
    Page<QuizAttemptAnswer> findByAttemptId(String attemptId, Pageable pageable);
    
    // Tìm answer theo attempt và question
    Optional<QuizAttemptAnswer> findByAttemptIdAndQuestionId(String attemptId, String questionId);
    List<QuizAttemptAnswer> findByAttemptAndQuestion(QuizAttempt attempt, QuizQuestion question);
    
    // Tìm answer đúng/sai theo attempt
    List<QuizAttemptAnswer> findByAttemptIdAndIsCorrectTrue(String attemptId);
    List<QuizAttemptAnswer> findByAttemptIdAndIsCorrectFalse(String attemptId);
    
    // Đếm số câu trả lời
    long countByAttempt(QuizAttempt attempt);
    long countByAttemptId(String attemptId);
    long countByAttemptIdAndIsCorrectTrue(String attemptId);
    long countByAttemptIdAndIsCorrectFalse(String attemptId);
    
    // Tính tổng điểm earned
    @Query("SELECT SUM(qaa.pointsEarned) FROM QuizAttemptAnswer qaa WHERE qaa.attempt.id = :attemptId")
    Double getTotalPointsEarnedByAttemptId(@Param("attemptId") String attemptId);
    
    // Tìm answer theo question (để thống kê)
    List<QuizAttemptAnswer> findByQuestionId(String questionId);
    Page<QuizAttemptAnswer> findByQuestionId(String questionId, Pageable pageable);
    
    // Thống kê độ khó của câu hỏi (% trả lời đúng)
    @Query("SELECT (COUNT(CASE WHEN qaa.isCorrect = true THEN 1 END) * 100.0 / COUNT(*)) FROM QuizAttemptAnswer qaa WHERE qaa.question.id = :questionId")
    Double getCorrectPercentageByQuestionId(@Param("questionId") String questionId);
    
    // Tìm câu trả lời phổ biến nhất cho câu hỏi
    @Query("SELECT qaa.selectedAnswer.id, COUNT(*) FROM QuizAttemptAnswer qaa WHERE qaa.question.id = :questionId GROUP BY qaa.selectedAnswer.id ORDER BY COUNT(*) DESC")
    List<Object[]> getMostSelectedAnswersByQuestionId(@Param("questionId") String questionId);
    
    // Tìm answer chưa được trả lời (selectedAnswer = null)
    List<QuizAttemptAnswer> findByAttemptIdAndSelectedAnswerIsNull(String attemptId);
    long countByAttemptIdAndSelectedAnswerIsNull(String attemptId);
    
    /**
     * Xóa tất cả các câu trả lời trong các lần thử (attempt answers) liên quan đến một câu hỏi cụ thể.
     * Phương thức này được dùng khi một câu hỏi bị xóa.
     * @param questionId ID của câu hỏi.
     */
    void deleteByQuestionId(String questionId);
    
    // Thống kê theo quiz
    @Query("SELECT qaa FROM QuizAttemptAnswer qaa WHERE qaa.attempt.quiz.id = :quizId")
    List<QuizAttemptAnswer> findByQuizId(@Param("quizId") String quizId);
    
    @Query("SELECT COUNT(qaa) FROM QuizAttemptAnswer qaa WHERE qaa.attempt.quiz.id = :quizId AND qaa.isCorrect = true")
    long countCorrectAnswersByQuizId(@Param("quizId") String quizId);
    
    @Query("SELECT COUNT(qaa) FROM QuizAttemptAnswer qaa WHERE qaa.attempt.quiz.id = :quizId")
    long countTotalAnswersByQuizId(@Param("quizId") String quizId);
    
    // Kiểm tra attempt đã hoàn thành chưa (tất cả câu hỏi đã được trả lời)
    @Query("SELECT COUNT(DISTINCT qaa.question.id) FROM QuizAttemptAnswer qaa WHERE qaa.attempt.id = :attemptId AND qaa.selectedAnswer IS NOT NULL")
    long countAnsweredQuestionsByAttemptId(@Param("attemptId") String attemptId);
} 