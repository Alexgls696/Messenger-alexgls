package com.alexgls.springboot.messagestorageservice.util.groups;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LeaveUserServiceMessage implements ServiceMessage {

    private String leavingUser;

    @Override
    public String getMessage() {
        return "Пользователь %s покинул группу.";
    }
}
