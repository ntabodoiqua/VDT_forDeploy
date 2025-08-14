package com.ntabodoiqua.online_course_management.specification;

import com.ntabodoiqua.online_course_management.dto.request.user.InstructorFilterRequest;
import com.ntabodoiqua.online_course_management.entity.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Specification class for filtering User entities with INSTRUCTOR or ADMIN roles
 * Handles database-level filtering for fields that are stored in the database
 */
public class InstructorSpecification {

    /**
     * Create specification for instructor filtering
     * Only handles database fields - calculated fields are filtered in-memory in service layer
     */
    public static Specification<User> filterInstructors(InstructorFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Base condition: only users with INSTRUCTOR or ADMIN roles
            predicates.add(createRolesPredicate(root, criteriaBuilder));
            
            // Only enabled users
            predicates.add(criteriaBuilder.isTrue(root.get("enabled")));
            
            // Name filtering (firstName or lastName)
            if (StringUtils.hasText(filter.getName())) {
                predicates.add(createNameSearchPredicate(root, criteriaBuilder, filter.getName()));
            }
            
            // Email filtering
            if (StringUtils.hasText(filter.getEmail())) {
                predicates.add(createEmailSearchPredicate(root, criteriaBuilder, filter.getEmail()));
            }
            

            
            // Created date range filtering
            if (filter.getCreatedFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("createdAt"), filter.getCreatedFrom()));
            }
            
            if (filter.getCreatedTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("createdAt"), filter.getCreatedTo()));
            }
            
            // Enabled status filtering (if explicitly specified)
            if (filter.getEnabled() != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), filter.getEnabled()));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Create predicate for role filtering - users must be INSTRUCTOR or ADMIN
     */
    private static Predicate createRolesPredicate(Root<User> root, CriteriaBuilder cb) {
        Join<Object, Object> roleJoin = root.join("roles", JoinType.INNER);
        return cb.or(
            cb.equal(roleJoin.get("name"), "INSTRUCTOR"),
            cb.equal(roleJoin.get("name"), "ADMIN")
        );
    }
    
    /**
     * Create predicate for name search (firstName or lastName)
     */
    private static Predicate createNameSearchPredicate(Root<User> root, CriteriaBuilder cb, String name) {
        String searchTerm = "%" + name.trim().toLowerCase() + "%";
        return cb.or(
            cb.like(cb.lower(root.get("firstName")), searchTerm),
            cb.like(cb.lower(root.get("lastName")), searchTerm)
        );
    }
    
    /**
     * Create predicate for email search
     */
    private static Predicate createEmailSearchPredicate(Root<User> root, CriteriaBuilder cb, String email) {
        String searchTerm = "%" + email.trim().toLowerCase() + "%";
        return cb.like(cb.lower(root.get("email")), searchTerm);
    }
    

    

    
    /**
     * Create specification for top instructors
     * Gets all instructors and admins ordered by creation date (more experienced first)
     */
    public static Specification<User> getTopInstructors() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Base condition: only users with INSTRUCTOR or ADMIN roles
            predicates.add(createRolesPredicate(root, criteriaBuilder));
            
            // Only enabled users
            predicates.add(criteriaBuilder.isTrue(root.get("enabled")));
            
            // Order by creation date (older accounts first - more experienced)
            if (query != null) {
                query.orderBy(criteriaBuilder.asc(root.get("createdAt")));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
} 