# Quiz Service Documentation

## Tổng quan

Hệ thống Quiz Service được thiết kế để quản lý việc tạo, thực hiện và đánh giá các bài quiz trong hệ thống quản lý khóa học online. Hệ thống hỗ trợ các tính năng sau:

- **Quản lý Quiz**: CRUD operations cho quiz
- **Quản lý Câu hỏi**: Thêm, sửa, xóa câu hỏi
- **Làm bài Quiz**: Hỗ trợ sinh viên làm bài
- **Chấm điểm tự động**: Tính điểm và đánh giá kết quả
- **Phân quyền**: Kiểm soát truy cập theo role
- **Thống kê**: Báo cáo và phân tích kết quả

## Cấu trúc Quiz

### 1. Quiz Entity
- **Loại Quiz**: `PRACTICE` (thực hành) hoặc `ASSESSMENT` (đánh giá)
- **Quan hệ**: Mỗi Quiz thuộc về đúng 1 Lesson (1:1)
- **Cấu hình thời gian**: Thời gian bắt đầu, kết thúc, giới hạn thời gian
- **Cấu hình attempts**: Số lần làm tối đa, phương pháp tính điểm
- **Điểm pass**: Mặc định 70%, có thể tùy chỉnh

### 2. Quiz Question
- **Loại câu hỏi**: Multiple choice (1 đáp án đúng)
- **Điểm số**: Mỗi câu hỏi có điểm riêng
- **Thứ tự**: Order index để sắp xếp câu hỏi

### 3. Quiz Answer
- **Đáp án**: Tối thiểu 2 đáp án, chính xác 1 đáp án đúng
- **Thứ tự**: Order index để sắp xếp đáp án

## Quyền truy cập

### Admin
- Toàn quyền CRUD với tất cả quiz
- Xem tất cả thống kê
- Không bị giới hạn bởi trạng thái active/inactive

### Instructor
- CRUD quiz của mình
- Xem quiz active của instructor khác
- Xem thống kê quiz của mình
- Quản lý câu hỏi trong quiz của mình

### Student
- Chỉ xem quiz active và có quyền truy cập
- Làm bài quiz theo quy định
- Xem kết quả và lịch sử của mình
- Không thể xem câu trả lời đúng (tùy cấu hình quiz)

## API Endpoints

### QuizService

#### 1. CRUD Operations

```java
// Tạo quiz mới
@PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
public QuizResponse createQuiz(QuizCreationRequest request)

// Lấy chi tiết quiz
public QuizResponse getQuizById(String quizId)

// Lấy quiz cho student
public QuizStudentResponse getQuizForStudent(String quizId)

// Cập nhật quiz
@PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
public QuizResponse updateQuiz(String quizId, QuizUpdateRequest request)

// Xóa quiz
@PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
public void deleteQuiz(String quizId)

// Bật/tắt quiz
@PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
public QuizResponse toggleQuizStatus(String quizId)
```

#### 2. Quiz Questions Management

```java
// Thêm câu hỏi
@PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
public QuizQuestionResponse addQuestionToQuiz(String quizId, QuizQuestionRequest request)

// Cập nhật câu hỏi
@PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
public QuizQuestionResponse updateQuestion(String questionId, QuizQuestionRequest request)

// Xóa câu hỏi
@PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
public void deleteQuestion(String questionId)
```

#### 3. Search & Filter

```java
// Lấy danh sách quiz với filter
public Page<QuizResponse> getQuizzes(QuizFilterRequest filter, Pageable pageable)

// Lấy quiz theo course
public List<QuizResponse> getQuizzesByCourse(String courseId)

// Lấy quiz available
public List<QuizResponse> getAvailableQuizzes(String courseId)

// Thống kê quiz
@PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
public QuizSummaryResponse getQuizSummary(String quizId)
```

### QuizAttemptService

#### 1. Quiz Taking

```java
// Bắt đầu làm quiz
@PreAuthorize("hasRole('STUDENT')")
public QuizAttemptResponse startQuizAttempt(String quizId)

// Nộp bài quiz
@PreAuthorize("hasRole('STUDENT')")
public QuizResultResponse submitQuiz(String attemptId)
```

## Business Rules

### 1. Quiz Creation Rules
- Mỗi lesson chỉ có tối đa 1 quiz
- Title quiz phải unique
- Chỉ instructor tạo lesson hoặc admin mới tạo được quiz cho lesson đó
- Quiz có thể có hoặc không có thời gian giới hạn
- Số lần attempt có thể unlimited (-1) hoặc giới hạn

### 2. Question Rules
- Mỗi câu hỏi phải có ít nhất 2 đáp án
- Chính xác 1 đáp án đúng (multiple choice)
- Không thể xóa câu hỏi đã có student trả lời
- Order index tự động assign nếu không cung cấp

### 3. Access Rules
- Student chỉ truy cập quiz của course đã enroll
- Quiz phải active và trong thời gian cho phép
- Kiểm tra số lần attempt còn lại
- Không thể xem đáp án đúng trong khi làm bài

### 4. Scoring Rules
- Điểm pass mặc định: 70%
- Phương pháp tính điểm: HIGHEST (cao nhất), LATEST (mới nhất), AVERAGE (trung bình)
- Điểm = (Tổng điểm đạt được / Tổng điểm tối đa) * 100
- Pass/Fail dựa trên % so với passing score

## Validation Rules

### Quiz Validation
```java
// Title không được trống và unique
// LessonId phải tồn tại và có quyền
// PassingScore: 0-100
// MaxAttempts: -1 (unlimited) hoặc > 0
// StartTime < EndTime nếu có
```

### Question Validation
```java
// QuestionText không được trống
// Points > 0
// Phải có ít nhất 2 answers
// Chính xác 1 answer đúng
```

### Answer Validation
```java
// AnswerText không được trống
// Phải có ít nhất 1 answer isCorrect = true
// Chỉ 1 answer isCorrect = true
```

## Error Handling

### Common Error Codes
```java
QUIZ_NOT_FOUND(1057)
QUIZ_ALREADY_EXISTS_FOR_LESSON(1058)
QUIZ_TITLE_ALREADY_EXISTS(1059)
QUIZ_NOT_AVAILABLE(1060)
QUIZ_HAS_ATTEMPTS_CANNOT_DELETE(1061)
QUESTION_NOT_FOUND(1062)
QUESTION_HAS_ATTEMPTS_CANNOT_DELETE(1063)
QUESTION_MUST_HAVE_ANSWERS(1064)
QUESTION_MUST_HAVE_AT_LEAST_TWO_ANSWERS(1065)
QUESTION_MUST_HAVE_EXACTLY_ONE_CORRECT_ANSWER(1066)
```

### Quiz Attempt Error Codes
```java
QUIZ_MAX_ATTEMPTS_EXCEEDED(1067)
QUIZ_ATTEMPT_NOT_FOUND(1068)
QUIZ_ATTEMPT_NOT_IN_PROGRESS(1069)
QUIZ_ATTEMPT_EXPIRED(1070)
QUIZ_NOT_STARTED(1076)
QUIZ_EXPIRED(1077)
```

## Usage Examples

### 1. Tạo Quiz
```java
QuizCreationRequest request = QuizCreationRequest.builder()
    .title("Java Basics Quiz")
    .description("Test your Java knowledge")
    .type(QuizType.ASSESSMENT)
    .lessonId("lesson-123")
    .passingScore(75.0)
    .maxAttempts(3)
    .timeLimitMinutes(60)
    .questions(questionRequests)
    .build();

QuizResponse quiz = quizService.createQuiz(request);
```

### 2. Student làm bài
```java
// Bắt đầu làm bài
QuizAttemptResponse attempt = quizAttemptService.startQuizAttempt("quiz-123");

// Nộp bài
QuizResultResponse result = quizAttemptService.submitQuiz(attempt.getId());
```

### 3. Lấy thống kê
```java
QuizSummaryResponse summary = quizService.getQuizSummary("quiz-123");
// summary.getTotalAttempts()
// summary.getPassedAttempts()
// summary.getAverageScore()
```

## Integration với Progress System

Quiz service tích hợp với ProgressService để:
- Cập nhật tiến độ lesson khi student pass quiz
- Đánh dấu lesson completed nếu quiz đạt yêu cầu
- Tracking điểm quiz trong progress record

## Performance Considerations

- Sử dụng `@Transactional` cho các operations quan trọng
- Lazy loading cho relationships không cần thiết
- Pagination cho các list operations
- Index database cho các truy vấn thường xuyên
- Cache quiz data cho student view

## Security

- Tất cả endpoints đều có `@PreAuthorize`
- Kiểm tra ownership trước khi cho phép modify
- Validate input để tránh injection attacks
- Rate limiting cho quiz attempts
- Audit log cho các thao tác quan trọng 