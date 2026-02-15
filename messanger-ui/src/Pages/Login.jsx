import React, { useState } from 'react';
// ИСПРАВЛЕНИЕ: Добавлен импорт навигации и ссылок
import { useNavigate, Link } from 'react-router-dom'; 

function Login() {
    // ИСПРАВЛЕНИЕ: Инициализация хука навигации
    const navigate = useNavigate(); 

    // Состояния для полей формы
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    // Константы для API (логика как в вашем коде)
    const gatewayAddress = `${window.location.hostname}:8080`;
    const API_BASE_URL = `https://${gatewayAddress}`;

    const accessToken = localStorage.getItem('accessToken');
    if(accessToken){
        navigate('/')
    }
    const handleSubmit = async (event) => {
        event.preventDefault(); // Предотвращаем перезагрузку страницы
        
        setError(''); // Сбрасываем старую ошибку
        setIsLoading(true); // Включаем режим загрузки (кнопка заблокируется)

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
                
                navigate('/');
            } else if (response.status === 401) {
                setError('Неверное имя пользователя или пароль.');
            } else {
                setError('Произошла ошибка на сервере. Попробуйте позже.');
            }
        } catch (err) {
            console.error('Ошибка сети:', err);
            setError(`Не удалось подключиться к серверу аутентификации. ${API_BASE_URL}`);
        } finally {
            setIsLoading(false); // Выключаем режим загрузки
        }
    };

    return (
        <div className="auth-container">
            <form id="login-form" className="auth-form" onSubmit={handleSubmit}>
                <h2>Вход в систему</h2>
                <p className="form-description">Пожалуйста, войдите в свой аккаунт</p>

                <div className="input-group">
                    <label htmlFor="username">Имя пользователя</label>
                    <input 
                        type="text" 
                        id="username" 
                        value={username}
                        onChange={(e) => setUsername(e.target.value)} // Синхронизируем ввод со стейтом
                        required 
                    />
                </div>

                <div className="input-group">
                    <label htmlFor="password">Пароль</label>
                    <input 
                        type="password" 
                        id="password" 
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required 
                    />
                </div>

                {/* Условный рендеринг ошибки */}
                {error && <div className="error-message">{error}</div>}

                <button type="submit" disabled={isLoading}>
                    {isLoading ? 'Вход...' : 'Войти'}
                </button>

                <div className="form-link">
                    Другие способы входа: <br />
                    <Link to="/simple-login">Войти по коду</Link>
                </div>
                <div className="form-link">
                    Нет аккаунта? <Link to="/register">Зарегистрироваться</Link>
                </div>
            </form>
        </div>
    );
}

export default Login;