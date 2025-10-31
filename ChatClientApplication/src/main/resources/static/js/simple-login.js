document.addEventListener('DOMContentLoaded', () => {
    // Получаем все необходимые элементы DOM
    const loginForm = document.getElementById('login-form');
    const verifyForm = document.getElementById('verify-form');

    const getCodeButton = document.getElementById('get-code-button');
    const verifyButton = document.getElementById('verify-button');
    const messageBox = document.getElementById('message-box');
    const verifyMessageBox = document.getElementById('verify-message-box');

    // --- Константы для API ---
    const API_VERIFICATION_URL = 'https://localhost:8080/api/verification';
    const API_AUTH_URL = 'https://localhost:8080/api/authentication';
    const INITIATE_URL = `${API_VERIFICATION_URL}/create-for-exists`;
    const LOGIN_URL = `${API_AUTH_URL}/login-by-email`;


    // Переменная для хранения ID операции между шагами
    let operationId = null;

    const clearMessages = () => {
        messageBox.className = 'message hidden';
        messageBox.textContent = '';
        verifyMessageBox.className = 'message hidden';
        verifyMessageBox.textContent = '';
    };

    // --- Обработчик отправки формы для получения кода (Шаг 1) ---
    loginForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearMessages();

        getCodeButton.disabled = true;
        getCodeButton.textContent = 'Отправка...';

        const emailValue = document.getElementById('email').value;

        // Формируем тело запроса: только email, остальные поля null
        const requestBody = {
            username: null,
            email: emailValue,
            phoneNumber: null
        };

        try {
            const response = await fetch(INITIATE_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(requestBody),
            });

            if (response.ok) {
                const data = await response.json(); // Ожидаем { "id": "..." }
                operationId = data.id; // Сохраняем ID операции

                if (operationId) {
                    // Переключаем формы
                    loginForm.classList.add('hidden');
                    verifyForm.classList.remove('hidden');
                } else {
                    throw new Error('Сервер не вернул ID операции.');
                }
            } else {
                const errorData = await response.json().catch(() => ({ message: 'Произошла ошибка на сервере' }));
                const errorMessage = errorData.message || `Ошибка ${response.status}: ${errorData.error}.`;
                throw new Error(errorMessage);
            }
        } catch (error) {
            console.log(error);
            messageBox.textContent = error.message || 'Не удалось подключиться к серверу.';
            messageBox.classList.add('error-message');
            messageBox.classList.remove('hidden');
        } finally {
            getCodeButton.disabled = false;
            getCodeButton.textContent = 'Получить код';
        }
    });

    // --- Обработчик отправки формы верификации (Шаг 2) ---
    verifyForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearMessages();

        verifyButton.disabled = true;
        verifyButton.textContent = 'Проверка...';

        const verificationCode = document.getElementById('verification-code').value;

        const requestBody = {
            id: operationId,
            code: verificationCode
        };

        try {
            const response = await fetch(LOGIN_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(requestBody),
            });

            if (response.ok) {
                const jwtResponse = await response.json();
                if (jwtResponse.accessToken && jwtResponse.refreshToken) {
                    localStorage.setItem('accessToken', jwtResponse.accessToken);
                    localStorage.setItem('refreshToken', jwtResponse.refreshToken);
                    window.location.href = '/index';
                } else {
                    throw new Error('Ответ сервера не содержит токенов.');
                }
            } else {
                const errorData = await response.json().catch(() => ({ message: 'Произошла ошибка на сервере' }));
                const errorMessage = errorData.message || 'Неверный код или истек срок действия операции.';
                throw new Error(errorMessage);
            }
        } catch (error) {
            verifyMessageBox.textContent = error.message;
            verifyMessageBox.classList.add('error-message');
            verifyMessageBox.classList.remove('hidden');
        } finally {
            verifyButton.disabled = false;
            verifyButton.textContent = 'Войти';
        }
    });
});
