package com.alexgls.springboot.messagestorageservice.util.groups;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RemoveUserServiceMessage implements ServiceMessage {

    private String removedUser;

    private String removedBy;

    @Override
    public String getMessage() {
        return "Администратор %s удалил пользователя %s из группы.".formatted(removedBy, removedUser);
    }
}
