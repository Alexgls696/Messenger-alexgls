import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch } from './utils/apiClient';
import {GATEWAY_URL} from './utils/apiClient'

function SetupProfile() {

    const navigate = useNavigate();

    const SET_PASSWORD_URL = `${GATEWAY_URL}/api/users/update-password`
    const API_CREATE_PROFILE_URL = `${GATEWAY_URL}/api/profiles/create`

    const [name, setName] = useState('');
    const [surname, setSurname] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');

    const [username, setUsername] = useState('');
    const [accessToken, setAccessToken] = useState(null);

    const [error, setError] = useState('');
    const [isLoading, setLoading] = useState(false);
    const [stage, setStage] = useState(1);

    const parseJwt = (token) => {
        try {
            return JSON.parse(atob(token.split('.')[1]));
        } catch {
            return null;
        }
    };

    useEffect(() => {
        const token = localStorage.getItem('accessToken');

        if (!token) {
            navigate('/login');
            return;
        }

        const decoded = parseJwt(token);

        if (!decoded || !decoded.sub) {
            localStorage.clear();
            navigate('/login');
            return;
        }

        setAccessToken(token);
        setUsername(decoded.sub);

    }, [navigate]);

    // --------------------------
    // Stage 1
    // --------------------------

    const handleSubmitFirstStage = async (event) => {
        event.preventDefault();
        setLoading(true);
        setError('');

        try {
            const response = await apiFetch('/api/users/update', {
                method: 'POST',
                body: JSON.stringify({
                    name,
                    surname: surname || '',
                    username
                })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.detail || errorData.error || 'Ошибка обновления');
            }

            setStage(2);

        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    // --------------------------
    // Stage 2
    // --------------------------

    const handleSubmitSecondStage = async (event) => {
        event.preventDefault();
        setLoading(true);
        setError('');

        if (password.length < 8) {
            setError('Пароль должен быть не менее 8 символов.');
            setLoading(false);
            return;
        }

        if (password !== confirmPassword) {
            setError('Пароли не совпадают.');
            setLoading(false);
            return;
        }

        try {
            const response = await fetch(SET_PASSWORD_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'text/plain',
                    'Authorization': `Bearer ${accessToken}`
                },
                body: password,
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.detail || errorData.error || 'Ошибка установки пароля');
            }

            await createUserProfile();

        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    async function createUserProfile() {
        const response = await fetch(API_CREATE_PROFILE_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`
            }
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.detail || errorData.error || 'Ошибка создания профиля');
        }

        navigate('/');
    }

    // --------------------------

    return (
        <div className='auth-container'>

            {stage === 1 && (
                <div id='profile-step'>
                    <form className="auth-form" onSubmit={handleSubmitFirstStage}>
                        <h2>Завершение регистрации</h2>

                        <div className="input-group">
                            <label htmlFor="name">Имя</label>
                            <input type="text" id="name" required
                                onChange={(e) => setName(e.target.value)} />
                        </div>

                        <div className="input-group">
                            <label htmlFor="surname">Фамилия</label>
                            <input type="text" id="surname"
                                onChange={(e) => setSurname(e.target.value)} />
                        </div>

                        <div className="input-group">
                            <label htmlFor="username">Имя пользователя</label>
                            <input type="text" id="username"
                                value={username} readOnly />
                        </div>

                        {error && <div className="error-message">{error}</div>}

                        <button type="submit" disabled={isLoading}>
                            {isLoading ? 'Подождите...' : 'Сохранить и продолжить'}
                        </button>
                    </form>
                </div>
            )}

            {stage === 2 && (
                <div id='profile-step'>
                    <form className="auth-form" onSubmit={handleSubmitSecondStage}>
                        <h2>Установите пароль</h2>

                        <div className="input-group">
                            <label htmlFor="password">Новый пароль</label>
                            <input type="password" required
                                onChange={(e) => setPassword(e.target.value)} />
                        </div>

                        <div className="input-group">
                            <label htmlFor="confirm-password">Подтвердите пароль</label>
                            <input type="password" required
                                onChange={(e) => setConfirmPassword(e.target.value)} />
                        </div>

                        {error && <div className="error-message">{error}</div>}

                        <button type="submit" disabled={isLoading}>
                            {isLoading ? 'Подождите...' : 'Завершить регистрацию'}
                        </button>
                    </form>
                </div>
            )}
        </div>
    );
}

export default SetupProfile;