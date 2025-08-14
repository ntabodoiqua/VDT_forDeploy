package com.ntabodoiqua.online_course_management.mapper.quiz;

import com.ntabodoiqua.online_course_management.dto.request.quiz.QuizAnswerRequest;
import com.ntabodoiqua.online_course_management.dto.response.quiz.QuizAnswerResponse;
import com.ntabodoiqua.online_course_management.dto.response.quiz.QuizAnswerStudentResponse;
import com.ntabodoiqua.online_course_management.entity.QuizAnswer;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface QuizAnswerMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "question", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    QuizAnswer toQuizAnswer(QuizAnswerRequest request);

    QuizAnswerResponse toQuizAnswerResponse(QuizAnswer answer);

    QuizAnswerStudentResponse toQuizAnswerStudentResponse(QuizAnswer answer);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "question", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateQuizAnswer(@MappingTarget QuizAnswer answer, QuizAnswerRequest request);
} 