import React, { useState, useEffect, useRef } from 'react';
import { apiFetch } from '../utils/apiClient';
import { imageLoader } from '../utils/imageLoader';
import AttachmentItem from './AttachmentItem';
import AddParticipantsModal from './AddParticipantsModal ';

import defaultGroupImage from '../images/group-default.png'
import defaultProfileImage from '../images/profile-default.png'

const GroupProfileModal = ({ isOpen, onClose, chatId, chatName, currentUserId, imageObserver, onOpenUserProfile, onOpenConfirm, participantCache}) => {
    const [participants, setParticipants] = useState([]);
    const [groupDetails, setGroupDetails] = useState(null);
    const [canRemove, setCanRemove] = useState(false);
    const [canEdit, setCanEdit] = useState(false);
    const [activeTab, setActiveTab] = useState('IMAGE');
    const [attachments, setAttachments] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isAttachmentsLoading, setIsAttachmentsLoading] = useState(false);

    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [editName, setEditName] = useState('');
    const [editDescription, setEditDescription] = useState('');
    const [isSaving, setIsSaving] = useState(false);
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);

    const [tooltip, setTooltip] = useState({ visible: false, x: 0, y: 0, content: '' });
    const metadataCache = useRef(new Map());



    // 1. Загрузка данных группы
    useEffect(() => {
        if (isOpen && chatId) {
            const fetchGroupData = async () => {
                setIsLoading(true);
                try {
                    const [parts, details, access] = await Promise.all([
                        apiFetch(`/api/chats/${chatId}/participants`),
                        apiFetch(`/api/chats/${chatId}`),
                        apiFetch(`/api/chats/groups/${chatId}/access`)
                            .catch(() => ({ role: 'MEMBER' }))
                    ]);

                    console.log(parts.participants)
                    setParticipants(parts.participants);
                    setGroupDetails(details);

                    const hasAccess = access === true ||
                        access?.canRemoveMembers === true ||
                        ['ADMIN', 'OWNER'].includes(access?.role);

                    const canEditRequest = access?.canEdit === true;
                    setCanEdit(canEditRequest);
                    setCanRemove(hasAccess);
                } catch (error) {
                    console.error("Ошибка загрузки группы:", error);
                } finally {
                    setIsLoading(false);
                }
            };
            fetchGroupData();
        }
    }, [isOpen, chatId]);

    // 2. Загрузка вложений
    useEffect(() => {
        if (isOpen && chatId && activeTab) {
            const fetchAttachments = async () => {
                setIsAttachmentsLoading(true);
                try {
                    const data = await apiFetch(`/api/attachments/find-by-type-and-chat-id?mediaType=${activeTab}&chatId=${chatId}`);
                    setAttachments(data || []);
                } catch (error) {
                    console.error("Ошибка загрузки вложений:", error);
                } finally {
                    setIsAttachmentsLoading(false);
                }
            };
            fetchAttachments();
        }
    }, [isOpen, chatId, activeTab]);

    // --- ЛОГИКА ОБНОВЛЕНИЯ ГРУППЫ ---
    const handleOpenEditModal = () => {
        setEditName(groupDetails?.name || '');
        setEditDescription(groupDetails?.description || '');
        setIsEditModalOpen(true);
    };

    const handleUpdateGroup = async (e) => {
        e.preventDefault();
        if (!editName.trim()) return;

        setIsSaving(true);
        try {
            const payload = {
                chatId: parseInt(chatId),
                name: editName.trim(),
                description: editDescription.trim()
            };

            await apiFetch('/api/chats/groups/update', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            // Обновляем локальное состояние, чтобы изменения были видны сразу
            setGroupDetails(prev => ({
                ...prev,
                name: editName.trim(),
                description: editDescription.trim()
            }));

            setIsEditModalOpen(false);
        } catch (error) {
            console.error("Ошибка при обновлении группы:", error);
            alert("Не удалось обновить данные группы");
        } finally {
            setIsSaving(false);
        }
    };

    const handleRemoveMember = (user) => {
        onOpenConfirm(
            `Вы действительно хотите удалить пользователя ${user.name} из группы?`,
            async () => {
                await apiFetch(`/api/chats/${chatId}/participants/${user.id}`, { method: 'DELETE' });
                setParticipants(prev => prev.filter(p => p.id !== user.id));
            }
        );
    };

    const refreshParticipants = async () => {
        try {
            const parts = await apiFetch(`/api/chats/${chatId}/participants`);
            setParticipants(parts.participants);
        } catch (e) {
            console.error("Ошибка обновления списка участников", e);
        }
    };

    const handleMouseOverAI = async (e, fileId) => {
        const rect = e.target.getBoundingClientRect();
        setTooltip({ visible: true, x: rect.left + rect.width / 2, y: rect.top, content: 'Загрузка...' });

        if (metadataCache.current.has(fileId)) {
            setTooltip(prev => ({ ...prev, content: metadataCache.current.get(fileId).summary }));
        } else {
            try {
                const data = await apiFetch(`/api/metadata/by-file-id/${fileId}`);
                metadataCache.current.set(fileId, data);
                setTooltip(prev => ({ ...prev, content: data.summary }));
            } catch (err) {
                setTooltip(prev => ({ ...prev, content: 'Ошибка анализа' }));
            }
        }
    };

    if (!isOpen) return null;

    return (
        <>
            <div className="modal" onClick={(e) => e.target.className === 'modal' && onClose()}>
                <div className="modal-content my-profile-modal-content">
                    <div className="modal-header">
                        <h2>Группа: {groupDetails?.name || chatName}</h2>
                        <button className="header-icon-btn" onClick={onClose}>
                            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2">
                                <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                            </svg>
                        </button>
                    </div>

                    <div className="modal-body user-profile-body">
                        {isLoading ? (
                            <div className="skeleton-list">{Array(4).fill().map((_, i) => <div key={i} className="skeleton skeleton-row" />)}</div>
                        ) : (
                            <>
                                <div className="profile-header">
                                    <img src={defaultGroupImage} className="profile-avatar" alt="Group" />
                                    <div className="profile-details" style={{ flexGrow: 1 }}>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                            <div className="profile-section-title" style={{ margin: 0, border: 'none' }}>
                                                {groupDetails?.name}
                                            </div>
                                            {/* КНОПКА РЕДАКТИРОВАНИЯ */}
                                            <div className="group-actions">
                                                {canEdit && (
                                                    <button className="header-icon-btn" onClick={handleOpenEditModal} title="Редактировать">
                                                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2">
                                                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                                        </svg>
                                                    </button>
                                                )}
                                                {canRemove && (
                                                    <button
                                                        className="header-icon-btn"
                                                        onClick={() => setIsAddModalOpen(true)}
                                                        title="Добавить участников"
                                                    >
                                                        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2.5">
                                                            <line x1="12" y1="5" x2="12" y2="19"></line>
                                                            <line x1="5" y1="12" x2="19" y2="12"></line>
                                                        </svg>
                                                    </button>
                                                )}
                                            </div>

                                        </div>
                                        <div className="profile-info-item">{participants.length} участников</div>
                                        {groupDetails?.description && (
                                            <div className="profile-description">{groupDetails.description}</div>
                                        )}
                                    </div>
                                </div>

                                <div className="participants-section">
                                    <h3 className="profile-section-title">Участники</h3>
                                    <div className="participants-list">
                                        {participants.map(p => (
                                            <ParticipantItem
                                                key={p.id}
                                                user={p}
                                                isMe={p.id === currentUserId}
                                                canRemove={canRemove}
                                                onRemove={() => handleRemoveMember(p)}
                                                onOpenProfile={() => onOpenUserProfile(p)}
                                            />
                                        ))}
                                    </div>
                                </div>

                                <div className="attachments-section">
                                    <div className="attachments-tabs">
                                        {['IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT'].map(t => (
                                            <button key={t} className={`tab-btn ${activeTab === t ? 'active' : ''}`} onClick={() => setActiveTab(t)}>
                                                {t === 'IMAGE' ? 'Фото' : t === 'VIDEO' ? 'Видео' : t === 'AUDIO' ? 'Аудио' : 'Файлы'}
                                            </button>
                                        ))}
                                    </div>
                                    <div id="groupAttachmentsContent">
                                        {isAttachmentsLoading ? <div className="skeleton-row" /> : (
                                            <div className={activeTab === 'IMAGE' || activeTab === 'VIDEO' ? "attachments-grid" : "attachments-list"}>

                                                {attachments.length > 0 ? attachments.map(att => (
                                                    <AttachmentItem
                                                        key={att.fileId}
                                                        att={att}
                                                        type={activeTab}
                                                        imageObserver={imageObserver}
                                                        onMouseOverAI={handleMouseOverAI}
                                                        onMouseOutAI={() => setTooltip(p => ({ ...p, visible: false }))}
                                                    />
                                                )) : <p className='placeholder'>Нет вложений в этой категории.</p>}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            </div>

            {/* ВТОРОЕ МОДАЛЬНОЕ ОКНО: РЕДАКТИРОВАНИЕ ДАННЫХ ГРУППЫ */}
            {isEditModalOpen && (
                <div className="modal" style={{ zIndex: 2100 }}>
                    <div className="modal-content edit-user-modal-content">
                        <div className="modal-header">
                            <h2>Изменить данные группы</h2>
                            <button className="header-icon-btn" onClick={() => setIsEditModalOpen(false)}>
                                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2">
                                    <line x1="18" y1="6" x2="6" y2="18"></line>
                                    <line x1="6" y1="6" x2="18" y2="18"></line>
                                </svg>
                            </button>
                        </div>
                        <form onSubmit={handleUpdateGroup}>
                            <div className="modal-body">
                                <div className="profile-form-group">
                                    <label>Название группы:</label>
                                    <input
                                        type="text"
                                        className="search-input"
                                        value={editName}
                                        onChange={(e) => setEditName(e.target.value)}
                                        required
                                        minLength={2}
                                        maxLength={64}
                                    />
                                </div>
                                <div className="profile-form-group">
                                    <label>Описание:</label>
                                    <textarea
                                        className="search-input"
                                        style={{ resize: 'vertical', minHeight: '100px' }}
                                        value={editDescription}
                                        onChange={(e) => setEditDescription(e.target.value)}
                                        maxLength={1024}
                                    />
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="submit" className="profile-save-btn" disabled={isSaving || !editName.trim()}>
                                    {isSaving ? 'Сохранение...' : 'Сохранить'}
                                </button>
                                <button type="button" className="profile-cancel-btn" onClick={() => setIsEditModalOpen(false)}>
                                    Отмена
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {tooltip.visible && (
                <div className="attachment-tooltip visible" style={{ position: 'fixed', left: tooltip.x, top: tooltip.y - 10, transform: 'translate(-50%, -100%)', zIndex: 4000 }}>
                    {tooltip.content}
                </div>
            )}


            <AddParticipantsModal
                isOpen={isAddModalOpen}
                onClose={() => setIsAddModalOpen(false)}
                chatId={chatId}
                onParticipantsAdded={refreshParticipants}
                participantCache={participantCache}
            />
        </>
    );
};

// --- Под-компоненты ---

const ParticipantItem = ({ user, isMe, canRemove, onRemove, onOpenProfile }) => {
    const [avatar, setAvatar] = useState(defaultProfileImage);
    const roleMap = { 'OWNER': 'Владелец', 'ADMIN': 'Админ', 'MEMBER': 'Участник' };

    useEffect(() => {
        apiFetch(`/api/profiles/images/user-avatar/${user.id}`).then(id => {
            if (id) imageLoader.getImageSrc(id).then(setAvatar);
        }).catch(() => { });
    }, [user.id]);

    return (
        <div className="participant-item" onClick={!isMe ? onOpenProfile : null}>
            <div className="avatar-container">
                <img className="participant-avatar" src={avatar} alt="" />
                {user.online && (
                    <span className="online-status-dot"></span>
                )}
            </div>
            <div className="participant-info">
                <div className="participant-header">
                    <span className="participant-name">{user.name} {user.surname}</span>
                    <span className={`participant-role role-${user.role?.toLowerCase()}`}>{roleMap[user.role] || user.role}</span>
                </div>
                <span className="participant-username">@{user.username}</span>
            </div>
            {canRemove && !isMe && (
                <button className="remove-participant-btn" onClick={(e) => { e.stopPropagation(); onRemove(); }}>&times;</button>
            )}
        </div>
    );
};

export default GroupProfileModal;