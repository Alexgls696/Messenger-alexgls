const myProfileManager = (() => {
    // --- Приватные переменные ---
    let currentUserId = null;
    let apiBaseUrl = null;
    let onUserDataUpdate = null;

    // --- DOM Элементы ---
    const profileModal = document.getElementById('myProfileModal');
    const closeProfileBtn = document.getElementById('closeMyProfileBtn');
    const cancelProfileBtn = document.getElementById('cancelMyProfileBtn');
    const saveProfileBtn = document.getElementById('saveMyProfileBtn');
    const profileContent = document.getElementById('myProfileContent');

    const editUserModal = document.getElementById('editUserModal');
    const closeEditUserBtn = document.getElementById('closeEditUserBtn');
    const cancelEditUserBtn = document.getElementById('cancelEditUserBtn');
    const editUserForm = document.getElementById('editUserForm');
    const saveEditUserBtn = document.getElementById('saveEditUserBtn');
    const errorContainer = document.getElementById('editUserError');

    // --- ФУНКЦИИ ДЛЯ РАБОТЫ С ФОТО ---
    const handlePhotoUpload = async (event) => {
        const file = event.target.files[0];
        if (!file) return;

        const addPhotoTile = document.getElementById('addPhotoTile');
        addPhotoTile.classList.add('loading');
        addPhotoTile.innerHTML = '<div class="spinner"></div>';

        try {
            const authToken = localStorage.getItem('accessToken');

            // Шаг 1: Загрузка файла
            const formData = new FormData();
            formData.append('file', file);
            const uploadResponse = await fetch(`${apiBaseUrl}/api/storage/upload`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${authToken}` },
                body: formData
            });
            if (!uploadResponse.ok) throw new Error(`Ошибка загрузки файла: ${uploadResponse.status}`);
            const newFile = await uploadResponse.json();

            // Шаг 2: Привязка изображения к профилю
            const linkResponse = await fetch(`${apiBaseUrl}/api/profiles/images`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${authToken}` },
                body: JSON.stringify({ imageId: newFile.id })
            });
            if (!linkResponse.ok) throw new Error(`Ошибка привязки изображения: ${linkResponse.status}`);

            // Шаг 3: Обновление интерфейса
            const newPhotoTile = document.createElement('div');
            newPhotoTile.className = 'photo-tile';
            const newImg = new Image();

            imageLoader.getImageSrc(newFile.id, apiBaseUrl, authToken)
                .then(src => {
                    newImg.src = src;
                    newPhotoTile.appendChild(newImg);
                });

            newPhotoTile.addEventListener('click', () => photoViewer.open(newFile.id));
            addPhotoTile.after(newPhotoTile);

            const avatarImgElement = document.getElementById('profileAvatarImg');
            if (avatarImgElement) {
                imageLoader.getImageSrc(newFile.id, apiBaseUrl, authToken)
                    .then(src => avatarImgElement.src = src);
            }

            if (onUserDataUpdate) await onUserDataUpdate();

        } catch (error) {
            console.error("Ошибка при обработке фото:", error);
            alert(`Не удалось обработать фото: ${error.message}. Попробуйте снова.`);
        } finally {
            addPhotoTile.classList.remove('loading');
            addPhotoTile.innerHTML = '<span class="add-photo-tile-icon">+</span>';
            event.target.value = '';
        }
    };

    const renderUserPhotos = (userImages) => {
        const grid = document.getElementById('userPhotosGrid');
        if (!grid) return;

        grid.innerHTML = '';

        const addPhotoTile = document.createElement('div');
        addPhotoTile.id = 'addPhotoTile';
        addPhotoTile.className = 'photo-tile add-photo-tile';
        addPhotoTile.innerHTML = '<span class="add-photo-tile-icon">+</span>';
        const fileInput = document.createElement('input');
        fileInput.type = 'file';
        fileInput.id = 'photoUploadInput';
        fileInput.accept = 'image/*';
        addPhotoTile.appendChild(fileInput);
        grid.appendChild(addPhotoTile);
        addPhotoTile.addEventListener('click', () => fileInput.click());
        fileInput.addEventListener('change', handlePhotoUpload);

        const authToken = localStorage.getItem('accessToken');

        userImages.forEach(image => {
            if (!image || !image.imageId) return;

            const tile = document.createElement('div');
            tile.className = 'photo-tile';

            const img = new Image();
            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'delete-photo-btn';
            deleteBtn.innerHTML = '&times;';

            deleteBtn.addEventListener('click', (event) => {
                event.stopPropagation();
                if (image.imageId) {
                    deletePhoto(image.imageId, tile);
                }
            });


            tile.appendChild(img);
            tile.appendChild(deleteBtn);

            grid.appendChild(tile);

            imageLoader.getImageSrc(image.imageId, apiBaseUrl, authToken)
                .then(src => {
                    img.src = src;
                });

            tile.addEventListener('click', () => {
                if (image.imageId) {
                    photoViewer.open(image.imageId);
                }
            });
        });
    };

    // --- ФУНКЦИИ ДЛЯ ОСНОВНОГО ОКНА ПРОФИЛЯ ---
    const closeProfileModal = () => profileModal.classList.add('hidden');

    const deletePhoto = async (imageId, tileElement) => {
        // Запрос подтверждения у пользователя
        if (!confirm("Вы уверены, что хотите удалить эту фотографию?")) {
            return;
        }

        try {
            const authToken = localStorage.getItem('accessToken');
            const response = await fetch(`${apiBaseUrl}/api/profiles/images/${imageId}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${authToken}` }
            });

            if (!response.ok) {
                throw new Error(`Ошибка сервера: ${response.status}`);
            }

            // Если удаление на сервере прошло успешно, плавно удаляем плитку из DOM
            tileElement.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
            tileElement.style.opacity = '0';
            tileElement.style.transform = 'scale(0.8)';

            setTimeout(() => {
                tileElement.remove();
            }, 300);


            try {
                const newAvatarId = await apiFetch(`${apiBaseUrl}/api/profiles/images/user-avatar`);


                const headerAvatar = document.getElementById('headerAvatarImg');
                const profileAvatar = document.getElementById('profileAvatarImg');

                if (headerAvatar) {
                    imageLoader.getImageSrc(newAvatarId, apiBaseUrl, authToken)
                        .then(src => { headerAvatar.src = src; })
                        .catch(() => { headerAvatar.src = '/images/profile-default.png'; });
                }
                if (profileAvatar) {
                    imageLoader.getImageSrc(newAvatarId, apiBaseUrl, authToken)
                        .then(src => { profileAvatar.src = src; })
                        .catch(() => { profileAvatar.src = '/images/profile-default.png'; });
                }
            } catch (avatarError) {
                if (avatarError.status === 404) {
                    const headerAvatar = document.getElementById('headerAvatarImg');
                    const profileAvatar = document.getElementById('profileAvatarImg');
                    if (headerAvatar) headerAvatar.src = '/images/profile-default.png';
                    if (profileAvatar) profileAvatar.src = '/images/profile-default.png';
                } else {
                    console.error("Не удалось обновить аватар после удаления:", avatarError);
                }
            }
            // --- КОНЕЦ НОВОЙ ЛОГИКИ ---

            // Обновляем данные в других частях приложения, если нужно
            if (onUserDataUpdate) await onUserDataUpdate();

        } catch (error) {
            console.error("Ошибка при удалении фотографии:", error);
            alert("Не удалось удалить фотографию. Попробуйте снова.");
        }
    };

    const saveProfileDetails = async () => {
        const statusInput = document.getElementById('profileStatusInput');
        const birthdayInput = document.getElementById('profileBirthdayInput');
        if (!statusInput || !birthdayInput) return;

        const payload = { status: statusInput.value, birthday: birthdayInput.value };
        saveProfileBtn.disabled = true;
        saveProfileBtn.textContent = 'Сохранение...';

        try {
            const response = await fetch(`${apiBaseUrl}/api/profiles/update`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${localStorage.getItem('accessToken')}` },
                body: JSON.stringify(payload)
            });
            if (!response.ok) throw new Error(`Ошибка сервера: ${response.status}`);

            saveProfileBtn.textContent = 'Сохранено!';
            saveProfileBtn.classList.add('success');
            setTimeout(() => {
                saveProfileBtn.textContent = 'Сохранить';
                saveProfileBtn.classList.remove('success');
                saveProfileBtn.disabled = false;
            }, 2000);
        } catch (error) {
            console.error("Ошибка при сохранении профиля:", error);
            alert("Не удалось сохранить изменения. Попробуйте снова.");
            saveProfileBtn.disabled = false;
            saveProfileBtn.textContent = 'Сохранить';
        }
    };

    const openProfileModal = async (userData) => {
        if (!currentUserId || !userData) { return; }

        profileModal.classList.remove('hidden');
        profileContent.innerHTML = `<div class="skeleton-list">...</div>`;

        try {
            const profileData = await apiFetch(`${apiBaseUrl}/api/profiles/${currentUserId}`);

            // УБРАНА ЛИШНЯЯ ОБЕРТКА И ЛЮБЫЕ ВНУТРЕННИЕ ЭЛЕМЕНТЫ ИЗ #userPhotosGrid
            profileContent.innerHTML = `
                <div class="profile-user-info">
                    <div class="user-info-item"><strong>Имя:</strong> ${userData.name}</div>
                    <div class="user-info-item"><strong>Фамилия:</strong> ${userData.surname || 'Не указана'}</div>
                    <div class="user-info-item"><strong>Имя пользователя:</strong> @${userData.username}</div>
                    <button id="editUserInfoBtn" class="profile-edit-btn">Редактировать основную информацию</button>
                </div>
                <div class="profile-form-header">
                    <div id="myAvatarContainer">
                        <img id="profileAvatarImg" src="/images/profile-default.png" class="profile-avatar">
                    </div>
                    <span style="font-size: 1.2em; font-weight: bold;">Дополнительная информация</span>
                </div>
                <div class="profile-form-group">
                    <label for="profileStatusInput">Статус:</label>
                    <textarea id="profileStatusInput" rows="3">${profileData.status || ''}</textarea>
                </div>
                <div class="profile-form-group">
                    <label for="profileBirthdayInput">Дата рождения:</label>
                    <input type="date" id="profileBirthdayInput" value="${profileData.birthday || ''}">
                </div>
                <div class="photos-section">
                    <div class="photos-section-header">Мои фотографии</div>
                    <div id="userPhotosGrid" class="photos-grid"></div>
                </div>
            `;

            document.getElementById('editUserInfoBtn').addEventListener('click', () => openEditModal(userData));

            const avatarImgElement = document.getElementById('profileAvatarImg');
            if (profileData.avatarId) {
                const authToken = localStorage.getItem('accessToken');
                imageLoader.getImageSrc(profileData.avatarId, apiBaseUrl, authToken)
                    .then(src => { avatarImgElement.src = src; });
            }

            renderUserPhotos(profileData.userImages || []);

        } catch (error) {
            console.error("Ошибка загрузки профиля:", error);
            profileContent.innerHTML = '<p>Не удалось загрузить данные профиля.</p>';
        }
    };

    // ИСПРАВЛЕНА HTML-РАЗМЕТКА
    const openWithPreloadedData = (userData, profileData) => {
        if (!currentUserId || !userData || !profileData) { return; }

        profileModal.classList.remove('hidden');

        // УБРАНА ЛИШНЯЯ ОБЕРТКА И ЛЮБЫЕ ВНУТРЕННИЕ ЭЛЕМЕНТЫ ИЗ #userPhotosGrid
        profileContent.innerHTML = `
            <div class="profile-user-info">
                <div class="user-info-item"><strong>Имя:</strong> ${userData.name}</div>
                <div class="user-info-item"><strong>Фамилия:</strong> ${userData.surname || 'Не указана'}</div>
                <div class="user-info-item"><strong>Имя пользователя:</strong> @${userData.username}</div>
                <button id="editUserInfoBtn" class="profile-edit-btn">Редактировать основную информацию</button>
            </div>
            <div class="profile-form-header">
                <div id="myAvatarContainer">
                    <img id="profileAvatarImg" src="/images/profile-default.png" class="profile-avatar">
                </div>
                <span style="font-size: 1.2em; font-weight: bold;">Дополнительная информация</span>
            </div>
            <div class="profile-form-group">
                <label for="profileStatusInput">Статус:</label>
                <textarea id="profileStatusInput" rows="3">${profileData.status || ''}</textarea>
            </div>
            <div class="profile-form-group">
                <label for="profileBirthdayInput">Дата рождения:</label>
                <input type="date" id="profileBirthdayInput" value="${profileData.birthday || ''}">
            </div>
            <div class="photos-section">
                <div class="photos-section-header">Мои фотографии</div>
                <div id="userPhotosGrid" class="photos-grid"></div>
            </div>
        `;

        document.getElementById('editUserInfoBtn').addEventListener('click', () => openEditModal(userData));

        const avatarImgElement = document.getElementById('profileAvatarImg');
        if (profileData.avatarId) {
            const authToken = localStorage.getItem('accessToken');
            imageLoader.getImageSrc(profileData.avatarId, apiBaseUrl, authToken)
                .then(src => { avatarImgElement.src = src; });
        }

        renderUserPhotos(profileData.userImages || []);
    };

    // --- ФУНКЦИИ ДЛЯ ОКНА РЕДАКТИРОВАНИЯ ---
    const closeEditModal = () => editUserModal.classList.add('hidden');

    const openEditModal = (userData) => {
        document.getElementById('editNameInput').value = userData.name;
        document.getElementById('editSurnameInput').value = userData.surname || '';
        document.getElementById('editUsernameInput').value = userData.username;
        errorContainer.classList.add('hidden');
        editUserModal.classList.remove('hidden');
    };

    const saveUserInfo = async (event) => {
        event.preventDefault();
        const name = document.getElementById('editNameInput').value.trim();
        const surname = document.getElementById('editSurnameInput').value.trim();
        const username = document.getElementById('editUsernameInput').value.trim();

        if (!name || !username) {
            errorContainer.textContent = "Имя и имя пользователя не могут быть пустыми.";
            errorContainer.classList.remove('hidden');
            return;
        }

        const payload = { name, surname, username };
        saveEditUserBtn.disabled = true;
        saveEditUserBtn.textContent = "Сохранение...";
        errorContainer.classList.add('hidden');

        try {
            const response = await fetch(`${apiBaseUrl}/api/users/update`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${localStorage.getItem('accessToken')}` },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const errorData = await response.text();
                throw new Error(errorData || `Ошибка сервера: ${response.status}`);
            }

            if (onUserDataUpdate) {
                await onUserDataUpdate();
            }

            closeEditModal();
            closeProfileModal();

        } catch (error) {
            errorContainer.textContent = `Ошибка: ${error.message}`;
            errorContainer.classList.remove('hidden');
        } finally {
            saveEditUserBtn.disabled = false;
            saveEditUserBtn.textContent = "Сохранить";
        }
    };

    // --- ИНИЦИАЛИЗАЦИЯ МОДУЛЯ ---
    const init = (userId, baseUrl, updateCallback) => {
        currentUserId = userId;
        apiBaseUrl = baseUrl;
        onUserDataUpdate = updateCallback;

        closeProfileBtn.addEventListener('click', closeProfileModal);
        cancelProfileBtn.addEventListener('click', closeProfileModal);
        saveProfileBtn.addEventListener('click', saveProfileDetails);

        closeEditUserBtn.addEventListener('click', closeEditModal);
        cancelEditUserBtn.addEventListener('click', closeEditModal);
        editUserForm.addEventListener('submit', saveUserInfo);
    };

    return { init, open: openProfileModal, openWithPreloadedData };
})();