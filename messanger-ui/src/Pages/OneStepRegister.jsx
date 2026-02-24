import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';


const API_CREATE_PROFILE_URL = 'https://localhost:8080/api/profiles/create';

function Register() {
    const navigate = useNavigate();

    // Состояния для полей формы (соответствуют UserRegisterDto)
    const [formData, setFormData] = useState({
        name: '',
        surname: '',
        username: '',
        password: '',
        email: ''
    });

    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const gatewayAddress = `${window.location.hostname}:8080`;
    const API_BASE_URL = `https://${gatewayAddress}`;

    useEffect(() => {
        const accessToken = localStorage.getItem('accessToken');
        if (accessToken) {
            navigate('/');
        }
    }, [navigate]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        setError('');
        setIsLoading(true);

        try {
            const response = await fetch(`${API_BASE_URL}/auth/register`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(formData),
            });

            if (response.ok) {
                const data = await response.json();

                localStorage.setItem('accessToken', data.accessToken);
                localStorage.setItem('refreshToken', data.refreshToken);

                navigate('/');
            } else if (response.status === 409) {
                setError('Пользователь с таким именем или email уже существует.');
            } else {
                const errorData = await response.json().catch(() => ({}));
                setError(errorData.message || 'Произошла ошибка при регистрации. Попробуйте позже.');
            }

            await createUserProfile(localStorage.getItem('accessToken'));
        } catch (err) {
            console.error('Ошибка сети:', err);
            setError('Не удалось подключиться к серверу.');
        } finally {
            setIsLoading(false);
        }
    };

    async function createUserProfile(accessToken) {
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

    return (
        <div className="auth-container">
            <form id="register-form" className="auth-form" onSubmit={handleSubmit}>
                <h2>Регистрация</h2>
                <p className="form-description">Создайте новый аккаунт в системе</p>

                <div className="input-group">
                    <label htmlFor="name">Имя</label>
                    <input
                        type="text"
                        id="name"
                        name="name"
                        value={formData.name}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div className="input-group">
                    <label htmlFor="surname">Фамилия</label>
                    <input
                        type="text"
                        id="surname"
                        name="surname"
                        value={formData.surname}
                        onChange={handleChange}
                    />
                </div>

                <div className="input-group">
                    <label htmlFor="username">Имя пользователя (логин)</label>
                    <input
                        type="text"
                        id="username"
                        name="username"
                        value={formData.username}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div className="input-group">
                    <label htmlFor="email">Электронная почта</label>
                    <input
                        type="email"
                        id="email"
                        name="email"
                        value={formData.email}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div className="input-group">
                    <label htmlFor="password">Пароль</label>
                    <input
                        type="password"
                        id="password"
                        name="password"
                        value={formData.password}
                        onChange={handleChange}
                        required
                    />
                </div>

                {error && <div className="error-message">{error}</div>}

                <button type="submit" disabled={isLoading}>
                    {isLoading ? 'Регистрация...' : 'Зарегистрироваться'}
                </button>

                <div className="form-link">
                    Уже есть аккаунт? <Link to="/login">Войти</Link>
                </div>
            </form>
        </div>
    );
}

export default Register;