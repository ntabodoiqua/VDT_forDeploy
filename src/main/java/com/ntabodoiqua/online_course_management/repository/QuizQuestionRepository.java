package com.ntabodoiqua.online_course_management.repository;

import com.ntabodoiqua.online_course_management.entity.Quiz;
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
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, String>, JpaSpecificationExecutor<QuizQuestion> {
    
    // Tìm câu hỏi theo quiz
    List<QuizQuestion> findByQuizOrderByOrderIndexAsc(Quiz quiz);
    List<QuizQuestion> findByQuizIdOrderByOrderIndexAsc(String quizId);
    Page<QuizQuestion> findByQuizId(String quizId, Pageable pageable);
    
    // Đếm số câu hỏi
    long countByQuiz(Quiz quiz);
    long countByQuizId(String quizId);
    
    // Tìm câu hỏi theo order index
    Optional<QuizQuestion> findByQuizIdAndOrderIndex(String quizId, Integer orderIndex);
    
    // Tìm câu hỏi theo text (tìm kiếm)
    @Query(value = "SELECT * FROM quiz_question q WHERE q.quiz_id = :quizId AND LOWER(q.question_text) LIKE LOWER(CONCAT('%', :keyword, '%'))", nativeQuery = true)
    List<QuizQuestion> findByQuizIdAndQuestionTextContainingIgnoreCase(@Param("quizId") String quizId, @Param("keyword") String keyword);
    
    // Tổng số điểm của quiz
    @Query("SELECT SUM(q.points) FROM QuizQuestion q WHERE q.quiz.id = :quizId")
    Double getTotalPointsByQuizId(@Param("quizId") String quizId);
    
    // Tìm câu hỏi có điểm cao nhất
    @Query("SELECT q FROM QuizQuestion q WHERE q.quiz.id = :quizId ORDER BY q.points DESC")
    List<QuizQuestion> findByQuizIdOrderByPointsDesc(@Param("quizId") String quizId);
    
    // Xóa tất cả câu hỏi của quiz
    void deleteByQuizId(String quizId);
    
    // Tìm order index lớn nhất trong quiz
    @Query("SELECT COALESCE(MAX(q.orderIndex), 0) FROM QuizQuestion q WHERE q.quiz.id = :quizId")
    Integer getMaxOrderIndexByQuizId(@Param("quizId") String quizId);
    
    // Tìm câu hỏi sau một order index nhất định
    List<QuizQuestion> findByQuizIdAndOrderIndexGreaterThanOrderByOrderIndexAsc(String quizId, Integer orderIndex);
    
    // Thống kê điểm trung bình các câu hỏi
    @Query("SELECT AVG(q.points) FROM QuizQuestion q WHERE q.quiz.id = :quizId")
    Double getAveragePointsByQuizId(@Param("quizId") String quizId);
    
    // Kiểm tra xem có câu hỏi nào trong quiz không
    boolean existsByQuizId(String quizId);
    
    // Kiểm tra xem orderIndex đã tồn tại trong quiz chưa
    boolean existsByQuizIdAndOrderIndex(String quizId, Integer orderIndex);
} 