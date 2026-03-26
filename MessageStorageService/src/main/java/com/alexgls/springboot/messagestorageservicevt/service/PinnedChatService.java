package com.alexgls.springboot.messagestorageservicevt.service;

import com.alexgls.springboot.messagestorageservicevt.dto.chats.ChatDto;
import com.alexgls.springboot.messagestorageservicevt.entity.Participants;
import com.alexgls.springboot.messagestorageservicevt.entity.PinnedChat;
import com.alexgls.springboot.messagestorageservicevt.entity.PinnedChatId;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchParticipantException;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchPinnedChatException;
import com.alexgls.springboot.messagestorageservicevt.exceptions.NoSuchUsersChatException;
import com.alexgls.springboot.messagestorageservicevt.repository.ChatsRepository;
import com.alexgls.springboot.messagestorageservicevt.repository.ParticipantsRepository;
import com.alexgls.springboot.messagestorageservicevt.repository.PinnedChatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PinnedChatService {

    private final ChatsRepository chatsRepository;

    private final ChatsService chatsService;

    private final ParticipantsRepository participantsRepository;

    private final PinnedChatsRepository pinnedChatsRepository;

    @Transactional
    public void savePinnedChat(long chatId, int userId) {
        boolean exists = chatsRepository.existsById(chatId);
        if (!exists) {
            throw new NoSuchUsersChatException("Чат не найден.");
        }
        Participants participants = participantsRepository.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> new NoSuchParticipantException("Вы не состоите в это чате."));

        PinnedChat pinnedChat = new PinnedChat(new PinnedChatId(userId, chatId));
        pinnedChatsRepository.save(pinnedChat);
    }

    @Transactional
    public ChatDto deletePinnedChat(long chatId, long userId, String token) {
        boolean exists = pinnedChatsRepository.existsById_ChatIdAndId_UserId(chatId, userId);
        if (!exists) {
            throw new NoSuchPinnedChatException("Закрепленный чат не найден");
        }
        pinnedChatsRepository.deleteById_ChatIdAndId_UserId(chatId, userId);
        return chatsService.findChatById(chatId, (int) userId, token);
    }

    public List<PinnedChat> getPinnedChatsByUserId(long userId) {
        return pinnedChatsRepository.findAllById_UserId(userId);
    }

}
