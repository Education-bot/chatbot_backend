package com.vk.education_bot.handler;

import com.vk.education_bot.entity.Project;
import com.vk.education_bot.entity.Section;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
class UserContext {
    private final long userId;
    private UserState state = UserState.MAIN_MENU;
    private Section currentSection;
    private Project currentProject;
    private Project adminCurrentProject;

    public void reset() {
        state = UserState.MAIN_MENU;
        currentSection = null;
        currentProject = null;
        adminCurrentProject = null;
    }
}