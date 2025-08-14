package com.ntabodoiqua.online_course_management.service;

import com.ntabodoiqua.online_course_management.dto.request.document.DocumentUploadRequest;
import com.ntabodoiqua.online_course_management.dto.response.document.DocumentResponse;
import com.ntabodoiqua.online_course_management.entity.Lesson;
import com.ntabodoiqua.online_course_management.entity.LessonDocument;
import com.ntabodoiqua.online_course_management.entity.User;
import com.ntabodoiqua.online_course_management.exception.AppException;
import com.ntabodoiqua.online_course_management.exception.ErrorCode;
import com.ntabodoiqua.online_course_management.mapper.DocumentMapper;
import com.ntabodoiqua.online_course_management.repository.LessonDocumentRepository;
import com.ntabodoiqua.online_course_management.repository.LessonRepository;
import com.ntabodoiqua.online_course_management.repository.UploadedFileRepository;
import com.ntabodoiqua.online_course_management.repository.UserRepository;
import com.ntabodoiqua.online_course_management.repository.EnrollmentRepository;
import com.ntabodoiqua.online_course_management.repository.CourseLessonRepository;
import com.ntabodoiqua.online_course_management.service.file.FileStorageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonDocumentService {
    
    LessonDocumentRepository lessonDocumentRepository;
    LessonRepository lessonRepository;
    UserRepository userRepository;
    EnrollmentRepository enrollmentRepository;
    CourseLessonRepository courseLessonRepository;
    UploadedFileRepository uploadedFileRepository;
    DocumentMapper documentMapper;
    FileStorageService fileStorageService;

    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Transactional
    public DocumentResponse uploadDocument(String lessonId, DocumentUploadRequest request, MultipartFile file) {
        // Kiểm tra bài học tồn tại
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        
        // Kiểm tra quyền (chỉ instructor tạo bài học hoặc admin mới được upload)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        boolean isCreator = lesson.getCreatedBy().equals(currentUser);
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));
        
        if (!isCreator && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        // Validate file
        if (file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_FILE);
        }
        
        // Store file
        String fileName = fileStorageService.storeFile(file, false).getFileName(); // Private file for lesson documents
        
        // Create document entity
        LessonDocument document = LessonDocument.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .fileName(fileName)
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .lesson(lesson)
                .uploadedBy(currentUser)
                .build();
        
        document = lessonDocumentRepository.save(document);
        
        DocumentResponse response = documentMapper.toLessonDocumentResponse(document);
        response.setDownloadUrl("/lessons/" + lessonId + "/documents/" + document.getId() + "/download");
        
        log.info("Document uploaded successfully for lesson: {}, document: {}", lessonId, document.getId());
        return response;
    }

    public List<DocumentResponse> getLessonDocuments(String lessonId) {
        // Kiểm tra bài học tồn tại
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        
        // Kiểm tra quyền truy cập (chỉ người tham gia khóa học chứa bài học hoặc instructor/admin)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        boolean isCreator = lesson.getCreatedBy().equals(currentUser);
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        
        // Kiểm tra xem user có đăng ký vào khóa học nào chứa bài học này không
        boolean hasAccess = isCreator || isAdmin || 
                courseLessonRepository.findByLesson(lesson).stream()
                    .anyMatch(courseLesson -> 
                        enrollmentRepository.existsByStudentIdAndCourseId(currentUser.getId(), 
                            courseLesson.getCourse().getId()));
        
        if (!hasAccess) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        List<LessonDocument> documents = lessonDocumentRepository.findByLessonId(lessonId);
        return documents.stream()
                .map(doc -> {
                    DocumentResponse response = documentMapper.toLessonDocumentResponse(doc);
                    response.setDownloadUrl("/lessons/" + lessonId + "/documents/" + doc.getId() + "/download");
                    return response;
                })
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Transactional
    public void deleteDocument(String lessonId, String documentId) {
        // Kiểm tra tài liệu tồn tại
        LessonDocument document = lessonDocumentRepository.findByIdAndLessonId(documentId, lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        
        // Kiểm tra quyền (chỉ instructor tạo bài học hoặc admin mới được xóa)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        boolean isCreator = document.getLesson().getCreatedBy().equals(currentUser);
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));
        
        if (!isCreator && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        // Delete physical file only (not the UploadedFile record to avoid circular dependency)
        try {
            var uploadedFile = uploadedFileRepository.findByFileName(document.getFileName())
                    .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
            fileStorageService.deletePhysicalFile(document.getFileName(), uploadedFile.isPublic());
        } catch (Exception e) {
            log.warn("Failed to delete physical file: {}", document.getFileName());
        }
        
        // Delete from database
        lessonDocumentRepository.delete(document);
        log.info("Document deleted successfully: {}", documentId);
    }

    public Resource downloadDocument(String lessonId, String documentId) {
        // Kiểm tra tài liệu tồn tại
        LessonDocument document = lessonDocumentRepository.findByIdAndLessonId(documentId, lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        
        // Kiểm tra quyền truy cập
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        boolean isCreator = document.getLesson().getCreatedBy().equals(currentUser);
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        
        // Kiểm tra xem user có đăng ký vào khóa học nào chứa bài học này không
        boolean hasAccess = isCreator || isAdmin || 
                courseLessonRepository.findByLesson(document.getLesson()).stream()
                    .anyMatch(courseLesson -> 
                        enrollmentRepository.existsByStudentIdAndCourseId(currentUser.getId(), 
                            courseLesson.getCourse().getId()));
        
        if (!hasAccess) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        var uploadedFile = uploadedFileRepository.findByFileName(document.getFileName())
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        
        return fileStorageService.loadFile(document.getFileName(), uploadedFile.isPublic());
    }
} 