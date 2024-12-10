package com.vk.education_bot.service;

import com.vk.education_bot.entity.Question;
import com.vk.education_bot.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    @Autowired
    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    // Получить ответ по тексту вопроса
    public String getAnswer(String text) {
        return questionRepository.findByText(text)
                .map(Question::getAnswer)
                .orElse(null);
    }

    // Получить список всех вопросов
    public List<String> getAllQuestionsText() {
        return questionRepository.findAll().stream()
                .map(e -> e.getId() + ". " + e.getText()) // Получаем только текст вопросов
                .toList();
    }
}

