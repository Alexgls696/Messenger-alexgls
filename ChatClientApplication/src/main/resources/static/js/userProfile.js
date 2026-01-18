const userProfile = (() => {
    // --- DOM Элементы и Состояние (без изменений) ---
    let modal = null, modalContent = null, closeBtn = null, modalTitle = null;
    let localApiBaseUrl = null, activeChatId = null, localAttachmentObserver = null;

    const tooltip = document.getElementById('attachment-tooltip');
    const metadataCache = new Map();
    let tooltipTimeout = null;

    // --- Функции ---
    const close = () => {
        if (modal) modal.classList.add('hidden');
    };

    const renderPhotos = (userImages, container, tileClass = 'photo-tile') => {
        if (!userImages || userImages.length === 0) {
            container.innerHTML = '<p class="placeholder">У пользователя нет фотографий.</p>';
            return;
        }

        const authToken = localStorage.getItem('accessToken');
        container.innerHTML = '';

        userImages.forEach(image => {
            const tile = document.createElement('div');
            tile.className = tileClass; // Используем переданный класс
            const img = new Image();

            imageLoader.getImageSrc(image.imageId, localApiBaseUrl, authToken)
                .then(src => {
                    img.src = src;
                    if (tileClass === 'profile-image-item') {
                        tile.appendChild(img);
                    } else {
                        tile.innerHTML = '';
                        tile.appendChild(img);
                    }
                });

            tile.addEventListener('click', () => photoViewer.open(image.imageId));
            container.appendChild(tile);
        });
    };

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

                // ИСПРАВЛЕНИЕ: ID файла добавляется ВСЕГДА, а не только при наличии анализа
                const fileIdAttr = `data-file-id="${att.fileId}"`;

                const aiIcon = hasAnalysis ? '<div class="ai-icon">AI</div>' : '';

                switch (type) {
                    case "IMAGE":
                        return `<div class="attachment-item viewer-enabled ${analysisClass}" ${fileIdAttr}>
                                <div class="skeleton skeleton-tile"></div>
                                <img class="lazy-load-attachment" data-src="${proxyUrl}" alt="Изображение" style="opacity:0;">
                                ${aiIcon}
                            </div>`;
                    case "VIDEO":
                        return `<div class="attachment-item ${analysisClass}" ${fileIdAttr}>
                                <div class="skeleton skeleton-tile"></div>
                                <video class="lazy-load-attachment" data-src="${proxyUrl}" controls style="opacity:0;"></video>
                                ${aiIcon}
                            </div>`;
                    default: // Для AUDIO и DOCUMENT
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
            if (localAttachmentObserver) {
                mediaToLazyLoad.forEach(media => localAttachmentObserver.observe(media));
            }

        } catch (e) {
            console.error("Ошибка загрузки вложений:", e);
            container.innerHTML = "<p class='placeholder'>Не удалось загрузить вложения.</p>";
        }
    };

    const open = async (userId, chatId, userName) => {
        activeChatId = chatId;
        modal.classList.remove('hidden');
        modalTitle.textContent = `Профиль: ${userName}`;

        modalContent.innerHTML = `<div class="skeleton-list">${Array(5).fill('<div class="skeleton skeleton-row"></div>').join("")}</div>`;

        try {
            const profileData = await apiFetch(`${localApiBaseUrl}/api/profiles/${userId}`);

            // ИЗМЕНЕНИЕ: Убираем лишнюю обертку <div class="profile-content-wrapper">
            modalContent.innerHTML = `
            <!-- Блок с аватаром и информацией -->
            <div class="profile-header">
                <img id="userProfileAvatarImg" src="/images/profile-default.png" class="profile-avatar">
                <div class="profile-details">
                    <div class="profile-info-item"><strong>Статус:</strong> ${profileData.status || 'Не указан'}</div>
                    <div class="profile-info-item"><strong>День рождения:</strong> ${profileData.birthday || 'Не указан'}</div>
                </div>
            </div>

            <!-- Блок с фотографиями пользователя -->
            <div class="photos-section">
                <h3 class="profile-section-title">Фотографии</h3>
                <div id="otherUserPhotosGrid" class="photos-grid"> <!-- Используем общий класс -->
                    <!-- Скелетоны -->
                </div>
            </div>

            <!-- Блок с вложениями из чата -->
            <div class="attachments-section">
                 <div class="attachments-tabs">
                    <button class="tab-btn active" data-type="IMAGE">Изображения</button>
                    <button class="tab-btn" data-type="VIDEO">Видео</button>
                    <button class="tab-btn" data-type="AUDIO">Аудио</button>
                    <button class="tab-btn" data-type="DOCUMENT">Файлы</button>
                </div>
                <div id="attachmentsContent"></div>
            </div>
        `;

            const avatarImg = document.getElementById('userProfileAvatarImg');
            if (profileData.avatarId) {
                const authToken = localStorage.getItem('accessToken');
                imageLoader.getImageSrc(profileData.avatarId, localApiBaseUrl, authToken)
                    .then(src => { avatarImg.src = src; });
            }

            const photosContainer = document.getElementById('otherUserPhotosGrid');
            renderPhotos(profileData.userImages, photosContainer, 'profile-image-item');

            const tabs = modalContent.querySelectorAll(".attachments-tabs .tab-btn");
            const attachmentsContainer = document.getElementById('attachmentsContent');

            tabs.forEach(tab => {
                tab.addEventListener('click', () => {
                    tabs.forEach(t => t.classList.remove('active'));
                    tab.classList.add('active');
                    loadAttachments(tab.dataset.type, attachmentsContainer);
                });
            });

            loadAttachments('IMAGE', attachmentsContainer);

        } catch (error) {
            console.error("Ошибка загрузки профиля:", error);
            modalContent.innerHTML = "<p class='placeholder'>Не удалось загрузить профиль пользователя.</p>";
        }
    };

    const showTooltip = async (event) => {
        // ИЗМЕНЕНИЕ: Теперь мы ищем наведение ТОЛЬКО на иконку .ai-icon
        const targetIcon = event.target.closest('.ai-icon');
        if (!targetIcon) return;

        // Находим родительский элемент, чтобы получить fileId
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

    const init = (config) => {
        modal = document.getElementById('userProfileModal');
        modalContent = document.getElementById('userProfileContent');
        closeBtn = document.getElementById('closeUserProfileBtn');
        modalTitle = document.getElementById('userProfileModalTitle');

        if (!config || !config.apiBaseUrl) {
            console.error("userProfile.init: Необходим конфиг с apiBaseUrl");
            return;
        }
        localApiBaseUrl = config.apiBaseUrl;
        localAttachmentObserver = config.observer;

        closeBtn.addEventListener('click', close);
        modal.addEventListener('click', (e) => {
            if (e.target === modal) close();
        });

        // Обработчики для модального окна
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

    return { init, open };
})();

