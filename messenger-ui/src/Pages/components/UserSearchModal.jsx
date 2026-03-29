import React, { useState, useEffect, useRef } from 'react';
import { apiFetch } from '../utils/apiClient';
import { imageLoader } from '../utils/imageLoader';

import userProfileDefault from '../images/profile-default.png'


let cachedUsersFromChats = null;

const UserSearchModal = ({ currentUserId,
    isOpen,
    onClose,
    onUserSelect,
    forwardMode = false,
    forwardMessagesCount = 0 }) => {
    const [username, setUsername] = useState('');
    const [results, setResults] = useState([]);
    const [isSearching, setIsSearching] = useState(false);
    const [isShowingHistory, setIsShowingHistory] = useState(true);

    const inputRef = useRef(null);

    // Логика инициализации при открытии
    useEffect(() => {
        if (isOpen) {
            setUsername('');
            setIsSearching(false);
            setIsShowingHistory(true);

            // Если данные уже есть в кеше — сразу их показываем
            if (cachedUsersFromChats) {
                setResults(cachedUsersFromChats);
            } else {
                fetchDefaultUsers();
            }

            setTimeout(() => inputRef.current?.focus(), 100);
        }
    }, [isOpen]);

    useEffect(() => {
        if (!isOpen) return;
        const handleKeydown = (event) => {
            if (event.key === 'Escape') {
                onClose()
            }
        }

        document.addEventListener('keydown', handleKeydown)

        return () => {
            document.removeEventListener('keydown', handleKeydown);
        }
    }, [isOpen, onClose])

    const fetchDefaultUsers = async () => {
        try {
            const data = await apiFetch(`/api/search/users/from-chats`);
            cachedUsersFromChats = data;
            setResults(data);
        } catch (error) {
            console.error("Ошибка загрузки списка чатов:", error);
            setResults([]);
        }
    };

    const handleSearch = async (e) => {
        e.preventDefault();
        const query = username.trim();
        if (!query) return;

        setIsSearching(true);
        setIsShowingHistory(false);
        setResults([]); // Очищаем список только перед новым поиском

        try {
            const data = await apiFetch(`/api/search/users/by-username/${query}`);
            const users = data.filter(user => user.id !== currentUserId);
            setResults(users || []);
        } catch (error) {
            console.error("Ошибка поиска:", error);
            setResults([]);
        } finally {
            setIsSearching(false);
        }
    };

    const handleInputChange = (e) => {
        const value = e.target.value;
        setUsername(value);

        // Если пользователь стер текст — возвращаем список из чатов
        if (value.trim().length === 0) {
            setIsShowingHistory(true);
            setResults(cachedUsersFromChats || []);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="modal" onClick={(e) => e.target.className === 'modal' && onClose()}>
            <div className="modal-content search-modal-content">
                <div className="modal-header">
                    <h2 id="searchModalTitle">
                        {forwardMode
                            ? `Переслать ${forwardMessagesCount} сообщений...`
                            : 'Поиск пользователей'}
                    </h2>
                    <button className="header-icon-btn" onClick={onClose}>
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2.5">
                            <line x1="18" y1="6" x2="6" y2="18"></line>
                            <line x1="6" y1="6" x2="18" y2="18"></line>
                        </svg>
                    </button>
                </div>
                <div className="search-content">
                    <form className="search-form" onSubmit={handleSearch}>
                        <input
                            ref={inputRef}
                            type="text"
                            className="search-input"
                            placeholder="Введите username (без @)"
                            value={username}
                            onChange={handleInputChange}
                            required
                        />
                        <button type="submit" className="search-button" disabled={isSearching}>
                            {isSearching ? '...' : 'Найти'}
                        </button>
                    </form>

                    <div className="user-list-container">
                        {isShowingHistory && results.length > 0 && (
                            <p style={{ margin: '0 0 10px 10px', fontSize: '0.9em', opacity: 0.7 }}>
                                Пользователи, с которыми вы общались
                            </p>
                        )}

                        {results.length > 0 ? (
                            results.map(user => (
                                <UserSearchItem
                                    key={user.id}
                                    user={user}
                                    onClick={() => {
                                        onUserSelect(user);
                                        onClose();
                                    }}
                                />
                            ))
                        ) : (
                            !isSearching && (
                                <p className="placeholder">
                                    {isShowingHistory ? "Список чатов пуст" : "Ничего не найдено"}
                                </p>
                            )
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}

const UserSearchItem = ({ user, onClick }) => {
    const [avatar, setAvatar] = useState(userProfileDefault);

    useEffect(() => {
        let isMounted = true;

        // Логика загрузки аватара
        apiFetch(`/api/profiles/images/user-avatar/${user.id}`)
            .then(avatarId => {
                if (avatarId && typeof avatarId === 'number' && isMounted) {
                    imageLoader.getImageSrc(avatarId).then(src => {
                        if (isMounted) setAvatar(src);
                    });
                }
            })
            .catch(() => { });

        return () => { isMounted = false; };
    }, [user.id]);

    return (
        <div className="user-item" onClick={onClick}>
            <img className="user-item-avatar" src={avatar} alt="" />
            <div className="user-item-info">
                <div className="user-item-name">{user.name} {user.surname || ''}</div>
                <div className="user-item-username">@{user.username}</div>
            </div>
        </div>
    );
};

export default UserSearchModal;