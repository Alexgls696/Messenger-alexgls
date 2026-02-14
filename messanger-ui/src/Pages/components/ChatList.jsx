import React, { useState, useEffect, useCallback, useImperativeHandle, forwardRef } from 'react';
import ChatItem from './ChatItem';
import { apiFetch } from '../utils/apiClient';

import '../Styles/Chats.css'

// Используем forwardRef, чтобы родитель мог получить доступ к методам компонента
const ChatList = forwardRef(({ activeChatId, onChatSelect, onContextMenu }, ref) => {
    const [chats, setChats] = useState([]);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [isLoading, setIsLoading] = useState(false);

    const fetchAndAddSingleChat = async (chatId) => {
        try {
            const fullChatDto = await apiFetch(`/api/chats/${chatId}`);
            setChats(prev => {
                if (prev.some(c => c.chatId === chatId)) return prev;
                return [fullChatDto, ...prev];
            });
        } catch (error) {
            console.error("Не удалось подгрузить данные нового чата:", error);
        }
    };

    // Экспортируем метод prependChat для родителя (ChatPage)
    useImperativeHandle(ref, () => ({
        // Метод для обновления метаданных чата при новом сообщении
        removeChatFromList(chatId) {
            setChats(prev => prev.filter(c => c.chatId !== chatId));
        },
        updateChatFromSocket(newMsg, isCurrentActive) {
            let chatExists = false;

            setChats(prev => {
                const chatIdx = prev.findIndex(c => c.chatId === newMsg.chatId);

                if (chatIdx > -1) {
                    chatExists = true;
                    const updatedChats = [...prev];
                    const targetChat = { ...updatedChats[chatIdx] };

                    targetChat.lastMessage = newMsg;

                    // Увеличиваем счетчик, если чат не активен и сообщение не наше
                    if (!isCurrentActive) {
                        targetChat.numberOfUnreadMessages = (targetChat.numberOfUnreadMessages || 0) + 1;
                    }

                    updatedChats.splice(chatIdx, 1);
                    return [targetChat, ...updatedChats];
                }

                chatExists = false;
                return prev;
            });

            if (!chatExists) {
                fetchAndAddSingleChat(newMsg.chatId);
            }
        },

        decrementBadge(chatId, amount) {
            setChats(prev => prev.map(c => {
                if (c.chatId === chatId) {
                    const newCount = Math.max(0, (c.numberOfUnreadMessages || 0) - amount);
                    return { ...c, numberOfUnreadMessages: newCount };
                }
                return c;
            }));
        },

        // Метод прочтения (сброс бейджа)
        resetBadge(chatId) {
            setChats(prev => prev.map(c =>
                c.chatId === chatId ? { ...c, numberOfUnreadMessages: 0 } : c
            ));
        },
        prependChat(newChat) {
            setChats(prev => {
                const exists = prev.some(c => c.chatId === newChat.chatId);
                if (exists) {
                    return [newChat, ...prev.filter(c => c.chatId !== newChat.chatId)];
                }
                return [newChat, ...prev];
            });
        }
    }));



    const loadChats = useCallback(async () => {
        if (isLoading || !hasMore) return;

        setIsLoading(true);
        try {
            const data = await apiFetch(`/api/chats/find-by-id/${page}`);

            if (Array.isArray(data) && data.length > 0) {
                setChats(prev => [...prev, ...data]);
                setPage(prev => prev + 1);
            } else {
                setHasMore(false);
            }
        } catch (error) {
            console.error("Ошибка загрузки чатов", error);
        } finally {
            setIsLoading(false);
        }
    }, [page, isLoading, hasMore]);

    useEffect(() => {
        loadChats();
    }, []);

    const handleScroll = (e) => {
        const { scrollTop, scrollHeight, clientHeight } = e.currentTarget;
        // Подгружаем, когда осталось 100px до конца
        if (scrollHeight - scrollTop <= clientHeight + 100) {
            loadChats();
        }
    };

    return (
        <ul className="chat-list" onScroll={handleScroll}>
            {chats.length === 0 && !isLoading && (
                <p className="placeholder">Чаты не найдены</p>
            )}

            {chats.map(chat => (
                <ChatItem
                    key={chat.chatId}
                    chat={chat}
                    isActive={chat.chatId === activeChatId}
                    onSelect={onChatSelect}
                    onContextMenu={onContextMenu}
                />
            ))}

            {isLoading && <p className="status">Загрузка...</p>}
        </ul>
    );
});

export default ChatList;