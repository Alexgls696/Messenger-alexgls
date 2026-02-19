import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';

function Register() {
    const navigate = useNavigate();

    // --- Состояние ---
    const [step, setStep] = useState(1); // 1 - ввод данных, 2 - ввод кода
    const [method, setMethod] = useState('email'); // 'email' или 'phone'
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    
    // Данные формы
    const [username, setUsername] = useState('');
    const [contactInfo, setContactInfo] = useState('');
    const [verificationCode, setVerificationCode] = useState('');
    const [operationId, setOperationId] = useState(null);

    // --- Константы API ---
    const GATEWAY_URL = `https://${window.location.hostname}:8080`;
    const INITIATE_URL = `${GATEWAY_URL}/api/verification/create`;
    const REGISTER_URL = `${GATEWAY_URL}/api/authentication/register`;

    // --- Шаг 1: Получение кода ---
    const handleInitiate = async (e) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        const requestBody = {
            username: username,
            email: method === 'email' ? contactInfo : null,
            phoneNumber: method === 'phone' ? contactInfo : null
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
                setStep(2); // Переходим к вводу кода
            } else {
                setError(data.message || 'Ошибка при отправке кода');
            }
        } catch (err) {
            setError('Не удалось подключиться к серверу.');
        } finally {
            setIsLoading(false);
        }
    };

    // --- Шаг 2: Верификация и регистрация ---
    const handleVerify = async (e) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        const requestBody = {
            id: operationId,
            code: verificationCode
        };

        try {
            const response = await fetch(REGISTER_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requestBody),
            });

            const data = await response.json();

            if (response.ok) {
                if (data.accessToken && data.refreshToken) {
                    localStorage.setItem('accessToken', data.accessToken);
                    localStorage.setItem('refreshToken', data.refreshToken);
                    navigate('/setup-profile');
                } else {
                    setError('Сервер не вернул токены.');
                }
            } else {
                setError(data.message || 'Неверный код или срок действия истек.');
            }
        } catch (err) {
            setError('Ошибка при регистрации.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="auth-container">
            {step === 1 ? (
                /* ШАГ 1: ФОРМА ИНИЦИАЦИИ */
                <form className="auth-form" onSubmit={handleInitiate}>
                    <h2>Регистрация</h2>
                    <p className="form-description">Введите данные и выберите способ получения кода.</p>

                    <div className="tab-switcher">
                        <button 
                            type="button" 
                            className={`tab-button ${method === 'email' ? 'active' : ''}`}
                            onClick={() => { setMethod('email'); setError(''); }}
                        >Email</button>
                        <button 
                            type="button" 
                            className={`tab-button ${method === 'phone' ? 'active' : ''}`}
                            onClick={() => { setMethod('phone'); setError(''); }}
                        >Телефон</button>
                    </div>

                    <div className="input-group">
                        <label>Имя пользователя</label>
                        <input 
                            type="text" 
                            value={username} 
                            onChange={(e) => setUsername(e.target.value)} 
                            required 
                        />
                    </div>

                    <div className="input-group">
                        <label>{method === 'email' ? 'Email' : 'Номер телефона'}</label>
                        <input 
                            type={method === 'email' ? 'email' : 'tel'} 
                            placeholder={method === 'email' ? 'your@email.com' : '+7 (999) 999-99-99'}
                            value={contactInfo} 
                            onChange={(e) => setContactInfo(e.target.value)} 
                            required 
                        />
                    </div>

                    {error && <div className="error-message">{error}</div>}

                    <button type="submit" disabled={isLoading}>
                        {isLoading ? 'Отправка...' : 'Получить код'}
                    </button>

                    <div className="form-link">
                        Уже есть аккаунт? <Link to="/login">Войти</Link>
                    </div>
                </form>
            ) : (
                /* ШАГ 2: ФОРМА ПОДТВЕРЖДЕНИЯ */
                <form className="auth-form" onSubmit={handleVerify}>
                    <h2>Подтверждение</h2>
                    <p className="form-description">
                        Код отправлен на ваш {method === 'email' ? 'Email' : 'телефон'}.
                    </p>

                    <div className="input-group">
                        <label>Код доступа</label>
                        <div className="code-input-container">
                            <div className="code-boxes">
                                {[...Array(6)].map((_, i) => (
                                    <span key={i} className="code-box">
                                        {verificationCode[i] || ''}
                                    </span>
                                ))}
                            </div>
                            <input 
                                type="text" 
                                className="hidden-code-input"
                                value={verificationCode}
                                onChange={(e) => setVerificationCode(e.target.value.slice(0, 6))}
                                maxLength="6"
                                autoFocus
                                required 
                            />
                        </div>
                    </div>

                    {error && <div className="error-message">{error}</div>}

                    <button type="submit" disabled={isLoading}>
                        {isLoading ? 'Проверка...' : 'Войти'}
                    </button>
                    
                    <button 
                        type="button" 
                        className="back-link-btn" 
                        onClick={() => { setStep(1); setVerificationCode(''); }}
                    >
                        Изменить данные
                    </button>
                </form>
            )}
        </div>
    );
}

export default Register;