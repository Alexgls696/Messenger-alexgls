document.addEventListener('DOMContentLoaded', () => {
    // Получаем элементы DOM
    const profileForm = document.getElementById('profile-form');
    const saveButton = document.getElementById('save-button');
    const messageBox = document.getElementById('message-box');
    const usernameInput = document.getElementById('username');
    const nameInput = document.getElementById('name');
    const surnameInput = document.getElementById('surname');

    // Адрес вашего API
    const API_URL = 'http://localhost:8080/api/users/update';

    // --- Вспомогательная функция для декодирования JWT ---
    // Она не проверяет подпись (это задача сервера), а только извлекает данные.
    const parseJwt = (token) => {
        try {
            return JSON.parse(atob(token.split('.')[1]));
        } catch (e) {
            return null;
        }
    };

    // --- Основная логика при загрузке страницы ---
    const accessToken = localStorage.getItem('accessToken');

    if (!accessToken) {
        window.location.href = '/login'; // или ваша страница входа
        return;
    }

    const decodedToken = parseJwt(accessToken);
    console.log(decodedToken);
    // Если токен невалидный или не содержит нужных данных
    if (!decodedToken || !decodedToken.userId || !decodedToken.sub) {
        alert('Ошибка авторизации. Пожалуйста, войдите снова.');
        //localStorage.clear();
        //window.location.href = '/login';
        return;
    }

    const userId = decodedToken.userId; // Получаем ID из токена
    usernameInput.value = decodedToken.sub; // 'sub' обычно содержит username

    // --- Обработчик отправки формы ---
    profileForm.addEventListener('submit', async (event) => {
        event.preventDefault();

        // Скрываем старые сообщения
        messageBox.classList.add('hidden');
        messageBox.textContent = '';

        saveButton.disabled = true;
        saveButton.textContent = 'Сохранение...';

        // Формируем тело запроса в соответствии с UpdateUserRequest
        const requestBody = {
            id: userId,
            name: nameInput.value,
            surname: surnameInput.value || '', // Отправляем пустую строку, если поле не заполнено
            username: usernameInput.value,
        };

        try {
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    // Обязательно передаем токен для авторизации запроса
                    'Authorization': `Bearer ${accessToken}`
                },
                body: JSON.stringify(requestBody),
            });

            if (response.ok) {
                // Успех! Перенаправляем на главную страницу
                window.location.href = '/index';
            } else {
                // Если произошла ошибка, пытаемся извлечь её из тела ответа
                const errorData = await response.json();
                let errorMessage = 'Произошла неизвестная ошибка.';

                // Формируем сообщение из полей detail и error, как вы и просили
                if (errorData.detail || errorData.error) {
                    errorMessage = `${errorData.detail || ''} ${errorData.error || ''}`.trim();
                }

                throw new Error(errorMessage);
            }

        } catch (error) {
            messageBox.textContent = error.message;
            messageBox.classList.add('error-message');
            messageBox.classList.remove('hidden');
        } finally {
            saveButton.disabled = false;
            saveButton.textContent = 'Сохранить и войти';
        }
    });
});