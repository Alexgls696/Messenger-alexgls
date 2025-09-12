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
    const logoutBtn = document.getElementById('logoutBtn');
    const backToListBtn = document.getElementById('backToListBtn');

    const findUserBtn = document.getElementById('findUserBtn');
    const closeModalBtn = document.getElementById('closeModalBtn');
    const userListContainer = document.getElementById('userListContainer');

    const attachFileBtn = document.getElementById('attachFileBtn');
    const fileInput = document.getElementById('fileInput');
    const attachmentPreviewContainer = document.getElementById('attachmentPreviewContainer');

    const attachmentsBtn = document.getElementById("attachmentsBtn");
    const attachmentsModal = document.getElementById("attachmentsModal");
    const closeAttachmentsBtn = document.getElementById("closeAttachmentsBtn");
    const attachmentsTabs = document.querySelectorAll(".attachments-tabs .tab-btn");
    const attachmentsContent = document.getElementById("attachmentsContent");

    const usernameContent = document.getElementById('username');
    let pendingAttachments = [];

    const contextMenu = document.createElement('div');
    contextMenu.id = 'messageContextMenu';
    contextMenu.className = 'context-menu hidden';
    document.body.appendChild(contextMenu);
    let contextMessageInfo = null;


    // --- Состояние приложения ---
    let activeChatId = null;
    let chatListPage = 0;
    let messagePage = 0;
    const pageSize = 15; // Сообщений на странице
    let isLoading = false;
    let hasMoreMessages = true;
    let participantCache = {};
    let currentUserId = null;
    let isChatsLoading = false;
    let hasMoreChats = true;

    const gatewayHost = window.location.hostname; // 'localhost'
    const gatewayPort = 8080; // Порт вашего Gateway
    const gatewayAddress = `${gatewayHost}:${gatewayPort}`;

    const httpProtocol = 'http:'; // Для локальной разработки

    const API_BASE_URL = `${httpProtocol}//${gatewayAddress}`;
    const WEB_SOCKET_API_URL = API_BASE_URL.replace('8080', '8086');

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

                        // Если сообщение имеет tempId, ищем и обновляем его
                        if (newMsg.tempId) {
                            const pendingEl = document.querySelector(`[data-temp-id='${newMsg.tempId}']`);
                            if (pendingEl) {
                                const finalEl = await createMessageElement(newMsg, isSentByMe);
                                pendingEl.replaceWith(finalEl);  // Заменяем элемент
                                return;
                            }
                        }

                        // В противном случае добавляем обычное сообщение
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
                    // Если удаление произошло в текущем активном чате
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

                // 1. Рисуем сообщение сразу в UI с временным статусом
                const pendingMsgHtml = renderPendingMessage(content, attachments, tempId);
                messagesEl.insertAdjacentHTML("beforeend", pendingMsgHtml);
                messagesEl.scrollTop = messagesEl.scrollHeight;

                // 2. Отправляем на сервер
                const chatMessage = {
                    chatId: activeChatId,
                    content: content,
                    attachments: attachments,
                    tempId: tempId
                };
                this.stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));

                // 3. Обновляем статус сразу, что сообщение отправляется
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
                // Добавляем анимацию исчезновения
                msgEl.classList.add('deleted-animation');
                // Удаляем элемент из DOM после завершения анимации
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
            hideContextMenu();


            if (!forAll) {
                handleMessageDeletion(messageIds);
            }

        } catch (error) {
            console.error('Ошибка при удалении сообщения:', error);
        }
    }

    function showContextMenu(event, messageElement) {
        event.preventDefault(); // Отменяем стандартное контекстное меню браузера

        const messageId = parseInt(messageElement.dataset.messageId);
        const isSentByMe = messageElement.classList.contains('sent');

        contextMessageInfo = {
            messageId: messageId,
            isSentByMe: isSentByMe
        };

        let menuItems = `<div class="context-menu-item" data-action="delete-for-me">Удалить у себя</div>`;
        if (isSentByMe) {
            menuItems += `<div class="context-menu-item" data-action="delete-for-all">Удалить у всех</div>`;
        }

        contextMenu.innerHTML = menuItems;
        contextMenu.style.top = `${event.pageY}px`;
        contextMenu.style.left = `${event.pageX}px`;
        contextMenu.classList.remove('hidden');
    }

    function hideContextMenu() {
        contextMenu.classList.add('hidden');
        contextMessageInfo = null;
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

        // Сбрасываем активный чат
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

        // Определяем, какое имя показать сразу
        const initialTitle = chat.group ? chat.name : 'Загрузка имени...';

        li.innerHTML = `
            <div class="chat-title">${initialTitle}</div>
            <div class="chat-type">Тип: ${chat.type}</div>
            <div class="last-message">${chat.lastMessage ? chat.lastMessage.content : 'Нет сообщений'}</div>
            <div class="message-time">${chat.lastMessage ? `Отправлено: ${formatDate(chat.lastMessage.createdAt)}` : ''}</div>
        `;

        if (!chat.group) {
            try {
                const recipient = await apiFetch(`${API_BASE_URL}/api/chats/find-recipient-by-private-chat-id/${chat.chatId}`);
                const titleDiv = li.querySelector('.chat-title');
                if (titleDiv) {
                    titleDiv.textContent = `${recipient.name} ${recipient.surname}`;
                }
            } catch (error) {
                console.error(`Не удалось загрузить собеседника для чата ${chat.chatId}:`, error);
                const titleDiv = li.querySelector('.chat-title');
                if (titleDiv) {
                    titleDiv.textContent = 'Ошибка загрузки чата'; // Имя по умолчанию в случае ошибки
                }
            }
        }

        li.addEventListener('click', () => openChat(chat));
        return li;
    }

    // В файле chats.js

    async function markMessagesAsRead(messagesToRead) {
        // Проверяем, есть ли вообще что отправлять
        if (!messagesToRead || messagesToRead.length === 0) {
            return;
        }

        // Формируем payload в том формате, который ожидает бэкенд
        const payload = messagesToRead.map(msg => ({
            messageId: msg.id,
            senderId: msg.senderId,
            chatId: activeChatId // Используем ID активного чата
        }));

        console.log('Отправка на прочтение:', payload);
        try {
            await apiFetch(`${API_BASE_URL}/api/messages/read-messages`, {
                method: 'POST',
                body: JSON.stringify(payload)
            });
        } catch (error) {
            console.error("Не удалось отправить статус прочтения:", error);
        }
    }

    async function openChat(chat) {
        if (activeChatId === chat.chatId && !chatWindowEl.classList.contains('hidden')) {
            return;
        }

        activeChatId = chat.chatId;
        messagePage = 0;
        hasMoreMessages = true;
        participantCache = {};
        // -------------------------------------------------------------------

        [...chatListEl.children].forEach(li => {
            li.classList.toggle('active', li.dataset.chatId == activeChatId);
        });

        chatWindowEl.classList.remove('hidden');
        document.body.classList.add('chat-active');

        chatTitleEl.textContent = 'Загрузка...';
        messagesEl.innerHTML = '<p class="placeholder">Загрузка данных...</p>';

        try {
            const [chatDetailsResult, messages] = await Promise.all([
                (async () => {
                    if (chat.group) {
                        chatTitleEl.textContent = chat.name;
                    } else {
                        const recipient = await apiFetch(`${API_BASE_URL}/api/chats/find-recipient-by-private-chat-id/${chat.chatId}`);
                        chatTitleEl.textContent = `Чат с ${recipient.name} ${recipient.surname}`;
                    }
                    const participants = await apiFetch(`${API_BASE_URL}/api/chats/${chat.chatId}/participants`);
                    participants.forEach(p => {
                        participantCache[p.id] = `${p.name} ${p.surname}`;
                    });
                })(),
                loadMessages(chat.chatId, messagePage)
            ]);
            renderMessages(messages);

            const unreadMessages = messages.filter(msg => !msg.read && msg.senderId !== currentUserId);
            await markMessagesAsRead(unreadMessages);

            if (messages.length === pageSize) {
                messagePage++;
            }

        } catch (error) {
            console.error("Ошибка открытия чата:", error);
            messagesEl.innerHTML = `<p class="placeholder">Не удалось загрузить данные чата.</p>`;
            chatTitleEl.textContent = 'Ошибка';
        }

        messageInput.focus();
    }

    async function loadMessages(chatId, page) {
        if (isLoading || !hasMoreMessages) return [];
        isLoading = true;
        try {
            const data = await apiFetch(`${API_BASE_URL}/api/messages?chatId=${chatId}&page=${page}&size=${pageSize}`);
            if (!Array.isArray(data) || data.length < pageSize) {
                hasMoreMessages = false;
            }
            return data;
        } catch (error) {
            console.error('Ошибка загрузки сообщений:', error);
            return [];
        } finally {
            isLoading = false;
        }
    }

    async function renderMessages(messages) {
        messagesEl.innerHTML = ''; // Очищаем контейнер
        if (!messages || messages.length === 0) {
            messagesEl.innerHTML = '<p class="placeholder">Сообщений пока нет. Напишите первым!</p>';
            return;
        }

        const fragment = document.createDocumentFragment();
        for (const msg of messages) {
            const isSentByMe = msg.senderId === currentUserId;
            const msgDiv = createMessageElement(msg, isSentByMe);
            fragment.appendChild(msgDiv);
        }

        messagesEl.appendChild(fragment); // Добавляем все сообщения в DOM за одну операцию
        messagesEl.scrollTop = messagesEl.scrollHeight; // Прокручиваем вниз
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
            attachmentsHtml = '<div class="attachments-container">';

            const attachmentItemsHtml = msg.attachments.map(att => {
                const proxyUrl = `${API_BASE_URL}/api/storage/proxy/download/by-id?id=${att.fileId}`;

                if (att.mimeType && att.mimeType.startsWith('image/')) {
                    // Просто создаем HTML-заготовку. URL на прокси кладется в data-src.
                    return `
                    <div class="attachment-item image-attachment">
                        <a href="${proxyUrl}" target="_blank" rel="noopener noreferrer">
                            <div class="skeleton skeleton-tile"></div>
                            <img class="attachment-image lazy-load" data-src="${proxyUrl}">
                        </a>
                    </div>`;
                } else {
                    // Для обычных файлов ссылка на прокси работает сразу на скачивание.
                    const fileName = att.fileName || 'file';
                    return `
                    <div class="attachment-item file-attachment">
                        <div class="file-icon">📁</div>
                        <div class="file-info">
                            <span class="file-name">${fileName || 'Файл'}</span>
                            <a href="${proxyUrl}" class="file-download-link" download="${fileName}">Скачать</a>
                        </div>
                    </div>`;
                }
            });

            attachmentsHtml += attachmentItemsHtml.join('');
            attachmentsHtml += '</div>';
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

        // 4. Находим все "ленивые" изображения в созданном сообщении...
        const imagesToLazyLoad = msgDiv.querySelectorAll('img.lazy-load');
        // ...и говорим нашему единому observer'у начать за ними следить.
        imagesToLazyLoad.forEach(img => imageObserver.observe(img));

        return msgDiv;
    }


    async function addMessageToUI(msg, isSentByMe, prepend = false) {
        const placeholder = messagesEl.querySelector('.placeholder');
        if (placeholder) placeholder.remove();

        const wasScrolledToBottom = messagesEl.scrollHeight - messagesEl.clientHeight <= messagesEl.scrollTop + 1;

        const msgDiv = createMessageElement(msg, isSentByMe);

        // Добавляем сообщение в начало или в конец
        if (prepend) {
            messagesEl.prepend(msgDiv);
        } else {
            messagesEl.appendChild(msgDiv);
        }

        // Если скроллили до низа, то прокручиваем вниз
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
            // Если изображение попало в зону видимости
            if (entry.isIntersecting) {
                const image = entry.target;
                // Запускаем асинхронную функцию загрузки
                lazyLoadImage(image);
                // Прекращаем наблюдение за этим изображением, чтобы не загружать его повторно
                observer.unobserve(image);
            }
        });
    });

    async function lazyLoadImage(imageElement) {
        const proxyUrl = imageElement.dataset.src; // Берем URL из data-src
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

            imageElement.src = objectUrl; // Устанавливаем реальный src

            // После успешной загрузки изображения в тег...
            imageElement.onload = () => {
                if (skeleton) skeleton.remove(); // ...убираем скелетон...
                // ...и освобождаем память, занятую Blob'ом
                URL.revokeObjectURL(objectUrl);
            };
            imageElement.onerror = () => {
                if (skeleton) skeleton.innerHTML = '⚠️'; // Показываем ошибку, если картинка не загрузилась
            }

        } catch (error) {
            console.error(`Failed to lazy-load image from ${proxyUrl}:`, error);
            if (skeleton) skeleton.innerHTML = '⚠️'; // Показываем ошибку
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

            // Обрабатываем успешную загрузку для разных типов медиа
            const onMediaLoaded = () => {
                if (skeleton) skeleton.remove(); // Убираем скелетон
                mediaElement.style.opacity = '1'; // Плавно показываем элемент
                // Важно освободить память после того, как медиа готово к показу
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

    function setFormEnabled(enabled) {
        messageInput.disabled = !enabled;
        sendBtn.disabled = !enabled;
        attachFileBtn.disabled = !enabled;
    }

    // Отображение превью
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


    // Удаление превью
    function removeAttachmentFromPreview(tempId) {
        pendingAttachments = pendingAttachments.filter(att => att.tempId !== tempId);
        const previewEl = attachmentPreviewContainer.querySelector(`[data-file-id='${tempId}']`);
        if (previewEl) previewEl.remove();
    }

    //Загрузка вложений
    async function loadAttachments(type) {
        if (type === "IMAGE" || type === "VIDEO") {
            attachmentsContent.innerHTML = `<div class="skeleton-grid">${Array(12).fill('<div class="skeleton skeleton-tile"></div>').join("")}</div>`;
        } else {
            attachmentsContent.innerHTML = `<div class="skeleton-list">${Array(6).fill('<div class="skeleton skeleton-row"></div>').join("")}</div>`;
        }

        try {
            const url = `${API_BASE_URL}/api/attachments/find-by-type-and-chat-id?mediaType=${type}&chatId=${activeChatId}`;
            const attachments = await apiFetch(url);

            if (!attachments || attachments.length === 0) {
                attachmentsContent.innerHTML = "<p>Нет вложений в этой категории</p>";
                return;
            }

            // Шаг 3: СИНХРОННО генерируем HTML-каркас. Больше нет await Promise.all!
            const itemsHtml = attachments.map(att => {
                // Ссылка на ваш прокси, который вернет файл
                const proxyUrl = `${API_BASE_URL}/api/storage/proxy/download/by-id?id=${att.fileId}`;
                const fileName = att.fileName || 'file';
                if (type === "IMAGE") {
                    // Генерируем "заготовку": скелетон + img с data-src
                    return `<div class="attachment-item">
                                <a href="${proxyUrl}" target="_blank">
                                    <div class="skeleton skeleton-tile"></div>
                                    <img class="lazy-load-attachment" data-src="${proxyUrl}" alt="Изображение" style="opacity:0;">
                                </a>
                            </div>`;
                } else if (type === "VIDEO") {
                    // То же самое для видео
                    return `<div class="attachment-item">
                                <div class="skeleton skeleton-tile"></div>
                                <video class="lazy-load-attachment" data-src="${proxyUrl}" controls style="opacity:0;"></video>
                            </div>`;
                } else if (type === "AUDIO") {
                    // Аудио и файлы не требуют ленивой загрузки, так как у них есть свои элементы управления
                    return `<div class="attachment-list-item">
                                <audio controls src="${proxyUrl}"></audio>
                                <a href="${proxyUrl}" download="${fileName}">Скачать</a>
                            </div>`;
                } else {
                    return `<div class="attachment-list-item">
                                <span>${fileName || "Файл"}</span>
                                <a href="${proxyUrl}" download="${fileName}">Скачать</a>
                            </div>`;
                }
            }).join('');

            // Шаг 4: Вставляем сгенерированный HTML в DOM
            if (type === "IMAGE" || type === "VIDEO") {
                attachmentsContent.innerHTML = `<div class="attachments-grid">${itemsHtml}</div>`;
            } else {
                attachmentsContent.innerHTML = `<div class="attachments-list">${itemsHtml}</div>`;
            }

            // Шаг 5: Находим все "ленивые" элементы и говорим наблюдателю начать за ними следить
            const mediaToLazyLoad = attachmentsContent.querySelectorAll('.lazy-load-attachment');
            mediaToLazyLoad.forEach(media => attachmentObserver.observe(media));

        } catch (e) {
            console.error("Ошибка загрузки вложений:", e);
            attachmentsContent.innerHTML = "<p>Не удалось загрузить вложения</p>";
        }
    }

    function renderPendingMessage(content, attachments, tempId) {
        return `
        <div class="message sent pending" data-temp-id="${tempId}">
            ${content ? `<div class="message-content">${content}</div>` : ""}
            ${attachments?.length ? renderAttachmentPreview(attachments) : ""}
            <div class="message-meta">
                <span>Отправка...</span>
                <span class="message-status sending">⏳</span>
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

            const scrollHeightBefore = messagesEl.scrollHeight;

            try {
                const messages = await loadMessages(activeChatId, messagePage);

                if (messages && messages.length > 0) {

                    const fragment = document.createDocumentFragment();


                    for (const msg of messages) {
                        const isSentByMe = msg.senderId === currentUserId;
                        const msgDiv = createMessageElement(msg, isSentByMe);
                        fragment.appendChild(msgDiv); // Добавляем в конец буфера, сохраняя порядок
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


    // Отправка сообщения по форме
    messageForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const content = messageInput.value.trim();

        if (!content && pendingAttachments.length === 0) return;

        // 1. Загружаем вложения
        const uploadedAttachments = [];
        for (let att of pendingAttachments) {
            try {
                const formData = new FormData();
                formData.append('file', att.file);

                const response = await fetch(`${API_BASE_URL}/api/storage/upload`, {
                    method: 'POST',
                    headers: {'Authorization': `Bearer ${localStorage.getItem('accessToken')}`},
                    body: formData
                });
                if (!response.ok) throw new Error("Ошибка при загрузке");

                const result = await response.json(); // { id: ... }
                uploadedAttachments.push({
                    mimeType: att.mimeType,
                    fileId: result.id,
                    fileName: att.file.name
                });

            } catch (err) {
                console.error("Ошибка загрузки файла:", err);
                alert(`Не удалось загрузить файл: ${att.file.name}`);
            }
        }

        // 2. Отправляем сообщение
        chatManager.sendMessageWithAttachments(content, uploadedAttachments);

        // 3. Чистим форму
        messageInput.value = '';
        attachmentPreviewContainer.innerHTML = '';
        pendingAttachments = [];
    });

    attachFileBtn.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', (e) => {
        const files = e.target.files;
        if (files && files.length > 0) {
            [...files].forEach(file => addAttachmentToPreview(file));
            fileInput.value = '';
        }
    });

    findUserBtn.addEventListener('click', loadAndShowUsers);

    closeModalBtn.addEventListener('click', () => userSearchModal.classList.add('hidden'));

    userSearchModal.addEventListener('click', (e) => {
        if (e.target === userSearchModal) {
            userSearchModal.classList.add('hidden');
        }
    });

    logoutBtn.addEventListener('click', () => {
        window.location.href = '/logout';
    });

    //Вложения
    attachmentsBtn.addEventListener("click", () => {
        if (!activeChatId) return;
        attachmentsModal.classList.remove("hidden");
        loadAttachments("IMAGE"); // по умолчанию картинки
    });

    closeAttachmentsBtn.addEventListener("click", () => {
        attachmentsModal.classList.add("hidden");
    });

    attachmentsTabs.forEach(tab => {
        tab.addEventListener("click", () => {
            attachmentsTabs.forEach(t => t.classList.remove("active"));
            tab.classList.add("active");
            loadAttachments(tab.dataset.type);
        });
    });

    window.addEventListener('click', (event) => {
        // Если клик не по меню, скрываем его
        if (!contextMenu.contains(event.target)) {
            hideContextMenu();
        }
    });

    contextMenu.addEventListener('click', (event) => {
        const action = event.target.dataset.action;
        if (action && contextMessageInfo) {
            const { messageId } = contextMessageInfo;
            if (action === 'delete-for-me') {
                deleteMessages([messageId], false);
            } else if (action === 'delete-for-all') {
                deleteMessages([messageId], true);
            }
        }
    });

    backToListBtn.addEventListener('click', closeActiveChat);

    // =================================================================
    // ИНИЦИАЛИЗАЦИЯ
    // =================================================================

    async function initializeApp() {
        try {
            const me = await apiFetch(`${API_BASE_URL}/api/users/me`);
            currentUserId = me.id;
            participantCache[me.id] = `${me.name} ${me.surname}`;
            usernameContent.textContent = `${me.surname} ${me.name}`;

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