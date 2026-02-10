package com.alexgls.springboot.messagestorageservicevt.controller;

import com.alexgls.springboot.messagestorageservicevt.dto.ChatDto;
import com.alexgls.springboot.messagestorageservicevt.dto.CreateGroupDto;
import com.alexgls.springboot.messagestorageservicevt.dto.GroupAccessDto;
import com.alexgls.springboot.messagestorageservicevt.dto.UpdateGroupDto;
import com.alexgls.springboot.messagestorageservicevt.service.ChatsService;
import com.alexgls.springboot.messagestorageservicevt.service.ParticipantsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.alexgls.springboot.messagestorageservicevt.util.SecurityUtils.getSenderId;

@RestController
@RequestMapping("/api/chats/groups")
@RequiredArgsConstructor
@Slf4j
public class GroupsController {

    private final ChatsService chatsService;
    private final ParticipantsService participantsService;

    @PostMapping
    public ChatDto createGroupChat(@RequestBody CreateGroupDto createGroupDto, Authentication authentication) {
        Integer id = getSenderId(authentication);
        log.info("Creating group chat, creator id: : {}", id);
        return chatsService.createGroup(createGroupDto, id);
    }

    /**
     * Определяет права текущего пользователя.
     *
     * @param groupId        - id группы, для которой требуется запрос прав
     * @param authentication - параметр подставится автоматически, необходим для определения id пользователя, отправившего запрос
     * @return GroupUserDto - объект, содержащий права текущего пользователя
     */
    @GetMapping("/{id}/access")
    public GroupAccessDto getUserRightsByGroupId(@PathVariable("id") int groupId, Authentication authentication) {
        log.info("Get user rights by group id: {}", groupId);
        Integer userId = getSenderId(authentication);
        return chatsService.getUserRightsByGroupId(groupId, userId);
    }

    @PostMapping("/update")
    public ChatDto updateGroupChat(@Valid @RequestBody UpdateGroupDto updateGroupDto, Authentication authentication) {
        log.info("Update group chat, actor id: {}", updateGroupDto.chatId());
        int userId = getSenderId(authentication);
        return chatsService.updateGroup(updateGroupDto, userId);
    }

    @PostMapping("/{id}/leave")
    public void leaveGroup(@PathVariable("id") int chatId, Authentication authentication) {
        Integer userId = getSenderId(authentication);
        log.info("Leave group chat, actor id: {}", userId);
        participantsService.leaveGroup(chatId, userId);
    }

}
