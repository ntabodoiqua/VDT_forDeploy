package com.ntabodoiqua.online_course_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    String title;
    
    String description;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    String content;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @ManyToOne
    User createdBy;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<LessonDocument> lessonDocuments;

    // Quan hệ 1:1 với Quiz
    @OneToOne(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    Quiz quiz;
}
