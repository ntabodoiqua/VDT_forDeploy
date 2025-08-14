package com.ntabodoiqua.online_course_management.entity;

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
public class QuizQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    Quiz quiz;
    
    @Column(nullable = false)
    @Lob
    String questionText;
    
    Integer orderIndex;
    Double points; // Điểm cho câu hỏi này
    
    @Lob
    String explanation; // Giải thích đáp án
    
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<QuizAnswer> answers; // Các lựa chọn A, B, C, D
} 