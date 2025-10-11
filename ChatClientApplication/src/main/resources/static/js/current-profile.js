const myProfileManager = (() => {
    // --- Приватные переменные ---
    let currentUserId = null;
    let apiBaseUrl = null;
    let onUserDataUpdate = null; // Коллбэк для обновления данных в chats.js

    // --- DOM Элементы ---
    // Основное окно профиля
    const profileModal = document.getElementById('myProfileModal');
    const closeProfileBtn = document.getElementById('closeMyProfileBtn');
    const cancelProfileBtn = document.getElementById('cancelMyProfileBtn');
    const saveProfileBtn = document.getElementById('saveMyProfileBtn');
    const profileContent = document.getElementById('myProfileContent');

    // Окно редактирования данных пользователя
    const editUserModal = document.getElementById('editUserModal');
    const closeEditUserBtn = document.getElementById('closeEditUserBtn');
    const cancelEditUserBtn = document.getElementById('cancelEditUserBtn');
    const editUserForm = document.getElementById('editUserForm');
    const saveEditUserBtn = document.getElementById('saveEditUserBtn');
    const errorContainer = document.getElementById('editUserError');

    // --- Функции для основного окна профиля ---
    const closeProfileModal = () => profileModal.classList.add('hidden');

    const saveProfileDetails = async () => {
        // ... (эта функция остается без изменений)
        const statusInput = document.getElementById('profileStatusInput');
        const birthdayInput = document.getElementById('profileBirthdayInput');
        if (!statusInput || !birthdayInput) return;
        const payload = { status: statusInput.value, birthday: birthdayInput.value };
        saveProfileBtn.disabled = true;
        saveProfileBtn.textContent = 'Сохранение...';
        try {
            const response = await fetch(`${apiBaseUrl}/api/profiles/update`, {
                method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${localStorage.getItem('accessToken')}` },
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
        if (!currentUserId || !userData) {
            console.error("Данные пользователя не были переданы в openProfileModal.");
            profileContent.innerHTML = '<p>Ошибка: не удалось получить данные пользователя.</p>';
            return;
        }

        profileModal.classList.remove('hidden');
        profileContent.innerHTML = `<div class="skeleton-list">${Array(3).fill('<div class="skeleton skeleton-row"></div>').join("")}</div>`;

        try {
            const profileData = await apiFetch(`${apiBaseUrl}/api/profiles/${currentUserId}`);

            profileContent.innerHTML = `
                <div class="profile-form-container">
                    <div class="profile-user-info">
                        <div class="user-info-item"><strong>Имя:</strong> ${userData.name}</div>
                        <div class="user-info-item"><strong>Фамилия:</strong> ${userData.surname || 'Не указана'}</div>
                        <div class="user-info-item"><strong>Имя пользователя:</strong> @${userData.username}</div>
                        <button id="editUserInfoBtn" class="profile-edit-btn">Редактировать основную информацию</button>
                    </div>
                    <div class="profile-form-header">
                        <div id="myAvatarContainer"></div>
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
                </div>`;

            document.getElementById('editUserInfoBtn').addEventListener('click', () => openEditModal(userData));

            const avatarContainer = document.getElementById('myAvatarContainer');
            if (profileData.avatarId) {
                avatarContainer.innerHTML = `<div class="skeleton profile-avatar"></div>`;
                const avatarImg = new Image();
                avatarImg.className = 'profile-avatar';
                avatarImg.onload = () => { avatarContainer.innerHTML = ''; avatarContainer.appendChild(avatarImg); };
                avatarImg.src = `${apiBaseUrl}/api/storage/proxy/download/by-id?id=${profileData.avatarId}`;
            } else {
                avatarContainer.innerHTML = `<img src="/images/profile-default.png" class="profile-avatar">`;
            }

        } catch (error) {
            console.error("Ошибка загрузки профиля:", error);
            profileContent.innerHTML = '<p>Не удалось загрузить данные профиля.</p>';
        }
    };


    // --- Функции для окна редактирования ---
    const closeEditModal = () => editUserModal.classList.add('hidden');

    const openEditModal = (userData) => {
        // Заполняем поля текущими данными
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
                const errorData = await response.text(); // Попробуем прочитать текст ошибки
                throw new Error(errorData || `Ошибка сервера: ${response.status}`);
            }

            if (onUserDataUpdate) {
                await onUserDataUpdate();
            }

            closeEditModal();
            closeProfileModal(); // Закрываем оба окна для лучшего UX

        } catch (error) {
            errorContainer.textContent = `Ошибка: ${error.message}`;
            errorContainer.classList.remove('hidden');
        } finally {
            saveEditUserBtn.disabled = false;
            saveEditUserBtn.textContent = "Сохранить";
        }
    };


    // --- Инициализация модуля ---
    const init = (userId, baseUrl, updateCallback) => {
        currentUserId = userId;
        apiBaseUrl = baseUrl;
        onUserDataUpdate = updateCallback;

        // Обработчики для основного окна
        closeProfileBtn.addEventListener('click', closeProfileModal);
        cancelProfileBtn.addEventListener('click', closeProfileModal);
        saveProfileBtn.addEventListener('click', saveProfileDetails);

        // Обработчики для окна редактирования
        closeEditUserBtn.addEventListener('click', closeEditModal);
        cancelEditUserBtn.addEventListener('click', closeEditModal);
        editUserForm.addEventListener('submit', saveUserInfo);
    };

    return { init, open: openProfileModal };
})();