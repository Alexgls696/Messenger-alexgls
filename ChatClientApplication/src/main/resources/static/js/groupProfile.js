const groupProfile = (() => {
    let modal = null, modalContent = null, closeBtn = null, modalTitle = null;
    let localApiBaseUrl = null, activeChatId = null, localAttachmentObserver = null;

    const tooltip = document.getElementById('attachment-tooltip');
    const metadataCache = new Map();
    let CURRENT_USER_ID = null;
    let tooltipTimeout = null;

    const close = () => {
        if (modal) modal.classList.add('hidden');
    };

    // --- Логика вложений (аналогична userProfile) ---
    const loadAttachments = async (type, container) => {
        const skeletonHtml = (type === 'IMAGE' || type === 'VIDEO')
            ? `<div class="attachments-grid">${Array(8).fill('<div class="skeleton skeleton-tile"></div>').join("")}</div>`
            : `<div class="skeleton-list">${Array(4).fill('<div class="skeleton skeleton-row"></div>').join("")}</div>`;
        container.innerHTML = skeletonHtml;

        try {
            const url = `${localApiBaseUrl}/api/attachments/find-by-type-and-chat-id?mediaType=${type}&chatId=${activeChatId}`;
            const attachments = await apiFetch(url);

            if (!attachments || attachments.length === 0) {
                container.innerHTML = "<p class='placeholder'>Нет вложений в этой категории.</p>";
                return;
            }

            const itemsHtml = attachments.map(att => {
                const proxyUrl = `${localApiBaseUrl}/api/storage/proxy/download/by-id?id=${att.fileId}`;
                const fileName = att.fileName || 'file';
                const hasAnalysis = att.hasAnalysis;
                const analysisClass = hasAnalysis ? 'has-analysis' : '';
                const fileIdAttr = `data-file-id="${att.fileId}"`; // ID всегда
                const aiIcon = hasAnalysis ? '<div class="ai-icon">AI</div>' : '';

                switch (type) {
                    case "IMAGE":
                        return `<div class="attachment-item viewer-enabled ${analysisClass}" ${fileIdAttr}>
                                    <div class="skeleton skeleton-tile"></div>
                                    <img class="lazy-load-attachment" data-src="${proxyUrl}" style="opacity:0;">
                                    ${aiIcon}
                                </div>`;
                    case "VIDEO":
                        return `<div class="attachment-item ${analysisClass}" ${fileIdAttr}>
                                    <div class="skeleton skeleton-tile"></div>
                                    <video class="lazy-load-attachment" data-src="${proxyUrl}" controls style="opacity:0;"></video>
                                    ${aiIcon}
                                </div>`;
                    default:
                        return `<div class="attachment-list-item ${analysisClass}" ${fileIdAttr}>
                                    <span>${fileName}</span>
                                    <div class="file-actions">
                                        ${aiIcon}
                                        <a href="${proxyUrl}" download="${fileName}">Скачать</a>
                                    </div>
                                </div>`;
                }
            }).join('');

            const containerClass = (type === 'IMAGE' || type === 'VIDEO') ? 'attachments-grid' : 'attachments-list';
            container.innerHTML = `<div class="${containerClass}">${itemsHtml}</div>`;

            const mediaToLazyLoad = container.querySelectorAll('.lazy-load-attachment');
            if (localAttachmentObserver) mediaToLazyLoad.forEach(media => localAttachmentObserver.observe(media));

        } catch (e) {
            console.error("Ошибка загрузки вложений:", e);
            container.innerHTML = "<p class='placeholder'>Не удалось загрузить вложения.</p>";
        }
    };

    const renderParticipants = async (participants, container, canRemove) => {
        if (!participants || participants.length === 0) {
            container.innerHTML = '<p class="placeholder">Нет участников.</p>';
            return;
        }

        const authToken = localStorage.getItem('accessToken');
        container.innerHTML = '';

        const roleMap = { 'OWNER': 'Владелец', 'ADMIN': 'Админ', 'MEMBER': 'Участник', 'Создатель': 'Владелец' };

        participants.forEach(user => {
            const div = document.createElement('div');
            div.className = 'participant-item';

            const userRole = user.role || 'MEMBER';
            const displayRole = roleMap[userRole] || userRole;
            const roleClass = String(userRole).toLowerCase();

            // Логика отображения кнопки удаления:
            // 1. У меня есть права (canRemove = true)
            // 2. Это не я сам (user.id !== CURRENT_USER_ID)
            // 3. (Опционально) Админ не может удалить Владельца (можно добавить проверку ролей здесь)
            let deleteBtnHtml = '';
            if (canRemove && user.id !== CURRENT_USER_ID) {
                deleteBtnHtml = `<button class="remove-participant-btn" title="Удалить из группы">&times;</button>`;
            }

            div.innerHTML = `
                <img class="participant-avatar" src="/images/profile-default.png">
                <div class="participant-info">
                    <div class="participant-header">
                        <span class="participant-name">${user.name} ${user.surname || ''}</span>
                        <span class="participant-role role-${roleClass}">${displayRole}</span>
                    </div>
                    <span class="participant-username">@${user.username}</span>
                </div>
                ${deleteBtnHtml}
            `;

            // Обработчик удаления
            const deleteBtn = div.querySelector('.remove-participant-btn');
            if (deleteBtn) {
                deleteBtn.addEventListener('click', (e) => {
                    e.stopPropagation(); // Чтобы не открылся профиль пользователя
                    removeParticipant(user.id, `${user.name} ${user.surname}`, div);
                });
            }

            // Обработчик открытия профиля
            div.addEventListener('click', async () => {
                if (user.id !== CURRENT_USER_ID) {
                    // Предполагаем, что userProfile доступен
                    const data = await apiFetch(`${localApiBaseUrl}/api/chats/find-chat-id-by-recipient-id/${user.id}`);
                    await userProfile.open(user.id, data?.chatId, `${user.name} ${user.surname}`);
                }
            });

            container.appendChild(div);

            // Подгрузка аватара
            const avatarImg = div.querySelector('.participant-avatar');
            apiFetch(`${localApiBaseUrl}/api/profiles/images/user-avatar/${user.id}`)
                .then(avatarId => {
                    if (avatarId) {
                        imageLoader.getImageSrc(avatarId, localApiBaseUrl, authToken)
                            .then(src => avatarImg.src = src);
                    }
                }).catch(() => {});
        });
    };

    // --- ОБНОВЛЕННАЯ ФУНКЦИЯ УДАЛЕНИЯ С КРАСИВЫМ ОКНОМ ---
    const removeParticipant = (userId, userName, divElement) => {
        // Вызываем наше кастомное окно
        confirmModal.open(
            `Вы действительно хотите удалить пользователя ${userName} из группы?`,
            async () => {
                // ВЕСЬ КОД УДАЛЕНИЯ ТЕПЕРЬ ЗДЕСЬ (ВНУТРИ КОЛЛБЭКА)
                try {
                    const authToken = localStorage.getItem('accessToken');
                    const response = await fetch(`${localApiBaseUrl}/api/chats/${activeChatId}/participants/${userId}`, {
                        method: 'DELETE',
                        headers: { 'Authorization': `Bearer ${authToken}` }
                    });

                    if (!response.ok) throw new Error('Ошибка при удалении');

                    // Анимация удаления
                    divElement.style.transition = 'all 0.3s ease';
                    divElement.style.opacity = '0';
                    divElement.style.transform = 'translateX(20px)';

                    setTimeout(() => {
                        divElement.remove();
                        // Здесь можно было бы обновить счетчик участников
                    }, 300);

                } catch (error) {
                    console.error("Не удалось удалить участника:", error);
                    alert("Ошибка при удалении пользователя.");
                }
            }
        );
    };

    const open = async (chatId, chatName) => {
        activeChatId = chatId;
        modal.classList.remove('hidden');
        modalTitle.textContent = chatName;

        modalContent.innerHTML = `<div class="skeleton-list">${Array(5).fill('<div class="skeleton skeleton-row"></div>').join("")}</div>`;

        try {
            const [participants, chatDetails, accessData] = await Promise.all([
                apiFetch(`${localApiBaseUrl}/api/chats/${chatId}/participants`),
                apiFetch(`${localApiBaseUrl}/api/chats/${chatId}`),
                // Запрос прав доступа. Если сервер возвращает 403 при отсутствии прав, оберните в try/catch
                apiFetch(`${localApiBaseUrl}/api/chats/groups/${chatId}/access`).catch(() => ({ canRemoveMembers: false }))
            ]);

            const canRemoveMembers = accessData === true || accessData?.canRemoveMembers === true || accessData?.role === 'ADMIN' || accessData?.role === 'OWNER';

            const descriptionHtml = chatDetails.description
                ? `<div class="profile-description">${chatDetails.description}</div>`
                : '';

            modalContent.innerHTML = `
                <div class="profile-header">
                    <img id="groupAvatarImg" src="/images/group-default.png" class="profile-avatar">
                    <div class="profile-details">
                        <div class="profile-section-title" style="margin:0; border:none;">${chatDetails.name || chatName}</div>
                        <div class="profile-info-item">${participants.length} участников</div>
                        ${descriptionHtml}
                    </div>
                </div>

                <div class="participants-section">
                    <h3 class="profile-section-title">Участники</h3>
                    <div id="groupParticipantsList" class="participants-list"></div>
                </div>

                <div class="attachments-section">
                     <div class="attachments-tabs">
                        <button class="tab-btn active" data-type="IMAGE">Изображения</button>
                        <button class="tab-btn" data-type="VIDEO">Видео</button>
                        <button class="tab-btn" data-type="AUDIO">Аудио</button>
                        <button class="tab-btn" data-type="DOCUMENT">Файлы</button>
                    </div>
                    <div id="groupAttachmentsContent"></div>
                </div>
            `;

            // Передаем флаг прав доступа в рендер
            const participantsContainer = document.getElementById('groupParticipantsList');
            renderParticipants(participants, participantsContainer, canRemoveMembers);

            const tabs = modalContent.querySelectorAll(".attachments-tabs .tab-btn");
            const attachmentsContainer = document.getElementById('groupAttachmentsContent');

            tabs.forEach(tab => {
                tab.addEventListener('click', () => {
                    tabs.forEach(t => t.classList.remove('active'));
                    tab.classList.add('active');
                    loadAttachments(tab.dataset.type, attachmentsContainer);
                });
            });

            loadAttachments('IMAGE', attachmentsContainer);

        } catch (error) {
            console.error("Ошибка загрузки группы:", error);
            modalContent.innerHTML = "<p class='placeholder'>Не удалось загрузить информацию о группе.</p>";
        }
    };

    const showTooltip = async (event) => {
        const targetIcon = event.target.closest('.ai-icon');
        if (!targetIcon) return;

        const parentItem = targetIcon.closest('.has-analysis');
        if (!parentItem) return;

        const fileId = parentItem.dataset.fileId;
        if (!fileId) return;

        clearTimeout(tooltipTimeout);

        tooltip.classList.add('visible');
        tooltip.style.opacity = '0';

        if (metadataCache.has(fileId)) {
            tooltip.innerHTML = metadataCache.get(fileId).summary || 'Нет описания.';
        } else {
            tooltip.innerHTML = 'Загрузка анализа...';
            try {
                const metadata = await apiFetch(`${localApiBaseUrl}/api/metadata/by-file-id/${fileId}`);
                if (metadata) {
                    metadataCache.set(fileId, metadata);
                    tooltip.innerHTML = metadata.summary || 'Нет описания.';
                } else {
                    tooltip.innerHTML = 'Не удалось загрузить анализ.';
                }
            } catch (error) {
                console.error(`Ошибка загрузки метаданных для файла ${fileId}:`, error);
                tooltip.innerHTML = 'Ошибка загрузки.';
            }
        }

        const tooltipRect = tooltip.getBoundingClientRect();
        // Позиционируем относительно иконки, а не курсора
        const iconRect = targetIcon.getBoundingClientRect();

        const topPosition = window.scrollY + iconRect.top - tooltipRect.height - 10; // 10px над иконкой
        const leftPosition = window.scrollX + iconRect.left + (iconRect.width / 2) - (tooltipRect.width / 2); // Центрируем над иконкой

        tooltip.style.top = `${topPosition}px`;
        tooltip.style.left = `${leftPosition}px`;
        tooltip.style.opacity = '1';
    };

    const hideTooltip = (event) => {
        const targetIcon = event.target.closest('.ai-icon');
        if (targetIcon) {
            tooltipTimeout = setTimeout(() => {
                tooltip.classList.remove('visible');
            }, 300);
        }
    };

    const init = (config, currentUserId) => {
        modal = document.getElementById('groupProfileModal');
        modalContent = document.getElementById('groupProfileContent');
        closeBtn = document.getElementById('closeGroupProfileBtn');
        modalTitle = document.getElementById('groupProfileModalTitle');

        if (!config || !config.apiBaseUrl) {
            console.error("groupProfile.init: Необходим конфиг");
            return;
        }
        localApiBaseUrl = config.apiBaseUrl;
        localAttachmentObserver = config.observer;
        CURRENT_USER_ID = currentUserId

        closeBtn.addEventListener('click', close);
        modal.addEventListener('click', (e) => {
            if (e.target === modal) close();
        });

        // Делегирование для фото и тултипов (важно!)
        if (modalContent) {
            // Тултипы
            modalContent.addEventListener('mouseover', showTooltip);
            modalContent.addEventListener('mouseout', hideTooltip);

            // Клик по фото (photoViewer)
            modalContent.addEventListener('click', (event) => {
                // Ищем элемент с классом viewer-enabled (это наши картинки)
                const viewerTarget = event.target.closest('.viewer-enabled');

                if (viewerTarget) {
                    event.preventDefault();
                    const fileId = parseInt(viewerTarget.dataset.fileId, 10);

                    // Теперь fileId будет существовать всегда
                    if (fileId) {
                        photoViewer.open(fileId);
                    } else {
                        console.warn("Не найден ID файла для открытия");
                    }
                }
            });
        }

        if (tooltip) {
            tooltip.addEventListener('mouseover', () => clearTimeout(tooltipTimeout));
            tooltip.addEventListener('mouseout', hideTooltip);
        }
    };

    return {init, open};
})();