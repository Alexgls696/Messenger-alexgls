import React, { useState, useEffect, useRef, useCallback, useImperativeHandle, forwardRef } from 'react';
import { apiFetch } from '../utils/apiClient';
import Message from './Message';
import { generateTempId, isDocumentType } from '../utils/messageUtils';
import defaultProfileImage from '../images/profile-default.png'
import { imageLoader } from '../utils/imageLoader';

const PAGE_SIZE = 50;

const ChatWindow = forwardRef(({
    activeChat,
    currentUserId,
    participantCache,
    imageObserver,
    messageReadObserver,
    photoViewer,
    onOpenProfile,
    onBack,
    onOpenGroupProfile,
    onOpenSearch,
    onMessageContextMenu,
    socketUpdates,
    deleteEvent,
    onChatCreated,
    messageUpdateEvent,
    editingMessage,
    setEditingMessage,
    deleteMessages,
    replyingTo,
    setReplyingTo,
    replyCache,
    onForwardMessages,
    firstSelectedMessage,
    setFirstSelectedMessage,
    isSelectionMode,
    setSelectionMode,
    forwardingMessages,
    setForwardingMessages,
    userOnlineChanged,
    clearSocketUpdates,
    isForbidden,
    setIsForbidden,
    isMobile
}, ref) => {
    const [messages, setMessages] = useState([]);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [isLoading, setIsLoading] = useState(false);
    const [chatDetails, setChatDetails] = useState({ title: 'Загрузка...', isGroup: false });
    const [recipientId, setRecipientId] = useState(null);

    const [avatar, setAvatar] = useState(defaultProfileImage);
    const [user, setUser] = useState(null);

    const [inputText, setInputText] = useState('');
    const [pendingFiles, setPendingFiles] = useState([]);

    const fileInputRef = useRef(null);
    const scrollContainerRef = useRef(null);
    const prevScrollHeightRef = useRef(0);
    const isInitialLoad = useRef(true);

    const inputTextRef = useRef(null);

    const [selectedMessages, setSelectedMessages] = useState([]);

    const fetchMessages = useCallback(async (chatId, pageNum, signal) => {
        try {
            const data = await apiFetch(`/api/messages?chatId=${chatId}&page=${pageNum}&pageSize=${PAGE_SIZE}`, { signal });
            const hasMoreData = Array.isArray(data) && data.length === PAGE_SIZE;
            return { fetchedMessages: data || [], hasMoreData };
        } catch (error) {
            if (error.name !== 'AbortError') console.error('Ошибка загрузки сообщений:', error);
            return { fetchedMessages: [], hasMoreData: false };
        }
    }, []);

    useImperativeHandle(ref, () => ({
        addMessageFromSocket(newMsg) {
            setMessages(prev => {
                if (newMsg.id && prev.some(m => m.id === newMsg.id)) {
                    return prev;
                }

                if (newMsg.tempId && prev.some(m => m.tempId === newMsg.tempId)) {
                    return prev.map(m => m.tempId === newMsg.tempId ? newMsg : m);
                }

                return [...prev, newMsg];
            });

            setTimeout(() => {
                if (scrollContainerRef.current) {
                    scrollContainerRef.current.scrollTop = scrollContainerRef.current.scrollHeight;
                }
            }, 50);
        },
        readMessageEvent(readMsg) {
            const idsToUpdate = new Set(readMsg.messageIds);

            setMessages(prev => prev.map(m => {
                if (idsToUpdate.has(m.id)) {
                    return { ...m, read: true };
                }
                // 3. Если не совпало, возвращаем старое сообщение без изменений
                return m;
            }));
        }
    }));

    useEffect(() => {
        if (!window.visualViewport) return;

        const handleResize = () => {
            const viewport = window.visualViewport;
            // Устанавливаем высоту всего приложения равной видимой части
            document.documentElement.style.setProperty('--vv-height', `${viewport.height}px`);

            // Автоматическая прокрутка вниз при открытии клавиатуры (опционально)
            if (document.activeElement.tagName === 'TEXTAREA') {
                setTimeout(() => {
                    scrollContainerRef.current?.scrollTo({
                        top: scrollContainerRef.current.scrollHeight,
                        behavior: 'smooth'
                    });
                }, 300);
            }
        };

        window.visualViewport.addEventListener('resize', handleResize);
        window.visualViewport.addEventListener('scroll', handleResize); // Для iOS

        return () => {
            window.visualViewport.removeEventListener('resize', handleResize);
            window.visualViewport.removeEventListener('scroll', handleResize);
        };
    }, []);

    useEffect(() => {
        if (!activeChat) return;
        const controller = new AbortController();
        const { signal } = controller;

        const initChat = async () => {
            setMessages([]);
            setPage(0);
            setHasMore(true);
            isInitialLoad.current = true;
            setIsForbidden(false);
            setInputText('');
            setIsLoading(true);

            if (activeChat.isNew) {
                const fullName = `${activeChat.recipient.name} ${activeChat.recipient.surname}`;
                setChatDetails({ title: fullName, isGroup: false });
                setRecipientId(activeChat.recipient.id);
                participantCache[activeChat.recipient.id] = fullName;

                setHasMore(false);
                setIsLoading(false);
                return;
            }

            try {
                const promises = [
                    activeChat.group
                        ? Promise.resolve(null)
                        : apiFetch(`/api/chats/find-recipient-by-private-chat-id/${activeChat.chatId}`, { signal }),

                    apiFetch(`/api/chats/${activeChat.chatId}/participants`, { signal }),
                    fetchMessages(activeChat.chatId, 0, signal)
                ];

                const [recipient, participantsDto, msgData] = await Promise.all(promises);

                if (signal.aborted) return;

                participantsDto.participants.forEach(p => {
                    participantCache[p.id] = `${p.name} ${p.surname}`;
                });

                if (activeChat.group) {
                    setChatDetails({ title: activeChat.name, isGroup: true });
                } else if (recipient) {
                    setUser(recipient);
                    setChatDetails({ title: `${recipient.name} ${recipient.surname}`, isGroup: false });
                    setRecipientId(recipient.id);
                }

                if (participantsDto.removed) {
                    setIsForbidden(true);
                }

                setMessages(msgData.fetchedMessages);
                setHasMore(msgData.hasMoreData);
                setPage(1);

            } catch (error) {
                if (signal.aborted) return;
                console.error('Ошибка инициализации чата:', error);
                setIsForbidden(true);
                setChatDetails({ title: 'Ошибка', isGroup: false });
                setInputText('Произошла ошибка при загрузке чата❗');
            } finally {
                if (!signal.aborted) setIsLoading(false);
            }
        };

        initChat();
        return () => controller.abort();
    }, [activeChat, fetchMessages]);

    // 2. ЕДИНЫЙ эффект для управления скроллом (useLayoutEffect)
    useEffect(() => {
        const container = scrollContainerRef.current;
        if (!container || messages.length === 0 || !isInitialLoad.current) return;

        requestAnimationFrame(() => {
            const firstUnread = messages.find(m => !m.read && m.senderId !== currentUserId);
            if (firstUnread) {
                const element = container.querySelector(`[data-message-id="${firstUnread.id}"]`);
                if (element) {
                    container.scrollTop = element.offsetTop - (container.clientHeight / 4);
                }
            } else {
                container.scrollTop = container.scrollHeight;
            }
            isInitialLoad.current = false;
        });
    }, [messages]);

    // Редактирование
    useEffect(() => {
        if (editingMessage) {
            setInputText(editingMessage.content);
            inputTextRef.current?.focus();
        }
    }, [editingMessage]);

    // Автомасштабирование
    useEffect(() => {
        const textarea = inputTextRef.current;
        if (textarea) {
            textarea.style.height = 'auto';
            textarea.style.height = `${textarea.scrollHeight}px`;
        }
    }, [inputText]);

    const cancelEdit = () => {
        setEditingMessage(null);
        setInputText('');
    };

    const handleScroll = async (e) => {
        const container = e.currentTarget;
        // Порог срабатывания (100px до верха), чтобы не ждать ровно 0
        if (container.scrollTop < 100 && hasMore && !isLoading && activeChat) {
            prevScrollHeightRef.current = container.scrollHeight; // Запоминаем текущую высоту
            setIsLoading(true);

            const { fetchedMessages, hasMoreData } = await fetchMessages(activeChat.chatId, page);

            if (fetchedMessages.length > 0) {
                // Используем функциональное обновление, чтобы не зависеть от замыкания
                setMessages(prev => [...fetchedMessages, ...prev]);
                setPage(p => p + 1);
                setHasMore(hasMoreData);
            } else {
                setHasMore(false);
            }
            setIsLoading(false);
        }
    };

    //WebSocket---------------------------------------------------------

    useEffect(() => {
        if (!activeChat || !socketUpdates || socketUpdates.length === 0) return;

        setMessages(prev => {
            let nextMessages = [...prev];
            let hasChanges = false;

            socketUpdates.forEach(update => {
                if (update.chatId !== activeChat.chatId) return;

                if (update.tempId && nextMessages.some(m => m.tempId === update.tempId)) {
                    nextMessages = nextMessages.map(m => m.tempId === update.tempId ? update : m);
                    hasChanges = true;
                } else if (!nextMessages.some(m => m.id === update.id)) {
                    nextMessages.push(update);
                    hasChanges = true;
                }
            });

            return hasChanges ? nextMessages : prev;
        });

        setTimeout(() => {
            if (scrollContainerRef.current)
                scrollContainerRef.current.scrollTop = scrollContainerRef.current.scrollHeight;
        }, 50);

        clearSocketUpdates();

    }, [socketUpdates, activeChat, clearSocketUpdates]);


    useEffect(() => {
        if (messageUpdateEvent && activeChat && messageUpdateEvent.chatId === activeChat.chatId) {
            setMessages(prev => prev.map(message => {
                if (message.id === messageUpdateEvent.id) {
                    return messageUpdateEvent;
                }
                return message;
            }));
        }
    }, [messageUpdateEvent, activeChat?.chatId]);

    useEffect(() => {
        if (!activeChat || !deleteEvent) return;

        if (deleteEvent.chatId === activeChat.chatId) {
            setMessages(prev => prev.filter(m => !deleteEvent.messagesId.includes(m.id)));
        }
    }, [deleteEvent, activeChat]);


    useEffect(() => {
        if (!userOnlineChanged) return;

        setUser(prev => {
            if (!prev || prev.id !== userOnlineChanged.userId) return prev;

            return {
                ...prev,
                online: userOnlineChanged.online
            };
        });

    }, [userOnlineChanged]);

    //Отправка сообщения ------------------------------------------------
    // --- Логика выбора файлов ---
    const handleFileSelect = (e) => {
        const files = Array.from(e.target.files);
        const newPending = files.map(file => ({
            file,
            tempId: generateTempId(),
            isAnalysed: false,
            previewUrl: file.type.startsWith('image/') ? URL.createObjectURL(file) : null
        }));
        setPendingFiles(prev => [...prev, ...newPending]);
        e.target.value = ''; // сброс инпута
    };


    const removeFile = (tempId) => {
        setPendingFiles(prev => {
            const filtered = prev.filter(f => f.tempId !== tempId);
            // Важно освобождать память от URL.createObjectURL
            const removed = prev.find(f => f.tempId === tempId);
            if (removed?.previewUrl) URL.revokeObjectURL(removed.previewUrl);
            return filtered;
        });
    };

    const toggleAnalyse = (tempId) => {
        setPendingFiles(prev => prev.map(f =>
            f.tempId === tempId ? { ...f, isAnalysed: !f.isAnalysed } : f
        ));
    };

    // --- ОТПРАВКА СООБЩЕНИЯ ---
    const handleFormSubmit = async (e) => {

        e.preventDefault();

        if (isForbidden) return;

        const content = inputText.trim();
        const filesToSend = [...pendingFiles];
        if (!content && filesToSend.length === 0 && forwardingMessages.length === 0) return;

        const tempId = generateTempId();

        let currentChatId = activeChat.chatId;
        const isNewChat = activeChat.isNew;

        if (editingMessage) {
            try {
                const payload = {
                    content: content,
                    chatId: activeChat.chatId
                };

                await apiFetch(`/api/messages/${editingMessage.id}`, {
                    method: 'PATCH',
                    body: JSON.stringify(payload)
                });

                setEditingMessage(null);
                setInputText('');

            } catch (error) {
                console.error('Ошибка при обновлении:', error);
                alert("Не удалось сохранить изменения");
            }
        } else {
            try {
                // 1. ЛЕНИВОЕ СОЗДАНИЕ ЧАТА (если это первое сообщение)
                if (isNewChat) {
                    // Создаем чат в базе данных
                    const createdChat = await apiFetch(`/api/chats/private/${activeChat.recipient.id}`, {
                        method: 'POST'
                    });

                    currentChatId = createdChat.chatId;

                    if (onChatCreated) {
                        onChatCreated(createdChat);
                    }

                    activeChat.isNew = false;
                    activeChat.chatId = currentChatId;
                }

                // 2. Оптимистичное обновление UI
                if (content || filesToSend.length > 0) {
                    const optimisticMsg = {
                        optimistic: true,
                        tempId: tempId,
                        chatId: currentChatId,
                        senderId: currentUserId,
                        content: content,
                        createdAt: new Date().toISOString(),
                        updatedAt: null,
                        isPending: true,
                        attachments: filesToSend.map(f => ({
                            fileId: f.tempId,
                            fileName: f.file.name,
                            mimeType: f.file.type,
                            localUrl: f.previewUrl
                        }))
                    };
                    setMessages(prev => [...prev, optimisticMsg]);
                }
                setInputText('');
                setPendingFiles([]);

                setTimeout(() => {
                    if (scrollContainerRef.current)
                        scrollContainerRef.current.scrollTop = scrollContainerRef.current.scrollHeight;
                }, 50);

                const uploadedAttachments = [];
                for (const item of filesToSend) {
                    try {
                        const data = await apiFetch('/api/media-storage/upload-url', {
                            method: 'POST',
                            body: JSON.stringify({ fileName: item.file.name, contentType: item.file.type })
                        });

                        const s3Response = await fetch(data.uploadUrl, {
                            method: "PUT",
                            headers: { "Content-Type": item.file.type },
                            body: item.file
                        });

                        if (!s3Response.ok) throw new Error(`S3 upload failed for ${item.file.name}`);

                        const fileMetadata = {
                            path: data.key,
                            filename: item.file.name,
                            chatId: currentChatId,
                            securityType: 'private'
                        };

                        const savedFile = await apiFetch('/api/files', {
                            method: 'POST',
                            body: JSON.stringify(fileMetadata)
                        });


                        if (item.isAnalysed) {
                            apiFetch('/api/analysis', {
                                method: 'POST',
                                body: JSON.stringify({
                                    key: data.key,
                                    fileId: savedFile.id,
                                    chatId: currentChatId,
                                    fileName: item.file.name
                                })
                            }).catch(e => console.error("Analysis trigger failed", e));
                        }

                        uploadedAttachments.push({
                            fileId: savedFile.id,
                            mimeType: item.file.type,
                            fileName: item.file.name,
                            hasAnalysis: item.isAnalysed
                        });
                    } catch (fileErr) {
                        console.error(`Error processing file ${item.file.name}:`, fileErr);
                        throw fileErr;
                    }
                }

                const messagePayload = {
                    chatId: currentChatId,
                    content: content,
                    attachments: uploadedAttachments,
                    tempId: tempId,
                    replyMessageId: replyingTo?.id || null
                };

                const forwardedMessagesIds = forwardingMessages.map((msg) => msg.id);
                if (forwardedMessagesIds.length > 0) {
                    const request = {
                        chatMessage: messagePayload,
                        forwardedMessagesIds: forwardedMessagesIds,
                    };

                    const responseData = await apiFetch(`/api/messages/forward`, {
                        method: 'POST',
                        body: JSON.stringify(request)
                    });

                    setMessages(prev => {
                        const optimisticIndex = prev.findIndex(m => m.tempId === tempId);

                        if (optimisticIndex !== -1) {
                            const newMessages = [...prev];
                            newMessages.splice(optimisticIndex, 1, ...responseData);
                            return newMessages;
                        }

                        return [...prev, ...responseData];
                    });

                    setForwardingMessages([]);
                } else {
                    const messageResponse = await apiFetch('/api/messages', {
                        method: 'POST',
                        body: JSON.stringify(messagePayload)
                    });

                    setMessages(prev => prev.map(m =>
                        m.tempId === tempId ? messageResponse : m
                    ));
                }
                setReplyingTo(null)

            } catch (err) {
                console.error("Ошибка при выполнении отправки:", err);
                setMessages(prev => prev.map(m =>
                    m.tempId === tempId ? { ...m, isError: true, isPending: false } : m
                ));
            }
        }
    };

    useEffect(() => {
        if (replyingTo) {
            inputTextRef.current?.focus();
        }
    }, [replyingTo])

    // Выделение и пересылка
    useEffect(() => {
        if (firstSelectedMessage) {
            setSelectedMessages([firstSelectedMessage]);
        }
    }, [firstSelectedMessage]);

    const toggleMessageSelection = (message) => {
        setSelectedMessages(prev => {
            const isAlreadySelected = prev.some(m => m.id === message.id);
            if (isAlreadySelected) {
                return prev.filter(m => m.id !== message.id);
            }
            return [...prev, message];
        });
    };

    const clearSelection = () => {
        setSelectedMessages([]);
        setSelectionMode(false);
        setFirstSelectedMessage(null)
    };

    const deleteMessageGroup = async (messages) => {
        let forAll = true;
        messages.forEach(message => {
            if (message.senderId !== currentUserId) {
                forAll = false;
            }
        })
        const messagesIds = messages.map(message => message.id)
        deleteMessages(messagesIds, forAll, 'Вы действительно хотите удалить выбранные сообщения? Это действие необратимо.');
        clearSelection();
    }

    const handleForwardClick = () => {
        if (onForwardMessages && selectedMessages.length > 0) {
            onForwardMessages(selectedMessages);
            clearSelection();
        }
    };

    useEffect(() => {
        if (replyingTo || (forwardingMessages && forwardingMessages.length > 0)) {
            inputTextRef.current?.focus();
        }
    }, [replyingTo, forwardingMessages]);

    useEffect(() => {
        if (user) {
            apiFetch(`/api/profiles/images/user-avatar/${user.id}`).then(id => {
                if (id) {
                    imageLoader.getImageSrc(id)
                        .then(setAvatar)
                } else {
                    setAvatar(defaultProfileImage)
                }
            }).catch(() => {
            });
        }
    }, [user])

    const handleOnButtonClickEvent = (event) => {
        if (event.key === 'Enter' && !event.shiftKey) {
            if (!isMobile) {
                event.preventDefault();
                handleFormSubmit(event);
            }
        }
    }

    if (!activeChat) return <section className="chat-window hidden" />;

    return (
        <section id="chatWindow" className="chat-window">
            <div className="chat-window__header">
                {isSelectionMode ? (
                    <div className="selection-header-wrapper">
                        <button className="header-icon-btn" onClick={() => {
                            clearSelection()
                        }} title="Отменить выделение">
                            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor"
                                strokeWidth="2">
                                <line x1="18" y1="6" x2="6" y2="18"></line>
                                <line x1="6" y1="6" x2="18" y2="18"></line>
                            </svg>
                        </button>
                        <span className="selection-count">Выбрано: {selectedMessages.length}</span>
                        <div className="selection-actions">
                            <button className="header-icon-btn" onClick={() => deleteMessageGroup(selectedMessages)}
                                title="Удалить">
                                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor"
                                    strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <polyline points="3 6 5 6 21 6"></polyline>
                                    <path
                                        d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                    <line x1="10" y1="11" x2="10" y2="17"></line>
                                    <line x1="14" y1="11" x2="14" y2="17"></line>
                                </svg>
                            </button>
                            <button className="header-icon-btn" onClick={handleForwardClick} title="Переслать">
                                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor"
                                    strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <polyline points="15 14 20 9 15 4"></polyline>
                                    <path d="M4 20v-7a4 4 0 0 1 4-4h12"></path>
                                </svg>
                            </button>
                        </div>
                    </div>
                ) : (
                    <>
                        <div className="chat-title-wrapper">
                            {!chatDetails.isGroup ? (
                                <div className="header-user-item"
                                    onClick={() => onOpenProfile(recipientId, activeChat.chatId, chatDetails.title)}>
                                    <div className="avatar-container">
                                        <img className="header-user-avatar" src={avatar} alt="" />
                                        {user?.online && (
                                            <span className="online-status-dot"></span>
                                        )}
                                    </div>
                                    <div className="header-user-info">
                                        <div className="header-user-header">
                                            <span className="header-user-name">{user?.name} {user?.surname}</span>
                                        </div>
                                        <span className="header-user-username">@{user?.username}</span>
                                    </div>
                                </div>

                            ) : (
                                <>
                                    <h3>{chatDetails.title}</h3>
                                    <button className="header-icon-btn" onClick={onOpenGroupProfile}>
                                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none"
                                            stroke="currentColor" strokeWidth="2" strokeLinecap="round"
                                            strokeLinejoin="round">
                                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                            <circle cx="9" cy="7" r="4"></circle>
                                            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                        </svg>
                                    </button>
                                </>
                            )}
                            <button className="header-icon-btn" onClick={onOpenSearch}>
                                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor"
                                    strokeWidth="2">
                                    <circle cx="11" cy="11" r="8"></circle>
                                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                </svg>
                            </button>
                        </div>
                        <button className="header-icon-btn" onClick={onBack} title="Закрыть чат">
                            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor"
                                strokeWidth="2">
                                <line x1="18" y1="6" x2="6" y2="18"></line>
                                <line x1="6" y1="6" x2="18" y2="18"></line>
                            </svg>
                        </button>
                    </>
                )}
            </div>

            <div
                id="messages"
                className="messages"
                ref={scrollContainerRef}
                onScroll={handleScroll}
                style={{ overflowAnchor: 'none' }}
            >
                {messages.map((msg) => (
                    <Message
                        key={msg.id}
                        msg={msg}
                        isSentByMe={msg.senderId === currentUserId}
                        participantCache={participantCache}
                        messageReadObserver={messageReadObserver}
                        imageObserver={imageObserver}
                        onOpenProfile={onOpenProfile}
                        photoViewer={photoViewer}
                        onContextMenu={onMessageContextMenu}
                        allMessages={messages}
                        replyCache={replyCache}

                        isSelected={selectedMessages.some(m => m.id === msg.id)}
                        selectionMode={isSelectionMode}
                        onSelect={() => toggleMessageSelection(msg)}
                        setReplyingTo={setReplyingTo}
                    />
                ))}
            </div>

            {/* ПРЕВЬЮ ВЛОЖЕНИЙ */}
            {!isForbidden && pendingFiles.length > 0 && (
                <div className="attachment-preview-container">
                    {pendingFiles.map(f => (
                        <div key={f.tempId}
                            className={`attachment-preview-item ${f.previewUrl ? 'is-image' : 'is-file'}`}>
                            {f.previewUrl ? (
                                <img src={f.previewUrl} alt="" />
                            ) : (
                                <div className="file-preview-info">
                                    <span className="file-icon">📁</span>
                                    <span>{f.file.name}</span>
                                </div>
                            )}

                            {isDocumentType(f.file.type) && (
                                <label className="analyse-checkbox-wrapper">
                                    <input
                                        type="checkbox"
                                        checked={f.isAnalysed}
                                        onChange={() => toggleAnalyse(f.tempId)}
                                    />
                                    Анализ
                                </label>
                            )}
                            <button className="remove-attachment-btn"
                                onClick={() => removeFile(f.tempId)}>&times;</button>
                        </div>
                    ))}
                </div>
            )}

            {editingMessage && (
                <div className="edit-message-bar">
                    <div className="edit-bar-icon">✎</div>
                    <div className="edit-bar-content">
                        <div className="edit-bar-title">Редактирование сообщения</div>
                        <div className="edit-bar-text">{editingMessage.content}</div>
                    </div>
                    <button className="header-icon-btn" onClick={cancelEdit}>
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor"
                            strokeWidth="2">
                            <line x1="18" y1="6" x2="6" y2="18"></line>
                            <line x1="6" y1="6" x2="18" y2="18"></line>
                        </svg>
                    </button>
                </div>
            )}

            {replyingTo && (
                <div className="edit-message-bar reply-bar">
                    <div className="edit-bar-icon">➦</div>
                    <div className="edit-bar-content">
                        <div className="edit-bar-title">Ответ пользователю {participantCache[replyingTo.senderId]}</div>
                        <div className="edit-bar-text">{replyingTo.content}</div>
                    </div>
                    <button className="header-icon-btn" onClick={() => setReplyingTo(null)}>
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor"
                            strokeWidth="2">
                            <line x1="18" y1="6" x2="6" y2="18"></line>
                            <line x1="6" y1="6" x2="18" y2="18"></line>
                        </svg>
                    </button>
                </div>
            )}

            {forwardingMessages && forwardingMessages.length > 0 && (
                <div className="edit-message-bar forward-bar">
                    <div className="edit-bar-icon">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor"
                            strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <polyline points="15 14 20 9 15 4"></polyline>
                            <path d="M4 20v-7a4 4 0 0 1 4-4h12"></path>
                        </svg>
                    </div>
                    <div className="edit-bar-content">
                        <div className="edit-bar-title">Пересылка сообщений</div>
                        <div className="edit-bar-text">
                            Выбрано сообщений: {forwardingMessages.length}
                        </div>
                    </div>
                    <button className="header-icon-btn" onClick={() => setForwardingMessages([])}>
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor"
                            strokeWidth="2">
                            <line x1="18" y1="6" x2="6" y2="18"></line>
                            <line x1="6" y1="6" x2="18" y2="18"></line>
                        </svg>
                    </button>
                </div>
            )}

            <form className="message-form" onSubmit={handleFormSubmit}>
                {!isForbidden && (
                    <>
                        <input
                            type="file"
                            ref={fileInputRef}
                            onChange={handleFileSelect}
                            hidden
                            multiple></input>
                        <button
                            type="button"
                            className="attach-btn"
                            onClick={() => fileInputRef.current.click()}
                            disabled={isForbidden}
                            title="Прикрепить файл"
                        >
                            <svg
                                viewBox="0 0 24 24"
                                width="24"
                                height="24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            >
                                <path
                                    d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"></path>
                            </svg>
                        </button>
                    </>
                )}


                {!isForbidden ? (<textarea ref={inputTextRef}
                    className="message-input"
                    value={inputText}
                    onChange={(e) => setInputText(e.target.value)}
                    onKeyDown={handleOnButtonClickEvent}
                    placeholder="Введите сообщение..."
                />) : (
                    <div className="forbidden-plaque">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                        </svg>
                        <span>У вас нет доступа к этому чату</span>
                    </div>
                )}


                {!isForbidden && (
                    <button
                        type="submit"
                        className="send-btn"
                        disabled={isForbidden || (!inputText.trim() && pendingFiles.length === 0 && forwardingMessages.length === 0)}
                        title="Отправить"
                    >
                        <svg
                            viewBox="0 0 24 24"
                            width="22"
                            height="22"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2.5"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                        >
                            <line x1="22" y1="2" x2="11" y2="13"></line>
                            <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                        </svg>
                    </button>
                )}

            </form>
        </section>


    );
});

export default ChatWindow;