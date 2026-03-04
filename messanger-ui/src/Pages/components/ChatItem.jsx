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
            className={`${isActive ? 'active' : ''} ${chat.pinned ? 'pinned' : ''}`}
            onClick={() => onSelect(chat)}
            onContextMenu={(e) => {
                e.preventDefault();
                onContextMenu(e, chat);
            }}
        >
            <img className="chat-item-avatar" src={avatar} alt="" />

            <div className="chat-info">
                <div className="chat-info-header">
                    <div className="chat-title">{chatTitle}</div>
                    <div className="message-time">
                        {chat.lastMessage ? formatDate(chat.lastMessage.createdAt) : ''}
                    </div>
                </div>

                <div className="chat-info-footer">
                    <div className="last-message">
                        {chat.lastMessage ? (chat.lastMessage.content || 'Вложение') : 'Нет сообщений'}
                    </div>

                    <div className="chat-status-icons">
                        {chat.pinned && (
                            <div className="pin-icon">
                                <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                                    <path d="M16,12V4H17V2H7V4H8V12L6,14V16H11.2V22H12.8V16H18V14L16,12Z" />
                                </svg>
                            </div>
                        )}
                        {chat.numberOfUnreadMessages > 0 && (
                            <div className="unread-badge">{chat.numberOfUnreadMessages}</div>
                        )}
                    </div>
                </div>
            </div>
        </li>
    );
}, (prevProps, nextProps) => {
    return (
        prevProps.isActive === nextProps.isActive &&
        prevProps.chat.pinned === nextProps.chat.pinned && 
        prevProps.chat.lastMessage?.id === nextProps.chat.lastMessage?.id &&
        prevProps.chat.numberOfUnreadMessages === nextProps.chat.numberOfUnreadMessages &&
        prevProps.chat.updatedAt === nextProps.chat.updatedAt
    );
});

export default ChatItem;