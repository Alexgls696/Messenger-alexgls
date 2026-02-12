package com.alexgls.springboot.messagestorageservicevt.service;

import com.alexgls.springboot.messagestorageservicevt.dto.ChatDto;
import com.alexgls.springboot.messagestorageservicevt.dto.ChatWithUnread;
import com.alexgls.springboot.messagestorageservicevt.dto.CreateGroupDto;
import com.alexgls.springboot.messagestorageservicevt.dto.UpdateGroupDto;
import com.alexgls.springboot.messagestorageservicevt.entity.Chat;
import com.alexgls.springboot.messagestorageservicevt.entity.ChatRole;
import com.alexgls.springboot.messagestorageservicevt.entity.Message;
import com.alexgls.springboot.messagestorageservicevt.entity.Participants;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchParticipantException;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchUsersChatException;
import com.alexgls.springboot.messagestorageservicevt.repository.ChatsRepository;
import com.alexgls.springboot.messagestorageservicevt.repository.DeletedMessagesRepository;
import com.alexgls.springboot.messagestorageservicevt.repository.MessagesRepository;
import com.alexgls.springboot.messagestorageservicevt.repository.ParticipantsRepository;
import com.alexgls.springboot.messagestorageservicevt.service.encryption.EncryptUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatsServiceTest {

    @InjectMocks
    private ChatsService chatsService;

    @Mock
    private ChatsRepository chatsRepository;

    @Mock
    private ParticipantsRepository participantsRepository;

    @Mock
    private MessagesRepository messagesRepository;

    @Mock
    private DeletedMessagesRepository deletedMessagesRepository;

    @Spy
    private EncryptUtils encryptUtils = new EncryptUtils("814c76c47c08276acca3e69371142996", "hmac");


    @Test
    void findOrCreatePrivateChatIfChatIsExistsTest() {
        //given
        int senderId = 2;
        int receiverId = 3;

        long existedChatIdValue = 1L;
        Optional<Long> existedChatId = Optional.of(existedChatIdValue);
        Timestamp timestamp = Timestamp.from(Instant.now());
        //when
        when(chatsRepository.findChatIdByParticipantsIdForPrivateChats(senderId, receiverId)).thenReturn(existedChatId)
                .thenReturn(existedChatId);
        when(chatsRepository.findById(existedChatIdValue))
                .thenReturn(Optional.of(new Chat(1,
                        "name",
                        "description",
                        true,
                        "GROUP",
                        timestamp,
                        timestamp,
                        null, null)));


        //then
        ChatDto expected = new ChatDto(1,
                "name",
                "description",
                true,
                "GROUP",
                timestamp,
                timestamp,
                null,
                null);

        assertEquals(expected, chatsService.findOrCreatePrivateChat(senderId, receiverId));
    }

    @Test
    void findOrCreatePrivateChatIfChatIsNotExistsTest() {
        // given
        int senderId = 2;
        int receiverId = 3;
        Optional<Long> existedChatId = Optional.empty();

        // Мы не можем угадать точное время, которое сгенерирует сервис,
        // поэтому для возвращаемого значения мока создадим фиксированное время.
        Timestamp mockTimestamp = Timestamp.from(Instant.now());

        // Этот объект вернет репозиторий, когда сервис попытается сохранить новый чат
        Chat savedChatFromDb = new Chat(1,
                null,
                null,
                false,
                "PRIVATE",
                mockTimestamp, // Важно: сервис вернет то, что отдал репозиторий
                mockTimestamp,
                null, null);

        // when
        // 1. Мокаем поиск ID (возвращаем пустоту, чтобы попасть в ветку else)
        when(chatsRepository.findChatIdByParticipantsIdForPrivateChats(senderId, receiverId))
                .thenReturn(existedChatId);

        // 2. Мокаем сохранение чата.
        // Используем any(Chat.class), потому что сервис создает новый объект Chat внутри себя.
        when(chatsRepository.save(any(Chat.class)))
                .thenReturn(savedChatFromDb);

        // 3. Мокаем сохранение участников.
        // Используем anyList(), так как список создается внутри сервиса.
        when(participantsRepository.saveAll(anyList()))
                .thenReturn(List.of()); // Возвращаемое значение тут особо не важно для результата метода

        // act
        ChatDto actualResult = chatsService.findOrCreatePrivateChat(senderId, receiverId);

        // then
        assertNotNull(actualResult);
        assertEquals(1, actualResult.getChatId()); // Проверяем, что ID подтянулся из savedChatFromDb
        assertEquals("PRIVATE", actualResult.getType());

        // Проверяем, что методы репозитория действительно вызывались
        verify(chatsRepository).save(any(Chat.class));
        verify(participantsRepository).saveAll(anyList());
    }

    @Test
    public void createGroup_ReturnsGroupDto() {
        //given
        int creatorId = 1;
        List<Integer> memberIds = List.of(2, 3); // Используем конкретные ID для проверки
        CreateGroupDto createGroupDto = new CreateGroupDto("Test group", "desc", memberIds);

        Chat savedChat = new Chat();
        savedChat.setId(10);
        savedChat.setName("Test group");
        savedChat.setGroup(true);

        //when

        when(chatsRepository.save(any(Chat.class)))
                .thenReturn(savedChat);
        when(participantsRepository.saveAll(anyList()))
                .thenReturn(List.of());

        ChatDto result = chatsService.createGroup(createGroupDto, creatorId);

        //then

        assertNotNull(result);
        assertEquals("Test group", result.getName());
        assertTrue(result.isGroup());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Participants>> captor = ArgumentCaptor.forClass(List.class);
        verify(participantsRepository).saveAll(captor.capture());

        List<Participants> savedParticipants = captor.getValue();
        assertEquals(3, savedParticipants.size());

        boolean creatorIsPresent = savedParticipants
                .stream()
                .anyMatch(p -> p.getUserId() == creatorId && p.getRole().equals(ChatRole.OWNER));
        assertTrue(creatorIsPresent, "Создатель чата должен быть добавлен в список участников с ролью OWNER");
    }

    @Test
    public void updateGroupTest_WhenParticipantNotExists() {
        //given
        int actorId = 1;
        UpdateGroupDto updateGroupDto = new UpdateGroupDto(1, "new name", "new description");
        //when

        when(participantsRepository.findByChatIdAndUserId(updateGroupDto.chatId(), actorId))
                .thenReturn(Optional.empty());

        //then

        assertThrows(NoSuchParticipantException.class, () -> chatsService.updateGroup(updateGroupDto, actorId));
    }

    @Test
    void updateGroupTest_WhenDontHavePermission() {

        //given
        int actorId = 1;
        UpdateGroupDto updateGroupDto = new UpdateGroupDto(1, "new name", "new description");
        Participants participants = new Participants();
        participants.setRole(ChatRole.MODERATOR);

        //when
        when(participantsRepository.findByChatIdAndUserId(updateGroupDto.chatId(), actorId))
                .thenReturn(Optional.of(participants));

        //then
        assertThrows(AccessDeniedException.class, () -> chatsService.updateGroup(updateGroupDto, actorId));
    }

    @Test
    void updateGroupTest_WhenChatNotExists() {
        int actorId = 1;
        UpdateGroupDto updateGroupDto = new UpdateGroupDto(1, "new name", "new description");
        Participants participants = new Participants();
        participants.setRole(ChatRole.ADMIN);

        //when
        when(participantsRepository.findByChatIdAndUserId(updateGroupDto.chatId(), actorId))
                .thenReturn(Optional.of(participants));

        when(chatsRepository.findById((long) updateGroupDto.chatId()))
                .thenReturn(Optional.empty());
        //then

        assertThrows(NoSuchUsersChatException.class, () -> chatsService.updateGroup(updateGroupDto, actorId));
    }

    @Test
    public void updateGroup_UpdatesChatAndReturnsDto_WhenUserIsAdmin() {
        // --- GIVEN ---
        int actorId = 1;
        int chatId = 10;
        UpdateGroupDto updateGroupDto = new UpdateGroupDto(chatId, "New Name", "New Desc");

        Participants participant = new Participants();
        participant.setRole(ChatRole.ADMIN);

        Chat oldChat = new Chat();
        oldChat.setId(chatId);
        oldChat.setName("Old Name");
        oldChat.setDescription("Old Desc");
        oldChat.setGroup(true);

        Chat savedChat = new Chat();
        savedChat.setId(chatId);
        savedChat.setName("New Name");
        savedChat.setDescription("New Desc");
        savedChat.setGroup(true);

        when(participantsRepository.findByChatIdAndUserId(chatId, actorId))
                .thenReturn(Optional.of(participant));

        when(chatsRepository.findById((long) chatId))
                .thenReturn(Optional.of(oldChat)); // Возвращаем СТАРЫЙ чат

        when(chatsRepository.save(any(Chat.class)))
                .thenReturn(savedChat); // Возвращаем обновленный (для маппера)

        // --- WHEN ---
        ChatDto result = chatsService.updateGroup(updateGroupDto, actorId);

        // --- THEN ---
        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals("New Desc", result.getDescription());

        ArgumentCaptor<Chat> chatCaptor = ArgumentCaptor.forClass(Chat.class);
        verify(chatsRepository).save(chatCaptor.capture());

        Chat capturedChat = chatCaptor.getValue();

        assertEquals("New Name", capturedChat.getName());
        assertEquals("New Desc", capturedChat.getDescription());

        assertEquals(chatId, capturedChat.getId());
    }

    @Test
    void findById_WhenChatNotExists() {
        //given
        long chatId = 1;
        int userId = 2;

        //when
        when(chatsRepository.findById(chatId))
                .thenReturn(Optional.empty());

        //then
        assertThrows(NoSuchUsersChatException.class, () -> chatsService.findById(chatId, userId));
    }

    @Test
    void findById_ShouldReturnChatWithDecryptedLastMessage() {
        //given
        long chatId = 1;
        int userId = 2;

        Chat chat = new Chat();
        chat.setId(chatId);
        Message message = new Message();
        message.setContent(encryptUtils.encrypt("message_content"));
        message.setChatId((int) chatId);

        //when
        when(chatsRepository.findById(chatId))
                .thenReturn(Optional.of(chat));

        when(messagesRepository.findLastMessageByChatIdAndUserId(chatId, userId))
                .thenReturn(Optional.of(message));

        var result = chatsService.findById(chatId, userId);
        //then

        assertNotNull(result);
        assertEquals(chatId, result.getChatId());
        assertNotNull(result.getLastMessage());
        assertEquals("message_content", result.getLastMessage().getContent());
        assertEquals(chat.getId(), result.getLastMessage().getChatId());
    }

    @Test
    void findById_ShouldReturnChatWithoutLastMessage() {
        //given
        long chatId = 1;
        int userId = 2;

        Chat chat = new Chat();
        chat.setId(chatId);
        //when

        when(chatsRepository.findById(chatId))
                .thenReturn(Optional.of(chat));

        when(messagesRepository.findLastMessageByChatIdAndUserId(chatId, userId))
                .thenReturn(Optional.empty());
        //then

        var result = chatsService.findById(chatId, userId);
        assertNotNull(result);
        assertEquals(chatId, result.getChatId());
    }

    @Test
    void deleteChatById_WhenParticipantsNotExists() {
        //given
        long chatId = 1;
        int userId = 2;

        //when
        when(participantsRepository.findByChatIdAndUserId(chatId, userId))
                .thenReturn(Optional.empty());
        //then

        assertThrows(NoSuchParticipantException.class, () -> chatsService.deleteChatById(chatId, userId));
    }

    @Test
    void deleteChatById_deleteChatById_WhenParticipantsExists() {
        //given
        long chatId = 1;
        int userId = 2;

        Participants participants = new Participants();
        participants.setId(10);

        Participants savedParticipants = new Participants();
        savedParticipants.setId(10);
        savedParticipants.setDeletedByUser(true);

        //when
        when(participantsRepository.findByChatIdAndUserId(chatId, userId))
                .thenReturn(Optional.of(participants));

        when(participantsRepository.save(participants))
                .thenReturn(savedParticipants);

        //then
        chatsService.deleteChatById(chatId, userId);

        ArgumentCaptor<Participants> argumentCaptor = ArgumentCaptor.forClass(Participants.class);
        verify(participantsRepository).save(argumentCaptor.capture());
        var capturedParticipants = argumentCaptor.getValue();

        assertTrue(capturedParticipants.isDeletedByUser());

        verify(deletedMessagesRepository).markAllMessagesAsRemovedWhenChatRemoving(chatId, userId);
    }

}