package com.ntabodoiqua.online_course_management.entity;

import com.ntabodoiqua.online_course_management.enums.AttemptStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class QuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    Quiz quiz;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    User student;
    
    // Thêm enrollment để tách biệt quiz attempts theo từng khóa học
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    Enrollment enrollment;
    
    Integer attemptNumber; // Lần thử thứ mấy
    
    LocalDateTime startedAt;
    LocalDateTime submittedAt;
    LocalDateTime completedAt;
    
    @Enumerated(EnumType.STRING)
    AttemptStatus status; // IN_PROGRESS, COMPLETED, ABANDONED, EXPIRED
    
    Double score; // Điểm số (0-100)
    Double percentage; // Phần trăm (0-100)
    Boolean isPassed; // Có đạt điểm tối thiểu không (>= 70%)
    
    Integer totalQuestions;
    Integer correctAnswers;
    Integer incorrectAnswers;
    Integer unansweredQuestions;
    
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<QuizAttemptAnswer> attemptAnswers;
} 