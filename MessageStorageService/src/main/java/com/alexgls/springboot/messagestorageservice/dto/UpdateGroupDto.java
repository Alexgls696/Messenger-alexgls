package com.alexgls.springboot.messagestorageservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateGroupDto(
        int chatId,
        @Size(min = 2, max = 64, message = "{errors.validation.group.update.name_size_error}")
        @NotBlank(message = "{errors.validation.group.update.name_is_black}")
        String name,

        @Size(max = 1024, message = "{errors.validation.group.update.description_length_is_very_large}")
        String description
) {
}
