import React, { useState, useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { handleTokenRefresh } from '../utils/apiClient'

const ProtectedRoute = ({ children }) => {
    // 'checking' - процесс проверки/обновления
    // 'authorized' - доступ разрешен
    // 'unauthorized' - редирект на логин
    const [authStatus, setAuthStatus] = useState('checking');

    useEffect(() => {
        const verifyAuth = async () => {
            const accessToken = localStorage.getItem('accessToken');
            const refreshToken = localStorage.getItem('refreshToken');

            // 1. Если accessToken уже есть, считаем пользователя авторизованным
            // (Если он протух, apiFetch обновит его позже при первом запросе)
            if (accessToken) {
                setAuthStatus('authorized');
                return;
            }

            // 2. Если accessToken нет, но есть refreshToken — пробуем обновиться
            if (refreshToken) {
                try {
                    await handleTokenRefresh();
                    setAuthStatus('authorized');
                } catch (err) {
                    console.error("Не удалось восстановить сессию:", err);
                    setAuthStatus('unauthorized');
                }
            } else {
                // 3. Нет вообще никаких токенов
                setAuthStatus('unauthorized');
            }
        };

        verifyAuth();
    }, []);

    // Пока идет асинхронная проверка, показываем пустой экран или спиннер
    if (authStatus === 'checking') {
        return (
            <div className="auth-loading-screen">
                {/* Сюда можно добавить ваш компонент скелетона или спиннер */}
                <div className="skeleton-row" style={{ width: '100px', margin: '20px auto' }}></div>
                <p style={{ textAlign: 'center', color: 'var(--text-secondary)' }}>Проверка сессии...</p>
            </div>
        );
    }

    if (authStatus === 'unauthorized') {
        return <Navigate to="/login" replace />;
    }

    return children;
};

export default ProtectedRoute;