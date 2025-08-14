package com.ntabodoiqua.online_course_management.specification;

import com.ntabodoiqua.online_course_management.entity.UploadedFile;
import com.ntabodoiqua.online_course_management.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class UploadedFileSpecification {

    public static Specification<UploadedFile> withFilter(User user, String contentType, String originalFileName) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by user
            predicates.add(criteriaBuilder.equal(root.get("uploadedBy"), user));

            // Filter by content type if provided
            if (StringUtils.hasText(contentType)) {
                predicates.add(criteriaBuilder.like(root.get("contentType"), contentType + "%"));
            }

            // Filter by original file name (case-insensitive) if provided
            if (StringUtils.hasText(originalFileName)) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("originalFileName")),
                        "%" + originalFileName.toLowerCase() + "%"
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Admin version - can see all files, not just their own
    public static Specification<UploadedFile> withFilterForAdmin(String contentType, String originalFileName) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by content type if provided
            if (StringUtils.hasText(contentType)) {
                predicates.add(criteriaBuilder.like(root.get("contentType"), contentType + "%"));
            }

            // Filter by original file name (case-insensitive) if provided
            if (StringUtils.hasText(originalFileName)) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("originalFileName")),
                        "%" + originalFileName.toLowerCase() + "%"
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<UploadedFile> getFilesByAdminCriteria(String contentType, String fileName, String uploaderName) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(contentType)) {
                predicates.add(criteriaBuilder.like(root.get("contentType"), contentType + "%"));
            }

            if (StringUtils.hasText(fileName)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("originalFileName")), "%" + fileName.toLowerCase() + "%"));
            }
            
            if (StringUtils.hasText(uploaderName)) {
                // Join with User entity and filter by a concatenation of firstName and lastName
                Predicate firstNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("uploadedBy").get("firstName")), "%" + uploaderName.toLowerCase() + "%");
                Predicate lastNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("uploadedBy").get("lastName")), "%" + uploaderName.toLowerCase() + "%");
                Predicate usernamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("uploadedBy").get("username")), "%" + uploaderName.toLowerCase() + "%");
                predicates.add(criteriaBuilder.or(firstNamePredicate, lastNamePredicate, usernamePredicate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
} 