package com.alexgls.springboot.messagestorageservicevt.controller;

import com.alexgls.springboot.messagestorageservicevt.dto.chats.*;
import com.alexgls.springboot.messagestorageservicevt.service.ChatsService;
import com.alexgls.springboot.messagestorageservicevt.service.ParticipantsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import com.alexgls.springboot.messagestorageservicevt.util.SecurityUtils;

@RestController
@RequestMapping("/api/chats/groups")
@RequiredArgsConstructor
@Slf4j
public class GroupsController {

    private final ChatsService chatsService;
    private final ParticipantsService participantsService;

    @PostMapping
    public ChatDto createGroupChat(@RequestBody CreateGroupDto createGroupDto, Authentication authentication) {
        Integer id = SecurityUtils.getSenderId(authentication);
        String token = SecurityUtils.getToken(authentication);
        log.info("Creating group chat, creator id: : {}", id);
        return chatsService.createGroup(createGroupDto, id, token);
    }

    @PostMapping("/add-participants")
    public void addParticipants(@RequestBody AddParticipantsToGroupDto addParticipantsToGroupDto, Authentication authentication) {
        log.info("Adding participants ids: {}", addParticipantsToGroupDto);
        Integer userId = SecurityUtils.getSenderId(authentication);
        String token = SecurityUtils.getToken(authentication);
        participantsService.addParticipantsToGroup(addParticipantsToGroupDto, userId, token);
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
        Integer userId = SecurityUtils.getSenderId(authentication);
        return chatsService.getUserRightsByGroupId(groupId, userId);
    }

    @PostMapping("/update")
    public ChatDto updateGroupChat(@Valid @RequestBody UpdateGroupDto updateGroupDto, Authentication authentication) {
        log.info("Update group chat, actor id: {}", updateGroupDto.chatId());
        int userId = SecurityUtils.getSenderId(authentication);
        return chatsService.updateGroup(updateGroupDto, userId);
    }

    @PostMapping("/{id}/leave")
    public void leaveGroup(@PathVariable("id") int chatId, Authentication authentication) {
        Integer userId = SecurityUtils.getSenderId(authentication);
        log.info("Leave group chat, actor id: {}", userId);
        participantsService.leaveGroup(chatId, userId);
    }


}
