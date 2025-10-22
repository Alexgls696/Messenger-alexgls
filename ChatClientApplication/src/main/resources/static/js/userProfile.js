const userProfile = (() => {
    // --- DOM Элементы и Состояние (без изменений) ---
    let modal = null, modalContent = null, closeBtn = null, modalTitle = null;
    let localApiBaseUrl = null, activeChatId = null, localAttachmentObserver = null;

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

// ИЗМЕНЕНО: Немного улучшена структура и логика
    const loadAttachments = async (type, container) => {
        // Показываем скелетон в зависимости от типа контента
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

                switch (type) {
                    case "IMAGE":
                        return `<div class="attachment-item viewer-enabled" data-file-id="${att.fileId}">
                                <div class="skeleton skeleton-tile"></div>
                                <img class="lazy-load-attachment" data-src="${proxyUrl}" alt="Изображение" style="opacity:0;">
                            </div>`;
                    case "VIDEO":
                        return `<div class="attachment-item">
                                <div class="skeleton skeleton-tile"></div>
                                <video class="lazy-load-attachment" data-src="${proxyUrl}" controls style="opacity:0;"></video>
                            </div>`;
                    default: // Для AUDIO и DOCUMENT
                        return `<div class="attachment-list-item">
                                <span>${fileName}</span>
                                <a href="${proxyUrl}" download="${fileName}">Скачать</a>
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
    };

    return { init, open };
})();

