package com.ntabodoiqua.online_course_management.repository;

import com.ntabodoiqua.online_course_management.entity.DocumentView;
import com.ntabodoiqua.online_course_management.entity.LessonDocument;
import com.ntabodoiqua.online_course_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentViewRepository extends JpaRepository<DocumentView, String> {
    
    // Kiểm tra user đã xem document chưa
    boolean existsByUserAndLessonDocument(User user, LessonDocument lessonDocument);
    
    // Lấy thời gian user xem document
    Optional<DocumentView> findByUserAndLessonDocument(User user, LessonDocument lessonDocument);
    
    // Đếm số document của lesson mà user đã xem
    @Query("SELECT COUNT(dv) FROM DocumentView dv WHERE dv.user = :user AND dv.lessonDocument.lesson.id = :lessonId")
    long countByUserAndLessonId(@Param("user") User user, @Param("lessonId") String lessonId);
    
    // Lấy tất cả documents của lesson mà user đã xem
    @Query("SELECT dv FROM DocumentView dv WHERE dv.user = :user AND dv.lessonDocument.lesson.id = :lessonId")
    List<DocumentView> findByUserAndLessonId(@Param("user") User user, @Param("lessonId") String lessonId);
    
    // Đếm tổng số documents của lesson
    @Query("SELECT COUNT(ld) FROM LessonDocument ld WHERE ld.lesson.id = :lessonId")
    long countDocumentsByLessonId(@Param("lessonId") String lessonId);
} 