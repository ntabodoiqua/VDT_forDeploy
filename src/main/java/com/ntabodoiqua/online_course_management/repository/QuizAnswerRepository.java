package com.ntabodoiqua.online_course_management.repository;

import com.ntabodoiqua.online_course_management.entity.QuizAnswer;
import com.ntabodoiqua.online_course_management.entity.QuizQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, String>, JpaSpecificationExecutor<QuizAnswer> {
    
    // Tìm câu trả lời theo question
    List<QuizAnswer> findByQuestionOrderByOrderIndexAsc(QuizQuestion question);
    List<QuizAnswer> findByQuestionIdOrderByOrderIndexAsc(String questionId);
    Page<QuizAnswer> findByQuestionId(String questionId, Pageable pageable);
    
    // Tìm câu trả lời đúng
    // Note: Business rule clarification needed - does system support single or multiple correct answers?
    // Currently keeping both methods but recommend clarifying the requirement:
    
    // For SINGLE correct answer questions (most common)
    Optional<QuizAnswer> findFirstByQuestionIdAndIsCorrectTrue(String questionId);
    
    // For MULTIPLE correct answers questions (if supported)
    List<QuizAnswer> findByQuestionIdAndIsCorrectTrue(String questionId);
    
    // Đếm số câu trả lời
    long countByQuestion(QuizQuestion question);
    long countByQuestionId(String questionId);
    
    // Tìm câu trả lời theo order index
    Optional<QuizAnswer> findByQuestionIdAndOrderIndex(String questionId, Integer orderIndex);
    
    // Tìm câu trả lời theo text (tìm kiếm)
    @Query(value = "SELECT * FROM quiz_answer qa WHERE qa.question_id = :questionId AND LOWER(qa.answer_text) LIKE LOWER(CONCAT('%', :keyword, '%'))", nativeQuery = true)
    List<QuizAnswer> findByQuestionIdAndAnswerTextContainingIgnoreCase(@Param("questionId") String questionId, @Param("keyword") String keyword);
    
    // Xóa tất cả câu trả lời của question
    @Modifying
    @Transactional
    void deleteByQuestionId(String questionId);
    
    // Tìm order index lớn nhất trong question
    @Query("SELECT COALESCE(MAX(qa.orderIndex), 0) FROM QuizAnswer qa WHERE qa.question.id = :questionId")
    Integer getMaxOrderIndexByQuestionId(@Param("questionId") String questionId);
    
    // Kiểm tra có câu trả lời đúng không
    boolean existsByQuestionIdAndIsCorrectTrue(String questionId);
    
    // Đếm số câu trả lời đúng
    long countByQuestionIdAndIsCorrectTrue(String questionId);
    
    // Tìm tất cả câu trả lời của một quiz (qua question)
    @Query("SELECT qa FROM QuizAnswer qa WHERE qa.question.quiz.id = :quizId ORDER BY qa.question.orderIndex, qa.orderIndex")
    List<QuizAnswer> findByQuizId(@Param("quizId") String quizId);
    
    // Xóa tất cả câu trả lời của quiz
    @Modifying
    @Transactional
    @Query("DELETE FROM QuizAnswer qa WHERE qa.question.quiz.id = :quizId")
    void deleteByQuizId(@Param("quizId") String quizId);
    
    // Kiểm tra xem orderIndex của answer đã tồn tại trong question chưa
    boolean existsByQuestionIdAndOrderIndex(String questionId, Integer orderIndex);
} 