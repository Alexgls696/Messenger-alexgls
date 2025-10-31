document.addEventListener('DOMContentLoaded', () => {
    // Получаем все необходимые элементы DOM
    const loginForm = document.getElementById('login-form');
    const verifyForm = document.getElementById('verify-form');

    const emailTab = document.getElementById('email-tab');
    const phoneTab = document.getElementById('phone-tab');

    const contactLabel = document.getElementById('contact-label');
    const contactInput = document.getElementById('contact-info');

    const getCodeButton = document.getElementById('get-code-button');
    const verifyButton = document.getElementById('verify-button');
    const messageBox = document.getElementById('message-box');
    const verifyMessageBox = document.getElementById('verify-message-box');

    // --- Константы для API ---
    const API_BASE_URL = 'https://localhost:8080/api/verification';
    const INITIATE_URL = `${API_BASE_URL}/create`;
    const REGISTER_URL = `https://localhost:8080/api/authentication/register`;

    // --- Переменные для хранения состояния ---
    let loginMethod = 'email'; // 'email' или 'phone'
    let operationId = null; // Для хранения ID операции между шагами

    const clearMessages = () => {
        messageBox.className = 'message hidden';
        messageBox.textContent = '';
        verifyMessageBox.className = 'message hidden';
        verifyMessageBox.textContent = '';
    };

    // --- Логика переключения табов ---
    const setActiveTab = (method) => {
        loginMethod = method;
        clearMessages();

        if (method === 'email') {
            emailTab.classList.add('active');
            phoneTab.classList.remove('active');
            contactLabel.textContent = 'Email';
            contactInput.type = 'email';
            contactInput.placeholder = 'your@email.com';
        } else {
            phoneTab.classList.add('active');
            emailTab.classList.remove('active');
            contactLabel.textContent = 'Номер телефона';
            contactInput.type = 'tel';
            contactInput.placeholder = '+7 (999) 999-99-99';
        }
    };

    emailTab.addEventListener('click', () => setActiveTab('email'));
    phoneTab.addEventListener('click', () => setActiveTab('phone'));

    // --- Обработчик отправки формы для получения кода (Шаг 1) ---
    loginForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearMessages();

        getCodeButton.disabled = true;
        getCodeButton.textContent = 'Отправка...';

        const formData = new FormData(loginForm);
        const contactValue = formData.get('contact-info');

        // Формируем тело запроса в соответствии с InitializeLoginRequest
        const requestBody = {
            username: formData.get('username'),
            email: loginMethod === 'email' ? contactValue : null,
            phoneNumber: loginMethod === 'phone' ? contactValue : null
        };

        try {
            // Отправляем реальный запрос на сервер
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

                // Если ID получен, переключаем формы
                if (operationId) {
                    loginForm.classList.add('hidden');
                    verifyForm.classList.remove('hidden');
                } else {
                    throw new Error('Сервер не вернул ID операции.');
                }
            } else {
                // Пытаемся получить текст ошибки от сервера
                const errorData = await response.json().catch(() => ({message: 'Произошла ошибка на сервере'}));
                const errorMessage = errorData.message || `Ошибка ${response.status}: ${errorData.error}.`;
                throw new Error(errorMessage);
            }
        } catch (error) {
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

        // Формируем тело запроса в соответствии с CheckCodeRequest
        const requestBody = {
            id: operationId,
            code: verificationCode
        };

        try {
            console.log(REGISTER_URL);
            console.log(requestBody);
            const response = await fetch(REGISTER_URL, {
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
                    window.location.href = '/index'; // Успех! Переадресация на главную
                } else {
                    throw new Error('Ответ сервера не содержит токенов.');
                }
            } else {
                const errorData = await response.json().catch(() => ({message: 'Произошла ошибка на сервере'}));
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