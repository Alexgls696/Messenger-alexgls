
document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('login-form');
    const loginButton = document.getElementById('login-button');
    const errorMessageDiv = document.getElementById('error-message');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');


    const gatewayHost = window.location.hostname; // 'localhost'
    const gatewayPort = 8080; // Порт вашего Gateway
    const gatewayAddress = `${gatewayHost}:${gatewayPort}`;

    const httpProtocol = 'http:'; // Для локальной разработки
    const API_BASE_URL = `${httpProtocol}//${gatewayAddress}`; // Базовый URL для всех REST API

    loginForm.addEventListener('submit', async (event) => {
        event.preventDefault();

        const username = usernameInput.value;
        const password = passwordInput.value;

        errorMessageDiv.classList.add('hidden');
        loginButton.disabled = true;
        loginButton.textContent = 'Вход...';

        try {
            const response = await fetch(`${API_BASE_URL}/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username, password }),
            });

            if (response.ok) {
                const data = await response.json();
                localStorage.setItem('accessToken', data.accessToken);
                localStorage.setItem('refreshToken', data.refreshToken);
                window.location.href = '/';

            } else if (response.status === 401) {
                showError('Неверное имя пользователя или пароль.');
            } else {
                showError('Произошла ошибка на сервере. Попробуйте позже.');
            }

        } catch (error) {
            console.error('Ошибка сети:', error);
            showError(`Не удалось подключиться к серверу аутентификации. ${API_BASE_URL}`);
        } finally {
            if (!localStorage.getItem('accessToken')) {
                loginButton.disabled = false;
                loginButton.textContent = 'Войти';
            }
        }
    });

    function showError(message) {
        errorMessageDiv.textContent = message;
        errorMessageDiv.classList.remove('hidden');
    }
});