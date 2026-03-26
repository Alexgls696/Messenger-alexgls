package com.alexgls.springboot.messagestorageservicevt.util.groups;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LeaveUserServiceMessage implements ServiceMessage {

    private String username;

    @Override
    public String getMessage() {
        return "Пользователь %s покинул группу.".formatted(username);
    }
}
