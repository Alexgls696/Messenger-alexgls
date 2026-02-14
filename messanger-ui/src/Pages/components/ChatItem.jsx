import React, { useState, useEffect, memo } from 'react';
import { apiFetch } from '../utils/apiClient';
import { formatDate } from '../utils/dateUtils';
import { imageLoader } from '../utils/imageLoader';
import defaultAvatar from '../images/profile-default.png'


// Используем memo, чтобы компонент не перерисовывался просто так
const ChatItem = memo(({ chat, isActive, onSelect, onContextMenu }) => {
    const [avatar, setAvatar] = useState(defaultAvatar);
    const [chatTitle, setChatTitle] = useState(chat.group ? chat.name : "...");

    useEffect(() => {
        let isMounted = true;

        const fetchDetails = async () => {
            try {
                let recipientId = null;

                // Загружаем имя, только если его еще нет (для приватных чатов)
                if (!chat.group) {
                    const recipient = await apiFetch(`/api/chats/find-recipient-by-private-chat-id/${chat.chatId}`);
                    if (isMounted) {
                        setChatTitle(`${recipient.name} ${recipient.surname || ''}`);
                        recipientId = recipient.id;
                    }
                }

                // Загружаем аватар
                if (recipientId || (chat.group && chat.avatarId)) {
                    const idToLoad = recipientId || chat.avatarId;
                    const avatarId = await apiFetch(`/api/profiles/images/user-avatar/${idToLoad}`);
                    if (avatarId && isMounted && avatarId > 0) {
                        const url = await imageLoader.getImageSrc(avatarId);
                        if (isMounted) setAvatar(url);
                    }
                }
            } catch (error) {
                console.warn("Error in ChatItem:", error);
            }
        };

        fetchDetails();

        return () => { isMounted = false; };
    }, [chat.chatId]);

    return (
        <li
            className={isActive ? 'active' : ''}
            onClick={() => onSelect(chat)}
            onContextMenu={(e) => {
                e.preventDefault();
                onContextMenu(e, chat.chatId);
            }}
        >
            <img className="chat-item-avatar" src={avatar} alt="" />
            <div className="chat-info">
                <div className="chat-title">{chatTitle}</div>
                <div className="last-message">
                    {chat.lastMessage ? (chat.lastMessage.content || 'Вложение') : 'Нет сообщений'}
                </div>
                <div className="message-time">
                    {chat.lastMessage ? formatDate(chat.lastMessage.createdAt) : ''}
                </div>
            </div>
            {chat.numberOfUnreadMessages > 0 && (
                <div className="unread-badge">{chat.numberOfUnreadMessages}</div>
            )}
        </li>
    );
}, (prevProps, nextProps) => {
    // Кастомная проверка для React.memo:
    // Перерисовывать только если изменился статус активности, 
    // текст последнего сообщения или счетчик непрочитанных.
    return (
        prevProps.isActive === nextProps.isActive &&
        prevProps.chat.lastMessage?.id === nextProps.chat.lastMessage?.id &&
        prevProps.chat.numberOfUnreadMessages === nextProps.chat.numberOfUnreadMessages
    );
});

export default ChatItem;