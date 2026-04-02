package com.alexgls.springboot.messagestorageservicevt.util.groups;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EnterGroupServiceMessage implements ServiceMessage{

    private final String actorName;

    @Override
    public String getMessage() {
        return "Пользователь %s присоединился к группе.".formatted(actorName);
    }
}
