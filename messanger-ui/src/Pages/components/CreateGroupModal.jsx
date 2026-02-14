import React, { useState, useEffect, useRef } from 'react';
import { apiFetch } from '../utils/apiClient';
import { UserAvatar } from './UserAvatar';

const CreateGroupModal = ({ isOpen, onClose, onGroupCreated }) => {
    const [groupName, setGroupName] = useState('');
    const [groupDesc, setGroupDesc] = useState('');
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [selectedUsers, setSelectedUsers] = useState(new Map()); // Map<id, userObject>
    const [isSearching, setIsSearching] = useState(false);
    const [isCreating, setIsCreating] = useState(false);

    const nameInputRef = useRef(null);

    // Сброс формы при открытии/закрытии
    useEffect(() => {
        if (isOpen) {
            setGroupName('');
            setGroupDesc('');
            setSearchQuery('');
            setSearchResults([]);
            setSelectedUsers(new Map());
            setTimeout(() => nameInputRef.current?.focus(), 100);
        }
    }, [isOpen]);

    const handleSearch = async (e) => {
        e.preventDefault();
        const query = searchQuery.trim();
        if (!query) return;

        setIsSearching(true);
        try {
            const data = await apiFetch(`/api/search/users/by-username/${query}`);
            setSearchResults(data || []);
        } catch (error) {
            console.error("Ошибка поиска участников:", error);
        } finally {
            setIsSearching(false);
        }
    };

    const toggleUser = (user) => {
        const newSelected = new Map(selectedUsers);
        if (newSelected.has(user.id)) {
            newSelected.delete(user.id);
        } else {
            newSelected.set(user.id, user);
        }
        setSelectedUsers(newSelected);
    };

    const handleCreate = async () => {
        if (!groupName.trim()) {
            alert("Введите название группы");
            return;
        }

        setIsCreating(true);
        const payload = {
            name: groupName.trim(),
            description: groupDesc.trim(),
            membersIds: Array.from(selectedUsers.keys())
        };

        try {
            const newChat = await apiFetch('/api/chats/groups', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            onGroupCreated(newChat); // Вызываем коллбэк для обновления списка в ChatPage
            onClose();
        } catch (error) {
            console.error("Ошибка создания группы:", error);
            alert("Не удалось создать группу");
        } finally {
            setIsCreating(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="modal" onClick={(e) => e.target.className === 'modal' && onClose()}>
            <div className="modal-content my-profile-modal-content">
                <div className="modal-header">
                    <h2>Создание группы</h2>
                    <button className="header-icon-btn" onClick={onClose}>
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2.5">
                            <line x1="18" y1="6" x2="6" y2="18"></line>
                            <line x1="6" y1="6" x2="18" y2="18"></line>
                        </svg>
                    </button>
                </div>

                <div className="modal-body user-profile-body">
                    {/* Поля ввода */}
                    <div className="profile-form-group">
                        <label>Название группы:</label>
                        <input
                            ref={nameInputRef}
                            type="text"
                            className="search-input"
                            value={groupName}
                            onChange={(e) => setGroupName(e.target.value)}
                            placeholder="Введите название..."
                        />
                    </div>

                    <div className="profile-form-group">
                        <label>Описание (опционально):</label>
                        <textarea
                            className="search-input"
                            value={groupDesc}
                            onChange={(e) => setGroupDesc(e.target.value)}
                            placeholder="О чем эта группа?"
                            rows="2"
                            style={{ resize: 'none' }}
                        />
                    </div>

                    {/* Выбранные участники */}
                    <div className="selected-members-section">
                        <label>Участники ({selectedUsers.size}):</label>
                        <div className="selected-members-list">
                            {selectedUsers.size === 0 ? (
                                <span className="placeholder-text">Никто не выбран</span>
                            ) : (
                                Array.from(selectedUsers.values()).map(user => (
                                    <div key={user.id} className="selected-member-chip">
                                        <span>{user.name}</span>
                                        <button className="remove-member-btn" onClick={() => toggleUser(user)}>&times;</button>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>

                    {/* Поиск */}
                    <div className="search-section" style={{ marginTop: '15px' }}>
                        <label>Добавить участников:</label>
                        <form className="search-form" onSubmit={handleSearch}>
                            <input
                                type="text"
                                className="search-input"
                                placeholder="Поиск по username"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                            />
                            <button type="submit" className="search-button" disabled={isSearching}>Найти</button>
                        </form>

                        <div className="user-list-container" style={{ maxHeight: '200px', overflowY: 'auto' }}>
                            {searchResults.map(user => (
                                <div key={user.id} className="user-item">
                                    <UserAvatar id={user.id} />
                                    <div className="user-item-info">
                                        <div className="user-item-name">{user.name} {user.surname}</div>
                                        <div className="user-item-username">@{user.username}</div>
                                    </div>
                                    <button
                                        className={`add-member-btn ${selectedUsers.has(user.id) ? 'added' : ''}`}
                                        onClick={() => toggleUser(user)}
                                        disabled={selectedUsers.has(user.id)}
                                    >
                                        {selectedUsers.has(user.id) ? 'Добавлен' : 'Добавить'}
                                    </button>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                <div className="modal-footer">
                    <button
                        className="profile-save-btn"
                        onClick={handleCreate}
                        disabled={isCreating || !groupName.trim()}
                    >
                        {isCreating ? 'Создание...' : 'Создать группу'}
                    </button>
                </div>
            </div>
        </div>
    );
};



export default CreateGroupModal;