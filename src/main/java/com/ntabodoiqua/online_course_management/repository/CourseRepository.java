package com.ntabodoiqua.online_course_management.repository;

import com.ntabodoiqua.online_course_management.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, String>, JpaSpecificationExecutor<Course> {
    List<Course> findByInstructor_Id(String instructorId);
    List<Course> findByIsActiveTrue();
    boolean existsByTitleIgnoreCase(String title);
    Page<Course> findAll(Specification<Course> spec, Pageable pageable);
    List<Course> findByInstructorId(String instructorId);
    List<Course> findByThumbnailUrl(String thumbnailUrl);
    
    // Methods for instructor statistics
    Long countByInstructorId(String instructorId);
    Long countByInstructorIdAndIsActiveTrue(String instructorId);
    Page<Course> findByInstructorIdAndIsActiveTrue(String instructorId, Pageable pageable);
}
