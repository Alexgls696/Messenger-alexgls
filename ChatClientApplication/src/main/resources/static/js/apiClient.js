// Файл: apiClient.js

const REFRESH_API_URL = 'https://localhost:8080/auth/refresh';

// --- Управление состоянием обновления токена ---
let isRefreshing = false;
// Массив "ожидающих" запросов, которые получили 401 и ждут нового токена
let failedQueue = [];

const processQueue = (error, token = null) => {
    failedQueue.forEach(prom => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve(token);
        }
    });
    failedQueue = [];
};

function logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/login';
}

async function apiFetch(url, options = {}) {
    const requestOptions = prepareRequestOptions(options);

    try {

        const response = await fetch(url, requestOptions);

        if (response.status !== 401) {
            return handleResponse(response);
        }


        if (isRefreshing) {
            return new Promise((resolve, reject) => {
                failedQueue.push({ resolve, reject });
            })
                .then(newToken => {
                    // Когда токен обновился, повторяем запрос с новым токеном
                    const newOptions = prepareRequestOptions(options, newToken);
                    return fetch(url, newOptions).then(handleResponse);
                });
        }

        // Если мы первые, кто получил 401, начинаем обновление
        isRefreshing = true;

        return new Promise(async (resolve, reject) => {
            const refreshToken = localStorage.getItem('refreshToken');
            if (!refreshToken) {
                logout();
                return reject(new Error("Сессия истекла."));
            }

            try {
                const refreshResponse = await fetch(REFRESH_API_URL, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ refreshToken }),
                });

                if (!refreshResponse.ok) {
                    throw new Error("Не удалось обновить токен.");
                }

                const data = await refreshResponse.json();
                localStorage.setItem('accessToken', data.accessToken);
                localStorage.setItem('refreshToken', data.refreshToken);

                // Успешно обновили! "Пробуждаем" все запросы из очереди с новым токеном
                processQueue(null, data.accessToken);

                // Повторяем наш оригинальный запрос
                const newOptions = prepareRequestOptions(options, data.accessToken);
                resolve(fetch(url, newOptions).then(handleResponse));

            } catch (error) {
                // Если обновление провалилось, "отменяем" все запросы из очереди
                processQueue(error, null);
                logout();
                reject(error);
            } finally {
                isRefreshing = false;
            }
        });

    } catch (error) {
        console.error(`Ошибка при запросе к ${url}:`, error);
        throw error;
    }
}



function prepareRequestOptions(options, overrideToken = null) {
    const token = overrideToken || localStorage.getItem('accessToken');
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
        signal: options.signal
    };
}


async function handleResponse(response) {
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
    return response.json();
}