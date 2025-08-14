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
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "lesson_document_id"})
})
public class DocumentView {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user; // Student đã xem

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_document_id", nullable = false)
    LessonDocument lessonDocument; // Document được xem

    LocalDateTime viewedAt; // Thời gian xem

    Long viewDurationSeconds; // Thời gian xem (seconds) - optional
} 