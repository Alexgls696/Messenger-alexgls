// Файл: apiClient.js

const REFRESH_API_URL = 'http://localhost:8080/auth/refresh';
let isRefreshing = false;
let refreshPromise = null;

function logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/login';
}


async function apiFetch(url, options = {}) {
    try {
        // ИСПРАВЛЕНИЕ: optionsWithSignal теперь будет содержать и заголовки, и signal.
        const optionsWithSignal = prepareRequestOptions(options);
        let response = await fetch(url, optionsWithSignal);

        if (response.status === 401) {
            await handleTokenRefresh();
            // Повторяем запрос с теми же опциями (включая signal)
            response = await fetch(url, optionsWithSignal);
        }

        if (!response.ok) {
            const error = new Error(`Ошибка API: ${response.status} ${response.statusText}`);
            error.status = response.status;
            throw error;
        }

        if (response.status === 204) {
            return null;
        }

        const contentLength = response.headers.get('content-length');
        if (contentLength === '0') {
            return null;
        }

        return await response.json();

    } catch (error) {
        console.warn(`Ошибка или отмена запроса к ${url}:`, error);
        throw error;
    }
}

function prepareRequestOptions(options) {
    const token = localStorage.getItem('accessToken');
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    return {
        ...options,
        headers,
        signal: options.signal // Добавляем эту строку
    };
}

// Главная логика обновления токена
async function handleTokenRefresh() {
    if (isRefreshing) {
        return refreshPromise;
    }

    isRefreshing = true;

    refreshPromise = new Promise(async (resolve, reject) => {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
            logout();
            return reject(new Error("Refresh token не найден."));
        }

        try {
            console.log("TRY TO UPDATE REFRESH TOKEN: "+refreshToken)
            const response = await fetch(REFRESH_API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ refreshToken }),
            });

            if (response.ok) {
                console.log("REFRESH_TOKEN_OK")
                const data = await response.json();
                localStorage.setItem('accessToken', data.accessToken);
                localStorage.setItem('refreshToken', data.refreshToken);
                resolve();
            } else {
                // Если refresh token тоже недействителен, выходим из системы
                logout();
                reject(new Error("Сессия истекла. Пожалуйста, войдите снова."));
            }
        } catch (error) {
            console.error('Критическая ошибка при обновлении токена:', error);
            logout();
            reject(error);
        } finally {
            isRefreshing = false;
            refreshPromise = null;
        }
    });

    return refreshPromise;
}