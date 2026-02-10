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
    const closeModalBtn = document.getElementById('closeSearchModalBtn');

    const attachFileBtn = document.getElementById('attachFileBtn');
    const fileInput = document.getElementById('fileInput');
    const attachmentPreviewContainer = document.getElementById('attachmentPreviewContainer');

    const profileBtn = document.getElementById("profileBtn");
    const myProfileBtn = document.getElementById('myProfileBtn');
    const usernameContent = document.getElementById('username');
    const groupInfoBtn = document.getElementById('groupInfoBtn');

    const settingsBtn = document.getElementById('settingsBtn');
    const settingsDropdown = document.getElementById('settingsDropdown');
    const dropdownThemeToggle = document.getElementById('dropdownThemeToggle');
    const themeToggleIcon = document.getElementById('themeToggleIcon');
    const dropdownLogout = document.getElementById('dropdownLogout');

    const mobileMenuBtn = document.getElementById('mobileMenuBtn');
    const mobileDropdownMenu = document.getElementById('mobileDropdownMenu');

    const myProfileBtnMobile = document.getElementById('myProfileBtnMobile');
    const headerAvatarImgMobile = document.getElementById('headerAvatarImgMobile');
    const usernameMobile = document.getElementById('usernameMobile');

    const mobileFindUser = document.getElementById('mobileFindUser');
    const mobileThemeToggle = document.getElementById('mobileThemeToggle');
    const mobileThemeIcon = document.getElementById('mobileThemeIcon');
    const mobileLogout = document.getElementById('mobileLogout');

    const usernameSearchForm = document.getElementById('usernameSearchForm');
    const usernameSearchInput = document.getElementById('usernameSearchInput');
    const usernameSearchResults = document.getElementById('usernameSearchResults');
    const closeSearchModalBtn = document.getElementById('closeSearchModalBtn');

    const searchMessagesBtn = document.getElementById('searchMessagesBtn');
    const messageSearchModal = document.getElementById('messageSearchModal');
    const closeMessageSearchBtn = document.getElementById('closeMessageSearchBtn');
    const messageSearchForm = document.getElementById('messageSearchForm');
    const messageSearchInput = document.getElementById('messageSearchInput');
    const messageSearchResults = document.getElementById('messageSearchResults');

    const messageSearchTabs = document.querySelectorAll('#messageSearchModal .tab-btn');
    const messageSearchTabContents = document.querySelectorAll('#messageSearchModal .search-tab-content');

    const attachmentSearchForm = document.getElementById('attachmentSearchForm');
    const attachmentSearchInput = document.getElementById('attachmentSearchInput');
    const attachmentSearchResults = document.getElementById('attachmentSearchResults');


    let currentUserData = null; // Базовые данные (из /users/me)
    let currentUserProfileData = null; // Полные данные профиля (из /profiles/{id})

    const currentTheme = localStorage.getItem('theme');


    let pendingAttachments = [];

    const contextMenu = document.createElement('div');
    contextMenu.id = 'messageContextMenu';
    contextMenu.className = 'context-menu hidden';
    document.body.appendChild(contextMenu);


    let contextMenuTarget = null;
    const body = document.body;


    // --- Состояние приложения ---
    let activeChatId = null;
    let activeChatRecipientId = null; // ИЗМЕНЕНИЕ: ID собеседника для загрузки профиля
    let chatListPage = 0;
    let messagePage = 0;
    const pageSize = 50;
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
                    } else {
                        if (newMsg.senderId !== currentUserId) {
                            incrementUnreadBadge(newMsg.chatId);
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

    // Убедитесь, что ваша функция renderUsers выглядит так:
    function renderUsers(users, containerEl) {
        containerEl.innerHTML = ''; // Очищаем переданный контейнер

        if (!users || users.length === 0) {
            containerEl.innerHTML = '<p class="placeholder">Пользователи не найдены.</p>';
            return;
        }

        const authToken = localStorage.getItem('accessToken');

        users.forEach(user => {
            if (user.id === currentUserId) return;

            const userDiv = document.createElement('div');
            userDiv.className = 'user-item';

            userDiv.innerHTML = `
                <img class="user-item-avatar" src="/images/profile-default.png" alt="Аватар">
                <div class="user-item-info">
                    <div class="user-item-name">${user.name} ${user.surname || ''}</div>
                    <div class="user-item-username">@${user.username}</div>
                </div>
            `;

            userDiv.addEventListener('click', () => {
                startChatWithUser(user);
                userSearchModal.classList.add('hidden');
            });

            containerEl.appendChild(userDiv);
            const avatarImg = userDiv.querySelector('.user-item-avatar');
            try {
                apiFetch(`${API_BASE_URL}/api/profiles/images/user-avatar/${user.id}`)
                    .then(avatarId => {
                        if (avatarId && typeof avatarId === 'number') {
                            imageLoader.getImageSrc(avatarId, API_BASE_URL, authToken)
                                .then(src => {
                                    avatarImg.src = src;
                                });
                        }
                    })
                    .catch(avatarError => {
                        if (avatarError.status === 404) {
                        } else {
                            console.warn(`Не удалось загрузить аватар для пользователя ${user.id}:`, avatarError);
                        }
                    });
            } catch (error) {
                console.error('Непредвиденная ошибка при запросе аватара:', error);
            }
        });
    }

    function closeActiveChat() {
        document.body.classList.remove('chat-active');
        chatWindowEl.classList.add('hidden');
        activeChatId = null;
        [...chatListEl.children].forEach(li => li.classList.remove('active'));
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

    // ЗАМЕНИТЕ СТАРУЮ ФУНКЦИЮ createChatItem НА ЭТУ
    async function createChatItem(chat) {
        const li = document.createElement('li');
        li.dataset.chatId = chat.chatId;

        const unreadCount = chat.numberOfUnreadMessages;
        const badgeHtml = (unreadCount && unreadCount > 0)
            ? `<div class="unread-badge">${unreadCount}</div>`
            : '';

        li.innerHTML = `
        <img class="chat-item-avatar" src="/images/profile-default.png" alt="Аватар чата">
        <div class="chat-info">
            <div class="chat-title">${chat.group ? chat.name : '...'}</div>
            <div class="last-message">${chat.lastMessage ? chat.lastMessage.content || 'Вложение' : 'Нет сообщений'}</div>
            <div class="message-time">${chat.lastMessage ? `Отправлено: ${formatDate(chat.lastMessage.createdAt)}` : ''}</div>
        </div>
        ${badgeHtml}
        `;

        if (!chat.group) {
            (async () => {
                const titleDiv = li.querySelector('.chat-title');
                const avatarImg = li.querySelector('.chat-item-avatar');

                try {
                    // 1. Загружаем информацию о собеседнике
                    const recipient = await apiFetch(`${API_BASE_URL}/api/chats/find-recipient-by-private-chat-id/${chat.chatId}`);

                    if (titleDiv) {
                        titleDiv.textContent = `${recipient.name} ${recipient.surname}`;
                    }

                    // 2. Загружаем аватар
                    try {
                        const avatarId = await apiFetch(`${API_BASE_URL}/api/profiles/images/user-avatar/${recipient.id}`);
                        if (avatarId && typeof avatarId === 'number') {
                            const authToken = localStorage.getItem('accessToken');
                            imageLoader.getImageSrc(avatarId, API_BASE_URL, authToken)
                                .then(src => {
                                    if (avatarImg) avatarImg.src = src;
                                });
                        }
                    } catch (avatarError) {
                        // Игнорируем 404 для аватара, оставляем дефолтный
                        if (avatarError.status !== 404) {
                            console.warn(`Не удалось загрузить аватар:`, avatarError);
                        }
                    }

                } catch (error) {
                    console.error(`Не удалось загрузить данные чата ${chat.chatId}:`, error);
                    if (titleDiv) titleDiv.textContent = 'Неизвестный пользователь';
                }
            })();
        }

        li.addEventListener('click', () => openChat(chat));
        li.addEventListener('contextmenu', (e) => showChatContextMenu(e, chat.chatId));

        // Возвращаем элемент МГНОВЕННО, не дожидаясь окончания запросов внутри
        return li;
    }

    function incrementUnreadBadge(chatId) {
        const chatItem = chatListEl.querySelector(`li[data-chat-id="${chatId}"]`);

        if (!chatItem) return;

        let badge = chatItem.querySelector('.unread-badge');

        if (badge) {
            let currentCount = parseInt(badge.textContent, 10);
            if (isNaN(currentCount)) currentCount = 0;
            badge.textContent = currentCount + 1;
        } else {
            // Если бейджа нет, создаем новый
            badge = document.createElement('div');
            badge.className = 'unread-badge';
            badge.textContent = '1';
            chatItem.appendChild(badge);
        }
    }

    async function markMessagesAsRead(messagesToRead) {
        if (!messagesToRead || messagesToRead.length === 0) {
            return;
        }

        const currentChatId = messagesToRead[0].chatId;
        decrementUnreadBadge(currentChatId, messagesToRead.length);
        // ---------------------------------------------------

        const payload = messagesToRead.map(msg => ({
            messageId: msg.messageId, // Обратите внимание: observer возвращает объект с полем messageId
            senderId: msg.senderId,
            chatId: msg.chatId
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

            if (chat.group) {
                profileBtn.style.display = 'none';
                groupInfoBtn.style.display = 'flex'; // Показываем кнопку группы
            } else {
                profileBtn.style.display = 'flex';
                groupInfoBtn.style.display = 'none';
            }

            // Загрузка сообщений с возможностью отмены
            const {messages, hasMore} = await loadMessages(openingChatId, 0, signal);

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
            const {firstUnreadId} = await renderMessages(messages);
            if (firstUnreadId) {
                const firstUnreadElement = messagesEl.querySelector(`[data-message-id='${firstUnreadId}']`);
                if (firstUnreadElement) {
                    firstUnreadElement.scrollIntoView({behavior: 'smooth', block: 'center'});
                }
            } else {
                messagesEl.scrollTop = messagesEl.scrollHeight;
            }


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
            const data = await apiFetch(`${API_BASE_URL}/api/messages?chatId=${chatId}&page=${page}&pageSize=${pageSize}`, {signal});

            const hasMore = Array.isArray(data) && data.length === pageSize;
            return {messages: data || [], hasMore};

        } catch (error) {
            if (error.name === 'AbortError') {
                // Это не ошибка, а ожидаемая отмена. Просто возвращаем пустой результат.
                console.log(`Запрос сообщений для чата ${chatId} был отменен.`);
                return {messages: [], hasMore: false};
            }
            console.error('Ошибка загрузки сообщений:', error);
            return {messages: [], hasMore: false};
        }
    }

    async function renderMessages(messages) {
        messagesEl.innerHTML = ''; // Очищаем контейнер
        if (!messages || messages.length === 0) {
            messagesEl.innerHTML = '<p class="placeholder">Сообщений пока нет. Напишите первым!</p>';
            return {firstUnreadId: null}; // Возвращаем, что непрочитанных нет
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

        return {firstUnreadId};
    }


    function createMessageElement(msg, isSentByMe) {
        const msgDiv = document.createElement('div');

        // --- ПРОВЕРКА НА СЕРВИСНОЕ СООБЩЕНИЕ ---
        // Jackson может сериализовать isService как "service", проверяем оба варианта
        const isService = msg.service || msg.isService;

        if (isService) {
            msgDiv.className = 'message service';
            msgDiv.dataset.messageId = msg.id;

            // Важно: сервисные сообщения тоже нужно помечать прочитанными,
            // чтобы сбросить счетчик непрочитанных
            if (!msg.read) {
                messageReadObserver.observe(msgDiv);
            }

            msgDiv.innerHTML = `
                <div class="service-content">
                    ${msg.content}
                </div>
            `;
            // Возвращаем сразу, остальная логика (аватарки, время, статус) не нужна
            return msgDiv;
        }
        // ---------------------------------------

        // Дальше идет стандартная логика для обычных сообщений
        msgDiv.className = `message ${isSentByMe ? 'sent' : 'received'}`;
        msgDiv.dataset.messageId = msg.id;
        msgDiv.dataset.senderId = msg.senderId;

        msgDiv.addEventListener('contextmenu', (event) => {
            showContextMenu(event, msgDiv);
        });

        if (!isSentByMe && !msg.read) {
            messageReadObserver.observe(msgDiv);
        }

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

                // Если картинок больше одной, оборачиваем их в сетку
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
                            <span class="file-name">${fileName}</span>
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

    // --- Observer для отслеживания прочтения сообщений ---
    const messageReadObserver = new IntersectionObserver((entries) => {
        const messagesToRead = [];

        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const msgEl = entry.target;

                // Перестаем следить за сообщением, так как оно уже "увидено"
                messageReadObserver.unobserve(msgEl);
                // Собираем данные для отправки на сервер
                messagesToRead.push({
                    messageId: parseInt(msgEl.dataset.messageId),
                    senderId: parseInt(msgEl.dataset.senderId),
                    chatId: activeChatId
                });
            }
        });

        if (messagesToRead.length > 0) {
            markMessagesAsRead(messagesToRead);
        }
    }, {
        root: messagesEl,
        threshold: 0.5
    });

    /**
     * Уменьшает счетчик непрочитанных сообщений в списке чатов.
     * @param {number} chatId - ID чата.
     * @param {number} amount - Количество прочитанных сообщений.
     */
    function decrementUnreadBadge(chatId, amount) {
        const chatItem = chatListEl.querySelector(`li[data-chat-id="${chatId}"]`);
        if (!chatItem) return;

        const badge = chatItem.querySelector('.unread-badge');
        if (!badge) return;

        let currentCount = parseInt(badge.textContent, 10);
        if (isNaN(currentCount)) return;

        currentCount -= amount;

        if (currentCount <= 0) {
            badge.remove(); // Если 0 или меньше, удаляем кружок
            // Также обновляем данные в объекте чата, если он где-то хранится (опционально)
        } else {
            badge.textContent = currentCount; // Обновляем число
        }
    }


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

        const isImage = file.type.startsWith('image/');
        const isDocument = isDocumentType(file.type);

        // ИЗМЕНЕНИЕ: Добавляем классы в зависимости от типа файла
        previewEl.className = `attachment-preview-item ${isImage ? 'is-image' : 'is-file'}`;
        previewEl.dataset.fileId = tempId;

        let previewContent = '';
        let analyseCheckbox = '';

        if (isImage) {
            previewContent = `<img src="${URL.createObjectURL(file)}" alt="${file.name}">`;
        } else {
            // Улучшаем отображение для файлов
            previewContent = `
                <div class="file-preview-info">
                    <span class="file-icon">📁</span>
                    <span>${file.name}</span>
                </div>
            `;
        }

        if (isDocument) {
            analyseCheckbox = `
                <label class="analyse-checkbox-wrapper">
                    <input type="checkbox" id="analyse-${tempId}">
                    Анализировать
                </label>
            `;
        }

        previewEl.innerHTML = `
            ${previewContent}
            ${analyseCheckbox}
            <button class="remove-attachment-btn">&times;</button>
        `;

        if (isDocument) {
            const checkbox = previewEl.querySelector(`#analyse-${tempId}`);
            checkbox.addEventListener('change', (event) => {
                const attachment = pendingAttachments.find(att => att.tempId === tempId);
                if (attachment) {
                    attachment.isAnalysed = event.target.checked;
                }
            });
        }

        previewEl.querySelector('.remove-attachment-btn').addEventListener('click', () => {
            removeAttachmentFromPreview(tempId);
        });

        attachmentPreviewContainer.appendChild(previewEl);

        pendingAttachments.push({
            file,
            mimeType: file.type,
            tempId,
            isAnalysed: false
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


    closeChatBtn.addEventListener('click', closeActiveChat);

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
            const scrollChatId = activeChatId;

            const scrollHeightBefore = messagesEl.scrollHeight;
            try {
                const messages = await loadMessages(scrollChatId, messagePage);
                if (scrollChatId !== activeChatId) {
                    return;
                }

                if (messages && messages.messages.length > 0) {
                    const fragment = document.createDocumentFragment();
                    for (const msg of messages.messages) {
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

    messageInput.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' && event.shiftKey) {
            return;
        }

        if (event.key === 'Enter') {
            event.preventDefault();

            messageForm.requestSubmit();
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

                    const uploadUrl = `${API_BASE_URL}/api/storage/upload`;

                    if (att.isAnalysed) {
                        formData.append('isAnalyse', 'true');
                        formData.append('chatId', activeChatId);
                    }
                    const response = await fetch(uploadUrl, {
                        method: 'POST',
                        headers: {
                            'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
                        },
                        body: formData
                    });

                    if (!response.ok) throw new Error(`Ошибка загрузки файла: ${att.file.name}`);

                    const result = await response.json();

                    uploadedAttachments.push({
                        fileId: result.id,
                        mimeType: att.mimeType,
                        fileName: att.file.name,
                        hasAnalysis: att.isAnalysed
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
                decrementUnreadBadge(activeChatId, Number.MAX_VALUE);
            } else {
                alert("Нет подключения для отправки сообщения.");
                const pendingEl = document.querySelector(`[data-temp-id='${tempId}']`);
                if (pendingEl) {
                    pendingEl.querySelector('.message-meta span').textContent = 'Ошибка сети';
                }
            }
        })();
    });


    function renderFoundFiles(files) {
        attachmentSearchResults.innerHTML = '';
        if (!files || files.length === 0) {
            attachmentSearchResults.innerHTML = '<p class="placeholder">Ничего не найдено.</p>';
            return;
        }

        files.forEach(fileMeta => {
            // Возвращаемся к использованию <div> как корневого элемента
            const itemDiv = document.createElement('div');
            itemDiv.className = 'found-file-item';

            // Формируем URL и имя файла для скачивания
            const proxyUrl = `${API_BASE_URL}/api/storage/proxy/download/by-id?id=${fileMeta.fileId}`;
            const fileName = fileMeta.title || `file-${fileMeta.fileId}`;

            // HTML-код для SVG-иконки
            const fileIconSvg = `
                <svg class="file-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                    <polyline points="14 2 14 8 20 8"></polyline>
                    <line x1="16" y1="13" x2="8" y2="13"></line>
                    <line x1="16" y1="17" x2="8" y2="17"></line>
                    <polyline points="10 9 9 9 8 9"></polyline>
                </svg>
            `;

            // ИЗМЕНЕНИЕ: Добавляем кнопку <a> с классом .found-file-download-btn
            itemDiv.innerHTML = `
                <div class="found-file-preview">
                    ${fileIconSvg}
                </div>
                <div class="found-file-info">
                    <div class="found-file-title">${fileMeta.title || 'Без названия'}</div>
                    <div class="found-file-summary">${fileMeta.summary || 'Нет описания.'}</div>
                    <a href="${proxyUrl}" download="${fileName}" class="found-file-download-btn">
                        Скачать
                    </a>
                </div>
            `;

            attachmentSearchResults.appendChild(itemDiv);
        });
    }

    function resetSearchModals() {
        // Очистка окна поиска пользователей
        if (usernameSearchInput) {
            usernameSearchInput.value = '';
        }
        if (usernameSearchResults) {
            usernameSearchResults.innerHTML = '<p class="placeholder">Начните поиск, чтобы увидеть результаты.</p>';
        }

        // Очистка окна поиска сообщений и вложений
        if (messageSearchInput) {
            messageSearchInput.value = '';
        }
        if (attachmentSearchInput) {
            attachmentSearchInput.value = '';
        }
        if (messageSearchResults) {
            messageSearchResults.innerHTML = '<p class="placeholder">Начните поиск, чтобы увидеть результаты.</p>';
        }
        if (attachmentSearchResults) {
            attachmentSearchResults.innerHTML = '<p class="placeholder">Начните поиск, чтобы увидеть результаты.</p>';
        }
    }

    function isDocumentType(mimeType) {
        const documentMimeTypes = [
            'application/pdf',
            'application/msword', // .doc
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document', // .docx
            'application/vnd.ms-excel', // .xls
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', // .xlsx
            'application/vnd.ms-powerpoint', // .ppt
            'application/vnd.openxmlformats-officedocument.presentationml.presentation', // .pptx
            'text/plain', // .txt
            'text/csv', // .csv
            'application/rtf' // .rtf
        ];
        return documentMimeTypes.includes(mimeType);
    }

    attachFileBtn.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', (e) => {
        const files = e.target.files;
        if (files && files.length > 0) {
            [...files].forEach(file => addAttachmentToPreview(file));
            fileInput.value = '';
        }
    });

    document.body.addEventListener('click', (event) => {
        const viewerTarget = event.target.closest('.viewer-enabled');

        if (viewerTarget) {
            event.preventDefault(); // Отменяем стандартное действие (переход по ссылке или скачивание)
            const fileId = parseInt(viewerTarget.dataset.fileId, 10);
            if (fileId) {
                photoViewer.open(fileId);
            }
        }
    });

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

    groupInfoBtn.addEventListener('click', () => {
        if (activeChatId) {
            // Берем название чата из заголовка
            const chatName = chatTitleEl.textContent;
            groupProfile.open(activeChatId, chatName);
        }
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
            const {messageId} = contextMenuTarget.data;
            if (action === 'delete-for-me') {
                deleteMessages([messageId], false);
            } else if (action === 'delete-for-all') {
                deleteMessages([messageId], true);
            }
        } else if (contextMenuTarget.type === 'chat') {
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

    mobileMenuBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        mobileDropdownMenu.classList.toggle('hidden');
    });

    // Дублируем действия десктопных кнопок
    mobileFindUser.addEventListener('click', () => {
        loadAndShowUsers();
        mobileDropdownMenu.classList.add('hidden');
    });

    mobileLogout.addEventListener('click', () => {
        window.location.href = '/logout';
    });

    mobileThemeToggle.addEventListener('click', () => {
        let newTheme;
        if (body.getAttribute('data-theme') === 'dark') {
            newTheme = 'light';
            themeToggleIcon.textContent = '🌙';
            mobileThemeIcon.textContent = '🌙';
        } else {
            newTheme = 'dark';
            themeToggleIcon.textContent = '☀️';
            mobileThemeIcon.textContent = '☀️';
        }
        body.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
    });

    // Дублируем открытие профиля
    myProfileBtnMobile.addEventListener('click', () => {
        myProfileBtn.click();
    });

    // Закрываем мобильное меню при клике в любом другом месте
    window.addEventListener('click', (e) => {
        if (!mobileDropdownMenu.classList.contains('hidden') && !e.target.closest('.mobile-header')) {
            mobileDropdownMenu.classList.add('hidden');
        }
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

        const fullName = `${name} ${surname}`.trim();

        usernameContent.textContent = fullName;
        usernameMobile.textContent = fullName; // Добавлена строка для мобильной шапки

        participantCache[userId] = fullName;
    };

    const refreshUserData = async () => {
        try {
            const headerAvatarImg = document.getElementById('headerAvatarImg');
            const me = await apiFetch(`${API_BASE_URL}/api/users/me`);
            updateHeaderUI(me);
            currentUserProfileData = await apiFetch(`${API_BASE_URL}/api/profiles/${me.id}`);

            if (currentUserProfileData && currentUserProfileData.avatarId) {
                const authToken = localStorage.getItem('accessToken');
                imageLoader.getImageSrc(currentUserProfileData.avatarId, API_BASE_URL, authToken)
                    .then(src => {
                        headerAvatarImg.src = src;
                        headerAvatarImgMobile.src = src;
                    });
            } else {
                headerAvatarImg.src = '/images/profile-default.png';
                headerAvatarImgMobile.src = '/images/profile-default.png';
            }
        } catch (error) {
            console.error("Не удалось перезагрузить данные пользователя:", error);
        }
    };

    findUserBtn.addEventListener('click', () => {
        userSearchModal.classList.remove('hidden');
        usernameSearchInput.value = ''; // Очищаем поле ввода
        usernameSearchResults.innerHTML = '<p class="placeholder">Начните поиск, чтобы увидеть результаты.</p>'; // Сбрасываем результаты
        usernameSearchInput.focus(); // Ставим фокус на поле ввода
    });


    usernameSearchForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = usernameSearchInput.value.trim();
        if (!username) return;

        usernameSearchResults.innerHTML = '<p class="placeholder">Поиск...</p>';
        try {
            const users = await apiFetch(`${API_BASE_URL}/api/search/users/by-username/${username}`);
            renderUsers(users, usernameSearchResults);
        } catch (error) {
            usernameSearchResults.innerHTML = `<p class="placeholder">Ошибка поиска: ${error.message}</p>`;
        }
    });


    function closeMessageSearchModal() {
        messageSearchModal.classList.add('hidden');
        resetSearchModals();
    }


    // Обработчик закрытия модального окна
    closeSearchModalBtn.addEventListener('click', closeUserSearchModal);
    userSearchModal.addEventListener('click', (e) => {
        if (e.target === userSearchModal) {
            closeUserSearchModal();
        }
    });

    searchMessagesBtn.addEventListener('click', () => {
        if (!activeChatId) return;
        messageSearchModal.classList.remove('hidden');
        // По умолчанию активируем первую вкладку
        messageSearchTabs[0].click();
        messageSearchInput.focus();
    });

    attachmentSearchForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const query = attachmentSearchInput.value.trim();
        if (!query) return;

        attachmentSearchResults.innerHTML = '<p class="placeholder">Поиск в файлах...</p>';

        const payload = {
            chatId: activeChatId,
            query: query
        };

        try {
            const foundFiles = await apiFetch(`${API_BASE_URL}/api/metadata`, {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            renderFoundFiles(foundFiles);
        } catch (error) {
            attachmentSearchResults.innerHTML = `<p class="placeholder">Ошибка поиска: ${error.message}</p>`;
        }
    });

    // Переключение вкладок
    messageSearchTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            messageSearchTabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            const tabId = tab.dataset.tab;
            messageSearchTabContents.forEach(content => {
                content.classList.toggle('active', content.id === `${tabId}SearchTab`);
            });
        });
    });

    searchMessagesBtn.addEventListener('click', () => {
        if (!activeChatId) return; // Не открываем, если чат не выбран

        // Сбрасываем состояние окна перед открытием
        messageSearchInput.value = '';
        messageSearchResults.innerHTML = '<p class="placeholder">Начните поиск, чтобы увидеть результаты.</p>';

        messageSearchModal.classList.remove('hidden');
        messageSearchInput.focus();
    });

    closeMessageSearchBtn.addEventListener('click', closeMessageSearchModal);
    messageSearchModal.addEventListener('click', (e) => {
        if (e.target === messageSearchModal) {
            closeMessageSearchModal();
        }
    });

    messageSearchModal.addEventListener('click', (e) => {
        if (e.target === messageSearchModal) {
            messageSearchModal.classList.add('hidden');
        }
    });

    function closeUserSearchModal() {
        userSearchModal.classList.add('hidden');
        resetSearchModals(); // ВЫЗЫВАЕМ ОЧИСТКУ
    }

    messageSearchForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const content = messageSearchInput.value.trim();
        if (!content) return;

        messageSearchResults.innerHTML = '<p class="placeholder">Поиск...</p>';

        const payload = {
            chatId: activeChatId,
            content: content
        };

        try {
            const foundMessages = await apiFetch(`${API_BASE_URL}/api/search/messages/find-by-content-in-chat`, {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            renderFoundMessages(foundMessages);
        } catch (error) {
            messageSearchResults.innerHTML = `<p class="placeholder">Ошибка поиска: ${error.message}</p>`;
        }
    });

    function renderFoundMessages(messages) {
        messageSearchResults.innerHTML = '';
        if (!messages || messages.length === 0) {
            messageSearchResults.innerHTML = '<p class="placeholder">Ничего не найдено.</p>';
            return;
        }

        const authToken = localStorage.getItem('accessToken');

        messages.forEach(msg => {
            const itemDiv = document.createElement('div');
            itemDiv.className = 'user-item';

            const senderName = participantCache[msg.senderId] || `Пользователь #${msg.senderId}`;

            itemDiv.innerHTML = `
                <img class="user-item-avatar" src="/images/profile-default.png" alt="Аватар">
                <div class="user-item-info">
                    <div class="user-item-name">${senderName}</div>
                    <div class="user-item-username">${msg.content || '<em>Вложение</em>'}</div>
                </div>
            `;


            messageSearchResults.appendChild(itemDiv);

            const avatarImg = itemDiv.querySelector('.user-item-avatar');
            apiFetch(`${API_BASE_URL}/api/profiles/images/user-avatar/${msg.senderId}`)
                .then(avatarId => {
                    if (avatarId && typeof avatarId === 'number') {
                        imageLoader.getImageSrc(avatarId, API_BASE_URL, authToken)
                            .then(src => avatarImg.src = src);
                    }
                })
                .catch(() => { /* Игнорируем ошибки загрузки аватара */
                });
        });
    }

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



            const handleNewGroupCreated = async (newGroupChat) => {
                const newChatItemEl = await createChatItem(newGroupChat);
                chatListEl.prepend(newChatItemEl);
                await openChat(newGroupChat);
            };

            createGroupManager.init(API_BASE_URL, handleNewGroupCreated);

            groupProfile.init({
                apiBaseUrl: API_BASE_URL,
                observer: attachmentObserver
            }, me.id);

            updateHeaderUI(me);

            const headerAvatarImg = document.getElementById('headerAvatarImg');
            const headerAvatarImgMobile = document.getElementById('headerAvatarImgMobile'); // ДОБАВЛЕНО
            const authToken = localStorage.getItem('accessToken');

            try {
                currentUserProfileData = await apiFetch(`${API_BASE_URL}/api/profiles/${currentUserId}`);
                if (currentUserProfileData && currentUserProfileData.avatarId) {
                    imageLoader.getImageSrc(currentUserProfileData.avatarId, API_BASE_URL, authToken)
                        .then(src => {
                            headerAvatarImg.src = src;
                            headerAvatarImgMobile.src = src;
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

    console.log("HELLO!")
    initializeApp();
});