package com.alexgls.springboot.messagestorageservicevt.util.groups;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LeaveUserServiceMessage implements ServiceMessage {

    @Override
    public String getMessage() {
        return "Пользователь %s покинул группу.";
    }
}
