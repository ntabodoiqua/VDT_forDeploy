package com.ntabodoiqua.online_course_management.mapper.quiz;

import com.ntabodoiqua.online_course_management.dto.request.quiz.QuizQuestionRequest;
import com.ntabodoiqua.online_course_management.dto.response.quiz.QuizQuestionResponse;
import com.ntabodoiqua.online_course_management.dto.response.quiz.QuizQuestionStudentResponse;
import com.ntabodoiqua.online_course_management.entity.QuizQuestion;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {QuizAnswerMapper.class})
public interface QuizQuestionMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quiz", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "answers", ignore = true)
    QuizQuestion toQuizQuestion(QuizQuestionRequest request);

    @Mapping(source = "answers", target = "answers")
    QuizQuestionResponse toQuizQuestionResponse(QuizQuestion question);

    @Mapping(source = "answers", target = "answers")
    QuizQuestionStudentResponse toQuizQuestionStudentResponse(QuizQuestion question);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "quiz", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "answers", ignore = true)
    void updateQuizQuestion(@MappingTarget QuizQuestion question, QuizQuestionRequest request);
} 