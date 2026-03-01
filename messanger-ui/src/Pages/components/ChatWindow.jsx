import React, { useState, useEffect, useRef, useCallback, useLayoutEffect } from 'react';
import { apiFetch } from '../utils/apiClient';
import Message from './Message';
import { generateTempId, isDocumentType } from '../utils/messageUtils';

const PAGE_SIZE = 50;

function ChatWindow({ activeChat,
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
    readEvent,
    deleteEvent,
    apiBaseUrl,
    onChatCreated,
    messageUpdateEvent,
    editingMessage,
    setEditingMessage,
    replyingTo,
    setReplyingTo,
    replyCache,
    onForwardMessages,
    firstSelectedMessage,
    isSelectionMode,
    setSelectionMode,
    forwardingMessages,
    setForwardingMessages
}) {
    const [messages, setMessages] = useState([]);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [isLoading, setIsLoading] = useState(false);
    const [chatDetails, setChatDetails] = useState({ title: 'Загрузка...', isGroup: false });
    const [recipientId, setRecipientId] = useState(null);

    const [inputText, setInputText] = useState('');
    const [pendingFiles, setPendingFiles] = useState([]); // [{ file, tempId, isAnalysed }]

    const fileInputRef = useRef(null);
    const scrollContainerRef = useRef(null);
    const prevScrollHeightRef = useRef(0);
    const isInitialLoad = useRef(true);

    const inputTextRef = useRef(null);

    const [isForbidden, setIsForbidden] = useState(false);

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

    useEffect(() => {
        if (!activeChat) return;
        const controller = new AbortController();

        const initChat = async () => {
            setMessages([]);
            setPage(0);
            setHasMore(true);
            isInitialLoad.current = true;
            setIsForbidden(false);
            setInputText('');

            if (activeChat.isNew) {
                setChatDetails({
                    title: `Чат с ${activeChat.recipient.name} ${activeChat.recipient.surname}`,
                    isGroup: false
                });
                setRecipientId(activeChat.recipient.id);
                participantCache[activeChat.recipient.id] = `${activeChat.recipient.name} ${activeChat.recipient.surname}`;

                setHasMore(false);
                setIsLoading(false);
                return;
            }

            // --- ЛОГИКА ДЛЯ СУЩЕСТВУЮЩЕГО ЧАТА ---
            setIsLoading(true);
            try {
                if (activeChat.group) {
                    setChatDetails({ title: activeChat.name, isGroup: true });
                } else {
                    const recipient = await apiFetch(`/api/chats/find-recipient-by-private-chat-id/${activeChat.chatId}`, { signal: controller.signal });
                    setChatDetails({ title: `Чат с ${recipient.name} ${recipient.surname}`, isGroup: false });
                    setRecipientId(recipient.id);
                }

                try {
                    const participantsDto = await apiFetch(`/api/chats/${activeChat.chatId}/participants`, { signal: controller.signal });
                    participantsDto.participants.forEach(p => {
                        participantCache[p.id] = `${p.name} ${p.surname}`;
                    });
                    if (participantsDto.removed) {
                        setIsForbidden(true);
                        setInputText('Вы не состоите в этой группе❗ ');
                        setIsLoading(false);
                    }
                } catch (error) {
                    setIsForbidden(true);
                    setInputText('Произошла ошибка при загрузке чата❗ ' + error);
                    setIsLoading(false);
                    return;
                }

                const { fetchedMessages, hasMoreData } = await fetchMessages(activeChat.chatId, 0, controller.signal);
                if (!controller.signal.aborted) {
                    setMessages(fetchedMessages);
                    setHasMore(hasMoreData);
                    setPage(1);
                }
            } catch (e) {
                if (!controller.signal.aborted) {
                    setChatDetails({ title: 'Ошибка', isGroup: false });
                }
            } finally {
                if (!controller.signal.aborted) setIsLoading(false);
            }
        };

        initChat();
        return () => controller.abort();
    }, [activeChat, fetchMessages]);

    // 2. ЕДИНЫЙ эффект для управления скроллом (useLayoutEffect)
    useLayoutEffect(() => {
        const container = scrollContainerRef.current;
        if (!container || messages.length === 0) return;

        if (isInitialLoad.current) {
            // ЛОГИКА: Первый вход в чат
            const firstUnread = messages.find(m => !m.read && m.senderId !== currentUserId);

            if (firstUnread) {
                const element = container.querySelector(`[data-message-id="${firstUnread.id}"]`);
                if (element) {
                    // Прокрутка к непрочитанному без анимации
                    container.scrollTop = element.offsetTop - (container.clientHeight / 4);
                }
            } else {
                // Все прочитано - в самый конец
                container.scrollTop = container.scrollHeight;
            }
            isInitialLoad.current = false;
        }
        else if (prevScrollHeightRef.current > 0) {
            // ЛОГИКА: Сохранение позиции при подгрузке старых сообщений (скролл вверх)
            const heightDifference = container.scrollHeight - prevScrollHeightRef.current;
            container.scrollTop = heightDifference;
            prevScrollHeightRef.current = 0;
        }
    }, [messages, currentUserId]);


    // Редактирование
    useEffect(() => {
        if (editingMessage) {
            setInputText(editingMessage.content);
            inputTextRef.current?.focus();
        }
    }, [editingMessage]);

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
                } 
                else if (!nextMessages.some(m => m.id === update.id)) {
                    nextMessages.push(update);
                    hasChanges = true;
                }
            });

            return hasChanges ? nextMessages : prev;
        });

        // Скролл вниз
        setTimeout(() => {
            if (scrollContainerRef.current)
                scrollContainerRef.current.scrollTop = scrollContainerRef.current.scrollHeight;
        }, 50);

    }, [socketUpdates, activeChat]);

    

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

    //  Обработка прочтения (readEvent)
    useEffect(() => {
        // ЗАЩИТА
        if (!activeChat || !readEvent) return;

        if (readEvent.chatId === activeChat.chatId) {
            setMessages(prev => prev.map(m =>
                readEvent.messageIds.includes(m.id) ? { ...m, read: true } : m
            ));
        }
    }, [readEvent, activeChat]);

    // Обработка удаления (deleteEvent)
    useEffect(() => {
        // ЗАЩИТА
        if (!activeChat || !deleteEvent) return;

        if (deleteEvent.chatId === activeChat.chatId) {
            setMessages(prev => prev.filter(m => !deleteEvent.messagesId.includes(m.id)));
        }
    }, [deleteEvent, activeChat]);


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
                if (content) {
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
                    const formData = new FormData();
                    formData.append('file', item.file);

                    if (item.isAnalysed) {
                        formData.append('isAnalyse', 'true');
                        formData.append('chatId', currentChatId);
                    }

                    const response = await fetch(`${apiBaseUrl}/api/storage/upload`, {
                        method: 'POST',
                        headers: { 'Authorization': `Bearer ${localStorage.getItem('accessToken')}` },
                        body: formData
                    });

                    if (!response.ok) throw new Error(`Ошибка загрузки файла: ${item.file.name}`);
                    const result = await response.json();

                    uploadedAttachments.push({
                        fileId: result.id,
                        mimeType: item.file.type,
                        fileName: item.file.name,
                        hasAnalysis: item.isAnalysed
                    });
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
                    }
                    await apiFetch(`/api/messages/forward`, {
                        method: 'POST',
                        body: JSON.stringify(request)
                    })
                    setForwardingMessages([])
                } else {
                    await apiFetch('/api/messages', {
                        method: 'POST',
                        body: JSON.stringify(messagePayload)
                    });
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
            setSelectedMessages([firstSelectedMessage.id]);
        }
    }, [firstSelectedMessage]);

    const toggleMessageSelection = (messageId) => {
        setSelectedMessages(prev => {
            if (prev.includes(messageId)) {
                return prev.filter(id => id !== messageId);
            }
            return [...prev, messageId];
        });
    };

    const clearSelection = () => {
        setSelectedMessages([]);
    };

    const handleForwardClick = () => {
        if (onForwardMessages && selectedMessages.length > 0) {
            const messagesToForward = messages.filter(m => selectedMessages.includes(m.id));
            onForwardMessages(messagesToForward);
            clearSelection();
        }
    };

    useEffect(() => {
        if (replyingTo || (forwardingMessages && forwardingMessages.length > 0)) {
            inputTextRef.current?.focus();
        }
    }, [replyingTo, forwardingMessages]);

    if (!activeChat) return <section className="chat-window hidden" />;

    return (
        <section id="chatWindow" className="chat-window">
            <div className="chat-window__header">
                {/* ЕСЛИ РЕЖИМ ВЫДЕЛЕНИЯ СООБЩЕНИЙ */}
                {isSelectionMode ? (
                    <div className="selection-header-wrapper">
                        <button className="header-icon-btn" onClick={() => { setSelectionMode(false); clearSelection() }} title="Отменить выделение">
                            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2">
                                <line x1="18" y1="6" x2="6" y2="18"></line>
                                <line x1="6" y1="6" x2="18" y2="18"></line>
                            </svg>
                        </button>
                        <span className="selection-count">Выбрано: {selectedMessages.length}</span>
                        <div className="selection-actions">
                            <button className="header-icon-btn" onClick={handleForwardClick} title="Переслать">
                                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <polyline points="15 14 20 9 15 4"></polyline>
                                    <path d="M4 20v-7a4 4 0 0 1 4-4h12"></path>
                                </svg>
                            </button>
                        </div>
                    </div>
                ) : (
                    /* ОБЫЧНАЯ ШАПКА */
                    <>
                        <div className="chat-title-wrapper">
                            <h2 id="chatTitle">{chatDetails.title}</h2>
                            {!chatDetails.isGroup ? (
                                <button className="header-icon-btn" onClick={() => onOpenProfile(recipientId, activeChat.chatId, chatDetails.title)}>
                                    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                        <circle cx="12" cy="7" r="4"></circle>
                                    </svg>
                                </button>
                            ) : (
                                <button className="header-icon-btn" onClick={onOpenGroupProfile}>
                                    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                        <circle cx="9" cy="7" r="4"></circle>
                                        <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                    </svg>
                                </button>
                            )}
                            <button className="header-icon-btn" onClick={onOpenSearch}>
                                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2">
                                    <circle cx="11" cy="11" r="8"></circle>
                                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                </svg>
                            </button>
                        </div>
                        <button className="header-icon-btn" onClick={onBack} title="Закрыть чат">
                            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2">
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

                        isSelected={selectedMessages.includes(msg.id)}
                        selectionMode={isSelectionMode}
                        onSelect={() => toggleMessageSelection(msg.id)}
                    />
                ))}
            </div>

            {/* ПРЕВЬЮ ВЛОЖЕНИЙ */}
            {!isForbidden && pendingFiles.length > 0 && (
                <div className="attachment-preview-container">
                    {pendingFiles.map(f => (
                        <div key={f.tempId} className={`attachment-preview-item ${f.previewUrl ? 'is-image' : 'is-file'}`}>
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
                            <button className="remove-attachment-btn" onClick={() => removeFile(f.tempId)}>&times;</button>
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
                    <button className="edit-bar-close" onClick={cancelEdit}>&times;</button>
                </div>
            )}

            {replyingTo && (
                <div className="edit-message-bar reply-bar">
                    <div className="edit-bar-icon">➦</div>
                    <div className="edit-bar-content">
                        <div className="edit-bar-title">Ответ пользователю {participantCache[replyingTo.senderId]}</div>
                        <div className="edit-bar-text">{replyingTo.content}</div>
                    </div>
                    <button className="edit-bar-close" onClick={() => setReplyingTo(null)}>&times;</button>
                </div>
            )}

            {forwardingMessages && forwardingMessages.length > 0 && (
                <div className="edit-message-bar forward-bar">
                    <div className="edit-bar-icon">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
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
                    <button className="edit-bar-close" onClick={() => setForwardingMessages(null)}>&times;</button>
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
                        <button type="button" className="attach-btn" onClick={() => fileInputRef.current.click()} disabled={isForbidden}>📎</button>
                    </>
                )}

                <textarea ref={inputTextRef}
                    className="message-input"
                    value={inputText}
                    readOnly={isForbidden}
                    disabled={isForbidden}
                    onChange={(e) => setInputText(e.target.value)}
                    onKeyDown={(e) => {
                        if (e.key === 'Enter' && !e.shiftKey) {
                            e.preventDefault();
                            handleFormSubmit(e);
                        }
                        if (e.key === 'Escape' && editingMessage) {
                            cancelEdit();
                        }
                    }}
                    placeholder="Введите сообщение..."
                />
                {!isForbidden && (
                    < button type="submit" className="send-btn" disabled={isForbidden || (!inputText.trim() && pendingFiles.length === 0)}>
                        Отправить
                    </button>
                )}

            </form>
        </section>
    );
}

export default ChatWindow;