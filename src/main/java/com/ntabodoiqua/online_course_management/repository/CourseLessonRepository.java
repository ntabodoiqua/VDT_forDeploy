package com.ntabodoiqua.online_course_management.repository;

import com.ntabodoiqua.online_course_management.entity.Course;
import com.ntabodoiqua.online_course_management.entity.CourseLesson;
import com.ntabodoiqua.online_course_management.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseLessonRepository extends JpaRepository<CourseLesson, String>, JpaSpecificationExecutor<CourseLesson> {
    List<CourseLesson> findByCourseOrderByOrderIndexAsc(Course course);
    List<CourseLesson> findByCourseAndIsVisibleTrueOrderByOrderIndexAsc(Course course);
    boolean existsByLessonId(String lessonId);
    Optional<CourseLesson> findByLesson_Id(String lessonId);
    List<CourseLesson> findByLesson(Lesson lesson);
    long countByLesson(Lesson lesson);
    @Transactional
    void deleteByCourseId(String courseId);
    boolean existsByPrerequisiteId(String prerequisiteId);
    List<CourseLesson> findByCourseAndOrderIndexGreaterThanOrderByOrderIndexAsc(Course course, int orderIndex);
    long countByCourse(Course course);

    // Methods for instructor statistics
    @Query("SELECT COUNT(cl) FROM CourseLesson cl WHERE cl.course.instructor.id = :instructorId")
    long countByInstructorId(@Param("instructorId") String instructorId);
}
