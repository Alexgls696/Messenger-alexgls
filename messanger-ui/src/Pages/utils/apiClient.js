export const GATEWAY_URL = `http://${window.location.hostname}:8080`;
const REFRESH_API_URL = `${GATEWAY_URL}/auth/refresh`;

let isRefreshing = false;
let refreshPromise = null;

export const logout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/login';
};

export const handleTokenRefresh = async () => {
    if (isRefreshing) return refreshPromise;

    isRefreshing = true;
    refreshPromise = (async () => {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
            logout();
            throw new Error("No refresh token");
        }

        try {
            const response = await fetch(REFRESH_API_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken }),
            });

            // Если сервер ПРЯМО сказал, что рефреш-токен невалиден
            if (response.status === 401 || response.status === 403) {
                console.error("Refresh token expired or invalid");
                logout();
                throw new Error("Session expired");
            }

            if (!response.ok) {
                // Если это 500 ошибка или лаг сети, мы НЕ делаем logout.
                // Мы просто выбрасываем ошибку, чтобы попробовать позже.
                throw new Error("Server error during refresh");
            }

            const data = await response.json();
            localStorage.setItem('accessToken', data.accessToken);
            localStorage.setItem('refreshToken', data.refreshToken);
            return data.accessToken;

        } catch (error) {
            // Если это ошибка сети (failed to fetch), просто пробрасываем её
            throw error;
        } finally {
            isRefreshing = false;
            refreshPromise = null;
        }
    })();

    return refreshPromise;
};

export const apiFetch = async (endpoint, options = {}) => {
    const url = endpoint.startsWith('http') ? endpoint : `${GATEWAY_URL}${endpoint}`;

    // Подготовка заголовков
    const getOptions = (token) => {
        const headers = { ...options.headers };
        if (!(options.body instanceof FormData)) headers['Content-Type'] = 'application/json';
        if (token) headers['Authorization'] = `Bearer ${token}`;
        return { ...options, headers };
    };

    try {
        let response = await fetch(url, getOptions(localStorage.getItem('accessToken')));

        if (response.status === 401) {
            // Пытаемся обновиться
            try {
                const newToken = await handleTokenRefresh();
                // Повторяем запрос
                response = await fetch(url, getOptions(newToken));
            } catch (err) {
                // Если handleTokenRefresh вызвал logout, код здесь уже не важен
                throw err;
            }
        }

        return await handleResponse(response);
    } catch (error) {
        throw error;
    }
};

async function handleResponse(response) {
    if (!response.ok) {
        // Здесь мы НЕ делаем автоматический logout, 
        // чтобы случайная ошибка 500 не закрыла сессию.
        const error = new Error(`API Error: ${response.status}`);
        error.status = response.status;
        throw error;
    }
    const contentLength = response.headers.get('content-length');
    if (response.status === 204 || contentLength === '0') return null;
    return response.json();
}