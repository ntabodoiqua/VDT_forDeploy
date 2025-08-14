package com.ntabodoiqua.online_course_management.repository;

import com.ntabodoiqua.online_course_management.entity.Lesson;
import com.ntabodoiqua.online_course_management.entity.LessonDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonDocumentRepository extends JpaRepository<LessonDocument, String> {
    List<LessonDocument> findByLesson(Lesson lesson);
    List<LessonDocument> findByLessonId(String lessonId);
    Optional<LessonDocument> findByIdAndLessonId(String id, String lessonId);
    void deleteByLessonId(String lessonId);
    
    // New method to find documents by fileName
    List<LessonDocument> findByFileName(String fileName);
    
    // Count method to check if lesson has documents  
    long countByLessonId(String lessonId);
} 