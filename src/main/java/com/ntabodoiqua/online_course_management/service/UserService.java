package com.ntabodoiqua.online_course_management.service;

import com.ntabodoiqua.online_course_management.constant.PredefinedRole;
import com.ntabodoiqua.online_course_management.dto.request.user.UserCreationRequest;
import com.ntabodoiqua.online_course_management.dto.request.user.UserUpdateRequest;
import com.ntabodoiqua.online_course_management.dto.response.user.UserResponse;
import com.ntabodoiqua.online_course_management.entity.Role;
import com.ntabodoiqua.online_course_management.entity.UploadedFile;
import com.ntabodoiqua.online_course_management.entity.User;
import com.ntabodoiqua.online_course_management.exception.AppException;
import com.ntabodoiqua.online_course_management.exception.ErrorCode;
import com.ntabodoiqua.online_course_management.mapper.UserMapper;
import com.ntabodoiqua.online_course_management.repository.RoleRepository;
import com.ntabodoiqua.online_course_management.repository.UploadedFileRepository;
import com.ntabodoiqua.online_course_management.repository.UserRepository;
import com.ntabodoiqua.online_course_management.repository.EnrollmentRepository;
import com.ntabodoiqua.online_course_management.service.file.FileStorageService;
import com.ntabodoiqua.online_course_management.configuration.properties.DigitalOceanSpacesProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    UploadedFileRepository uploadedFileRepository;
    UserRepository userRepository;
    RoleRepository roleRepository;
    EnrollmentRepository enrollmentRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    FileStorageService fileStorageService;
    DigitalOceanSpacesProperties spacesProperties;

    // Service tạo người dùng mới

    public UserResponse createUser(UserCreationRequest request){
        // Kiểm tra trùng email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        // Kiểm tra trùng số điện thoại
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_EXISTED);
        }
        // Kiểm tra trùng username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        // Mã hóa mật khẩu
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Thiết lập mặc định
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // Gán role mặc định
        Set<Role> roles = new HashSet<>();
        roleRepository.findById(PredefinedRole.STUDENT_ROLE)
                .ifPresent(roles::add);
        user.setRoles(roles);

        // Lưu và trả về thông tin người dùng
        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        return userMapper.toUserResponse(user);
    }

    // Service lấy thông tin người dùng hiện tại
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public UserResponse getMyInfo(){
        // Lấy securityContext từ SecurityContextHolder
        var context = SecurityContextHolder.getContext();

        // Lấy tên người dùng từ authentication
        String name = context.getAuthentication().getName();

        // Kiểm tra xem người dùng có tồn tại không
        User user = userRepository.findByUsername(name).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    // Service người dùng đổi mật khẩu
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public String changeMyPassword(String oldPassword, String newPassword) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Kiểm tra mật khẩu cũ có đúng không
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new AppException(ErrorCode.OLD_PASSWORD_FALSE);
        }

        // Kiểm tra mật khẩu mới có khác mật khẩu cũ không
        if (oldPassword.equals(newPassword)) {
            throw new AppException(ErrorCode.NEW_PASSWORD_SAME_AS_OLD);
        }
        // Mã hóa mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        // Ghi log
        log.info("User {} changed password successfully", username);
        return "Password changed successfully";
    }

    // Service người dùng cập nhật ảnh đại diện
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public String setAvatar(MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AppException(ErrorCode.INVALID_IMAGE_TYPE);
        }

        String fileName = fileStorageService.storeFile(file, true);
        String avatarUrl = spacesProperties.getBaseUrl() + "/" + fileName;

        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return avatarUrl;
    }

    // Service người dùng cập nhật ảnh đại diện từ file đã tải lên
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public String setAvatarFromUploadedFile(String fileName) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UploadedFile uploadedFile = uploadedFileRepository.findByFileName(fileName)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        // Chỉ cho phép dùng ảnh và phải là file public
        if (!uploadedFile.getContentType().startsWith("image/") || !uploadedFile.isPublic()) {
            throw new AppException(ErrorCode.INVALID_IMAGE_TYPE);
        }

        String avatarUrl = spacesProperties.getBaseUrl() + "/" + uploadedFile.getFileName();
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return avatarUrl;
    }

    // Service người dùng cập nhật thông tin cá nhân
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public UserResponse updateMyInfo(UserUpdateRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Nếu email trong request khác với email hiện tại của user
        // và email đó đã tồn tại trong hệ thống (của người dùng khác)
        String newEmail = request.getEmail();
        String currentEmail = user.getEmail();

        if (!newEmail.equalsIgnoreCase(currentEmail) && userRepository.existsByEmail(newEmail)) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        // Nếu số điện thoại trong request khác với số hiện tại của user
        // và số điện thoại đó đã tồn tại trong hệ thống (của người dùng khác)
        String newPhone = request.getPhone();
        String currentPhone = user.getPhone();

        if (!newPhone.equals(currentPhone) && userRepository.existsByPhone(newPhone)) {
            throw new AppException(ErrorCode.PHONE_EXISTED);
        }

        // Cập nhật thông tin người dùng
        user.setUpdatedAt(LocalDateTime.now());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setBio(request.getBio());
        user.setDob(request.getDob());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    // Service người dùng xóa tài khoản
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public String deleteMyAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Xóa người dùng
        userRepository.delete(user);
        log.info("User {} deleted their account successfully", username);
        return "Account deleted successfully";
    }

    // Service người dùng disable tài khoản
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public String disableMyAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Disable tài khoản
        user.setEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("User {} disabled their account successfully", username);
        return "Account disabled successfully";
    }

    // Service kiểm tra user có đăng ký khóa học không
    public boolean isEnrolled(String username, String courseId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        return enrollmentRepository.existsByStudentIdAndCourseId(user.getId(), courseId);
    }

}
