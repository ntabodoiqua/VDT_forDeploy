package com.ntabodoiqua.online_course_management.service;

import com.ntabodoiqua.online_course_management.dto.request.document.DocumentUploadRequest;
import com.ntabodoiqua.online_course_management.dto.response.document.DocumentResponse;
import com.ntabodoiqua.online_course_management.entity.Course;
import com.ntabodoiqua.online_course_management.entity.CourseDocument;
import com.ntabodoiqua.online_course_management.entity.User;
import com.ntabodoiqua.online_course_management.exception.AppException;
import com.ntabodoiqua.online_course_management.exception.ErrorCode;
import com.ntabodoiqua.online_course_management.mapper.DocumentMapper;
import com.ntabodoiqua.online_course_management.repository.CourseDocumentRepository;
import com.ntabodoiqua.online_course_management.repository.CourseRepository;
import com.ntabodoiqua.online_course_management.repository.UploadedFileRepository;
import com.ntabodoiqua.online_course_management.repository.UserRepository;
import com.ntabodoiqua.online_course_management.repository.EnrollmentRepository;
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
public class CourseDocumentService {
    
    CourseDocumentRepository courseDocumentRepository;
    CourseRepository courseRepository;
    UserRepository userRepository;
    EnrollmentRepository enrollmentRepository;
    UploadedFileRepository uploadedFileRepository;
    DocumentMapper documentMapper;
    FileStorageService fileStorageService;

    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Transactional
    public DocumentResponse uploadDocument(String courseId, DocumentUploadRequest request, MultipartFile file) {
        // Kiểm tra khóa học tồn tại
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
        
        // Kiểm tra quyền (chỉ instructor của khóa học hoặc admin mới được upload)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        boolean isInstructor = course.getInstructor().equals(currentUser);
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));
        
        if (!isInstructor && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        // Validate file
        if (file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_FILE);
        }
        
        // Store file
        String fileName = fileStorageService.storeFile(file, false).getFileName(); // Private file for course documents
        
        // Create document entity
        CourseDocument document = CourseDocument.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .fileName(fileName)
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .course(course)
                .uploadedBy(currentUser)
                .build();
        
        document = courseDocumentRepository.save(document);
        
        DocumentResponse response = documentMapper.toCourseDocumentResponse(document);
        response.setDownloadUrl("/courses/" + courseId + "/documents/" + document.getId() + "/download");
        
        log.info("Document uploaded successfully for course: {}, document: {}", courseId, document.getId());
        return response;
    }

    public List<DocumentResponse> getCourseDocuments(String courseId) {
        // Kiểm tra khóa học tồn tại
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
        
        // Kiểm tra quyền truy cập (chỉ người tham gia khóa học hoặc instructor/admin)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        boolean hasAccess = course.getInstructor().equals(currentUser) ||
                currentUser.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN")) ||
                enrollmentRepository.existsByStudentIdAndCourseId(currentUser.getId(), courseId);
        
        if (!hasAccess) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        List<CourseDocument> documents = courseDocumentRepository.findByCourseId(courseId);
        return documents.stream()
                .map(doc -> {
                    DocumentResponse response = documentMapper.toCourseDocumentResponse(doc);
                    response.setDownloadUrl("/courses/" + courseId + "/documents/" + doc.getId() + "/download");
                    return response;
                })
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Transactional
    public void deleteDocument(String courseId, String documentId) {
        // Kiểm tra tài liệu tồn tại
        CourseDocument document = courseDocumentRepository.findByIdAndCourseId(documentId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        
        // Kiểm tra quyền (chỉ instructor của khóa học hoặc admin mới được xóa)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        boolean isInstructor = document.getCourse().getInstructor().equals(currentUser);
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));
        
        if (!isInstructor && !isAdmin) {
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
        courseDocumentRepository.delete(document);
        log.info("Document deleted successfully: {}", documentId);
    }

    public Resource downloadDocument(String courseId, String documentId) {
        // Kiểm tra tài liệu tồn tại
        CourseDocument document = courseDocumentRepository.findByIdAndCourseId(documentId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        
        // Kiểm tra quyền truy cập
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        boolean hasAccess = document.getCourse().getInstructor().equals(currentUser) ||
                currentUser.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN")) ||
                enrollmentRepository.existsByStudentIdAndCourseId(currentUser.getId(), courseId);
        
        if (!hasAccess) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        var uploadedFile = uploadedFileRepository.findByFileName(document.getFileName())
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        
        return fileStorageService.loadFile(document.getFileName(), uploadedFile.isPublic());
    }
} 