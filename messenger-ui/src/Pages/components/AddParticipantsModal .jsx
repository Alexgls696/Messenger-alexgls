import React, { useState, useEffect, useRef } from 'react';
import { apiFetch } from '../utils/apiClient';
import { imageLoader } from '../utils/imageLoader';
import profileDefaultImage from '../images/profile-default.png'


const AddParticipantsModal = ({ isOpen, onClose, chatId, onParticipantsAdded, participants }) => {
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [selectedUsers, setSelectedUsers] = useState(new Map());
    const [isSearching, setIsSearching] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const searchInputRef = useRef(null);

    useEffect(() => {
        if (isOpen) {
            setSearchQuery('');
            setSearchResults([]);
            setSelectedUsers(new Map());
            setTimeout(() => searchInputRef.current?.focus(), 100);
        }
    }, [isOpen]);

    useEffect(() => {
        if (!isOpen) return;

        const handleKeydown = (event) => {
            if (event.key === 'Escape') {
                onClose();
            }
        };

        document.addEventListener('keydown', handleKeydown);

        return () => {
            document.removeEventListener('keydown', handleKeydown);
        };
    }, [isOpen, onClose]);

    const handleSearch = async (e) => {
        e.preventDefault();
        const query = searchQuery.trim();
        if (!query) return;

        setIsSearching(true);
        try {
            const data = await apiFetch(`/api/search/users/by-key/${query}`);
            const filtered = data.filter((user) => !participants.some(p => p.id === user.id));
            setSearchResults(filtered || []);
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

    const handleAddParticipants = async () => {
        if (selectedUsers.size === 0) return;

        setIsSubmitting(true);
        const payload = {
            chatId: parseInt(chatId),
            participantsIds: Array.from(selectedUsers.keys())
        };

        try {
            await apiFetch('/api/chats/groups/add-participants', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            // Уведомляем родителя, чтобы он обновил список участников
            if (onParticipantsAdded) {
                onParticipantsAdded();
            }
            onClose();
        } catch (error) {
            console.error("Ошибка добавления участников:", error);
            alert("Не удалось добавить участников");
        } finally {
            setIsSubmitting(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="modal" style={{ zIndex: 2200 }} onClick={(e) => e.target.className === 'modal' && onClose()}>
            <div className="modal-content my-profile-modal-content">
                <div className="modal-header">
                    <h2>Добавить участников</h2>
                    <button className="header-icon-btn" onClick={onClose}>
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2.5">
                            <line x1="18" y1="6" x2="6" y2="18"></line>
                            <line x1="6" y1="6" x2="18" y2="18"></line>
                        </svg>
                    </button>
                </div>

                <div className="modal-body user-profile-body">
                    {/* Список выбранных */}
                    <div className="selected-members-section">
                        <label>Выбрано ({selectedUsers.size}):</label>
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
                        <form className="search-form" onSubmit={handleSearch}>
                            <input
                                ref={searchInputRef}
                                type="text"
                                className="search-input"
                                placeholder="Поиск по username"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                            />
                            <button type="submit" className="search-button" disabled={isSearching}>Найти</button>
                        </form>

                        <div className="user-list-container" style={{ maxHeight: '250px', overflowY: 'auto' }}>
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
                                    >
                                        {selectedUsers.has(user.id) ? 'Удалить' : 'Выбрать'}
                                    </button>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                <div className="modal-footer">
                    <button
                        className="profile-save-btn"
                        onClick={handleAddParticipants}
                        disabled={isSubmitting || selectedUsers.size === 0}
                    >
                        {isSubmitting ? 'Добавление...' : 'Добавить выбранных'}
                    </button>
                </div>
            </div>
        </div>
    );
};

// Вспомогательный компонент аватара (как в вашем коде)
const UserAvatar = ({ id }) => {
    const [src, setSrc] = useState(profileDefaultImage);
    useEffect(() => {
        apiFetch(`/api/profiles/images/user-avatar/${id}`)
            .then(avatarId => {
                if (avatarId) imageLoader.getImageSrc(avatarId).then(setSrc);
            }).catch(() => { });
    }, [id]);
    return <img className="user-item-avatar" src={src} alt="" />;
};

export default AddParticipantsModal;