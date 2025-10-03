document.addEventListener('DOMContentLoaded', () => {
    // --- Получаем все необходимые элементы DOM ---
    // Шаг 1: Профиль
    const profileStep = document.getElementById('profile-step');
    const profileForm = document.getElementById('profile-form');
    const saveProfileButton = document.getElementById('save-profile-button');
    const profileMessageBox = document.getElementById('profile-message-box');
    const usernameInput = document.getElementById('username');
    const nameInput = document.getElementById('name');
    const surnameInput = document.getElementById('surname');

    // Шаг 2: Пароль
    const passwordStep = document.getElementById('password-step');
    const passwordForm = document.getElementById('password-form');
    const setPasswordButton = document.getElementById('set-password-button');
    const passwordMessageBox = document.getElementById('password-message-box');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirm-password');

    // --- Адреса API ---
    const UPDATE_USER_URL = 'http://localhost:8080/api/users/update';
    // ВАЖНО: Ваш код использует "/update-password", а не "/set-password". Используем эндпоинт из кода.
    const SET_PASSWORD_URL = 'http://localhost:8080/api/users/update-password';

    const parseJwt = (token) => {
        try {
            return JSON.parse(atob(token.split('.')[1]));
        } catch (e) { return null; }
    };

    // --- Основная логика при загрузке страницы ---
    const accessToken = localStorage.getItem('accessToken');
    if (!accessToken) {
        window.location.href = '/login';
        return;
    }

    const decodedToken = parseJwt(accessToken);
    if (!decodedToken || !decodedToken.userId || !decodedToken.sub) {
        alert('Ошибка авторизации. Не удалось получить данные пользователя из токена. Пожалуйста, войдите снова.');
        localStorage.clear();
        window.location.href = '/login';
        return;
    }

    const userId = decodedToken.userId;
    usernameInput.value = decodedToken.sub;

    // --- Обработчик формы профиля (Шаг 1) ---
    profileForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        profileMessageBox.classList.add('hidden');
        saveProfileButton.disabled = true;
        saveProfileButton.textContent = 'Сохранение...';

        const requestBody = {
            id: userId,
            name: nameInput.value,
            surname: surnameInput.value || '',
            username: usernameInput.value,
        };

        try {
            const response = await fetch(UPDATE_USER_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${accessToken}`
                },
                body: JSON.stringify(requestBody),
            });

            if (response.ok) {
                // Успех! Не перенаправляем, а переключаемся на шаг установки пароля.
                profileStep.classList.add('hidden');
                passwordStep.classList.remove('hidden');
            } else {
                const errorData = await response.json();
                let errorMessage = 'Произошла неизвестная ошибка.';
                if (errorData.detail || errorData.error) {
                    errorMessage = `${errorData.detail || ''} ${errorData.error || ''}`.trim();
                }
                throw new Error(errorMessage);
            }
        } catch (error) {
            profileMessageBox.textContent = error.message;
            profileMessageBox.classList.add('error-message');
            profileMessageBox.classList.remove('hidden');
        } finally {
            saveProfileButton.disabled = false;
            saveProfileButton.textContent = 'Сохранить и продолжить';
        }
    });

    // --- Обработчик формы пароля (Шаг 2) ---
    passwordForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        passwordMessageBox.classList.add('hidden');

        // Клиентская валидация
        const password = passwordInput.value;
        const confirmPassword = confirmPasswordInput.value;
        if (password.length < 8) {
            passwordMessageBox.textContent = 'Пароль должен быть не менее 8 символов.';
            passwordMessageBox.classList.add('error-message');
            passwordMessageBox.classList.remove('hidden');
            return;
        }
        if (password !== confirmPassword) {
            passwordMessageBox.textContent = 'Пароли не совпадают.';
            passwordMessageBox.classList.add('error-message');
            passwordMessageBox.classList.remove('hidden');
            return;
        }

        setPasswordButton.disabled = true;
        setPasswordButton.textContent = 'Завершение...';

        try {
            const response = await fetch(SET_PASSWORD_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'text/plain',
                    'Authorization': `Bearer ${accessToken}`
                },
                // Отправляем пароль как обычный текст, а не JSON
                body: password,
            });

            if (response.ok) {
                // Финальный успех! Перенаправляем на главную.
                window.location.href = '/index';
            } else {
                const errorData = await response.json();
                let errorMessage = 'Произошла неизвестная ошибка.';
                if (errorData.detail || errorData.error) {
                    errorMessage = `${errorData.detail || ''} ${errorData.error || ''}`.trim();
                }
                throw new Error(errorMessage);
            }
        } catch (error) {
            passwordMessageBox.textContent = error.message;
            passwordMessageBox.classList.add('error-message');
            passwordMessageBox.classList.remove('hidden');
        } finally {
            setPasswordButton.disabled = false;
            setPasswordButton.textContent = 'Завершить регистрацию';
        }
    });
});