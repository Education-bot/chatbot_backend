package com.vk.education_bot.service;

import com.vk.education_bot.entity.Section;
import com.vk.education_bot.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionRepository sectionRepository;

    public List<Section> getAllSections() {
        return sectionRepository.findAll();
    }
}