// Файл: apiClient.js

const REFRESH_API_URL = 'https://localhost:8080/auth/refresh';

// --- Управление состоянием обновления токена ---
let isRefreshing = false;
let refreshPromise = null;

function logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/login';
}

/**
 * ГЛАВНАЯ ФУНКЦИЯ ОБНОВЛЕНИЯ ТОКЕНА.
 * Может быть вызвана из любого места (apiFetch, chats.js и т.д.).
 * Гарантирует, что только один запрос на обновление будет выполнен одновременно.
 * @returns {Promise<string>} Промис, который разрешается новым токеном доступа.
 */
function handleTokenRefresh() {
    if (isRefreshing) {
        // Если обновление уже запущено, просто возвращаем его промис
        return refreshPromise;
    }

    isRefreshing = true;

    // Создаем и сохраняем промис, чтобы другие вызовы могли его дождаться
    refreshPromise = new Promise(async (resolve, reject) => {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
            logout();
            return reject(new Error("Сессия истекла."));
        }

        try {
            console.log("Попытка обновить токен...");
            const response = await fetch(REFRESH_API_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken }),
            });

            if (!response.ok) {
                throw new Error("Не удалось обновить токен.");
            }

            const data = await response.json();
            localStorage.setItem('accessToken', data.accessToken);
            localStorage.setItem('refreshToken', data.refreshToken);

            console.log("Токен успешно обновлен.");
            resolve(data.accessToken); // Успешно, возвращаем новый токен

        } catch (error) {
            console.error("Критическая ошибка при обновлении токена:", error);
            logout(); // Выходим из системы при ошибке
            reject(error);
        } finally {
            // Сбрасываем состояние, чтобы следующий запрос мог инициировать обновление
            isRefreshing = false;
            refreshPromise = null;
        }
    });

    return refreshPromise;
}

/**
 * "Умная" функция для выполнения запросов к API.
 */
async function apiFetch(url, options = {}) {
    try {
        const requestOptions = prepareRequestOptions(options);
        let response = await fetch(url, requestOptions);

        if (response.status === 401) {
            // ИСПОЛЬЗУЕМ ОБЩУЮ ФУНКЦИЮ
            const newToken = await handleTokenRefresh();

            // Повторяем запрос с новым токеном
            const newOptions = prepareRequestOptions(options, newToken);
            response = await fetch(url, newOptions);
        }

        // Обрабатываем ответ
        return await handleResponse(response);

    } catch (error) {
        if (error.name !== 'AbortError') {
            console.error(`Ошибка при запросе к ${url}:`, error);
        }
        throw error;
    }
}

// --- Вспомогательные функции (без изменений) ---

function prepareRequestOptions(options, overrideToken = null) {
    const token = overrideToken || localStorage.getItem('accessToken');
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return { ...options, headers, signal: options.signal };
}

async function handleResponse(response) {
    if (!response.ok) {
        const error = new Error(`Ошибка API: ${response.status} ${response.statusText}`);
        error.status = response.status;
        throw error;
    }
    if (response.status === 204 || response.headers.get('content-length') === '0') {
        return null;
    }
    return response.json();
}