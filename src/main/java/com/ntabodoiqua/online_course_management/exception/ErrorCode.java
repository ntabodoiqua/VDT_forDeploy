package com.ntabodoiqua.online_course_management.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),

    // Lỗi User
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    USERNAME_REQUIRED(1004, "Username must not be blank", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1005, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED(1006, "Password must not be blank", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1007, "User not existed", HttpStatus.NOT_FOUND),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(1009, "Email is invalid", HttpStatus.BAD_REQUEST),
    INVALID_PHONE(1010, "Phone number is invalid", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(1011, "Email existed", HttpStatus.BAD_REQUEST),
    PHONE_EXISTED(1012, "Phone number existed", HttpStatus.BAD_REQUEST),
    USER_ALREADY_ENABLED(1013, "User is already enabled", HttpStatus.BAD_REQUEST),
    USER_ALREADY_DISABLED(1014, "User is already disabled", HttpStatus.BAD_REQUEST),
    // Lỗi xác thực
    UNAUTHENTICATED(1015, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1016, "You do not have permission", HttpStatus.FORBIDDEN),
    OLD_PASSWORD_FALSE(1017, "Old password is false", HttpStatus.BAD_REQUEST),
    NEW_PASSWORD_SAME_AS_OLD(1018, "New password must be different from old password", HttpStatus.BAD_REQUEST),

    // Lỗi khóa học
    CATEGORY_NAME_REQUIRED(1019, "Category name must not be blank", HttpStatus.BAD_REQUEST),
    CATEGORY_EXISTED(1020, "Category existed", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_EXISTED(1021, "Category not existed", HttpStatus.NOT_FOUND),
    COURSE_EXISTED(1022, "Course existed", HttpStatus.BAD_REQUEST),
    COURSE_NOT_EXISTED(1023, "Course not existed", HttpStatus.NOT_FOUND),
    COURSE_NOT_ACTIVE(1079, "Course is not active", HttpStatus.FORBIDDEN),

    // Lỗi file
    FILE_CANNOT_STORED(1024, "File cannot be stored", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_NOT_FOUND(1025, "File not found", HttpStatus.NOT_FOUND),
    INVALID_IMAGE_TYPE(1026, "Invalid image type", HttpStatus.BAD_REQUEST),


    // Lỗi đăng ký khóa học
    COURSE_NOT_AVAILABLE(1027, "Course is not available", HttpStatus.BAD_REQUEST),
    ALREADY_ENROLLED(1028, "You are already enrolled in this course", HttpStatus.BAD_REQUEST),
    ENROLLMENT_NOT_EXISTED(1029, "Enrollment not existed", HttpStatus.NOT_FOUND),
    CANNOT_CANCEL_COMPLETED_ENROLLMENT(1030, "Cannot cancel completed enrollment", HttpStatus.BAD_REQUEST),
    ENROLLMENT_ALREADY_PROCESSED(1031, "Enrollment has already been processed", HttpStatus.BAD_REQUEST),

    // Lỗi bài học
    LESSON_NOT_FOUND(1032, "Lesson not found", HttpStatus.NOT_FOUND),
    LESSON_IS_USED_IN_COURSE(1033, "Lesson is used in course", HttpStatus.BAD_REQUEST),

    // Lỗi bài học trong khóa học
    LESSON_ALREADY_IN_COURSE(1034, "Lesson already in course", HttpStatus.BAD_REQUEST),
    PREQUISITE_NOT_FOUND(1035, "Prerequisite lesson not found", HttpStatus.NOT_FOUND),
    PREQUISITE_MUST_SAME_COURSE(1036, "Prerequisite lesson must be in the same course", HttpStatus.BAD_REQUEST),
    COURSE_LESSON_NOT_FOUND(1037, "Course lesson not found", HttpStatus.NOT_FOUND),
    COURSE_LESSON_COURSE_MISMATCH(1038, "Course lesson does not belong to the specified course", HttpStatus.BAD_REQUEST),
    PREQUISITE_CANNOT_SELF(1039, "Prerequisite cannot be the same as the lesson itself", HttpStatus.BAD_REQUEST),
    PREQUISITE_CIRCULAR(1040, "Circular prerequisite detected", HttpStatus.BAD_REQUEST),
    COURSE_LESSON_HAS_DEPENDENT(1041, "Course lesson has dependent lessons", HttpStatus.BAD_REQUEST),

    USER_DISABLED(1042, "User is disabled", HttpStatus.BAD_REQUEST),
    USER_DISABLED_DUE_TO_TOO_MANY_ATTEMPTS(1043, "User is disabled due to too many login attempts", HttpStatus.BAD_REQUEST),
    TOO_MANY_ATTEMPTS(1044, "Too many login attempts, please try again later", HttpStatus.BAD_REQUEST),
    INVALID_RECAPTCHA(1045, "Invalid reCAPTCHA", HttpStatus.BAD_REQUEST),

    // Lỗi đánh giá khóa học
    CANNOT_REVIEW_UNCOMPLETED_COURSE(1046, "Cannot review course before completion", HttpStatus.BAD_REQUEST),
    ALREADY_REVIEWED(1047, "You have already reviewed this course", HttpStatus.BAD_REQUEST),
    COURSE_LESSON_TOTAL_LESSONS_ZERO(1048, "Total lessons in course cannot be zero", HttpStatus.BAD_REQUEST),
    DATA_INTEGRITY_VIOLATION(1049, "Không thể xóa hoặc cập nhật do ràng buộc dữ liệu. Vui lòng kiểm tra các bản ghi liên quan.", HttpStatus.CONFLICT),
    LESSON_NOT_ASSIGNED_TO_COURSE(1050, "Lesson is not assigned to any course.", HttpStatus.NOT_FOUND),
    LESSON_TITLE_ALREADY_EXISTS(1051, "Lesson title already exists in the course.", HttpStatus.BAD_REQUEST),
    COURSE_REVIEW_NOT_EXISTED(1052, "Course review not existed", HttpStatus.NOT_FOUND),
    FILE_DELETION_FAILED(1053, "File deletion failed", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // Lỗi tài liệu
    DOCUMENT_NOT_FOUND(1054, "Document not found", HttpStatus.NOT_FOUND),
    INVALID_FILE(1055, "Invalid file", HttpStatus.BAD_REQUEST),
    ACCESS_DENIED(1056, "Access denied", HttpStatus.FORBIDDEN),
    
    // Lỗi Quiz
    QUIZ_NOT_FOUND(1057, "Quiz not found", HttpStatus.NOT_FOUND),
    QUIZ_ALREADY_EXISTS_FOR_LESSON(1058, "Quiz already exists for this lesson", HttpStatus.BAD_REQUEST),
    QUIZ_TITLE_ALREADY_EXISTS(1059, "Quiz title already exists", HttpStatus.BAD_REQUEST),
    QUIZ_NOT_AVAILABLE(1060, "Quiz is not available", HttpStatus.BAD_REQUEST),
    QUIZ_HAS_ATTEMPTS_CANNOT_DELETE(1061, "Quiz has attempts and cannot be deleted", HttpStatus.BAD_REQUEST),
    QUESTION_NOT_FOUND(1062, "Question not found", HttpStatus.NOT_FOUND),
    QUESTION_HAS_ATTEMPTS_CANNOT_DELETE(1063, "Question has attempts and cannot be deleted", HttpStatus.BAD_REQUEST),
    QUESTION_MUST_HAVE_ANSWERS(1064, "Question must have answers", HttpStatus.BAD_REQUEST),
    QUESTION_MUST_HAVE_AT_LEAST_TWO_ANSWERS(1065, "Question must have at least two answers", HttpStatus.BAD_REQUEST),
    QUESTION_MUST_HAVE_EXACTLY_ONE_CORRECT_ANSWER(1066, "Question must have exactly one correct answer", HttpStatus.BAD_REQUEST),
    
    // Lỗi Quiz Attempt
    QUIZ_MAX_ATTEMPTS_EXCEEDED(1067, "Maximum attempts exceeded", HttpStatus.BAD_REQUEST),
    QUIZ_ATTEMPT_NOT_FOUND(1068, "Quiz attempt not found", HttpStatus.NOT_FOUND),
    QUIZ_ATTEMPT_NOT_IN_PROGRESS(1069, "Quiz attempt is not in progress", HttpStatus.BAD_REQUEST),
    QUIZ_ATTEMPT_EXPIRED(1070, "Quiz attempt has expired", HttpStatus.BAD_REQUEST),
    ATTEMPT_ANSWER_NOT_FOUND(1071, "Attempt answer not found", HttpStatus.NOT_FOUND),
    ANSWER_NOT_FOUND(1072, "Answer not found", HttpStatus.NOT_FOUND),
    INVALID_ANSWER_FOR_QUESTION(1073, "Invalid answer for this question", HttpStatus.BAD_REQUEST),
    NO_ACTIVE_ATTEMPT_FOUND(1074, "No active attempt found", HttpStatus.NOT_FOUND),
    NO_ATTEMPTS_FOUND(1075, "No attempts found", HttpStatus.NOT_FOUND),
    QUIZ_NOT_STARTED(1076, "Quiz has not started yet", HttpStatus.BAD_REQUEST),
    QUIZ_EXPIRED(1077, "Quiz has expired", HttpStatus.BAD_REQUEST),
    QUESTION_ORDER_INDEX_ALREADY_EXISTS(1078, "Question order index already exists in this quiz", HttpStatus.BAD_REQUEST),


    // Lỗi khác
    ADMIN_CANNOT_DELETE_SELF(1080, "Admin cannot delete themselves", HttpStatus.BAD_REQUEST),
    ;





    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
