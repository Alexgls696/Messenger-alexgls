import React, { useState, useEffect } from 'react';
import { apiFetch } from '../utils/apiClient';
import { imageLoader } from '../utils/imageLoader';
import ConfirmationModal from './ConfirmationModal';
import defaultAvatar from '../images/profile-default.png';
import { toast } from 'react-toastify';

// Вспомогательный компонент для строки пользователя (для изоляции загрузки аватарок)
const BlockedUserItem = ({ user, onUnblock, onSelect }) => {
    const [avatar, setAvatar] = useState(defaultAvatar);

    useEffect(() => {
        let isMounted = true;
        const fetchAvatar = async () => {
            try {
                const avatarId = await apiFetch(`/api/profiles/images/user-avatar/${user.id}`);
                if (avatarId && avatarId > 0 && isMounted) {
                    const url = await imageLoader.getImageSrc(avatarId);
                    if (isMounted) setAvatar(url);
                }
            } catch (error) {
                console.warn(`Ошибка загрузки аватара для пользователя ${user.id}:`, error);
            }
        };

        fetchAvatar();
        return () => { isMounted = false; };
    }, [user.id]);

    return (
        <div className="user-item" onClick={() => onSelect(user)} style={{ cursor: 'pointer' }}>
            <img 
                src={avatar} 
                alt="" 
                style={{ 
                    width: '40px', 
                    height: '40px', 
                    borderRadius: '50%', 
                    marginRight: '12px', 
                    objectFit: 'cover' 
                }} 
            />
            <div className="user-item-info">
                <div className="user-item-name">{user.name} {user.surname || ''}</div>
                <div className="user-item-username">@{user.username}</div>
            </div>
            <button
                className="profile-cancel-btn danger"
                style={{ marginLeft: 'auto', padding: '6px 12px', fontSize: '13px' }}
                onClick={(e) => {
                    e.stopPropagation(); // Предотвращаем переход к чату при нажатии разблокировки
                    onUnblock(user);
                }}
            >
                Разблокировать
            </button>
        </div>
    );
};

const BlackListModal = ({ isOpen, onClose, currentUserId, onUserSelect }) => {
    const [blockedUsers, setBlockedUsers] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [userToUnblock, setUserToUnblock] = useState(null);

    useEffect(() => {
        if (isOpen) {
            fetchBlockedUsers();
        }
    }, [isOpen]);

    const fetchBlockedUsers = async () => {
        setIsLoading(true);
        try {
            const data = await apiFetch('/api/users/black-list');
            setBlockedUsers(data || []);
        } catch (error) {
            console.error("Не удалось загрузить черный список:", error);
            toast.error("Не удалось загрузить черный список");
        } finally {
            setIsLoading(false);
        }
    };

     const handleUnblockConfirm = async () => {
        if (!userToUnblock) return;
        try {
            await apiFetch(`/api/users/black-list/black-list/delete?targetUserId=${userToUnblock.id}`, {
                method: 'POST'
            });

            setBlockedUsers(prev => prev.filter(u => u.id !== userToUnblock.id));
            toast.success(`Пользователь ${userToUnblock.name} успешно разблокирован`);
        } catch (error) {
            console.error("Ошибка при удалении из черного списка:", error);
            toast.error("Не удалось разблокировать пользователя");
        } finally {
            setUserToUnblock(null);
        }
    };

    if (!isOpen) return null;

    return (
        <>
            <div className="modal" onClick={(e) => e.target.className === 'modal' && onClose()}>
                <div className="modal-content my-profile-modal-content">
                    <div className="modal-header">
                        <h2>Черный список</h2>
                        <button className="header-icon-btn" onClick={onClose}>
                            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2.5">
                                <line x1="18" y1="6" x2="6" y2="18"></line>
                                <line x1="6" y1="6" x2="18" y2="18"></line>
                            </svg>
                        </button>
                    </div>

                    <div className="modal-body user-profile-body">
                        {isLoading ? (
                            <div style={{ textAlign: 'center', padding: '20px' }}>Загрузка...</div>
                        ) : blockedUsers.length === 0 ? (
                            <div style={{ textAlign: 'center', opacity: 0.6, padding: '20px' }}>
                                Черный список пуст
                            </div>
                        ) : (
                            <div className="user-list-container" style={{ maxHeight: '300px', overflowY: 'auto' }}>
                                {blockedUsers.map(user => (
                                    <BlockedUserItem
                                        key={user.id}
                                        user={user}
                                        onSelect={onUserSelect}
                                        onUnblock={setUserToUnblock}
                                    />
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Модальное окно подтверждения разблокировки */}
            <ConfirmationModal
                isOpen={!!userToUnblock}
                title="Разблокировать пользователя?"
                message={`Вы действительно хотите убрать ${userToUnblock?.name} ${userToUnblock?.surname || ''} из черного списка?`}
                onConfirm={handleUnblockConfirm}
                onCancel={() => setUserToUnblock(null)}
            />
        </>
    );
};

export default BlackListModal;