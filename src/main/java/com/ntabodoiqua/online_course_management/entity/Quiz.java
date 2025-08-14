package com.ntabodoiqua.online_course_management.entity;

import com.ntabodoiqua.online_course_management.enums.QuizType;
import com.ntabodoiqua.online_course_management.enums.ScoringMethod;
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
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    
    @Column(nullable = false)
    String title;
    
    String description;
    
    @Enumerated(EnumType.STRING)
    QuizType type; // PRACTICE, ASSESSMENT
    
    // Quan hệ 1:1 với Lesson
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false, unique = true)
    Lesson lesson;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    User createdBy;
    
    // Configuration settings
    LocalDateTime startTime;
    LocalDateTime endTime;
    Integer timeLimitMinutes; // Thời gian giới hạn (phút)
    Integer maxAttempts; // Số lần thử tối đa (-1 = unlimited)
    Double passingScore; // Điểm tối thiểu để pass (70%)
    
    @Enumerated(EnumType.STRING)
    ScoringMethod scoringMethod; // HIGHEST, LATEST, AVERAGE
    
    Boolean isActive;
    Boolean shuffleQuestions;
    Boolean shuffleAnswers;
    Boolean showResults; // Hiện kết quả sau khi làm xong
    Boolean showCorrectAnswers; // Hiện đáp án đúng
    
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<QuizQuestion> questions;
    
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<QuizAttempt> attempts;
} 