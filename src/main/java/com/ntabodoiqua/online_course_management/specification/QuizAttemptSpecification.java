package com.ntabodoiqua.online_course_management.specification;

import com.ntabodoiqua.online_course_management.entity.QuizAttempt;
import com.ntabodoiqua.online_course_management.enums.AttemptStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class QuizAttemptSpecification {
    
    public static Specification<QuizAttempt> byQuizId(String quizId) {
        return (root, query, cb) -> {
            if (StringUtils.hasText(quizId)) {
                return cb.equal(root.get("quiz").get("id"), quizId);
            }
            return cb.conjunction();
        };
    }
    
    public static Specification<QuizAttempt> byStudentId(String studentId) {
        return (root, query, cb) -> {
            if (StringUtils.hasText(studentId)) {
                return cb.equal(root.get("student").get("id"), studentId);
            }
            return cb.conjunction();
        };
    }
    
    public static Specification<QuizAttempt> byStatus(AttemptStatus status) {
        return (root, query, cb) -> {
            if (status != null) {
                return cb.equal(root.get("status"), status);
            }
            return cb.conjunction();
        };
    }
    
    public static Specification<QuizAttempt> byInstructorId(String instructorId) {
        return (root, query, cb) -> {
            if (StringUtils.hasText(instructorId)) {
                return cb.equal(root.get("quiz").get("createdBy").get("id"), instructorId);
            }
            return cb.conjunction();
        };
    }
    
    public static Specification<QuizAttempt> byCourseId(String courseId) {
        return (root, query, cb) -> {
            if (StringUtils.hasText(courseId)) {
                // Use subquery to check if quiz's lesson exists in CourseLesson with specified courseId
                var subquery = query.subquery(String.class);
                var courseLessonRoot = subquery.from(com.ntabodoiqua.online_course_management.entity.CourseLesson.class);
                subquery.select(courseLessonRoot.get("lesson").get("id"))
                       .where(cb.equal(courseLessonRoot.get("course").get("id"), courseId));
                
                return cb.in(root.get("quiz").get("lesson").get("id")).value(subquery);
            }
            return cb.conjunction();
        };
    }
    
    public static Specification<QuizAttempt> startedBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startedAt"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startedAt"), endTime));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    public static Specification<QuizAttempt> completedBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("completedAt"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("completedAt"), endTime));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    public static Specification<QuizAttempt> isPassed() {
        return (root, query, cb) -> cb.equal(root.get("isPassed"), true);
    }
    
    public static Specification<QuizAttempt> isFailed() {
        return (root, query, cb) -> cb.equal(root.get("isPassed"), false);
    }
    
    public static Specification<QuizAttempt> scoreGreaterThan(Double minScore) {
        return (root, query, cb) -> {
            if (minScore != null) {
                return cb.greaterThanOrEqualTo(root.get("score"), minScore);
            }
            return cb.conjunction();
        };
    }
    
    public static Specification<QuizAttempt> scoreLessThan(Double maxScore) {
        return (root, query, cb) -> {
            if (maxScore != null) {
                return cb.lessThanOrEqualTo(root.get("score"), maxScore);
            }
            return cb.conjunction();
        };
    }
    
    public static Specification<QuizAttempt> scoreBetween(Double minScore, Double maxScore) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (minScore != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("score"), minScore));
            }
            if (maxScore != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("score"), maxScore));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    public static Specification<QuizAttempt> isAbandonedInProgress() {
        return (root, query, cb) -> {
            LocalDateTime now = LocalDateTime.now();
            return cb.and(
                cb.equal(root.get("status"), AttemptStatus.IN_PROGRESS),
                cb.lessThan(root.get("startedAt"), now.minusHours(24)) // Consider abandoned after 24 hours
            );
        };
    }
    
    // Keep the old method name for backward compatibility but mark as deprecated
    @Deprecated(since = "1.0", forRemoval = true)
    public static Specification<QuizAttempt> isExpired() {
        return isAbandonedInProgress();
    }
    
    public static Specification<QuizAttempt> isCompleted() {
        return (root, query, cb) -> cb.equal(root.get("status"), AttemptStatus.COMPLETED);
    }
    
    public static Specification<QuizAttempt> isInProgress() {
        return (root, query, cb) -> cb.equal(root.get("status"), AttemptStatus.IN_PROGRESS);
    }
    
    // Composite specification for filtering attempts with permissions
    public static Specification<QuizAttempt> withPermission(String userId, String role) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if ("ADMIN".equals(role)) {
                // Admin can see all attempts
                return cb.conjunction();
            } else if ("INSTRUCTOR".equals(role)) {
                // Instructor can see attempts on their quizzes
                predicates.add(cb.equal(root.get("quiz").get("createdBy").get("id"), userId));
            } else {
                // Student can only see their own attempts
                predicates.add(cb.equal(root.get("student").get("id"), userId));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
} 