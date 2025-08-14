package com.ntabodoiqua.online_course_management.specification;

import com.ntabodoiqua.online_course_management.dto.request.quiz.QuizFilterRequest;
import com.ntabodoiqua.online_course_management.entity.Quiz;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class QuizSpecification {
    
    public static Specification<Quiz> withFilter(QuizFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter != null) {
                // Filter by title
                if (StringUtils.hasText(filter.getTitle())) {
                    predicates.add(cb.like(cb.lower(root.get("title")),
                            "%" + filter.getTitle().toLowerCase().trim() + "%"));
                }

                // Filter by type
                if (filter.getType() != null) {
                    predicates.add(cb.equal(root.get("type"), filter.getType()));
                }

                // Filter by lessonId
                if (StringUtils.hasText(filter.getLessonId())) {
                    predicates.add(cb.equal(root.get("lesson").get("id"), filter.getLessonId()));
                }

                // Filter by courseId (through lesson -> courseLesson using subquery)
                if (StringUtils.hasText(filter.getCourseId())) {
                    // Use subquery to check if lesson exists in CourseLesson with specified courseId
                    var subquery = query.subquery(String.class);
                    var courseLessonRoot = subquery.from(com.ntabodoiqua.online_course_management.entity.CourseLesson.class);
                    subquery.select(courseLessonRoot.get("lesson").get("id"))
                           .where(cb.equal(courseLessonRoot.get("course").get("id"), filter.getCourseId()));
                    
                    predicates.add(cb.in(root.get("lesson").get("id")).value(subquery));
                }

                // Filter by instructorId (createdBy)
                if (StringUtils.hasText(filter.getInstructorId())) {
                    predicates.add(cb.equal(root.get("createdBy").get("id"), filter.getInstructorId()));
                }

                // Filter by isActive
                if (filter.getIsActive() != null) {
                    predicates.add(cb.equal(root.get("isActive"), filter.getIsActive()));
                }

                // Filter by startTime range
                if (filter.getStartTimeFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), filter.getStartTimeFrom()));
                }
                if (filter.getStartTimeTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("startTime"), filter.getStartTimeTo()));
                }

                // Filter by endTime range
                if (filter.getEndTimeFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("endTime"), filter.getEndTimeFrom()));
                }
                if (filter.getEndTimeTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("endTime"), filter.getEndTimeTo()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    public static Specification<Quiz> withFilterAndPermission(QuizFilterRequest filter,
                                                              boolean canViewInactive,
                                                              String instructorUsername) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Apply basic filters
            Specification<Quiz> baseSpec = withFilter(filter);
            Predicate basePredicate = baseSpec.toPredicate(root, query, cb);
            if (basePredicate != null) {
                predicates.add(basePredicate);
            }

            // Apply permission filters
            if (!canViewInactive) {
                if (instructorUsername != null) {
                    // Instructor: view active quizzes + own quizzes (including inactive)
                    Predicate activeQuizzes = cb.equal(root.get("isActive"), true);
                    Predicate ownQuizzes = cb.equal(root.get("createdBy").get("username"), instructorUsername);
                    predicates.add(cb.or(activeQuizzes, ownQuizzes));
                } else {
                    // Student/Guest: only view active quizzes
                    predicates.add(cb.equal(root.get("isActive"), true));
                }
            }
            // Admin: no additional restrictions (can view all)

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    public static Specification<Quiz> isActive() {
        return (root, query, cb) -> cb.equal(root.get("isActive"), true);
    }
    
    public static Specification<Quiz> byInstructorId(String instructorId) {
        return (root, query, cb) -> {
            if (StringUtils.hasText(instructorId)) {
                return cb.equal(root.get("createdBy").get("id"), instructorId);
            }
            return cb.conjunction();
        };
    }
    
    public static Specification<Quiz> byCourseId(String courseId) {
        return (root, query, cb) -> {
            if (StringUtils.hasText(courseId)) {
                // Use subquery to check if lesson exists in CourseLesson with specified courseId
                var subquery = query.subquery(String.class);
                var courseLessonRoot = subquery.from(com.ntabodoiqua.online_course_management.entity.CourseLesson.class);
                subquery.select(courseLessonRoot.get("lesson").get("id"))
                       .where(cb.equal(courseLessonRoot.get("course").get("id"), courseId));
                
                return cb.in(root.get("lesson").get("id")).value(subquery);
            }
            return cb.conjunction();
        };
    }
    
    public static Specification<Quiz> byType(com.ntabodoiqua.online_course_management.enums.QuizType type) {
        return (root, query, cb) -> {
            if (type != null) {
                return cb.equal(root.get("type"), type);
            }
            return cb.conjunction();
        };
    }
    
    public static Specification<Quiz> isExpired() {
        return (root, query, cb) -> {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            return cb.and(
                cb.isNotNull(root.get("endTime")),
                cb.lessThan(root.get("endTime"), now)
            );
        };
    }
    
    public static Specification<Quiz> isAvailable() {
        return (root, query, cb) -> {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            List<Predicate> predicates = new ArrayList<>();
            
            // Quiz must be active
            predicates.add(cb.equal(root.get("isActive"), true));
            
            // Start time check (if set)
            Predicate startTimeCheck = cb.or(
                cb.isNull(root.get("startTime")),
                cb.lessThanOrEqualTo(root.get("startTime"), now)
            );
            predicates.add(startTimeCheck);
            
            // End time check (if set)
            Predicate endTimeCheck = cb.or(
                cb.isNull(root.get("endTime")),
                cb.greaterThan(root.get("endTime"), now)
            );
            predicates.add(endTimeCheck);
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
} 