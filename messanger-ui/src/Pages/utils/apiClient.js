// src/services/apiClient.js

const GATEWAY_URL = `https://${window.location.hostname}:8080`;
const REFRESH_API_URL = `${GATEWAY_URL}/auth/refresh`;

let isRefreshing = false;
let refreshPromise = null;

export const logout = () => {
   localStorage.removeItem('accessToken');
   localStorage.removeItem('refreshToken');
    // В React-приложении жесткий редирект гарантирует очистку всего стейта
    window.location.href = '/login';
};

/**
 * Функция обновления токена
 */
export const handleTokenRefresh = () => {
    if (isRefreshing) {
        return refreshPromise;
    }

    isRefreshing = true;

    refreshPromise = new Promise(async (resolve, reject) => {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
            logout();
            return reject(new Error("Сессия истекла."));
        }

        try {
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


            
            resolve(data.accessToken);
        } catch (error) {
            logout();
            reject(error);
        } finally {
            isRefreshing = false;
            refreshPromise = null;
        }
    });

    return refreshPromise;
};

/**
 * Вспомогательная функция подготовки заголовков
 */
function prepareRequestOptions(options, overrideToken = null) {
    const token = overrideToken || localStorage.getItem('accessToken');
    const headers = { ...options.headers };

    // Если мы отправляем НЕ FormData, добавляем JSON header
    if (!(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
    }

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    return { ...options, headers };
}

async function handleResponse(response) {
    if (!response.ok) {
        const error = new Error(`Ошибка API: ${response.status}`);
        error.status = response.status;
        throw error;
    }

    if (response.status === 204 || response.headers.get('content-length') === '0') {
        return response;
    }

    return response.json();
}

export const apiFetch = async (endpoint, options = {}) => {
    // Автоматически добавляем базовый URL, если передан только путь
    const url = endpoint.startsWith('http') ? endpoint : `${GATEWAY_URL}${endpoint}`;
    
    try {
        const requestOptions = prepareRequestOptions(options);
        let response = await fetch(url, requestOptions);

        if (response.status === 401) {
            const newToken = await handleTokenRefresh();
            const retryOptions = prepareRequestOptions(options, newToken);
            response = await fetch(url, retryOptions);
        }

        return await handleResponse(response);
    } catch (error) {
        if (error.name !== 'AbortError') {
            console.warn(`Запрос к ${url} завершился ошибкой:`, error);
        }
        throw error;
    }
};