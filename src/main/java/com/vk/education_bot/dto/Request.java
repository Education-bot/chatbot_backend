package com.vk.education_bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Request (
    String type,
    @JsonProperty("group_id")
    int groupId
) {
}
