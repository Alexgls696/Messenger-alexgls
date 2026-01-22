package com.alexgls.springboot.messagestorageservice.controller;

import com.alexgls.springboot.messagestorageservice.dto.ChatDto;
import com.alexgls.springboot.messagestorageservice.dto.CreateGroupDto;
import com.alexgls.springboot.messagestorageservice.dto.GroupAccessDto;
import com.alexgls.springboot.messagestorageservice.dto.UpdateGroupDto;
import com.alexgls.springboot.messagestorageservice.service.ChatsService;
import com.alexgls.springboot.messagestorageservice.service.ParticipantsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static com.alexgls.springboot.messagestorageservice.util.SecurityUtils.*;

@RestController
@RequestMapping("/api/chats/groups")
@RequiredArgsConstructor
@Slf4j
public class GroupsController {

    private final ChatsService chatsService;
    private final ParticipantsService participantsService;

    @PostMapping
    public Mono<ChatDto> createGroupChat(@RequestBody CreateGroupDto createGroupDto, Authentication authentication) {
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
    public Mono<GroupAccessDto> getUserRightsByGroupId(@PathVariable("id") int groupId, Authentication authentication) {
        log.info("Get user rights by group id: {}", groupId);
        Integer userId = getSenderId(authentication);
        return chatsService.getUserRightsByGroupId(groupId, userId);
    }

    @PostMapping("/update")
    public Mono<ChatDto> updateGroupChat(@Valid @RequestBody UpdateGroupDto updateGroupDto, Authentication authentication) {
        log.info("Update group chat, actor id: {}", updateGroupDto.chatId());
        int userId = getSenderId(authentication);
        return chatsService.updateGroup(updateGroupDto, userId);
    }

    @PostMapping("/{id}/leave")
    public Mono<Void> leaveGroup(@PathVariable("id") int chatId, Authentication authentication) {
        Integer userId = getSenderId(authentication);
        log.info("Leave group chat, actor id: {}", userId);
        return participantsService.leaveGroup(chatId, userId);
    }
}
