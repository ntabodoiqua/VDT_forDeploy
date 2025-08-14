package com.ntabodoiqua.online_course_management.repository;

import com.ntabodoiqua.online_course_management.entity.Quiz;
import com.ntabodoiqua.online_course_management.enums.QuizType;
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
public interface QuizRepository extends JpaRepository<Quiz, String>, JpaSpecificationExecutor<Quiz> {
    
    // Tìm quiz theo lesson
    Optional<Quiz> findByLessonId(String lessonId);
    boolean existsByLessonId(String lessonId);
    
    // Tìm quiz theo instructor
    List<Quiz> findByCreatedById(String instructorId);
    Page<Quiz> findByCreatedById(String instructorId, Pageable pageable);
    
    // Tìm quiz active
    List<Quiz> findByIsActiveTrue();
    Page<Quiz> findByIsActiveTrue(Pageable pageable);
    
    // Tìm quiz theo type
    List<Quiz> findByType(QuizType type);
    Page<Quiz> findByTypeAndIsActiveTrue(QuizType type, Pageable pageable);
    
    // Tìm quiz theo course (thông qua lesson)
    @Query("SELECT q FROM Quiz q JOIN q.lesson l JOIN CourseLesson cl ON cl.lesson = l WHERE cl.course.id = :courseId")
    List<Quiz> findByCourseId(@Param("courseId") String courseId);
    
    @Query("SELECT q FROM Quiz q JOIN q.lesson l JOIN CourseLesson cl ON cl.lesson = l WHERE cl.course.id = :courseId AND q.isActive = true")
    List<Quiz> findByCourseIdAndIsActiveTrue(@Param("courseId") String courseId);
    
    // Tìm quiz theo title
    boolean existsByTitleIgnoreCase(String title);
    List<Quiz> findByTitleContainingIgnoreCase(String title);
    
    // Thống kê cho instructor
    @Query("SELECT COUNT(q) FROM Quiz q WHERE q.createdBy.id = :instructorId")
    long countByInstructorId(@Param("instructorId") String instructorId);
    
    @Query("SELECT COUNT(q) FROM Quiz q WHERE q.createdBy.id = :instructorId AND q.isActive = true")
    long countActiveQuizzesByInstructorId(@Param("instructorId") String instructorId);
    
    // Thống kê theo type
    @Query("SELECT q.type, COUNT(q) FROM Quiz q WHERE q.createdBy.id = :instructorId GROUP BY q.type")
    List<Object[]> countQuizTypesByInstructorId(@Param("instructorId") String instructorId);
    
    // Tìm quiz hết hạn
    @Query("SELECT q FROM Quiz q WHERE q.endTime IS NOT NULL AND q.endTime < :currentTime AND q.isActive = true")
    List<Quiz> findExpiredQuizzes(@Param("currentTime") LocalDateTime currentTime);
    
    // Tìm quiz sắp hết hạn (trong 24h tới)
    @Query("SELECT q FROM Quiz q WHERE q.endTime IS NOT NULL AND q.endTime BETWEEN :currentTime AND :next24Hours AND q.isActive = true")
    List<Quiz> findQuizzesExpiringSoon(@Param("currentTime") LocalDateTime currentTime, @Param("next24Hours") LocalDateTime next24Hours);
    
    // Note: Removed unsafe deleteByCourseId method. 
    // Use service layer to fetch entities and call repository.deleteAll() to ensure cascade deletion.
} 