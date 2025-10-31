document.addEventListener('DOMContentLoaded', () => {
    // --- DOM Элементы ---
    const statusEl = document.getElementById('status');
    const chatListEl = document.getElementById('chatList');
    const chatWindowEl = document.getElementById('chatWindow');
    const chatTitleEl = document.getElementById('chatTitle');
    const messagesEl = document.getElementById('messages');
    const closeChatBtn = document.getElementById('closeChatBtn');
    const messageForm = document.getElementById('messageForm');
    const messageInput = document.getElementById('messageInput');
    const userSearchModal = document.getElementById('userSearchModal');

    const backToListBtn = document.getElementById('backToListBtn');

    const findUserBtn = document.getElementById('findUserBtn');
    const closeModalBtn = document.getElementById('closeModalBtn');
    const userListContainer = document.getElementById('userListContainer');

    const attachFileBtn = document.getElementById('attachFileBtn');
    const fileInput = document.getElementById('fileInput');
    const attachmentPreviewContainer = document.getElementById('attachmentPreviewContainer');

    const profileBtn = document.getElementById("profileBtn");
    const myProfileBtn = document.getElementById('myProfileBtn');
    const usernameContent = document.getElementById('username');

    const settingsBtn = document.getElementById('settingsBtn');
    const settingsDropdown = document.getElementById('settingsDropdown');
    const dropdownThemeToggle = document.getElementById('dropdownThemeToggle');
    const themeToggleIcon = document.getElementById('themeToggleIcon');
    const dropdownLogout = document.getElementById('dropdownLogout');

    let currentUserData = null; // Базовые данные (из /users/me)
    let currentUserProfileData = null; // Полные данные профиля (из /profiles/{id})

    const currentTheme = localStorage.getItem('theme');


    let pendingAttachments = [];

    const contextMenu = document.createElement('div');
    contextMenu.id = 'messageContextMenu';
    contextMenu.className = 'context-menu hidden';
    document.body.appendChild(contextMenu);


    let contextMenuTarget = null;
    const themeToggleButton = document.getElementById('theme-toggle');
    const body = document.body;


    // --- Состояние приложения ---
    let activeChatId = null;
    let activeChatRecipientId = null; // ИЗМЕНЕНИЕ: ID собеседника для загрузки профиля
    let chatListPage = 0;
    let messagePage = 0;
    const pageSize = 15;
    let isLoading = false;
    let hasMoreMessages = true;
    let participantCache = {};
    let currentUserId = null;
    let isChatsLoading = false;
    let hasMoreChats = true;

    const gatewayHost = window.location.hostname;
    const gatewayPort = 8080;
    const gatewayAddress = `${gatewayHost}:${gatewayPort}`;
    const httpProtocol = 'https:';

    const API_BASE_URL = `${httpProtocol}//${gatewayAddress}`;
    const WEB_SOCKET_API_URL = API_BASE_URL;

    const chatManager = {
        stompClient: null,
        isConnected: false,
        isConnecting: false,
        retryCount: 0,
        maxRetries: 5,

        start: function () {
            if (this.isConnected || this.isConnecting) return;
            this.connect();
        },
        connect: function () {
            this.isConnecting = true;
            const accessToken = localStorage.getItem('accessToken');
            if (!accessToken) {
                statusEl.textContent = "Ошибка: токен доступа не найден. Пожалуйста, войдите снова.";
                return;
            }
            const socket = new SockJS(`${WEB_SOCKET_API_URL}/ws-chat?token=${accessToken}`);
            this.stompClient = Stomp.over(socket);
            this.stompClient.debug = null;

            this.stompClient.connect({},
                (frame) => this.onConnectSuccess(frame),
                (error) => this.onConnectError(error)
            );
        },

        onConnectSuccess: function (frame) {
            console.log('WebSocket Connected: ' + frame);
            this.isConnecting = false;
            this.isConnected = true;
            this.retryCount = 0;

            this.stompClient.subscribe(`/user/queue/messages`, async (message) => {
                try {
                    const newMsg = JSON.parse(message.body);
                    await updateOrFetchChatInList(newMsg);

                    if (newMsg.chatId === activeChatId) {
                        const isSentByMe = newMsg.senderId === currentUserId;

                        if (newMsg.tempId) {
                            const pendingEl = document.querySelector(`[data-temp-id='${newMsg.tempId}']`);
                            if (pendingEl) {
                                const finalEl = await createMessageElement(newMsg, isSentByMe);
                                pendingEl.replaceWith(finalEl);
                                return;
                            }
                        }

                        addMessageToUI(newMsg, isSentByMe);

                        if (!isSentByMe) {
                            markMessagesAsRead([newMsg]);
                        }
                    }
                } catch (error) {
                    console.error('Ошибка обработки нового сообщения:', error);
                }
            });


            this.stompClient.subscribe(`/user/queue/read-status`, (notification) => {
                try {
                    const readInfo = JSON.parse(notification.body);
                    if (readInfo.chatId === activeChatId) {
                        readInfo.messageIds.forEach(id => {
                            const msgEl = document.querySelector(`[data-message-id='${id}']`);
                            if (msgEl) {
                                const statusEl = msgEl.querySelector('.message-status');
                                if (statusEl) {
                                    statusEl.textContent = 'Прочитано';
                                    statusEl.classList.add('read');
                                }
                            }
                        });
                    }
                } catch (error) {
                    console.error('Ошибка обработки уведомления о прочтении:', error);
                }
            });

            this.stompClient.subscribe(`/user/queue/delete-event`, (message) => {
                try {
                    const deleteInfo = JSON.parse(message.body);
                    if (deleteInfo.chatId === activeChatId) {
                        handleMessageDeletion(deleteInfo.messagesId);
                    }
                } catch (error) {
                    console.error('Ошибка обработки события удаления:', error);
                }
            });
        },

        onConnectError: function (error) {
            this.isConnecting = false;
            this.isConnected = false;
            if (this.retryCount >= this.maxRetries) {
                alert("Не удалось подключиться к серверу чатов. Попробуйте обновить страницу.");
                return;
            }
            this.retryCount++;
            const delay = 1000 * this.retryCount;
            handleTokenRefresh()
            console.error(`Соединение потеряно. Повторная попытка через ${delay}ms...`, error);
            setTimeout(() => this.connect(), delay);
        },

        sendMessageWithAttachments: function (content, attachments) {
            if (this.stompClient && this.isConnected && activeChatId) {
                const tempId = generateTempId();
                const pendingMsgHtml = renderPendingMessage(content, attachments, tempId);
                messagesEl.insertAdjacentHTML("beforeend", pendingMsgHtml);
                messagesEl.scrollTop = messagesEl.scrollHeight;

                const chatMessage = {
                    chatId: activeChatId,
                    content: content,
                    attachments: attachments,
                    tempId: tempId
                };
                this.stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));

                const pendingEl = document.querySelector(`[data-temp-id='${tempId}']`);
                if (pendingEl) {
                    const statusEl = pendingEl.querySelector('.message-status');
                    statusEl.textContent = "Отправка...";
                    statusEl.classList.add('sending');
                }
            } else {
                alert("Нет подключения для отправки сообщения.");
            }
        },
    };


    function handleMessageDeletion(messageIds) {
        if (!Array.isArray(messageIds)) return;

        messageIds.forEach(id => {
            const msgEl = document.querySelector(`.message[data-message-id='${id}']`);
            if (msgEl) {
                msgEl.classList.add('deleted-animation');
                setTimeout(() => {
                    msgEl.remove();
                    // Если сообщений не осталось, показываем плейсхолдер
                    if (messagesEl.children.length === 0) {
                        messagesEl.innerHTML = '<p class="placeholder">Сообщений пока нет.</p>';
                    }
                }, 400); // Длительность анимации
            }
        });
    }

    async function deleteChat(chatId) {
        const isConfirmed = confirm("Вы действительно хотите удалить этот чат? Это действие необратимо.");
        if (!isConfirmed) return;

        try {
            await apiFetch(`${API_BASE_URL}/api/chats/${chatId}`, {
                method: 'DELETE'
            });

            const chatLi = chatListEl.querySelector(`[data-chat-id='${chatId}']`);
            if (chatLi) {
                chatLi.remove();
            }

            if (activeChatId === chatId) {
                closeActiveChat();
            }

        } catch (error) {
            console.error("Ошибка при удалении чата:", error);
            alert("Не удалось удалить чат. Возможно, он уже удален или у вас нет прав.");
        } finally {
            hideContextMenu();
        }
    }

    async function deleteMessages(messageIds, forAll) {
        if (!messageIds || messageIds.length === 0 || !activeChatId) return;

        const payload = {
            messagesId: messageIds,
            senderId: currentUserId,
            chatId: activeChatId,
            forAll: forAll
        };

        try {
            await apiFetch(`${API_BASE_URL}/api/messages`, {
                method: 'DELETE',
                body: JSON.stringify(payload)
            });
            if (!forAll) {
                handleMessageDeletion(messageIds);
            }

        } catch (error) {
            console.error('Ошибка при удалении сообщения:', error);
        } finally {
            hideContextMenu()
        }
    }

    function showChatContextMenu(event, chatId) {
        event.preventDefault();
        event.stopPropagation();

        contextMenuTarget = {
            type: 'chat',
            chatId: chatId
        };

        contextMenu.innerHTML = `
            <div class="context-menu-item danger" data-action="delete-chat">
                Удалить чат
            </div>
        `;

        contextMenu.style.top = `${event.pageY}px`;
        contextMenu.style.left = `${event.pageX}px`;
        contextMenu.classList.remove('hidden');
    }

    function showContextMenu(event, messageElement) {
        event.preventDefault();

        const messageId = parseInt(messageElement.dataset.messageId);
        const isSentByMe = messageElement.classList.contains('sent');

        contextMenuTarget = {
            type: 'message',
            data: {
                messageId: messageId,
                isSentByMe: isSentByMe
            }
        };

        let menuItems = `<div class="context-menu-item" data-action="delete-for-me">Удалить у себя</div>`;
        if (isSentByMe) {
            menuItems += `<div class="context-menu-item danger" data-action="delete-for-all">Удалить у всех</div>`;
        }

        contextMenu.innerHTML = menuItems;
        contextMenu.style.top = `${event.pageY}px`;
        contextMenu.style.left = `${event.pageX}px`;
        contextMenu.classList.remove('hidden');
    }

    function hideContextMenu() {
        contextMenu.classList.add('hidden');
        contextMenuTarget = null;
    }

    async function updateOrFetchChatInList(newMsg) {
        const chatId = newMsg.chatId;
        const existingChatItemEl = chatListEl.querySelector(`[data-chat-id='${chatId}']`);

        if (existingChatItemEl) {
            const lastMsgEl = existingChatItemEl.querySelector('.last-message');
            const timeEl = existingChatItemEl.querySelector('.message-time');

            if (lastMsgEl) {
                lastMsgEl.textContent = newMsg.content || 'Вложение';
            }
            if (timeEl) {
                timeEl.textContent = `Отправлено: ${formatDate(newMsg.createdAt)}`;
            }
            chatListEl.prepend(existingChatItemEl);
        } else {
            try {
                const newChatDto = await apiFetch(`${API_BASE_URL}/api/chats/${chatId}`);
                const newChatItemEl = await createChatItem(newChatDto);
                chatListEl.prepend(newChatItemEl);

            } catch (error) {
                console.error(`Не удалось загрузить информацию о новом чате #${chatId}:`, error);
            }
        }
    }

    function renderUsers(users) {
        userListContainer.innerHTML = '';
        if (!users || users.length === 0) {
            userListContainer.innerHTML = '<p class="placeholder">Пользователи не найдены.</p>';
            return;
        }
        users.forEach(user => {
            if (user.id === currentUserId) return;

            const userDiv = document.createElement('div');
            userDiv.className = 'user-item';
            userDiv.innerHTML = `
                <div class="user-name">${user.name} ${user.surname}</div>
                <div class="user-username">@${user.username}</div>
            `;
            // Вот здесь используется ваша функция startChatWithUser
            userDiv.addEventListener('click', () => startChatWithUser(user));
            userListContainer.appendChild(userDiv);
        });
    }

    function closeActiveChat() {
        document.body.classList.remove('chat-active');
        chatWindowEl.classList.add('hidden');
        activeChatId = null;
        [...chatListEl.children].forEach(li => li.classList.remove('active'));
    }

    async function loadAndShowUsers() {
        userListContainer.innerHTML = '<p class="placeholder">Загрузка пользователей...</p>';
        userSearchModal.classList.remove('hidden');
        try {
            const users = await apiFetch(`${API_BASE_URL}/api/users`);
            renderUsers(users);
        } catch (error) {
            userListContainer.innerHTML = `<p class="placeholder">Ошибка загрузки пользователей: ${error.message}</p>`;
        }
    }

    function formatDate(isoString) {
        if (!isoString) return '';
        const date = new Date(isoString);
        return date.toLocaleString('ru-RU', {
            hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric'
        });
    }

    async function loadChats() {
        if (isChatsLoading || !hasMoreChats) return;
        isChatsLoading = true;
        try {
            const data = await apiFetch(`${API_BASE_URL}/api/chats/find-by-id/${chatListPage}`);
            statusEl.textContent = '';
            if (Array.isArray(data) && data.length > 0) {
                const chatItemsPromises = data.map(chat => createChatItem(chat));
                const chatItems = await Promise.all(chatItemsPromises);
                chatItems.forEach(li => chatListEl.appendChild(li));
                chatListPage++;
            } else {
                hasMoreChats = false;
                if (chatListPage === 0) {
                    statusEl.textContent = 'Чаты не найдены';
                }
            }
        } catch (error) {
            statusEl.textContent = `Ошибка загрузки чатов: ${error.message}`;
        } finally {
            isChatsLoading = false;
        }
    }

    async function createChatItem(chat) {
        const li = document.createElement('li');
        li.dataset.chatId = chat.chatId;

        li.innerHTML = `
        <img class="chat-item-avatar" src="/images/profile-default.png" alt="Аватар чата">
        <div class="chat-info">
            <div class="chat-title">${chat.group ? chat.name : 'Загрузка...'}</div>
            <div class="last-message">${chat.lastMessage ? chat.lastMessage.content || 'Вложение' : 'Нет сообщений'}</div>
            <div class="message-time">${chat.lastMessage ? `Отправлено: ${formatDate(chat.lastMessage.createdAt)}` : ''}</div>
        </div>
    `;

        if (!chat.group) {
            const titleDiv = li.querySelector('.chat-title');
            const avatarImg = li.querySelector('.chat-item-avatar');

            try {
                const recipient = await apiFetch(`${API_BASE_URL}/api/chats/find-recipient-by-private-chat-id/${chat.chatId}`);

                if (titleDiv) {
                    titleDiv.textContent = `${recipient.name} ${recipient.surname}`;
                }

                try {
                    const avatarId = await apiFetch(`${API_BASE_URL}/api/profiles/images/user-avatar/${recipient.id}`);
                    if (avatarId && typeof avatarId === 'number') {
                        const authToken = localStorage.getItem('accessToken');
                        imageLoader.getImageSrc(avatarId, API_BASE_URL, authToken)
                            .then(src => {
                                avatarImg.src = src;
                            });
                    }
                } catch (avatarError) {
                    if (avatarError.status === 404) {
                    } else {
                        console.warn(`Не удалось загрузить аватар для пользователя ${recipient.id}:`, avatarError);
                    }
                }

            } catch (error) {
                console.error(`Не удалось загрузить собеседника для чата ${chat.chatId}:`, error);
                if (titleDiv) {
                    titleDiv.textContent = 'Ошибка загрузки чата';
                }
            }
        }

        li.addEventListener('click', () => openChat(chat));

        li.addEventListener('contextmenu', (e) => showChatContextMenu(e, chat.chatId));

        li.addEventListener('click', () => openChat(chat));
        return li;
    }

    async function markMessagesAsRead(messagesToRead) {
        if (!messagesToRead || messagesToRead.length === 0) {
            return;
        }
        const payload = messagesToRead.map(msg => ({
            messageId: msg.id,
            senderId: msg.senderId,
            chatId: activeChatId
        }));
        try {
            await apiFetch(`${API_BASE_URL}/api/messages/read-messages`, {
                method: 'POST',
                body: JSON.stringify(payload)
            });
        } catch (error) {
            console.error("Не удалось отправить статус прочтения:", error);
        }
    }

    let chatLoadController = null;

    async function openChat(chat) {
        if (activeChatId === chat.chatId && !chatWindowEl.classList.contains('hidden')) {
            return;
        }

        if (chatLoadController) {
            chatLoadController.abort();
        }
        // Создаем новый контроллер для текущей операции
        chatLoadController = new AbortController();
        const signal = chatLoadController.signal;

        // --- БЛОК СБРОСА СОСТОЯНИЯ ---
        activeChatId = chat.chatId;
        messagePage = 0; // Сбрасываем страницу
        hasMoreMessages = true; // Сбрасываем флаг
        participantCache = {};
        isLoading = false; // Этот флаг все еще полезен для скролла

        const openingChatId = chat.chatId;

        // --- Обновление UI ---
        [...chatListEl.children].forEach(li => {
            li.classList.toggle('active', li.dataset.chatId == activeChatId);
        });
        chatWindowEl.classList.remove('hidden');
        document.body.classList.add('chat-active');
        chatTitleEl.textContent = 'Загрузка...';
        messagesEl.innerHTML = '<p class="placeholder">Загрузка данных...</p>';
        profileBtn.style.display = chat.group ? 'none' : 'inline-block';

        // --- Загрузка данных ---
        try {
            // Загрузка деталей чата
            await (async () => {
                // ... ваш код загрузки деталей чата без изменений ...
                if (chat.group) {
                    chatTitleEl.textContent = chat.name;
                } else {
                    const recipient = await apiFetch(`${API_BASE_URL}/api/chats/find-recipient-by-private-chat-id/${chat.chatId}`);
                    chatTitleEl.textContent = `Чат с ${recipient.name} ${recipient.surname}`;
                    activeChatRecipientId = recipient.id;
                }
                const participants = await apiFetch(`${API_BASE_URL}/api/chats/${chat.chatId}/participants`);
                participants.forEach(p => {
                    participantCache[p.id] = `${p.name} ${p.surname}`;
                });
            })();

            // Загрузка сообщений с возможностью отмены
            const { messages, hasMore } = await loadMessages(openingChatId, 0, signal);

            // Проверяем, не была ли операция отменена во время выполнения
            if (signal.aborted) {
                return;
            }

            // Обновляем состояние ПОСЛЕ получения ответа
            hasMoreMessages = hasMore;
            if (hasMore) {
                messagePage = 1;
            }

            // Рендеринг и прокрутка
            const { firstUnreadId } = await renderMessages(messages);
            if (firstUnreadId) {
                const firstUnreadElement = messagesEl.querySelector(`[data-message-id='${firstUnreadId}']`);
                if (firstUnreadElement) {
                    firstUnreadElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            } else {
                messagesEl.scrollTop = messagesEl.scrollHeight;
            }

            // Отметка о прочтении
            const unreadMessages = messages.filter(msg => !msg.read && msg.senderId !== currentUserId);
            await markMessagesAsRead(unreadMessages);

        } catch (error) {
            if (error.name !== 'AbortError') {
                console.error("Ошибка открытия чата:", error);
                if (openingChatId === activeChatId) {
                    messagesEl.innerHTML = `<p class="placeholder">Не удалось загрузить данные чата.</p>`;
                    chatTitleEl.textContent = 'Ошибка';
                }
            }
        }

        messageInput.focus();
    }

    async function loadMessages(chatId, page, signal) {
        try {
            const data = await apiFetch(`${API_BASE_URL}/api/messages?chatId=${chatId}&page=${page}&pageSize=${pageSize}`, { signal });

            const hasMore = Array.isArray(data) && data.length === pageSize;
            return { messages: data || [], hasMore };

        } catch (error) {
            if (error.name === 'AbortError') {
                // Это не ошибка, а ожидаемая отмена. Просто возвращаем пустой результат.
                console.log(`Запрос сообщений для чата ${chatId} был отменен.`);
                return { messages: [], hasMore: false };
            }
            console.error('Ошибка загрузки сообщений:', error);
            return { messages: [], hasMore: false };
        }
    }

    async function renderMessages(messages) {
        messagesEl.innerHTML = ''; // Очищаем контейнер
        if (!messages || messages.length === 0) {
            messagesEl.innerHTML = '<p class="placeholder">Сообщений пока нет. Напишите первым!</p>';
            return { firstUnreadId: null }; // Возвращаем, что непрочитанных нет
        }

        let firstUnreadId = null;

        const fragment = document.createDocumentFragment();
        for (const msg of messages) {
            const isSentByMe = msg.senderId === currentUserId;

            if (!isSentByMe && !msg.read && firstUnreadId === null) {
                firstUnreadId = msg.id;
            }

            const msgDiv = createMessageElement(msg, isSentByMe);
            fragment.appendChild(msgDiv);
        }

        messagesEl.appendChild(fragment);

        return { firstUnreadId };
    }


    function createMessageElement(msg, isSentByMe) {
        const msgDiv = document.createElement('div');
        msgDiv.className = `message ${isSentByMe ? 'sent' : 'received'}`;
        msgDiv.dataset.messageId = msg.id;

        msgDiv.addEventListener('contextmenu', (event) => {
            showContextMenu(event, msgDiv);
        });

        const messageType = msg.type || msg.messageType;
        const senderName = isSentByMe ? '' : (participantCache[msg.senderId] || `Пользователь #${msg.senderId}`);
        const senderHtml = senderName ? `<div class="message-sender">${senderName}</div>` : '';

        let attachmentsHtml = '';
        if (msg.attachments && msg.attachments.length > 0) {

            // Разделяем вложения на картинки и файлы
            const imageAttachments = msg.attachments.filter(att => att.mimeType && att.mimeType.startsWith('image/'));
            const fileAttachments = msg.attachments.filter(att => !att.mimeType || !att.mimeType.startsWith('image/'));

            let imageContentHtml = '';
            let fileContentHtml = '';

            // --- Генерация HTML для картинок ---
            if (imageAttachments.length > 0) {
                const imageItemsHtml = imageAttachments.map(att => {
                    const proxyUrl = `${API_BASE_URL}/api/storage/proxy/download/by-id?id=${att.fileId}`;

                    return `
                    <div class="attachment-item image-attachment viewer-enabled" data-file-id="${att.fileId}">
                        <div class="skeleton skeleton-tile"></div>
                        <img class="attachment-image lazy-load" data-src="${proxyUrl}">
                    </div>`;
                }).join('');

                // Если картинок больше одной, оборачиваем их в сетку. Если одна - оставляем как есть.
                if (imageAttachments.length > 1) {
                    imageContentHtml = `<div class="image-gallery-grid">${imageItemsHtml}</div>`;
                } else {
                    imageContentHtml = imageItemsHtml;
                }
            }

            // --- Генерация HTML для файлов ---
            if (fileAttachments.length > 0) {
                fileContentHtml = fileAttachments.map(att => {
                    const proxyUrl = `${API_BASE_URL}/api/storage/proxy/download/by-id?id=${att.fileId}`;
                    const fileName = att.fileName || 'file';
                    return `
                    <div class="attachment-item file-attachment">
                        <div class="file-icon">📁</div>
                        <div class="file-info">
                            <span class="file-name">${fileName || 'Файл'}</span>
                            <a href="${proxyUrl}" class="file-download-link" download="${fileName}">Скачать</a>
                        </div>
                    </div>`;
                }).join('');
            }

            // Собираем всё вместе
            attachmentsHtml = `<div class="attachments-container">${imageContentHtml}${fileContentHtml}</div>`;
        }

        const contentHtml = messageType === 'TEXT'
            ? `<div class="message-content">${msg.content}</div>`
            : (msg.content && attachmentsHtml ? `<div class="message-content">${msg.content}</div>` : '');

        const statusText = isSentByMe ? (msg.read ? 'Прочитано' : 'Доставлено') : '';
        const statusClass = isSentByMe && msg.read ? 'read' : '';

        msgDiv.innerHTML = `
            ${senderHtml}
            ${attachmentsHtml}
            ${contentHtml}
            <div class="message-meta">
                <span>${formatDate(msg.createdAt)}</span>
                <span class="message-status ${statusClass}">${statusText}</span>
            </div>`;

        const imagesToLazyLoad = msgDiv.querySelectorAll('img.lazy-load');
        imagesToLazyLoad.forEach(img => imageObserver.observe(img));

        return msgDiv;
    }


    async function addMessageToUI(msg, isSentByMe, prepend = false) {
        const placeholder = messagesEl.querySelector('.placeholder');
        if (placeholder) placeholder.remove();

        const wasScrolledToBottom = messagesEl.scrollHeight - messagesEl.clientHeight <= messagesEl.scrollTop + 1;

        const msgDiv = createMessageElement(msg, isSentByMe);

        if (prepend) {
            messagesEl.prepend(msgDiv);
        } else {
            messagesEl.appendChild(msgDiv);
        }

        if (wasScrolledToBottom && !prepend) {
            messagesEl.scrollTop = messagesEl.scrollHeight;
        }
    }


    async function startChatWithUser(user) {
        console.log(`Попытка начать чат с пользователем ID: ${user.id}`);
        try {
            const chat = await apiFetch(`${API_BASE_URL}/api/chats/private/${user.id}`, {
                method: 'POST',
            });

            userSearchModal.classList.add('hidden');

            const existingChatItem = chatListEl.querySelector(`[data-chat-id='${chat.chatId}']`);
            if (!existingChatItem) {
                const newChatItem = await createChatItem(chat);
                chatListEl.prepend(newChatItem);
            }
            openChat(chat);
        } catch (error) {
            alert(`Не удалось создать чат: ${error.message}`);
        }
    }

    const attachmentObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const mediaElement = entry.target;
                lazyLoadAttachmentMedia(mediaElement);
                observer.unobserve(mediaElement);
            }
        });
    });

    const imageObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const image = entry.target;
                lazyLoadImage(image);
                observer.unobserve(image);
            }
        });
    });

    async function lazyLoadImage(imageElement) {
        const proxyUrl = imageElement.dataset.src;
        if (!proxyUrl) return;

        const accessToken = localStorage.getItem('accessToken');
        const skeleton = imageElement.closest('.image-attachment')?.querySelector('.skeleton');

        try {
            const response = await fetch(proxyUrl, {
                headers: {'Authorization': `Bearer ${accessToken}`}
            });
            if (!response.ok) throw new Error(`Network error: ${response.status}`);

            const fileBlob = await response.blob();
            const objectUrl = URL.createObjectURL(fileBlob);

            imageElement.src = objectUrl;

            imageElement.onload = () => {
                if (skeleton) skeleton.remove();
                URL.revokeObjectURL(objectUrl);
            };
            imageElement.onerror = () => {
                if (skeleton) skeleton.innerHTML = '⚠️';
            }

        } catch (error) {
            console.error(`Failed to lazy-load image from ${proxyUrl}:`, error);
            if (skeleton) skeleton.innerHTML = '⚠️';
        }
    }

    async function lazyLoadAttachmentMedia(mediaElement) {
        const proxyUrl = mediaElement.dataset.src;
        if (!proxyUrl) return;

        const accessToken = localStorage.getItem('accessToken');
        const container = mediaElement.closest('.attachment-item');
        const skeleton = container?.querySelector('.skeleton');

        try {
            const response = await fetch(proxyUrl, {
                headers: {'Authorization': `Bearer ${accessToken}`}
            });
            if (!response.ok) throw new Error(`Network error: ${response.status}`);

            const fileBlob = await response.blob();
            const objectUrl = URL.createObjectURL(fileBlob);

            mediaElement.src = objectUrl;

            const onMediaLoaded = () => {
                if (skeleton) skeleton.remove();
                mediaElement.style.opacity = '1';
                setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
            };

            if (mediaElement.tagName === 'VIDEO') {
                mediaElement.onloadeddata = onMediaLoaded;
            } else {
                mediaElement.onload = onMediaLoaded;
            }

            mediaElement.onerror = () => {
                if (skeleton) skeleton.innerHTML = '⚠️';
            }

        } catch (error) {
            console.error(`Failed to lazy-load attachment from ${proxyUrl}:`, error);
            if (skeleton) skeleton.innerHTML = '⚠️';
        }
    }


    function addAttachmentToPreview(file) {
        const tempId = `temp-${Date.now()}`;
        const previewEl = document.createElement('div');
        previewEl.className = 'attachment-preview-item';
        previewEl.dataset.fileId = tempId;

        const isImage = file.type.startsWith('image/');
        const previewContent = isImage
            ? `<img src="${URL.createObjectURL(file)}" alt="${file.name}">`
            : `<span>📁 ${file.name}</span>`;

        previewEl.innerHTML = `
        ${previewContent}
        <button class="remove-attachment-btn">&times;</button>
    `;

        previewEl.querySelector('.remove-attachment-btn').addEventListener('click', () => {
            removeAttachmentFromPreview(tempId);
        });

        attachmentPreviewContainer.appendChild(previewEl);

        pendingAttachments.push({
            file,
            mimeType: file.type,
            tempId
        });
    }


    function removeAttachmentFromPreview(tempId) {
        pendingAttachments = pendingAttachments.filter(att => att.tempId !== tempId);
        const previewEl = attachmentPreviewContainer.querySelector(`[data-file-id='${tempId}']`);
        if (previewEl) previewEl.remove();
    }

    function renderPendingMessage(content, localAttachments, tempId) {

        let attachmentsHtml = '';
        if (localAttachments && localAttachments.length > 0) {

            // Мы повторяем ту же логику группировки, что и в createMessageElement
            const imageAttachments = localAttachments.filter(att => att.file.type.startsWith('image/'));
            const fileAttachments = localAttachments.filter(att => !att.file.type.startsWith('image/'));

            let imageContentHtml = '';
            let fileContentHtml = '';

            if (imageAttachments.length > 0) {
                const imageItemsHtml = imageAttachments.map(att => {
                    const localUrl = URL.createObjectURL(att.file);
                    // ИСПОЛЬЗУЕМ ТЕ ЖЕ КЛАССЫ, ЧТО И В createMessageElement
                    return `
                    <div class="attachment-item image-attachment">
                        <div class="skeleton skeleton-tile" style="background-image: url(${localUrl}); background-size: cover;"></div>
                    </div>`;
                }).join('');

                if (imageAttachments.length > 1) {
                    imageContentHtml = `<div class="image-gallery-grid">${imageItemsHtml}</div>`;
                } else {
                    imageContentHtml = imageItemsHtml;
                }
            }

            if (fileAttachments.length > 0) {
                fileContentHtml = fileAttachments.map(att => {
                    return `
                    <div class="attachment-item file-attachment">
                        <div class="file-icon">📁</div>
                        <div class="file-info">
                            <span class="file-name">${att.file.name || 'Файл'}</span>
                        </div>
                    </div>`;
                }).join('');
            }

            attachmentsHtml = `<div class="attachments-container">${imageContentHtml}${fileContentHtml}</div>`;
        }

        const contentHtml = content ? `<div class="message-content">${content}</div>` : '';

        return `
            <div class="message sent pending" data-temp-id="${tempId}">
                ${attachmentsHtml}
                ${contentHtml}
                <div class="message-meta">
                    <span>Отправка...</span>
                    <span class="message-status">⏳</span>
                </div>
            </div>
        `;
    }


    async function renderAttachmentPreview(attachments) {
        return `
        <div class="attachments-container">
            ${attachments.map(a => {
            if (a.mimeType.startsWith("image/")) {
                // Для изображений показываем скелетон до загрузки
                return `
                        <div class="attachment-item">
                            <div class="image-skeleton skeleton"></div>
                            <img src="${a.src}" alt="Изображение" class="attachment-image" style="display:none;">
                        </div>`;
            } else {
                return `
                        <div class="attachment-item">
                            <span>${a.mimeType}</span>
                        </div>`;
            }
        }).join("")}
        </div>
    `;
    }

    const allImages = document.querySelectorAll('.attachment-image');
    allImages.forEach(img => {
        img.onload = () => handleImageLoad(img);
        img.src = img.dataset.src;  // Применяем реальный src
    });

    function handleImageLoad(imgElement) {
        imgElement.style.display = "block"; // Показываем изображение
        const skeleton = imgElement.previousElementSibling;
        if (skeleton && skeleton.classList.contains("image-skeleton")) {
            skeleton.remove(); // Убираем скелетон
        }
    }


    function generateTempId() {
        return 'temp-' + Date.now() + '-' + Math.floor(Math.random() * 10000);
    }


    // --- Обработчики событий ---

    closeChatBtn.addEventListener('click', () => {
        chatWindowEl.classList.add('hidden');
        activeChatId = null;
        [...chatListEl.children].forEach(li => li.classList.remove('active'));
    });

    function debounce(func, delay) {
        let timeout;
        return function (...args) {
            const context = this;
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(context, args), delay);
        };
    }

    chatListEl.addEventListener('scroll', debounce(() => {
        if (chatListEl.scrollTop + chatListEl.clientHeight >= chatListEl.scrollHeight - 100) {
            loadChats();
        }
    }, 300));

    messagesEl.addEventListener('scroll', async () => {
        if (messagesEl.scrollTop === 0 && hasMoreMessages && !isLoading) {

            const scrollChatId = activeChatId; // Запоминаем ID чата в момент начала скролла

            const scrollHeightBefore = messagesEl.scrollHeight;
            try {
                const messages = await loadMessages(scrollChatId, messagePage);


                if (scrollChatId !== activeChatId) {
                    return;
                }

                if (messages && messages.length > 0) {
                    const fragment = document.createDocumentFragment();
                    for (const msg of messages) {
                        const isSentByMe = msg.senderId === currentUserId;
                        const msgDiv = createMessageElement(msg, isSentByMe);
                        fragment.appendChild(msgDiv);
                    }
                    messagesEl.prepend(fragment);
                    messagesEl.scrollTop = messagesEl.scrollHeight - scrollHeightBefore;
                    messagePage++;
                }
            } catch (error) {
                console.error("Ошибка при подгрузке старых сообщений:", error);
            }
        }
    });

    messageForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const content = messageInput.value.trim();
        const localAttachments = [...pendingAttachments]; // Копируем массив, так как он будет очищен

        if (!content && localAttachments.length === 0) return;

        // 1. Генерируем временный ID
        const tempId = generateTempId();

        // 2. Немедленно отображаем временное сообщение в UI
        const pendingMsgHtml = renderPendingMessage(content, localAttachments, tempId);
        messagesEl.insertAdjacentHTML("beforeend", pendingMsgHtml);
        messagesEl.scrollTop = messagesEl.scrollHeight;

        // 3. Немедленно очищаем форму
        messageInput.value = '';
        attachmentPreviewContainer.innerHTML = '';
        pendingAttachments = [];

        // 4. Запускаем загрузку файлов и отправку в фоне, не блокируя UI
        (async () => {
            const uploadedAttachments = [];
            for (let att of localAttachments) {
                try {
                    const formData = new FormData();
                    formData.append('file', att.file);

                    const response = await fetch(`${API_BASE_URL}/api/storage/upload`, {
                        method: 'POST',
                        headers: {'Authorization': `Bearer ${localStorage.getItem('accessToken')}`},
                        body: formData
                    });

                    if (!response.ok) throw new Error(`Ошибка загрузки файла: ${att.file.name}`);

                    const result = await response.json();
                    uploadedAttachments.push({
                        mimeType: att.mimeType,
                        fileId: result.id,
                        fileName: att.file.name
                    });

                } catch (err) {
                    console.error("Ошибка загрузки файла:", err);
                    const pendingEl = document.querySelector(`[data-temp-id='${tempId}']`);
                    if (pendingEl) {
                        pendingEl.querySelector('.message-meta span').textContent = 'Ошибка отправки';
                    }
                    return;
                }
            }

            const chatMessage = {
                chatId: activeChatId,
                content: content,
                attachments: uploadedAttachments,
                tempId: tempId
            };

            if (chatManager.stompClient && chatManager.isConnected) {
                chatManager.stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
            } else {
                alert("Нет подключения для отправки сообщения.");
                const pendingEl = document.querySelector(`[data-temp-id='${tempId}']`);
                if (pendingEl) {
                    pendingEl.querySelector('.message-meta span').textContent = 'Ошибка сети';
                }
            }
        })();
    });

    attachFileBtn.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', (e) => {
        const files = e.target.files;
        if (files && files.length > 0) {
            [...files].forEach(file => addAttachmentToPreview(file));
            fileInput.value = '';
        }
    });

    document.body.addEventListener('click', (event) => {
        // Ищем ближайшего родителя с классом 'viewer-enabled'
        const viewerTarget = event.target.closest('.viewer-enabled');

        if (viewerTarget) {
            event.preventDefault(); // Отменяем стандартное действие (переход по ссылке или скачивание)
            const fileId = parseInt(viewerTarget.dataset.fileId, 10);
            if (fileId) {
                photoViewer.open(fileId);
            }
        }
    });

    findUserBtn.addEventListener('click', loadAndShowUsers);
    closeModalBtn.addEventListener('click', () => userSearchModal.classList.add('hidden'));
    userSearchModal.addEventListener('click', (e) => {
        if (e.target === userSearchModal) {
            userSearchModal.classList.add('hidden');
        }
    });

    profileBtn.addEventListener("click", () => {
        if (!activeChatRecipientId || !activeChatId) return;

        const recipientName = participantCache[activeChatRecipientId] || 'Собеседник';

        userProfile.open(activeChatRecipientId, activeChatId, recipientName);
    });

    window.addEventListener('click', (event) => {
        if (!contextMenu.contains(event.target)) {
            hideContextMenu();
        }
    });

    contextMenu.addEventListener('click', (event) => {
        const action = event.target.dataset.action;

        if (!action || !contextMenuTarget) return;

        if (contextMenuTarget.type === 'message') {
            const { messageId } = contextMenuTarget.data;
            if (action === 'delete-for-me') {
                deleteMessages([messageId], false);
            } else if (action === 'delete-for-all') {
                deleteMessages([messageId], true);
            }
        }

        else if (contextMenuTarget.type === 'chat') {
            if (action === 'delete-chat') {
                deleteChat(contextMenuTarget.chatId);
            }
        }

        // Скрываем меню после любого действия
        hideContextMenu();
    });

    backToListBtn.addEventListener('click', closeActiveChat);

    myProfileBtn.addEventListener('click', () => {
        if (currentUserData && currentUserProfileData) {
            myProfileManager.openWithPreloadedData(currentUserData, currentUserProfileData);
        } else {
            console.warn("Данные профиля не были предзагружены, используется стандартный метод открытия.");
            myProfileManager.open(currentUserData);
        }
    });

    settingsBtn.addEventListener('click', (event) => {
        event.stopPropagation(); // Останавливаем "всплытие" события, чтобы не сработал window.onclick
        settingsDropdown.classList.toggle('hidden');
    });

    // Закрытие меню по клику в любом другом месте экрана
    window.addEventListener('click', () => {
        if (!settingsDropdown.classList.contains('hidden')) {
            settingsDropdown.classList.add('hidden');
        }
    });

    if (currentTheme === 'dark') {
        body.setAttribute('data-theme', 'dark');
        themeToggleIcon.textContent = '☀️';
    } else {
        body.setAttribute('data-theme', 'light');
        themeToggleIcon.textContent = '🌙';
    }

    dropdownThemeToggle.addEventListener('click', () => {
        let newTheme;
        if (body.getAttribute('data-theme') === 'dark') {
            newTheme = 'light';
            themeToggleIcon.textContent = '🌙';
        } else {
            newTheme = 'dark';
            themeToggleIcon.textContent = '☀️';
        }
        body.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
    });

    // Логика выхода (перенесена сюда)
    dropdownLogout.addEventListener('click', () => {
        window.location.href = '/logout';
    });

    const updateHeaderUI = (userData) => {
        if (!userData) {
            console.error("updateHeaderUI была вызвана без данных пользователя.");
            return;
        }

        currentUserData = userData; // Сохраняем базовые данные

        const name = userData.name || '';
        const surname = userData.surname || '';
        const userId = userData.id;

        if (!userId || !name) {
            console.error("Полученные данные пользователя не содержат id или name.", userData);
            return;
        }

        usernameContent.textContent = `${name} ${surname}`.trim();
        participantCache[userId] = `${name} ${surname}`.trim();
    };

    const refreshUserData = async () => {
        try {
            const me = await apiFetch(`${API_BASE_URL}/api/users/me`);
            updateHeaderUI(me);
            currentUserProfileData = await apiFetch(`${API_BASE_URL}/api/profiles/${me.id}`);
        } catch (error) {
            console.error("Не удалось перезагрузить данные пользователя:", error);
        }
    };

    // --- ИНИЦИАЛИЗАЦИЯ ---
    async function initializeApp() {
        try {
            const me = await apiFetch(`${API_BASE_URL}/api/users/me`);
            if (me.name === null) {
                window.location.href = 'setup-profile';
                return;
            }
            currentUserId = me.id;
            myProfileManager.init(currentUserId, API_BASE_URL, refreshUserData);
            photoViewer.init({apiBaseUrl: API_BASE_URL});
            userProfile.init({
                apiBaseUrl: API_BASE_URL,
                observer: attachmentObserver
            });

            updateHeaderUI(me);

            participantCache[me.id] = `${me.name} ${me.surname}`;
            const headerAvatarImg = document.getElementById('headerAvatarImg');
            const authToken = localStorage.getItem('accessToken');

            try {
                currentUserProfileData = await apiFetch(`${API_BASE_URL}/api/profiles/${currentUserId}`);
                if (currentUserProfileData && currentUserProfileData.avatarId) {
                    imageLoader.getImageSrc(currentUserProfileData.avatarId, API_BASE_URL, authToken)
                        .then(src => {
                            headerAvatarImg.src = src;
                        });
                }
            } catch (profileError) {
                console.error("Не удалось предварительно загрузить данные профиля:", profileError);
            }


            statusEl.textContent = 'Загрузка чатов...';
            loadChats();
            chatManager.start();

        } catch (error) {
            console.error("Ошибка инициализации:", error);
            statusEl.textContent = "Не удалось загрузить данные пользователя. Пожалуйста, войдите снова.";
        }
    }

    initializeApp();
});