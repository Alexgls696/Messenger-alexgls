import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import './Styles/auth.css'; // Используем те же стили, так как структура идентична

function SimpleLogin() {
    const navigate = useNavigate();

    // --- Состояние ---
    const [step, setStep] = useState(1); // 1 - ввод email, 2 - ввод кода
    const [email, setEmail] = useState('');
    const [verificationCode, setVerificationCode] = useState('');
    const [operationId, setOperationId] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');

    // --- Константы API ---
    const GATEWAY_URL = `http://${window.location.hostname}:8080`;
    const INITIATE_URL = `${GATEWAY_URL}/api/verification/create-for-exists`;
    const LOGIN_URL = `${GATEWAY_URL}/api/authentication/login-by-email`;

    // --- Шаг 1: Запрос кода ---
    const handleGetCode = async (e) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        const requestBody = {
            username: null,
            email: email,
            phoneNumber: null
        };

        try {
            const response = await fetch(INITIATE_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requestBody),
            });

            const data = await response.json();

            if (response.ok && data.id) {
                setOperationId(data.id);
                setStep(2);
            } else {
                setError(data.message || 'Произошла ошибка на сервере');
            }
        } catch (err) {
            setError('Не удалось подключиться к серверу.');
        } finally {
            setIsLoading(false);
        }
    };

    // --- Шаг 2: Проверка кода и вход ---
    const handleVerify = async (e) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        const requestBody = {
            id: operationId,
            code: verificationCode
        };

        try {
            const response = await fetch(LOGIN_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requestBody),
            });

            const jwtResponse = await response.json();

            if (response.ok) {
                if (jwtResponse.accessToken && jwtResponse.refreshToken) {
                    localStorage.setItem('accessToken', jwtResponse.accessToken);
                    localStorage.setItem('refreshToken', jwtResponse.refreshToken);
                    navigate('/'); // Переход на главную
                } else {
                    setError('Ответ сервера не содержит токенов.');
                }
            } else {
                setError(jwtResponse.message || 'Неверный код или истек срок действия.');
            }
        } catch (err) {
            setError('Ошибка при проверке кода.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="auth-container">
            {step === 1 ? (
                /* ШАГ 1: ВВОД EMAIL */
                <form className="auth-form" onSubmit={handleGetCode}>
                    <h2>Вход в аккаунт</h2>
                    <p className="form-description">Введите ваш email, чтобы получить код для входа.</p>

                    <div className="input-group">
                        <label htmlFor="email">Email</label>
                        <input 
                            type="email" 
                            id="email"
                            placeholder="Ваш адрес электронной почты"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required 
                        />
                    </div>

                    {error && <div className="error-message">{error}</div>}

                    <button type="submit" disabled={isLoading}>
                        {isLoading ? 'Отправка...' : 'Получить код'}
                    </button>

                    <div className="form-link">
                        Другие способы входа: <br />
                        <Link to="/login">Войти с помощью пароля</Link>
                    </div>
                    <div className="form-link">
                        Нет аккаунта? <Link to="/register">Зарегистрироваться</Link>
                    </div>
                </form>
            ) : (
                /* ШАГ 2: ВВОД КОДА */
                <form className="auth-form" onSubmit={handleVerify}>
                    <h2>Подтверждение</h2>
                    <p className="form-description">
                        Мы отправили код на вашу почту <strong>{email}</strong>.
                    </p>

                    <div className="input-group">
                        <label>Код доступа</label>
                        <div className="code-input-container">
                            <div className="code-boxes">
                                {[...Array(6)].map((_, i) => (
                                    <span key={i} className={`code-box ${verificationCode[i] ? 'filled' : ''}`}>
                                        {verificationCode[i] || ''}
                                    </span>
                                ))}
                            </div>
                            <input 
                                type="text" 
                                className="hidden-code-input"
                                value={verificationCode}
                                onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                                maxLength="6"
                                inputMode="numeric"
                                autoFocus
                                required 
                            />
                        </div>
                    </div>

                    {error && <div className="error-message">{error}</div>}

                    <button type="submit" disabled={isLoading || verificationCode.length < 6}>
                        {isLoading ? 'Проверка...' : 'Войти'}
                    </button>
                    
                    <button 
                        type="button" 
                        className="back-link-btn" 
                        onClick={() => { setStep(1); setVerificationCode(''); }}
                    >
                        Изменить Email
                    </button>
                </form>
            )}
        </div>
    );
}

export default SimpleLogin;