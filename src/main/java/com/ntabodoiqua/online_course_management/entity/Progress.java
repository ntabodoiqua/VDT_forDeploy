package com.ntabodoiqua.online_course_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Progress {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    // Tham chiếu đến Enrollment để biết học sinh nào đang học khóa nào
    @ManyToOne
    Enrollment enrollment;

    @ManyToOne
    Lesson lesson; // Bài học hiện tại

    @Column(name = "is_completed")
    boolean isCompleted; // Đã hoàn thành bài học hay chưa
    LocalDate completionDate; // Ngày hoàn thành bài học

    // Quiz completion tracking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_quiz_id")
    Quiz completedQuiz; // Quiz đã hoàn thành (nếu có)

    Double quizScore; // Điểm quiz đạt được
}
