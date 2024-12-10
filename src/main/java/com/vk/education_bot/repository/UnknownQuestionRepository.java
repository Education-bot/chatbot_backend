package com.vk.education_bot.repository;

import com.vk.education_bot.entity.UnknownQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnknownQuestionRepository extends JpaRepository<UnknownQuestion, Long> {

    Optional<UnknownQuestion> findByQuestionText(String questionText);
}
