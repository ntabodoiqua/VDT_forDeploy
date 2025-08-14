package com.ntabodoiqua.online_course_management.repository;

import com.ntabodoiqua.online_course_management.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, String> {
    List<Progress> findByEnrollmentId(String enrollmentId);
    Optional<Progress> findByEnrollmentIdAndLessonId(String enrollmentId, String lessonId);
    
    @Query("SELECT COUNT(p) FROM Progress p WHERE p.enrollment.id = :enrollmentId AND p.isCompleted = true")
    long countByEnrollmentIdAndIsCompletedTrue(@Param("enrollmentId") String enrollmentId);
}
