package com.ntabodoiqua.online_course_management.service.file;

import com.ntabodoiqua.online_course_management.entity.UploadedFile;
import com.ntabodoiqua.online_course_management.entity.User;
import com.ntabodoiqua.online_course_management.entity.Course;
import com.ntabodoiqua.online_course_management.entity.CourseDocument;
import com.ntabodoiqua.online_course_management.entity.LessonDocument;
import com.ntabodoiqua.online_course_management.exception.AppException;
import com.ntabodoiqua.online_course_management.exception.ErrorCode;
import com.ntabodoiqua.online_course_management.repository.UploadedFileRepository;
import com.ntabodoiqua.online_course_management.repository.UserRepository;
import com.ntabodoiqua.online_course_management.repository.CourseDocumentRepository;
import com.ntabodoiqua.online_course_management.repository.LessonDocumentRepository;
import com.ntabodoiqua.online_course_management.repository.CourseRepository;
import com.ntabodoiqua.online_course_management.specification.UploadedFileSpecification;
import com.ntabodoiqua.online_course_management.dto.response.document.FileUsageResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.file.ConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.ntabodoiqua.online_course_management.configuration.properties.DigitalOceanSpacesProperties;
import com.amazonaws.HttpMethod;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileStorageService {
    UserRepository userRepository;
    UploadedFileRepository uploadedFileRepository;
    CourseDocumentRepository courseDocumentRepository;
    LessonDocumentRepository lessonDocumentRepository;
    CourseRepository courseRepository;
    AmazonS3 s3Client;
    DigitalOceanSpacesProperties spacesProperties;

    public UploadedFile storeFile(MultipartFile file, boolean isPublic) {
        try {
            String originalFileName = file.getOriginalFilename();
            String sanitizedFileName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");

            String folder = isPublic ? "public/" : "private/";
            String fileName = folder + UUID.randomUUID() + "_" + sanitizedFileName;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            s3Client.putObject(new PutObjectRequest(spacesProperties.getBucketName(), fileName, file.getInputStream(), metadata)
                    .withCannedAcl(isPublic ? CannedAccessControlList.PublicRead : CannedAccessControlList.Private));

            log.info("File stored successfully in S3: {}", fileName);
            // Lưu thông tin file vào cơ sở dữ liệu
            UploadedFile uploadedFile = UploadedFile.builder()
                    .fileName(fileName)
                    .originalFileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .isPublic(isPublic)
                    .uploadedAt(LocalDateTime.now())
                    .uploadedBy(userRepository.findByUsername(
                            SecurityContextHolder.getContext().getAuthentication().getName())
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)))
                    .build();
            uploadedFileRepository.save(uploadedFile);
            return uploadedFile;
        } catch (IOException e) {
            log.error("File cannot be stored in S3", e);
            throw new AppException(ErrorCode.FILE_CANNOT_STORED);
        }
    }

    public Resource loadFile(String fileName, boolean isPublic) {
        log.info("Attempting to load file with fileName: '{}'", fileName);
        try {
            UploadedFile uploadedFile = uploadedFileRepository.findByFileName(fileName)
                .orElseThrow(() -> {
                    log.error("File not found in database with fileName: '{}'", fileName);
                    return new AppException(ErrorCode.FILE_NOT_FOUND);
                });

            if (!uploadedFile.isPublic()) {
                // For private files, generate a pre-signed URL
                java.util.Date expiration = new java.util.Date();
                long expTimeMillis = expiration.getTime();
                expTimeMillis += 1000 * 60 * 60; // 1 hour
                expiration.setTime(expTimeMillis);

                GeneratePresignedUrlRequest generatePresignedUrlRequest =
                        new GeneratePresignedUrlRequest(spacesProperties.getBucketName(), fileName)
                                .withMethod(HttpMethod.GET)
                                .withExpiration(expiration);
                URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
                return new org.springframework.core.io.UrlResource(url);
            }

            // For public files, construct the URL directly
            URL url = new URL(spacesProperties.getBaseUrl() + "/" + fileName);
            Resource resource = new org.springframework.core.io.UrlResource(url);

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new AppException(ErrorCode.FILE_NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("Could not load file from S3: {}", fileName, e);
            throw new AppException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    // Service để làm cho file trở nên công khai
    public String makeFilePublic(String fileName, String currentUsername) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileName(fileName)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Allow admin to make any file public, others can only modify their own files
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));
        
        if (!isAdmin && !uploadedFile.getUploadedBy().getUsername().equals(currentUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (uploadedFile.isPublic()) {
            return "File is already public: " + fileName;
        }

        // Move file from private to public folder
        String newFileName = fileName.replaceFirst("private/", "public/");
        s3Client.copyObject(
                spacesProperties.getBucketName(),
                fileName,
                spacesProperties.getBucketName(),
                newFileName
        );
        s3Client.deleteObject(spacesProperties.getBucketName(), fileName);

        s3Client.setObjectAcl(spacesProperties.getBucketName(), newFileName, CannedAccessControlList.PublicRead);
        uploadedFile.setPublic(true);
        uploadedFile.setFileName(newFileName);
        uploadedFileRepository.save(uploadedFile);
        return "File is now public: " + newFileName;
    }

    // Service để làm cho file trở nên riêng tư
    public String makeFilePrivate(String fileName, String currentUsername) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileName(fileName)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Allow admin to make any file private, others can only modify their own files
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));
        
        if (!isAdmin && !uploadedFile.getUploadedBy().getUsername().equals(currentUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!uploadedFile.isPublic()) {
            return "File is already private: " + fileName;
        }

        // Move file from public to private folder
        String newFileName = fileName.replaceFirst("public/", "private/");
        s3Client.copyObject(
                spacesProperties.getBucketName(),
                fileName,
                spacesProperties.getBucketName(),
                newFileName
        );
        s3Client.deleteObject(spacesProperties.getBucketName(), fileName);

        s3Client.setObjectAcl(spacesProperties.getBucketName(), newFileName, CannedAccessControlList.Private);
        uploadedFile.setPublic(false);
        uploadedFile.setFileName(newFileName);
        uploadedFileRepository.save(uploadedFile);
        return "File is now private: " + newFileName;
    }

    // Service để lấy tất cả ảnh công khai của người dùng
    public List<String> getAllPublicImagesOfUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return uploadedFileRepository.findByUploadedByAndIsPublicTrue(user)
                .stream()
                .filter(file -> file.getContentType() != null && file.getContentType().startsWith("image/"))
                .map(file -> spacesProperties.getBaseUrl() + "/" + file.getFileName())
                .toList();
    }

    // Helper method to delete only physical file without touching database records
    // Used internally by document services to avoid circular dependencies
    public void deletePhysicalFile(String fileName, boolean isPublic) throws IOException {
        try {
            s3Client.deleteObject(spacesProperties.getBucketName(), fileName);
            log.info("Physical file deleted from S3: {}", fileName);
        } catch (AmazonS3Exception e) {
            log.error("Error deleting file from S3: {}", fileName, e);
            throw new IOException("Failed to delete file from S3", e);
        }
    }

    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Transactional
    public void deleteFile(String fileName) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UploadedFile uploadedFile = uploadedFileRepository.findByFileName(fileName)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        // Allow admin to delete any file, others can only delete their own files
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));
        
        if (!isAdmin && !uploadedFile.getUploadedBy().equals(user)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        try {
            // 1. Delete all course documents that reference this file
            List<CourseDocument> courseDocuments = courseDocumentRepository.findByFileName(fileName);
            if (!courseDocuments.isEmpty()) {
                courseDocumentRepository.deleteAll(courseDocuments);
                log.info("Deleted {} course documents referencing file: {}", courseDocuments.size(), fileName);
            }

            // 2. Delete all lesson documents that reference this file
            List<LessonDocument> lessonDocuments = lessonDocumentRepository.findByFileName(fileName);
            if (!lessonDocuments.isEmpty()) {
                lessonDocumentRepository.deleteAll(lessonDocuments);
                log.info("Deleted {} lesson documents referencing file: {}", lessonDocuments.size(), fileName);
            }

            // 3. Clear avatar URLs that reference this file
            String fileUrl = spacesProperties.getBaseUrl() + "/" + fileName;
            List<User> usersWithAvatar = userRepository.findByAvatarUrl(fileUrl);
            for (User userWithAvatar : usersWithAvatar) {
                userWithAvatar.setAvatarUrl(null);
                userRepository.save(userWithAvatar);
                log.info("Cleared avatar URL for user: {}", userWithAvatar.getUsername());
            }

            // 4. Clear course thumbnail URLs that reference this file
            List<Course> coursesWithThumbnail = courseRepository.findByThumbnailUrl(fileUrl);
            for (Course course : coursesWithThumbnail) {
                course.setThumbnailUrl(null);
                courseRepository.save(course);
                log.info("Cleared thumbnail URL for course: {}", course.getTitle());
            }

            // 5. Delete the physical file from S3
            deletePhysicalFile(fileName, uploadedFile.isPublic());

            // 6. Delete the UploadedFile record
            uploadedFileRepository.delete(uploadedFile);
            
            log.info("File deleted successfully with all references cleaned up: {}", fileName);
        } catch (IOException e) {
            log.error("Could not delete file: {}", fileName, e);
            throw new AppException(ErrorCode.FILE_DELETION_FAILED);
        }
    }

    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public Page<UploadedFile> getAllFilesOfUser(String contentType, String fileName, Pageable pageable) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Check if user is admin
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));

        if (isAdmin) {
            // Admin can see all files
            return uploadedFileRepository.findAll(
                    UploadedFileSpecification.withFilterForAdmin(contentType, fileName),
                    pageable
            );
        } else {
            // Regular users can only see their own files
            return uploadedFileRepository.findAll(
                    UploadedFileSpecification.withFilter(user, contentType, fileName),
                    pageable
            );
        }
    }

    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public FileUsageResponse checkFileUsage(String fileName) {
        log.info("Checking file usage for fileName: '{}'", fileName);
        UploadedFile uploadedFile = uploadedFileRepository.findByFileName(fileName)
                .orElseThrow(() -> {
                    log.warn("File usage check failed: File not found in database with fileName: '{}'", fileName);
                    return new AppException(ErrorCode.FILE_NOT_FOUND);
                });

        String fileUrl = spacesProperties.getBaseUrl() + "/" + uploadedFile.getFileName();

        List<FileUsageResponse.FileUsageDetail> usageDetails = new ArrayList<>();

        // Check user avatars
        List<User> usersWithAvatar = userRepository.findByAvatarUrl(fileUrl);
        usersWithAvatar.stream()
                .map(user -> new FileUsageResponse.FileUsageDetail("user_avatar", user.getId().toString(), user.getUsername(), "User Avatar for " + user.getUsername()))
                .forEach(usageDetails::add);

        // Check course thumbnails
        List<Course> coursesWithThumbnail = courseRepository.findByThumbnailUrl(fileUrl);
        coursesWithThumbnail.stream()
                .map(course -> new FileUsageResponse.FileUsageDetail("course_thumbnail", course.getId().toString(), course.getTitle(), "Thumbnail for course " + course.getTitle()))
                .forEach(usageDetails::add);

        // Check course documents
        List<CourseDocument> courseDocuments = courseDocumentRepository.findByFileName(fileName);
        courseDocuments.stream()
                .map(doc -> new FileUsageResponse.FileUsageDetail("course_document", doc.getCourse().getId().toString(), doc.getTitle(), "Document in course " + doc.getCourse().getTitle()))
                .forEach(usageDetails::add);

        // Check lesson documents
        List<LessonDocument> lessonDocuments = lessonDocumentRepository.findByFileName(fileName);
        lessonDocuments.stream()
                .map(doc -> new FileUsageResponse.FileUsageDetail("lesson_document", doc.getLesson().getId().toString(), doc.getTitle(), "Document in lesson " + doc.getLesson().getTitle()))
                .forEach(usageDetails::add);

        return FileUsageResponse.builder()
                .fileName(fileName)
                .isUsed(!usageDetails.isEmpty())
                .usageDetails(usageDetails)
                .build();
    }

    // Lấy tất cả file từ người dùng trong hệ thống cho admin
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UploadedFile> getAllUploadedFiles(String contentType, String fileName, String uploaderName, Pageable pageable) {
        return uploadedFileRepository.findAll(UploadedFileSpecification.getFilesByAdminCriteria(contentType, fileName, uploaderName), pageable);
    }
}
