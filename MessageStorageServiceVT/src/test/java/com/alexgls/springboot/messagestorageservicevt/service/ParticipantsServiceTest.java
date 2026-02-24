package com.alexgls.springboot.messagestorageservicevt.service;

import com.alexgls.springboot.messagestorageservicevt.client.AuthRestClient;
import com.alexgls.springboot.messagestorageservicevt.dto.GetUserDto;
import com.alexgls.springboot.messagestorageservicevt.dto.messages.MessageDto;
import com.alexgls.springboot.messagestorageservicevt.entity.Chat;
import com.alexgls.springboot.messagestorageservicevt.entity.ChatRole;
import com.alexgls.springboot.messagestorageservicevt.entity.Participants;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchParticipantException;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchUserException;
import com.alexgls.springboot.messagestorageservicevt.repository.ParticipantsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipantsServiceTest {

    @InjectMocks
    private ParticipantsService participantsService;

    @Mock
    private ParticipantsRepository participantsRepository;

    @Mock
    private MessagesService messagesService;

    @Mock
    private KafkaSenderService kafkaSenderService;

    @Mock
    private AuthRestClient authRestClient;

    public static List<Participants> getTestParticipants() {
        Chat chat1 = new Chat();
        chat1.setId(1L);
        chat1.setName("Test Chat 1");

        Chat chat2 = new Chat();
        chat2.setId(2L);
        chat2.setName("Test Chat 2");

        return List.of(
                // Участник 1 - Администратор
                new Participants(
                        1L,  // id
                        chat1,  // chat
                        101,  // userId
                        Timestamp.valueOf(LocalDateTime.now().minusDays(5)),  // joinedAt
                        false,  // isDeletedByUser
                        50L,  // lastReadMessageId
                        2,  // unreadCount
                        ChatRole.ADMIN,  // role
                        false,  // leave
                        false,  // removed
                        null  // removeAt
                ),

                // Участник 2 - Обычный пользователь
                new Participants(
                        2L,
                        chat1,
                        102,
                        Timestamp.valueOf(LocalDateTime.now().minusDays(3)),
                        false,
                        45L,
                        5,
                        ChatRole.MEMBER,
                        false,
                        false,
                        null
                ),

                // Участник 3 - Покинул чат
                new Participants(
                        3L,
                        chat1,
                        2,
                        Timestamp.valueOf(LocalDateTime.now().minusDays(10)),
                        false,
                        30L,
                        0,
                        ChatRole.OWNER,
                        true,  // leave = true
                        false,
                        null
                ),

                // Участник 4 - Удалён из чата
                new Participants(
                        4L,
                        chat2,
                        104,
                        Timestamp.valueOf(LocalDateTime.now().minusDays(7)),
                        false,
                        20L,
                        0,
                        ChatRole.MODERATOR,
                        false,
                        true,  // removed = true
                        Timestamp.valueOf(LocalDateTime.now().minusDays(1))  // removeAt
                )
        );
    }

    public static List<GetUserDto> getTestGetUserDto() {
        return List.of(new GetUserDto(101, "ivan", "vallenok", "ivanss", "MEMBER"),
                new GetUserDto(102, "alex", "glualex", "alexgls", "MEMBER"),
                new GetUserDto(2, "marina", "lutaya", "marinaluts", "MEMBER"),
                new GetUserDto(104, "nicolay", "smirnov", "nikonov", "MEMBER"));
    }

    public static List<GetUserDto> getTestSortedUsersDto() {
        return List.of(new GetUserDto(2, "marina", "lutaya", "marinaluts", "Создатель"),
                new GetUserDto(102, "alex", "glualex", "alexgls", "Участник"),
                new GetUserDto(101, "ivan", "vallenok", "ivanss", "Администратор"),
                new GetUserDto(104, "nicolay", "smirnov", "nikonov", "Модератор"));
    }

    @Test
    void findAllByChatId_WhenParticipantsISEmpty() {
        //given
        int chatId = 1;
        String authToken = "token";
        int user = 2;
        List<GetUserDto> expectedList = Collections.emptyList();
        //when
        when(participantsRepository.findAllByChatId(chatId))
                .thenReturn(Collections.emptyList());

        var result = participantsService.findAllByChatId(chatId, authToken, user);
        //then

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findAllByChatId_WhenParticipantsIsNotEmptyAndCurrentUsersNotExistsInParticipants() {
        //given
        int chatId = 1;
        String authToken = "token";
        int user = 5;
        List<Participants> participants = getTestParticipants();
        List<GetUserDto> unsortedUsers = getTestGetUserDto();

        List<GetUserDto> sortedUsers = getTestSortedUsersDto()
                .stream()
                .sorted(Comparator.comparing(GetUserDto::name))
                .toList();

        //when
        when(participantsRepository.findAllByChatId(chatId))
                .thenReturn(participants);

        when(authRestClient.findAllUsers(any(), eq(authToken)))
                .thenReturn(unsortedUsers);

        var result = participantsService.findAllByChatId(chatId, authToken, user);
        //then

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(result.size(), sortedUsers.size());

        assertEquals(sortedUsers.size(), result.size());

        sortedUsers = sortedUsers
                .stream()
                .sorted(Comparator.comparing(GetUserDto::name))
                .toList();

        for (int i = 0; i < sortedUsers.size(); i++) {
            assertEquals(sortedUsers.get(i), result.get(i));
        }

        verify(authRestClient).findAllUsers(any(), eq(authToken));
    }

    @Test
    void findAllByChatId_WhenParticipantsIsNotEmpty() {
        //given
        int chatId = 1;
        String authToken = "token";
        int user = 2;
        List<Participants> participants = getTestParticipants();
        List<GetUserDto> unsortedUsers = getTestGetUserDto();

        var sortedUsersExpectedList = getTestSortedUsersDto();

        //when
        when(participantsRepository.findAllByChatId(chatId))
                .thenReturn(participants);
        when(authRestClient.findAllUsers(any(), eq(authToken)))
                .thenReturn(unsortedUsers);


        var result = participantsService.findAllByChatId(chatId, authToken, user);

        //then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(sortedUsersExpectedList.size(), result.size());

        for (int i = 0; i < sortedUsersExpectedList.size(); i++) {
            assertEquals(sortedUsersExpectedList.get(i), result.get(i));
        }

    }

    @Test
    void findUserIdsByChatId() {
        //given
        int chatId = 1;
        //when

        when(participantsRepository.findUserIdsByChatId(chatId))
                .thenReturn(List.of(1, 2, 3));

        //then
        var result = participantsService.findUserIdsByChatId(chatId);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(List.of(1, 2, 3).containsAll(result));
    }

    @Test
    void deleteParticipantFromGroup_WhenActorEqualsRemovingUsers_ShouldThrowException() {
        //given
        int chatId = 1;
        int removingUser = 3;
        int actorId = 3;
        String authToken = "token";
        //when

        //then
        assertThrows(AccessDeniedException.class, () -> participantsService.deleteParticipantFromGroup(chatId, removingUser, actorId, authToken));
    }

    @Test
    void deleteParticipantFromGroup_WhenActorIsModerator_ShouldThrowException() {
        //given
        int chatId = 1;
        int removingUser = 3;
        int actorId = 2;
        String authToken = "token";

        Timestamp timestamp = Timestamp.from(Instant.now());
        Participants participants = new Participants(1, null, actorId, timestamp, false, 1L,
                0, ChatRole.MODERATOR, false, false, null);

        //when
        when(participantsRepository.findByChatIdAndUserId(chatId, actorId))
                .thenReturn(Optional.of(participants));
        //then
        assertThrows(AccessDeniedException.class, () -> participantsService.deleteParticipantFromGroup(chatId, removingUser, actorId, authToken));
    }

    @Test
    void deleteParticipantFromGroup_WhenActorIsMember_ShouldThrowException() {
        //given
        int chatId = 1;
        int removingUser = 3;
        int actorId = 2;
        String authToken = "token";

        Timestamp timestamp = Timestamp.from(Instant.now());
        Participants participants = new Participants(1, null, actorId, timestamp, false, 1L,
                0, ChatRole.MEMBER, false, false, null);

        //when
        when(participantsRepository.findByChatIdAndUserId(chatId, actorId))
                .thenReturn(Optional.of(participants));
        //then
        assertThrows(AccessDeniedException.class, () -> participantsService.deleteParticipantFromGroup(chatId, removingUser, actorId, authToken));
    }

    @Test
    void deleteParticipantFromGroup_WhenParticipantsNotFound_ShouldThrowException() {
        //given
        int chatId = 1;
        int removingUser = 3;
        int actorId = 2;
        String authToken = "token";

        //when
        when(participantsRepository.findByChatIdAndUserId(chatId, actorId))
                .thenReturn(Optional.empty());

        //then
        assertThrows(NoSuchParticipantException.class, () -> participantsService.deleteParticipantFromGroup(chatId, removingUser, actorId, authToken));
    }

    @Test
    void deleteParticipantFromGroup_WhenActorIsOwner_ShouldBeSuccess() {
        //given
        int chatId = 1;
        int removingUser = 3;
        int actorId = 2;
        String authToken = "token";

        Timestamp timestamp = Timestamp.from(Instant.now());
        Participants participants = new Participants(1, null, actorId, timestamp, false, 1L,
                0, ChatRole.OWNER, false, false, null);

        Participants removingUserParticipants = new Participants(2, null, removingUser, timestamp, false, 1L,
                0, ChatRole.MEMBER, false, false, null);

        //when
        when(participantsRepository.findByChatIdAndUserId(chatId, actorId))
                .thenReturn(Optional.of(participants));

        when(messagesService.saveServiceMessage(any(), eq(chatId), eq(actorId)))
                .thenReturn(new MessageDto());

        when(participantsRepository.findByChatIdAndUserId(chatId, removingUser))
                .thenReturn(Optional.of(removingUserParticipants));

        when(authRestClient.findUserById(removingUser, authToken))
                .thenReturn(new GetUserDto(removingUser, "name", "surname", "username", "role"));

        when(authRestClient.findUserById(actorId, authToken))
                .thenReturn(new GetUserDto(actorId, "name", "surname", "username", "role"));


        participantsService.deleteParticipantFromGroup(chatId, removingUser, actorId, authToken);

        //then

        verify(participantsRepository).findByChatIdAndUserId(chatId, actorId);
        verify(messagesService).saveServiceMessage(any(), eq(chatId), eq(actorId));
        verify(participantsRepository).findByChatIdAndUserId(chatId, removingUser);
        verify(participantsRepository).removingUserFromGroupByChatIdAndUserId(chatId, removingUser, Timestamp.from(Instant.now()));
        verify(kafkaSenderService).sendMessage(any(MessageDto.class));
    }

    @Test
    void deleteParticipantFromGroup_WhenActorIsAdmin_ShouldBeSuccess() {
        //given
        int chatId = 1;
        int removingUser = 3;
        int actorId = 2;
        String authToken = "token";

        Timestamp timestamp = Timestamp.from(Instant.now());
        Participants participants = new Participants(1, null, actorId, timestamp, false, 1L,
                0, ChatRole.ADMIN, false, false, null);

        Participants removingUserParticipants = new Participants(2, null, removingUser, timestamp, false, 1L,
                0, ChatRole.MEMBER, false, false, null);

        //when
        when(participantsRepository.findByChatIdAndUserId(chatId, actorId))
                .thenReturn(Optional.of(participants));

        when(messagesService.saveServiceMessage(any(), eq(chatId), eq(actorId)))
                .thenReturn(new MessageDto());

        when(participantsRepository.findByChatIdAndUserId(chatId, removingUser))
                .thenReturn(Optional.of(removingUserParticipants));

        when(authRestClient.findUserById(removingUser, authToken))
                .thenReturn(new GetUserDto(removingUser, "name", "surname", "username", "role"));

        when(authRestClient.findUserById(actorId, authToken))
                .thenReturn(new GetUserDto(actorId, "name", "surname", "username", "role"));


        participantsService.deleteParticipantFromGroup(chatId, removingUser, actorId, authToken);

        //then

        verify(participantsRepository).findByChatIdAndUserId(chatId, actorId);
        verify(messagesService).saveServiceMessage(any(), eq(chatId), eq(actorId));
        verify(participantsRepository).findByChatIdAndUserId(chatId, removingUser);
        verify(participantsRepository).removingUserFromGroupByChatIdAndUserId(chatId, removingUser,  Timestamp.from(Instant.now()));
        verify(kafkaSenderService).sendMessage(any(MessageDto.class));
    }

    @Test
    void generateRemovingMessageContent_WhenRemovingUserIsNull() {
        //given
        int removingUserId = 3;
        int actorId = 2;
        String token = "token";
        GetUserDto actorUseDto = GetUserDto
                .builder()
                .id(2)
                .name("alex")
                .surname("glu")
                .username("alexgls")
                .build();

        //when

        when(authRestClient.findUserById(removingUserId, token))
                .thenReturn(null);
        when(authRestClient.findUserById(actorId, token))
                .thenReturn(actorUseDto);
        //then

        assertThrows(NoSuchUserException.class, () -> participantsService.generateRemovingMessageContent(removingUserId, actorId, token));
    }

    @Test
    void generateRemovingMessageContent_WhenActorUserIsNull() {
        //given
        int removingUserId = 3;
        int actorId = 2;
        String token = "token";
        GetUserDto removingUser = GetUserDto
                .builder()
                .id(3)
                .name("alex")
                .surname("glu")
                .username("alexgls")
                .build();

        //when

        when(authRestClient.findUserById(removingUserId, token))
                .thenReturn(removingUser);
        when(authRestClient.findUserById(actorId, token))
                .thenReturn(null);
        //then
        assertThrows(NoSuchUserException.class, () -> participantsService.generateRemovingMessageContent(removingUserId, actorId, token));
    }

    @Test
    void leaveGroup_WhenParticipantsIsNotExists_ShouldThrowsException() {
        //given
        int chatId = 1;
        int userId = 2;

        //when
        when(participantsRepository.existsByChatIdAndUserId(chatId, userId))
                .thenReturn(false);
        //then

        assertThrows(NoSuchParticipantException.class, () -> participantsService.leaveGroup(chatId, userId), "Если участник чата не найден - выброшено исключение");
    }

    @Test
    void leaveGroup_WhenParticipantsIsExists_ShouldBeSuccess() {
        //given
        long chatId = 1L;
        int userId = 2;
        //when
        when(participantsRepository.existsByChatIdAndUserId(chatId, userId))
                .thenReturn(true);
        //then
        participantsService.leaveGroup(chatId, userId);

        ArgumentCaptor<Long> chatIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> userIdCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(participantsRepository).leavingFromGroupByChatIdAndUserId(chatIdCaptor.capture(), userIdCaptor.capture());

        assertEquals(chatId, chatIdCaptor.getValue());
        assertEquals(userId, userIdCaptor.getValue());
    }


}