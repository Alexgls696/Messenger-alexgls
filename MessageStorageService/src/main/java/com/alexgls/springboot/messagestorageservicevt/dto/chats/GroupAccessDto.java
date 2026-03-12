package com.alexgls.springboot.messagestorageservicevt.dto.chats;

public record GroupAccessDto(
        //Изменение описания группы
        boolean canEdit,

        //Удаление участников группы или добавление участников
        boolean canRemoveMembers,

        boolean canRemoveMessages
) {
}
