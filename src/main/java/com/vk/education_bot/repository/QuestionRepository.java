package com.vk.education_bot.repository;

import com.vk.education_bot.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    Optional<Question> findByText(String questionText);
}
