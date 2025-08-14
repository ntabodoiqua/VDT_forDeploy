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
public class UploadedFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false, unique = true)
    String fileName; // Tên tệp đã tải lên
    String originalFileName; // Tên tệp gốc
    String contentType; // Loại nội dung của tệp (ví dụ: image/png, application/pdf)
    Long fileSize; // Dung lượng tệp (bytes)
    boolean isPublic; // Trạng thái công khai của tệp (có thể truy cập công khai hay không)
    LocalDateTime uploadedAt; // Ngày giờ tải lên tệp

    @ManyToOne
    User uploadedBy; // Người dùng đã tải lên tệp

}
