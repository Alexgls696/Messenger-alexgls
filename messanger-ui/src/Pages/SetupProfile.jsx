
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch } from './utils/apiClient';

function SetupProfile() {

    const navigate = useNavigate();
    // --- Адреса API ---
    const SET_PASSWORD_URL = 'https://localhost:8080/api/users/update-password';
    const API_CREATE_PROFILE_URL = 'https://localhost:8080/api/profiles/create';

    const [name, setName] = useState('');
    const [surname, setSurname] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setcConfirmPassword] = useState('');

    const [error, setError] = useState('');
    const [isLoading, setLoading] = useState(false);
    const [stage, setStage] = useState(1);

    const [passwordMessageBox, setPasswordMessageBoxContent] = useState('');

    const parseJwt = (token) => {
        try {
            return JSON.parse(atob(token.split('.')[1]));
        } catch (e) { return null; }
    };

    const accessToken = localStorage.getItem('accessToken');
    if (!accessToken) {
        navigate('/login');
    }

    const decodedToken = parseJwt(accessToken);
    if (!decodedToken || !decodedToken.userId || !decodedToken.sub) {
        localStorage.clear();
        alert('Ошибка авторизации. Не удалось получить данные пользователя из токена. Пожалуйста, войдите снова.');
        navigate('/login');
    }

    const [username, setUsername] = useState(decodedToken.sub);

    const requestBody = {
        name: name,
        surname: surname || '',
        username: username,
    };



    const handleSubmitFirstStage = async (event) => {
        event.preventDefault();
        setLoading(true);
        setError('');
        try {
            const response = await apiFetch('/api/users/update', {
                method: 'POST',
                body: JSON.stringify(requestBody)
            });
            if (response.ok) {
                setStage(2);
                setLoading(false);
            } else {
                const errorData = await response.json();
                let errorMessage = 'Произошла неизвестная ошибка.';
                if (errorData.detail || errorData.error) {
                    errorMessage = `${errorData.detail || ''} ${errorData.error || ''}`.trim();
                }
                setError(errorMessage);
            }
        } catch (error) {
            console.warn(error);
            setError('Не удалось обновить данные пользователя.');
        } finally {
            //setFirstStepButtonText('Сохранить и продолжить');
        }
    }

    const handleSubmitSecondStage = async (event) => {
        event.preventDefault();
        setLoading(true);
        setError('');

        if (password.length < 8) {
            setError('Пароль должен быть не менее 8 символов.');
            return;
        }
        if (password !== confirmPassword) {
            setError('Пароли не совпадают.');
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

            if (response.ok) {
                await createUserProfile();
            } else {
                const errorData = await response.json();
                let errorMessage = 'Произошла неизвестная ошибка.';
                if (errorData.detail || errorData.error) {
                    errorMessage = `${errorData.detail || ''} ${errorData.error || ''}`.trim();
                }
                setError(errorMessage);
            }
        } catch (error) {
            setError(error.message);
        } finally {

        }

    }

    async function createUserProfile() {
        const response = await fetch(API_CREATE_PROFILE_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`
            }
        });
        if (response.ok) {
            navigate('/')
        } else {
            const errorData = await response.json();
            let errorMessage = 'Произошла неизвестная ошибка.';
            if (errorData.detail || errorData.error) {
                errorMessage = `${errorData.detail || ''} ${errorData.error || ''}`.trim();
            }
            throw new Error(errorMessage);
        }
    }

    return (
        <>
            <div className='auth-container'>
                {stage == 1 && <div id='profile-step'>
                    <form id="profile-form" className="auth-form" onSubmit={handleSubmitFirstStage}>
                        <h2>Завершение регистрации</h2>
                        <p className="form-description">Остался последний шаг! Пожалуйста, укажите ваши данные.</p>

                        <div className="input-group">
                            <label htmlFor="name">Имя</label>
                            <input type="text" id="name" name="name" onChange={(event) => { setName(event.target.value) }} required></input>
                        </div>
                        <div className="input-group">
                            <label htmlFor="surname">Фамилия (необязательно)</label>
                            <input type="text" id="surname" name="surname" onChange={(event) => setSurname(event.target.value)} ></input>
                        </div>
                        <div className="input-group">
                            <label htmlFor="username">Имя пользователя</label>
                            <input type="text" id="username" name="username" value={username} required readOnly></input>
                        </div>

                        <div id="profile-message-box" className="message hidden"></div>
                        <button type="submit" id="save-profile-button" disabled={isLoading}>  {!isLoading ? 'Сохранить и продолжить' : 'Подождите...'} </button>
                    </form>
                </div>}

                {stage == 2 && <div id='profile-step'>
                    <form id="password-form" class="auth-form" onSubmit={handleSubmitSecondStage}>
                        <h2>Установите пароль</h2>
                        <p className="form-description">Пароль должен содержать не менее 8 символов.</p>

                        <div className="input-group">
                            <label for="password">Новый пароль</label>
                            <input type="password" id="password" name="password" required onChange={(event) => setPassword(event.target.value)}></input>
                        </div>
                        <div className="input-group">
                            <label for="confirm-password">Подтвердите пароль</label>
                            <input type="password" id="confirm-password" name="confirm-password" required onChange={(event) => setcConfirmPassword(event.target.value)}></input>
                        </div>
                        {error && <div id="password-message-box" className="error-message">{error}</div>}
                        <button type="submit" id="set-password-button" disabled={isLoading}>{!isLoading ? 'Завершить регистрацию' : 'Подождите...'}</button>
                    </form>
                </div>}
            </div>
        </>
    );
}

export default SetupProfile;