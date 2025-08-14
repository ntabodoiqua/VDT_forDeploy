# Quiz Repository và Specification

## Tổng quan

Hệ thống repository và specification cho quiz được thiết kế theo pattern chuẩn Spring Boot với JPA Specification để hỗ trợ:
- Phân trang (Pagination)
- Lọc dữ liệu (Filtering) 
- Sắp xếp (Sorting)
- Phân quyền truy cập (Permission-based access)

## Cấu trúc Repository

### 1. QuizRepository
- **Extends**: `JpaRepository<Quiz, String>`, `JpaSpecificationExecutor<Quiz>`
- **Chức năng**: CRUD cơ bản + tìm kiếm nâng cao cho Quiz
- **Methods chính**:
  - `findByLessonId()` - Tìm quiz theo lesson
  - `findByCourseId()` - Tìm quiz theo course (qua lesson)
  - `findByCreatedById()` - Tìm quiz theo instructor
  - `countByInstructorId()` - Thống kê quiz của instructor

### 2. QuizQuestionRepository
- **Extends**: `JpaRepository<QuizQuestion, String>`, `JpaSpecificationExecutor<QuizQuestion>`
- **Chức năng**: Quản lý câu hỏi trong quiz
- **Methods chính**:
  - `findByQuizIdOrderByOrderIndexAsc()` - Lấy câu hỏi theo thứ tự
  - `getTotalPointsByQuizId()` - Tính tổng điểm quiz
  - `getMaxOrderIndexByQuizId()` - Lấy order index lớn nhất

### 3. QuizAnswerRepository
- **Extends**: `JpaRepository<QuizAnswer, String>`, `JpaSpecificationExecutor<QuizAnswer>`
- **Chức năng**: Quản lý đáp án của câu hỏi
- **Methods chính**:
  - `findByQuestionIdOrderByOrderIndexAsc()` - Lấy đáp án theo thứ tự
  - `findFirstByQuestionIdAndIsCorrectTrue()` - Tìm đáp án đúng
  - `existsByQuestionIdAndIsCorrectTrue()` - Kiểm tra có đáp án đúng

### 4. QuizAttemptRepository
- **Extends**: `JpaRepository<QuizAttempt, String>`, `JpaSpecificationExecutor<QuizAttempt>`
- **Chức năng**: Quản lý lần làm quiz của student
- **Methods chính**:
  - `findByQuizIdAndStudentIdOrderByAttemptNumberDesc()` - Lấy attempt theo quiz và student
  - `countByQuizIdAndStudentId()` - Đếm số lần làm
  - `getAverageScoreByQuizId()` - Tính điểm trung bình

### 5. QuizAttemptAnswerRepository
- **Extends**: `JpaRepository<QuizAttemptAnswer, String>`, `JpaSpecificationExecutor<QuizAttemptAnswer>`
- **Chức năng**: Quản lý câu trả lời của student trong attempt
- **Methods chính**:
  - `findByAttemptIdAndQuestionId()` - Tìm câu trả lời cụ thể
  - `getTotalPointsEarnedByAttemptId()` - Tính tổng điểm đạt được
  - `getCorrectPercentageByQuestionId()` - Thống kê độ khó câu hỏi

## Specification Classes

### 1. QuizSpecification
```java
// Lọc cơ bản
QuizSpecification.withFilter(QuizFilterRequest filter)

// Lọc với phân quyền
QuizSpecification.withFilterAndPermission(filter, canViewInactive, instructorUsername)

// Các specification đơn lẻ
QuizSpecification.isActive()
QuizSpecification.byInstructorId(instructorId)
QuizSpecification.byCourseId(courseId)
QuizSpecification.isAvailable() // Active + trong thời gian cho phép
QuizSpecification.isExpired()
```

### 2. QuizAttemptSpecification
```java
// Lọc theo các tiêu chí
QuizAttemptSpecification.byQuizId(quizId)
QuizAttemptSpecification.byStudentId(studentId)
QuizAttemptSpecification.byStatus(status)
QuizAttemptSpecification.startedBetween(startTime, endTime)
QuizAttemptSpecification.scoreBetween(minScore, maxScore)

// Phân quyền
QuizAttemptSpecification.withPermission(userId, role)
```

## Cách sử dụng trong Service

### 1. Lọc và phân trang cơ bản
```java
@Service
public class QuizService {
    
    public Page<QuizResponse> getQuizzes(QuizFilterRequest filter, Pageable pageable) {
        // Tạo specification với filter
        Specification<Quiz> spec = QuizSpecification.withFilter(filter);
        
        // Lấy dữ liệu với phân trang
        Page<Quiz> quizPage = quizRepository.findAll(spec, pageable);
        
        // Convert sang response
        return quizPage.map(quizMapper::toQuizResponse);
    }
}
```

### 2. Kết hợp nhiều specification
```java
public List<QuizResponse> getAvailableQuizzes(String courseId) {
    Specification<Quiz> spec = Specification
            .where(QuizSpecification.byCourseId(courseId))
            .and(QuizSpecification.isAvailable());
    
    List<Quiz> quizzes = quizRepository.findAll(spec);
    return quizzes.stream()
            .map(quizMapper::toQuizResponse)
            .collect(Collectors.toList());
}
```

### 3. Phân quyền truy cập
```java
public Page<QuizResponse> getQuizzesWithPermission(QuizFilterRequest filter, Pageable pageable) {
    // Lấy thông tin user hiện tại
    QuizAccessInfo accessInfo = getCurrentUserAccessInfo();
    
    // Tạo specification với phân quyền
    Specification<Quiz> spec = QuizSpecification.withFilterAndPermission(
            filter,
            accessInfo.canViewInactive(),
            accessInfo.instructorUsername()
    );
    
    Page<Quiz> quizPage = quizRepository.findAll(spec, pageable);
    return quizPage.map(quizMapper::toQuizResponse);
}
```

## Quy tắc phân quyền

### Quiz Access
- **Admin**: Xem tất cả quiz (kể cả inactive)
- **Instructor**: Xem quiz active + quiz của mình (kể cả inactive)
- **Student/Guest**: Chỉ xem quiz active và available (trong thời gian cho phép)

### QuizAttempt Access
- **Admin**: Xem tất cả attempt
- **Instructor**: Xem attempt trên quiz của mình
- **Student**: Chỉ xem attempt của mình

## Filter Request

### QuizFilterRequest
```java
public class QuizFilterRequest {
    String title;           // Tìm theo title (LIKE)
    QuizType type;          // Lọc theo type (PRACTICE/ASSESSMENT)
    String lessonId;        // Lọc theo lesson
    String courseId;        // Lọc theo course (qua lesson)
    String instructorId;    // Lọc theo instructor
    Boolean isActive;       // Lọc theo trạng thái active
    LocalDateTime startTimeFrom/To;  // Lọc theo thời gian bắt đầu
    LocalDateTime endTimeFrom/To;    // Lọc theo thời gian kết thúc
    
    // Pagination
    Integer page;
    Integer size;
    String sortBy;
    String sortDir;
}
```

## Ví dụ sử dụng

### 1. Tìm quiz của một course
```java
List<Quiz> quizzes = quizRepository.findByCourseIdAndIsActiveTrue(courseId);
```

### 2. Thống kê quiz của instructor
```java
long totalQuizzes = quizRepository.countByInstructorId(instructorId);
long activeQuizzes = quizRepository.countActiveQuizzesByInstructorId(instructorId);
```

### 3. Tìm quiz hết hạn
```java
Specification<Quiz> spec = QuizSpecification.isExpired();
List<Quiz> expiredQuizzes = quizRepository.findAll(spec);
```

### 4. Lấy attempt của student cho quiz
```java
List<QuizAttempt> attempts = quizAttemptRepository
    .findByQuizIdAndStudentIdOrderByAttemptNumberDesc(quizId, studentId);
```

### 5. Thống kê điểm quiz
```java
Double avgScore = quizAttemptRepository.getAverageScoreByQuizId(quizId);
Double maxScore = quizAttemptRepository.getMaxScoreByQuizId(quizId);
long passedCount = quizAttemptRepository.countPassedByQuizId(quizId);
```

## Lưu ý quan trọng

1. **Performance**: Sử dụng `@Query` với JPQL cho các truy vấn phức tạp
2. **Lazy Loading**: Các entity có `FetchType.LAZY`, cần chú ý N+1 problem
3. **Transaction**: Các method delete cần `@Transactional`
4. **Validation**: Luôn validate input trước khi query
5. **Security**: Luôn áp dụng phân quyền trong specification 