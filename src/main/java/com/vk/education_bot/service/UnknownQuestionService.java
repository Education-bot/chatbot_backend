package com.vk.education_bot.service;

import com.vk.education_bot.entity.UnknownQuestion;
import com.vk.education_bot.repository.UnknownQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UnknownQuestionService {

    private final UnknownQuestionRepository unknownQuestionRepository;

    @Autowired
    public UnknownQuestionService(UnknownQuestionRepository unknownQuestionRepository) {
        this.unknownQuestionRepository = unknownQuestionRepository;
    }

    public void saveUnknownQuestion(long userId, String questionText) {
        UnknownQuestion unknownQuestion = new UnknownQuestion(userId, questionText);
        if (unknownQuestionRepository.findByQuestionText(questionText).isEmpty()) {
            unknownQuestionRepository.save(unknownQuestion);
        }
    }

    public List<UnknownQuestion> getAllUnknownQuestions() {
        return unknownQuestionRepository.findAll();
    }

    public Optional<UnknownQuestion> findByQuestionText(String questionText) {
        return unknownQuestionRepository.findByQuestionText(questionText);
    }

    public void deleteByQuestionText(String questionText) {
        unknownQuestionRepository.findByQuestionText(questionText)
                .ifPresent(unknownQuestionRepository::delete);
    }

    public List<UnknownQuestion> findAllByIds(List<Long> ids) {
        return unknownQuestionRepository.findAllById(ids);
    }

    public void deleteAll(List<UnknownQuestion> unknownQuestions) {
        unknownQuestionRepository.deleteAll(unknownQuestions);
    }

}
