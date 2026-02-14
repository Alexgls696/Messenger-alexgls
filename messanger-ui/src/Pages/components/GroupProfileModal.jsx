import React, { useState, useEffect, useRef } from 'react';
import { apiFetch } from '../utils/apiClient';
import { imageLoader } from '../utils/imageLoader';
import AttachmentItem from './AttachmentItem';

import defaultGroupImage from '../images/group-default.png'
import defaultProfileImage from '../images/profile-default.png'

const GroupProfileModal = ({ isOpen, onClose, chatId, chatName, currentUserId, imageObserver, onOpenUserProfile, onOpenConfirm}) => {
    const [participants, setParticipants] = useState([]);
    const [groupDetails, setGroupDetails] = useState(null);
    const [canRemove, setCanRemove] = useState(false);
    const [activeTab, setActiveTab] = useState('IMAGE');
    const [attachments, setAttachments] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isAttachmentsLoading, setIsAttachmentsLoading] = useState(false);

    // Состояние для AI тултипа
    const [tooltip, setTooltip] = useState({ visible: false, x: 0, y: 0, content: '' });
    const metadataCache = useRef(new Map());

    // 1. Загрузка данных группы (участники, описание, доступ)
    useEffect(() => {
        if (isOpen && chatId) {
            const fetchGroupData = async () => {
                setIsLoading(true);
                try {
                    const [parts, details, access] = await Promise.all([
                        apiFetch(`/api/chats/${chatId}/participants`),
                        apiFetch(`/api/chats/${chatId}`),
                        apiFetch(`/api/chats/groups/${chatId}/access`).catch(() => ({ role: 'MEMBER' }))
                    ]);

                    setParticipants(parts);
                    setGroupDetails(details);

                    // Логика прав на удаление
                    const hasAccess = access === true ||
                        access?.canRemoveMembers === true ||
                        ['ADMIN', 'OWNER'].includes(access?.role);
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

    // 2. Загрузка вложений при смене вкладки
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

    const handleRemoveMember = (user) => {
        onOpenConfirm (
            `Вы действительно хотите удалить пользователя ${user.name} из группы?`,
            async () => {
                await apiFetch(`/api/chats/${chatId}/participants/${user.id}`, { method: 'DELETE' });
                setParticipants(prev => prev.filter(p => p.id !== user.id));
            }
        );
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
        <div className="modal" onClick={(e) => e.target.className === 'modal' && onClose()}>
            <div className="modal-content my-profile-modal-content">
                <div className="modal-header">
                    <h2>Группа: {chatName}</h2>
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
                                <div className="profile-details">
                                    <div className="profile-section-title" style={{ margin: 0, border: 'none' }}>{groupDetails?.name}</div>
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

            {tooltip.visible && (
                <div className="attachment-tooltip visible" style={{ position: 'fixed', left: tooltip.x, top: tooltip.y - 10, transform: 'translate(-50%, -100%)', zIndex: 4000 }}>
                    {tooltip.content}
                </div>
            )}
        </div>
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
            <img className="participant-avatar" src={avatar} alt="" />
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