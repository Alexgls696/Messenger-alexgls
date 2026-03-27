import React, { useState, useEffect, useCallback, useImperativeHandle, forwardRef } from 'react';
import ChatItem from './ChatItem';
import { apiFetch } from '../utils/apiClient';

import '../Styles/Chats.css'

const ChatList = forwardRef(({ activeChatId, onChatSelect, onContextMenu, currentUserId }, ref) => {
    const [chats, setChats] = useState([]);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [isLoading, setIsLoading] = useState(false);

    const fetchAndAddSingleChat = async (msg) => {
        try {
            const chatId = msg.chatId;
            const fullChatDto = await apiFetch(`/api/chats/${chatId}`);
            setChats(prev => {
                if (prev.some(c => c.chatId === chatId)) return prev;

                const pinnedChats = prev.filter(c => c.pinned);
                const regularChats = prev.filter(c => !c.pinned);
                if (msg.senderId !== currentUserId) {
                    fullChatDto.numberOfUnreadMessages = (fullChatDto.numberOfUnreadMessages || 0) + 1;
                }
                if (fullChatDto.pinned) {
                    return [fullChatDto, ...pinnedChats, ...regularChats];
                } else {
                    return [...pinnedChats, fullChatDto, ...regularChats];
                }
            });
        } catch (error) {
            console.error("Не удалось подгрузить данные нового чата:", error);
        }
    };

    useImperativeHandle(ref, () => ({
        removeChatFromList(chatId) {
            setChats(prev => prev.filter(c => c.chatId !== chatId));
        },
        updateChatFromSocket(newMsg, isCurrentActive) {
            setChats(prev => {
                const chatIdx = prev.findIndex(c => String(c.chatId) === String(newMsg.chatId));

                if (chatIdx > -1) {
                    const targetChat = { ...prev[chatIdx] };
                    targetChat.lastMessage = newMsg;
                    targetChat.updatedAt = newMsg.createdAt;

                    if (!isCurrentActive) {
                        targetChat.numberOfUnreadMessages = (targetChat.numberOfUnreadMessages || 0) + 1;
                    }

                    const filtered = prev.filter(c => String(c.chatId) !== String(newMsg.chatId));
                    const pinned = filtered.filter(c => c.pinned);
                    const regular = filtered.filter(c => !c.pinned);

                    return targetChat.pinned ? [targetChat, ...pinned, ...regular] : [...pinned, targetChat, ...regular];
                } else {
                    setTimeout(() => {
                        fetchAndAddSingleChat(newMsg);
                    }, 0);
                    return prev;
                }
            });
        },
        updateChatPinStatus: (updatedChat) => {
            setChats(prevChats => {
                const newChats = prevChats.map(c =>
                    c.chatId === updatedChat.chatId ? { ...c, pinned: updatedChat.pinned } : c
                );

                return newChats.sort((a, b) => {
                    if (a.pinned !== b.pinned) {
                        return a.pinned ? -1 : 1;
                    }
                    const dateA = new Date(a.updatedAt || 0);
                    const dateB = new Date(b.updatedAt || 0);
                    return dateB - dateA;
                });
            });
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
        },
        updateUserOnlineStatus: (userId, online) => {
            setChats(prev => prev.map(chat => {
                if (!chat.group && chat.recipientId === userId) {
                    return { ...chat, isOnline: online };
                }
                return chat;
            }));
        }
    }));

    const loadChats = useCallback(async () => {
        if (isLoading || !hasMore) return;

        setIsLoading(true);
        try {
            const data = await apiFetch(`/api/chats/find-all/${page}`);

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