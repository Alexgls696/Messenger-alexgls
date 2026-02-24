package com.alexgls.springboot.messagestorageservicevt.util.groups;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateGroupServiceMessage implements ServiceMessage {

    private final String username;
    private final String groupName;

    @Override
    public String getMessage() {
        return "Пользователь %s создал группу %s".formatted(username, groupName);
    }
}
