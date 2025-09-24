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

    const API_BASE_URL = `http://${window.location.hostname}:8080`;

    // Переменные для хранения состояния
    let loginMethod = 'email'; // 'email' или 'phone'
    let loginData = {}; // Для хранения данных пользователя между шагами

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
        loginData = {
            username: formData.get('username'),
            [loginMethod]: formData.get('contact-info') // Динамический ключ: 'email' или 'phone'
        };

        try {
            // --- ЗАГЛУШКА API: Запрос на отправку кода ---
            // В реальном приложении здесь будет fetch(`${API_BASE_URL}/auth/login/initiate`, ...)
            const response = await new Promise(resolve => {
                setTimeout(() => {
                    console.log('Отправка запроса на получение кода (заглушка):', loginData);
                    resolve({ ok: true }); // Имитируем успешный ответ
                }, 1000);
            });
            // --- КОНЕЦ ЗАГЛУШКИ ---

            if (response.ok) {
                loginForm.classList.add('hidden');
                verifyForm.classList.remove('hidden');
            } else {
                throw new Error('Не удалось отправить код. Проверьте введенные данные.');
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
        const finalPayload = {
            ...loginData,
            code: verificationCode
        };

        try {
            // --- ЗАГЛУШКА API: Запрос на проверку кода ---
            // В реальном приложении здесь будет fetch(`${API_BASE_URL}/auth/login/verify`, ...)
            const response = await new Promise((resolve, reject) => {
                setTimeout(() => {
                    if (verificationCode === "123456") { // "Правильный" код для теста
                        console.log('Отправка запроса на верификацию (заглушка):', finalPayload);
                        const jwtResponse = {
                            accessToken: 'fake-access-token-from-mock-api-123',
                            refreshToken: 'fake-refresh-token-from-mock-api-456'
                        };
                        resolve({ ok: true, json: () => Promise.resolve(jwtResponse) });
                    } else {
                        reject(new Error('Неверный код доступа.'));
                    }
                }, 1000);
            });
            // --- КОНЕЦ ЗАГЛУШКИ ---

            if (response.ok) {
                const jwtResponse = await response.json();
                if (jwtResponse.accessToken && jwtResponse.refreshToken) {
                    localStorage.setItem('accessToken', jwtResponse.accessToken);
                    localStorage.setItem('refreshToken', jwtResponse.refreshToken);
                    window.location.href = '/index'; // Переадресация на главную
                } else {
                    throw new Error('Ответ сервера не содержит токенов.');
                }
            }
        } catch (error) {
            verifyMessageBox.textContent = error.message || 'Произошла неизвестная ошибка.';
            verifyMessageBox.classList.add('error-message');
            verifyMessageBox.classList.remove('hidden');
        } finally {
            verifyButton.disabled = false;
            verifyButton.textContent = 'Войти';
        }
    });
});