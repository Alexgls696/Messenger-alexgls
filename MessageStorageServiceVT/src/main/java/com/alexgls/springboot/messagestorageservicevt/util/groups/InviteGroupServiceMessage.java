package com.alexgls.springboot.messagestorageservicevt.util.groups;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InviteGroupServiceMessage implements ServiceMessage{

    private final String actorUsername;

    private final String invitedUsername;

    @Override
    public String getMessage() {
        return "Пользователь %s пригласил %s в группу.".formatted(actorUsername, invitedUsername);
    }
}
