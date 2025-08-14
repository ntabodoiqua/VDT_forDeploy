package com.ntabodoiqua.online_course_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class QuizAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    QuizQuestion question;
    
    @Column(nullable = false)
    @Lob
    String answerText;
    
    Boolean isCorrect; // Chỉ có 1 đáp án isCorrect = true
    Integer orderIndex; // Thứ tự A=1, B=2, C=3, D=4
    
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
} 