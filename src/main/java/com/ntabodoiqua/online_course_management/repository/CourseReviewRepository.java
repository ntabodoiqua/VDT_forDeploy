package com.ntabodoiqua.online_course_management.repository;

import com.ntabodoiqua.online_course_management.entity.CourseReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, String>, JpaSpecificationExecutor<CourseReview> {

    boolean existsByStudentIdAndCourseId(String studentId, String courseId);

    Page<CourseReview> findByCourseId(String courseId, Pageable pageable);

    Page<CourseReview> findByCourseIdAndIsApprovedTrue(String courseId, Pageable pageable);

    Page<CourseReview> findByCourseIdAndIsApprovedTrueAndIsRejectedFalse(String courseId, Pageable pageable);


    void deleteByCourseId(String courseId);

    Page<CourseReview> findByCourseIdAndIsApprovedFalse(String courseId, Pageable pageable);

    // Methods for instructor statistics
    @Query("SELECT cr FROM CourseReview cr WHERE cr.course.instructor.id = :instructorId ORDER BY cr.reviewDate DESC")
    Page<CourseReview> findByInstructorIdOrderByReviewDateDesc(@Param("instructorId") String instructorId, Pageable pageable);

    // Advanced rating statistics
    @Query("SELECT AVG(cr.rating) FROM CourseReview cr WHERE cr.course.instructor.id = :instructorId AND cr.isApproved = true")
    Double findAverageRatingByInstructorId(@Param("instructorId") String instructorId);

    // Monthly rating trends (last 6 months)
    @Query(value = "SELECT DATE_FORMAT(cr.review_date, '%Y-%m') as month, AVG(cr.rating) as avgRating " +
                   "FROM course_review cr " +
                   "JOIN course c ON cr.course_id = c.id " +
                   "WHERE c.instructor_id = :instructorId " +
                   "AND cr.is_approved = true " +
                   "AND cr.review_date >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH) " +
                   "GROUP BY DATE_FORMAT(cr.review_date, '%Y-%m') " +
                   "ORDER BY month DESC", nativeQuery = true)
    List<Object[]> findMonthlyRatingTrendsByInstructorId(@Param("instructorId") String instructorId);

    // Course-specific rating methods
    @Query("SELECT AVG(cr.rating) FROM CourseReview cr WHERE cr.course.id = :courseId AND cr.isApproved = true")
    Double findAverageRatingByCourseId(@Param("courseId") String courseId);

    Integer countByCourseIdAndIsApprovedTrue(String courseId);
    
    // Count approved reviews by instructor
    @Query("SELECT COUNT(cr) FROM CourseReview cr WHERE cr.course.instructor.id = :instructorId AND cr.isApproved = true")
    Integer countApprovedReviewsByInstructorId(@Param("instructorId") String instructorId);
}
