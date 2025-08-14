package com.ntabodoiqua.online_course_management.controller;

import com.ntabodoiqua.online_course_management.dto.request.ApiResponse;
import com.ntabodoiqua.online_course_management.dto.request.quiz.*;
import com.ntabodoiqua.online_course_management.dto.response.quiz.*;
import com.ntabodoiqua.online_course_management.service.QuizService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/quizzes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuizController {
    
    QuizService quizService;
    
    // =============== CRUD OPERATIONS ===============
    
    /**
     * Tạo quiz mới
     * Chỉ Instructor và Admin có thể tạo quiz
     */
    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ApiResponse<QuizResponse> createQuiz(@Valid @RequestBody QuizCreationRequest request) {
        log.info("Creating new quiz: {}", request.getTitle());
        
        QuizResponse quiz = quizService.createQuiz(request);
        
        return ApiResponse.<QuizResponse>builder()
                .message("Quiz created successfully")
                .result(quiz)
                .build();
    }
    
    /**
     * Lấy chi tiết quiz theo ID
     * Có phân quyền theo role
     */
    @GetMapping("/{quizId}")
    public ApiResponse<QuizResponse> getQuizById(@PathVariable String quizId) {
        log.info("Getting quiz by ID: {}", quizId);
        
        QuizResponse quiz = quizService.getQuizById(quizId);
        
        return ApiResponse.<QuizResponse>builder()
                .message("Quiz retrieved successfully")
                .result(quiz)
                .build();
    }
    
    /**
     * Lấy quiz dành cho student (không có đáp án đúng)
     * Chỉ Student có thể truy cập
     */
    @GetMapping("/{quizId}/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<QuizStudentResponse> getQuizForStudent(@PathVariable String quizId) {
        log.info("Getting quiz for student: {}", quizId);
        
        QuizStudentResponse quiz = quizService.getQuizForStudent(quizId);
        
        return ApiResponse.<QuizStudentResponse>builder()
                .message("Quiz retrieved successfully for student")
                .result(quiz)
                .build();
    }

    /**
     * Lấy quiz status cho student theo enrollment context
     * API mới với course context
     */
    @GetMapping("/{quizId}/student/status")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<QuizStudentResponse> getQuizStatusForStudent(
            @PathVariable String quizId,
            @RequestParam String courseId) {
        log.info("Getting quiz status for student - quiz: {}, course: {}", quizId, courseId);
        
        QuizStudentResponse quiz = quizService.getQuizStatusForStudent(quizId, courseId);
        
        return ApiResponse.<QuizStudentResponse>builder()
                .message("Quiz status retrieved successfully")
                .result(quiz)
                .build();
    }
    
    /**
     * Cập nhật quiz
     * Chỉ Instructor tạo quiz hoặc Admin có thể cập nhật
     */
    @PutMapping("/{quizId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ApiResponse<QuizResponse> updateQuiz(
            @PathVariable String quizId,
            @Valid @RequestBody QuizUpdateRequest request) {
        log.info("Updating quiz: {}", quizId);
        
        QuizResponse quiz = quizService.updateQuiz(quizId, request);
        
        return ApiResponse.<QuizResponse>builder()
                .message("Quiz updated successfully")
                .result(quiz)
                .build();
    }
    
    /**
     * Xóa quiz
     * Chỉ Instructor tạo quiz hoặc Admin có thể xóa
     */
    @DeleteMapping("/{quizId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ApiResponse<String> deleteQuiz(@PathVariable String quizId) {
        log.info("Deleting quiz: {}", quizId);
        
        quizService.deleteQuiz(quizId);
        
        return ApiResponse.<String>builder()
                .message("Quiz deleted successfully")
                .result("Quiz with ID " + quizId + " has been deleted")
                .build();
    }
    
    /**
     * Bật/tắt trạng thái quiz
     * Chỉ Instructor tạo quiz hoặc Admin có thể toggle
     */
    @PatchMapping("/{quizId}/toggle-status")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ApiResponse<QuizResponse> toggleQuizStatus(@PathVariable String quizId) {
        log.info("Toggling quiz status: {}", quizId);
        
        QuizResponse quiz = quizService.toggleQuizStatus(quizId);
        
        return ApiResponse.<QuizResponse>builder()
                .message("Quiz status updated successfully")
                .result(quiz)
                .build();
    }
    
    // =============== QUIZ QUESTIONS MANAGEMENT ===============
    
    /**
     * Thêm câu hỏi vào quiz
     * Chỉ Instructor tạo quiz hoặc Admin có thể thêm
     */
    @PostMapping("/{quizId}/questions")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ApiResponse<QuizQuestionResponse> addQuestionToQuiz(
            @PathVariable String quizId,
            @Valid @RequestBody QuizQuestionRequest request) {
        log.info("Adding question to quiz: {}", quizId);
        
        QuizQuestionResponse question = quizService.addQuestionToQuiz(quizId, request);
        
        return ApiResponse.<QuizQuestionResponse>builder()
                .message("Question added successfully")
                .result(question)
                .build();
    }
    
    /**
     * Cập nhật câu hỏi
     * Chỉ Instructor tạo quiz hoặc Admin có thể cập nhật
     */
    @PutMapping("/questions/{questionId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ApiResponse<QuizQuestionResponse> updateQuestion(
            @PathVariable String questionId,
            @Valid @RequestBody QuizQuestionRequest request) {
        log.info("Updating question: {}", questionId);
        
        QuizQuestionResponse question = quizService.updateQuestion(questionId, request);
        
        return ApiResponse.<QuizQuestionResponse>builder()
                .message("Question updated successfully")
                .result(question)
                .build();
    }
    
    /**
     * Xóa câu hỏi
     * Chỉ Instructor tạo quiz hoặc Admin có thể xóa
     */
    @DeleteMapping("/questions/{questionId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ApiResponse<String> deleteQuestion(@PathVariable String questionId) {
        log.info("Deleting question: {}", questionId);
        
        quizService.deleteQuestion(questionId);
        
        return ApiResponse.<String>builder()
                .message("Question deleted successfully")
                .result("Question with ID " + questionId + " has been deleted")
                .build();
    }
    
    /**
     * Sắp xếp lại thứ tự câu hỏi trong quiz
     * Chỉ Instructor tạo quiz hoặc Admin có thể reorder
     */
    @PutMapping("/{quizId}/questions/reorder")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ApiResponse<List<QuizQuestionResponse>> reorderQuestions(
            @PathVariable String quizId,
            @Valid @RequestBody List<QuestionOrderRequest> orderRequests) {
        log.info("Reordering questions for quiz: {}", quizId);
        
        List<QuizQuestionResponse> questions = quizService.reorderQuestions(quizId, orderRequests);
        
        return ApiResponse.<List<QuizQuestionResponse>>builder()
                .message("Questions reordered successfully")
                .result(questions)
                .build();
    }
    
    // =============== SEARCH & FILTER ===============
    
    /**
     * Lấy danh sách quiz với filter và pagination
     * Có phân quyền theo role
     */
    @GetMapping
    public ApiResponse<Page<QuizResponse>> getQuizzes(
            @ModelAttribute QuizFilterRequest filter,
            Pageable pageable) {
        log.info("Getting quizzes with filter: {}", filter);
        
        Page<QuizResponse> quizzes = quizService.getQuizzes(filter, pageable);
        
        return ApiResponse.<Page<QuizResponse>>builder()
                .message("Quizzes retrieved successfully")
                .result(quizzes)
                .build();
    }
    
    /**
     * Lấy quiz theo course
     * Có phân quyền theo role
     */
    @GetMapping("/course/{courseId}")
    public ApiResponse<List<QuizResponse>> getQuizzesByCourse(@PathVariable String courseId) {
        log.info("Getting quizzes for course: {}", courseId);
        
        List<QuizResponse> quizzes = quizService.getQuizzesByCourse(courseId);
        
        return ApiResponse.<List<QuizResponse>>builder()
                .message("Course quizzes retrieved successfully")
                .result(quizzes)
                .build();
    }
    
    /**
     * Lấy quiz available cho student
     * API public cho student
     */
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @GetMapping("/course/{courseId}/available")
    public ApiResponse<List<QuizResponse>> getAvailableQuizzes(@PathVariable String courseId) {
        log.info("Getting available quizzes for course: {}", courseId);
        
        List<QuizResponse> quizzes = quizService.getAvailableQuizzes(courseId);
        
        return ApiResponse.<List<QuizResponse>>builder()
                .message("Available quizzes retrieved successfully")
                .result(quizzes)
                .build();
    }
    
    /**
     * Lấy thống kê quiz
     * Chỉ Instructor tạo quiz hoặc Admin có thể xem
     */
    @GetMapping("/{quizId}/summary")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ApiResponse<QuizSummaryResponse> getQuizSummary(@PathVariable String quizId) {
        log.info("Getting quiz summary: {}", quizId);
        
        QuizSummaryResponse summary = quizService.getQuizSummary(quizId);
        
        return ApiResponse.<QuizSummaryResponse>builder()
                .message("Quiz summary retrieved successfully")
                .result(summary)
                .build();
    }
    
    // =============== QUIZ ATTEMPTS ===============
    // Quiz Attempt endpoints have been moved to QuizAttemptController for better organization
} 