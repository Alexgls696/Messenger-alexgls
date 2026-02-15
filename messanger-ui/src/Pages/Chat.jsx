import React, { useState, useRef, useEffect, useCallback, useMemo} from 'react';
import { useNavigate } from 'react-router-dom';
import Header from './components/Header';
import ChatList from './components/ChatList';
import ChatWindow from './components/ChatWindow';
import { apiFetch, logout } from './utils/apiClient';
import { imageLoader } from './utils/imageLoader';
import photoViewer from './utils/photoViewer';
import UserProfileModal from './components/UserProfileModal';
import GroupProfileModal from './components/GroupProfileModal';
import MyProfileManager from './components/MyProfileManager';
import ChatSearchModal from './components/ChatSearchModal';
import UserSearchModal from './components/UserSearchModal';
import CreateGroupModal from './components/CreateGroupModal';
import ConfirmationModal from './components/ConfirmationModal';

import { useChatWebSocket } from '../hooks/useChatWebSocket';

import ContextMenu from './components/ContextMenu';


import useSound from 'use-sound';
import messageSound from '../sound/message.mp3'

import './Styles/Messages.css'
import './Styles/PhotoViewer.css'
import './Styles/MyProfile.css'
import './Styles/UsersProfile.css'
import './Styles/Group.css'
import './Styles/SearchModal.css'
import './Styles/auth.css';

const API_BASE_URL = `https://${window.location.hostname}:8080`;

function ChatPage() {
    const navigate = useNavigate();

    // --- Состояние ---
    const [profile, setProfile] = useState(null);
    const [user, setUser] = useState(null);
    const [activeChat, setActiveChat] = useState(null);
    const [participantCache, setParticipantCache] = useState({});

    // Состояния модальных окон
    const [isProfileOpen, setIsProfileOpen] = useState(false);
    const [selectedUserProfile, setSelectedUserProfile] = useState(null);
    const [isGroupProfileOpen, setIsGroupProfileOpen] = useState(false);
    const [isSearchOpen, setIsSearchOpen] = useState(false);
    const [isUserSearchOpen, setIsUserSearchOpen] = useState(false);
    const [isCreateGroupOpen, setIsCreateGroupOpen] = useState(false);
    const [contextMenu, setContextMenu] = useState(null); // { x, y, options }
    const [confirmConfig, setConfirmConfig] = useState({ isOpen: false, message: '', onConfirm: null });

    // События WebSocket
    const [socketUpdate, setSocketUpdate] = useState(null);
    const [readEvent, setReadEvent] = useState(null);
    const [deleteEvent, setDeleteEvent] = useState(null);

    const chatListRef = useRef();
    const activeChatRef = useRef(null);


     const [playNotification] = useSound(messageSound, { 
        volume: 0.5, // Громкость от 0 до 1
    });
    
    useEffect(() => {
        activeChatRef.current = activeChat;
    }, [activeChat]);

    // --- Обработчики WebSocket ---
    const onMessageReceived = useCallback((newMsg) => {
        const curActive = activeChatRef.current;
        const isMsgForActive = curActive?.chatId === newMsg.chatId;


         if (newMsg.senderId !== user.id) {
            playNotification();
        }

        // 1. Обновляем список чатов (бейджи, последнее сообщение)
        chatListRef.current?.updateChatFromSocket(newMsg, isMsgForActive || newMsg.senderId === user?.id);

        // 2. Если чат открыт — отправляем в ChatWindow (добавляем метку времени для срабатывания useEffect)
        if (isMsgForActive) {
            setSocketUpdate({ ...newMsg, _ts: Date.now() });
        }
    }, [user, playNotification]);

    const onDeleteEvent = useCallback((info) => {
        setDeleteEvent({ ...info, _ts: Date.now() });
    }, []);


    const { sendMessage } = useChatWebSocket(
        API_BASE_URL,
        onMessageReceived,
        (info) => setReadEvent(info),
        onDeleteEvent
    );

    // --- Логика прочтения ---
    const markMessagesAsRead = useCallback(async (messagesToRead) => {
        if (!messagesToRead.length) return;

        const chatId = messagesToRead[0].chatId;
        chatListRef.current?.decrementBadge(chatId, messagesToRead.length);

        const payload = messagesToRead.map(m => ({
            messageId: m.messageId,
            senderId: m.senderId,
            chatId: m.chatId
        }));

        try {
            await apiFetch('/api/messages/read-messages', {
                method: 'POST',
                body: JSON.stringify(payload)
            });
        } catch (e) { console.error("Ошибка при отправке прочтения", e); }
    }, []);

    const messageReadObserver = useMemo(() => new IntersectionObserver((entries) => {
        const toRead = entries
            .filter(e => e.isIntersecting)
            .map(e => {
                messageReadObserver.unobserve(e.target);
                return {
                    messageId: parseInt(e.target.dataset.messageId),
                    senderId: parseInt(e.target.dataset.senderId),
                    chatId: activeChatRef.current?.chatId // Берем из рефа
                };
            })
            .filter(m => m.chatId); // Убеждаемся, что chatId есть

        if (toRead.length > 0) markMessagesAsRead(toRead);
    }, { threshold: 0.5 }), [markMessagesAsRead]);

    // --- Инициализация данных ---
    useEffect(() => {
        imageLoader.init(API_BASE_URL);
        photoViewer.init({ apiBaseUrl: API_BASE_URL });

        apiFetch('/api/users/me')
            .then(data => {
                setUser(data);
                setParticipantCache(prev => ({ ...prev, [data.id]: `${data.name} ${data.surname || ''}` }));
                return apiFetch(`/api/profiles/${data.id}`);
            })
            .then(profData => setProfile(profData))
            .catch(err => {
                console.error("Ошибка инициализации", err);
                logout();
            });
    }, []);

    const refreshUserData = async () => {
        try {
            const me = await apiFetch('/api/users/me');
            setUser(me);
            const profileData = await apiFetch(`/api/profiles/${me.id}`);
            setProfile(profileData);
        } catch (error) { console.error("Ошибка обновления данных:", error); }
    };

    // --- Вспомогательные функции ---
    const handleChatSelect = (chat) => {
        setActiveChat(chat);
        setSocketUpdate(null); // Сбрасываем старые обновления
        setIsGroupProfileOpen(false);
        document.body.classList.add('chat-active');
    };

    const handleBackToList = () => {
        setActiveChat(null);
        document.body.classList.remove('chat-active');
    };

    const handleStartChat = async (targetUser) => {
        try {
            const chat = await apiFetch(`/api/chats/private/${targetUser.id}`, {
                method: 'POST'
            });

            if (chatListRef.current) {
                chatListRef.current.prependChat(chat);
            }
            handleChatSelect(chat);
        } catch (error) {
            alert("Не удалось создать чат");
        }
    };

    const openConfirm = useCallback((message, action) => {
        setConfirmConfig({
            isOpen: true,
            message: message,
            onConfirm: () => {
                action();
                setConfirmConfig(prev => ({ ...prev, isOpen: false }));
            }
        });
    }, []);

    const handleGroupCreated = (newChat) => {
        if (chatListRef.current) {
            chatListRef.current.prependChat(newChat);
        }
        handleChatSelect(newChat);
    };

    const imageObserver = useMemo(() => new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const media = entry.target;
                const fileId = media.getAttribute('data-file-id');
                if (fileId) {
                    imageLoader.getImageSrc(fileId).then(src => {
                        media.src = src;
                        media.onload = () => {
                            media.style.opacity = '1';
                            media.parentElement.querySelector('.skeleton')?.remove();
                        };
                    });
                }
                imageObserver.unobserve(media);
            }
        });
    }, { threshold: 0.1 }), []);

    const deleteMessages = async (messageIds, forAll) => {
        const payload = {
            messagesId: messageIds,
            senderId: user.id,
            chatId: activeChat.chatId,
            forAll: forAll
        };

        try {
            await apiFetch('/api/messages', {
                method: 'DELETE',
                body: JSON.stringify(payload)
            });

            if (!forAll) {
                setDeleteEvent({ messagesId: messageIds, chatId: activeChat.chatId, _ts: Date.now() });
            }
        } catch (error) {
            console.error('Ошибка при удалении сообщения:', error);
        }
    };

    const deleteChat = useCallback((chatId) => {
        openConfirm(
            "Вы действительно хотите удалить этот чат? Это действие необратимо.",
            async () => {
                try {
                    await apiFetch(`/api/chats/${chatId}`, { method: 'DELETE' });
                    setActiveChat(null);
                    setContextMenu(null);
                    chatListRef.current?.removeChatFromList(chatId);
                } catch (error) {
                    alert("Не удалось удалить чат.");
                }
            }
        );
    }, [openConfirm]);

    // --- Обработчики вызова меню ---
    const handleMessageContextMenu = useCallback((e, msg) => {
        e.preventDefault();
        e.stopPropagation();

        // Проверяем, кто отправитель (используем user.id из стейта)
        const isSentByMe = msg.senderId === user?.id;

        const options = [
            {
                label: 'Удалить у себя',
                action: () => deleteMessages([msg.id], false)
            }
        ];

        if (isSentByMe) {
            options.push({
                label: 'Удалить у всех',
                danger: true,
                action: () => deleteMessages([msg.id], true)
            });
        }

        // Используем clientX/Y для fixed позиционирования
        setContextMenu({
            x: e.clientX,
            y: e.clientY,
            options: options
        });
    }, [user, deleteMessages]);

    const handleChatContextMenu = useCallback((e, chatId) => {
        e.preventDefault();
        setContextMenu({
            x: e.clientX,
            y: e.clientY,
            options: [
                { label: 'Удалить чат', danger: true, action: () => deleteChat(chatId) }
            ]
        });
    }, [deleteChat]);

    return (
        <div className="container">
            <Header
                userData={user}
                onLogout={logout}
                onSearchClick={() => setIsUserSearchOpen(true)}
                onCreateGroupClick={() => setIsCreateGroupOpen(true)}
                onProfileClick={() => setIsProfileOpen(true)}
                avatarId={profile?.avatarId}
            />

            <main className="chat-wrapper">
                <ChatList
                    activeChatId={activeChat?.chatId}
                    onChatSelect={handleChatSelect}
                    ref={chatListRef}
                    onContextMenu={handleChatContextMenu}
                />

                <ChatWindow
                    activeChat={activeChat}
                    currentUserId={user?.id}
                    participantCache={participantCache}
                    imageObserver={imageObserver}
                    photoViewer={photoViewer}
                    messageReadObserver={messageReadObserver}
                    onBack={handleBackToList}
                    onOpenProfile={(id, chatId, name) => setSelectedUserProfile({ id, chatId, name })}
                    onOpenGroupProfile={() => setIsGroupProfileOpen(true)}
                    onOpenSearch={() => setIsSearchOpen(true)}
                    socketUpdate={socketUpdate}
                    readEvent={readEvent}
                    deleteEvent={deleteEvent}
                    onSendMessage={(msg) => sendMessage(msg)} а
                    apiBaseUrl={API_BASE_URL}
                    onMessageContextMenu={handleMessageContextMenu}
                />

                {/* Модальные окна */}
                <MyProfileManager
                    isOpen={isProfileOpen}
                    onClose={() => setIsProfileOpen(false)}
                    userData={user}
                    onUserDataUpdate={refreshUserData}
                />

                <UserProfileModal
                    isOpen={!!selectedUserProfile}
                    onClose={() => setSelectedUserProfile(null)}
                    {...selectedUserProfile}
                    imageObserver={imageObserver}
                />

                <GroupProfileModal
                    isOpen={isGroupProfileOpen}
                    onClose={() => setIsGroupProfileOpen(false)}
                    chatId={activeChat?.chatId}
                    chatName={activeChat?.name}
                    currentUserId={user?.id}
                    imageObserver={imageObserver}
                    onOpenUserProfile={(p) => setSelectedUserProfile({ id: p.id, chatId: null, name: `${p.name} ${p.surname}` })}
                    onOpenConfirm={openConfirm}
                />

                <ChatSearchModal
                    isOpen={isSearchOpen}
                    onClose={() => setIsSearchOpen(false)}
                    chatId={activeChat?.chatId}
                    participantCache={participantCache}
                />

                <UserSearchModal
                    isOpen={isUserSearchOpen}
                    onClose={() => setIsUserSearchOpen(false)}
                    onUserSelect={handleStartChat}
                />

                <CreateGroupModal
                    isOpen={isCreateGroupOpen}
                    onClose={() => setIsCreateGroupOpen(false)}
                    onGroupCreated={handleGroupCreated}
                />

                {contextMenu && (
                    <ContextMenu
                        x={contextMenu.x}
                        y={contextMenu.y}
                        options={contextMenu.options}
                        onClose={() => setContextMenu(null)} // Очищаем состояние
                    />
                )}

                <ConfirmationModal
                    isOpen={confirmConfig.isOpen}
                    message={confirmConfig.message}
                    onConfirm={confirmConfig.onConfirm}
                    onCancel={() => setConfirmConfig(prev => ({ ...prev, isOpen: false }))}
                />
            </main>
        </div>
    );
}

export default ChatPage;