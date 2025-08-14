package com.ntabodoiqua.online_course_management.mapper;

import com.ntabodoiqua.online_course_management.dto.request.course.CourseCreationRequest;
import com.ntabodoiqua.online_course_management.dto.request.course.CourseUpdateRequest;
import com.ntabodoiqua.online_course_management.dto.response.course.CourseResponse;
import com.ntabodoiqua.online_course_management.entity.Course;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {UserMapper.class, CategoryMapper.class})
public interface CourseMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "instructor", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    Course toCourse(CourseCreationRequest request);

    // Map Course entity to CourseResponse DTO
    @Mapping(source = "active", target = "isActive")
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "totalReviews", ignore = true)
    CourseResponse toCourseResponse(Course course);

    // Cập nhật thông tin từ update request vào Course entity
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "category", ignore = true)
    void updateCourse(@MappingTarget Course course, CourseUpdateRequest request);
}
