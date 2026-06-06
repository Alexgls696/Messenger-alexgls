import React, { useState, useEffect, useRef } from 'react';
import { apiFetch } from '../utils/apiClient';
import { imageLoader } from '../utils/imageLoader';
import photoViewer from '../utils/photoViewer';
import AttachmentItem from './AttachmentItem';

import { toast } from 'react-toastify';
import ConfirmationModal from './ConfirmationModal';

import defaultProfileImage from '../images/profile-default.png'

const UserProfileModal = ({ isOpen, onClose, id, chatId, name, imageObserver, currentUserId }) => {
    const [profileData, setProfileData] = useState(null);
    const [activeTab, setActiveTab] = useState('IMAGE');
    const [attachments, setAttachments] = useState([]);
    const [isProfileLoading, setIsProfileLoading] = useState(false);
    const [isAttachmentsLoading, setIsAttachmentsLoading] = useState(false);

    // Логика блокировки
    const [isBlocked, setIsBlocked] = useState(false);
    const [isConfirmBlockOpen, setIsConfirmBlockOpen] = useState(false);
    const [isBlocking, setIsBlocking] = useState(false);

    const [tooltipData, setTooltipData] = useState({ visible: false, x: 0, y: 0, content: '' });
    const metadataCache = useRef(new Map());


    useEffect(() => {
        let isMounted = true;
        if (isOpen && id && currentUserId && Number(id) !== Number(currentUserId)) {
            const checkBlockStatus = async () => {
                try {
                    const response = await apiFetch(`/api/users/black-list/is_blocked?targetUserId=${id}`, {
                        method: 'POST'
                    });
                    if (isMounted) {
                        setIsBlocked(response?.isBlocked || false);
                    }
                } catch (error) {
                    console.error("Ошибка при проверке статуса ЧС:", error);
                }
            };
            checkBlockStatus();
        }
        return () => { isMounted = false; };
    }, [isOpen, id, currentUserId]);

    // Загрузка профиля
    useEffect(() => {
        if (isOpen && id) {
            const fetchProfile = async () => {
                setIsProfileLoading(true);
                try {
                    const data = await apiFetch(`/api/profiles/${id}`);
                    setProfileData(data);
                } catch (error) {
                    console.error("Ошибка загрузки профиля:", error);
                } finally {
                    setIsProfileLoading(false);
                }
            };
            fetchProfile();
        }
    }, [isOpen, id]);

    // Обработка клавиши Escape
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

    // Загрузка вложений
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

    const handleMouseOverAI = async (e, fileId) => {
        const iconRect = e.target.getBoundingClientRect();

        setTooltipData(prev => ({
            ...prev,
            visible: true,
            x: iconRect.left + iconRect.width / 2,
            y: iconRect.top,
            content: 'Загрузка анализа...'
        }));

        if (metadataCache.current.has(fileId)) {
            const cached = metadataCache.current.get(fileId);
            setTooltipData(prev => ({ ...prev, content: cached.summary || 'Нет описания.' }));
        } else {
            try {
                const metadata = await apiFetch(`/api/metadata/by-file-id/${fileId}`);
                metadataCache.current.set(fileId, metadata);
                setTooltipData(prev => ({ ...prev, content: metadata.summary || 'Нет описания.' }));
            } catch (err) {
                setTooltipData(prev => ({ ...prev, content: 'Ошибка загрузки.' }));
            }
        }
    };

    const handleToggleBlock = async () => {
        if (!id) return;
        setIsBlocking(true);

        const url = isBlocked
            ? `/api/users/black-list/black-list/delete?targetUserId=${id}`
            : `/api/users/black-list/black-list?targetUserId=${id}`;

        try {
            await apiFetch(url, {
                method: 'POST'
            });

            const newStatus = !isBlocked;
            setIsBlocked(newStatus);
            toast.success(newStatus ? "Пользователь добавлен в черный список" : "Пользователь удален из черного списка");
            setIsConfirmBlockOpen(false);
        } catch (error) {
            console.error("Ошибка при изменении статуса блокировки:", error);
            toast.error(isBlocked ? "Не удалось разблокировать пользователя" : "Не удалось заблокировать пользователя");
        } finally {
            setIsBlocking(false);
        }
    };

    const handleMouseOutAI = () => {
        setTooltipData(prev => ({ ...prev, visible: false }));
    };

    if (!isOpen) return null;

    return (
        <div className="modal" onClick={(e) => e.target.className === 'modal' && onClose()}>
            <div className="modal-content my-profile-modal-content">
                <div className="modal-header">
                    <h2>Профиль: {name}</h2>
                    <div className="header-actions" style={{ display: 'flex', gap: '8px', alignItems: 'center', marginLeft: 'auto' }}>
                        {/* Кнопка блокировки/разблокировки в виде SVG */}
                        {id && Number(id) !== Number(currentUserId) && (
                            <button
                                className="header-icon-btn"
                                onClick={() => setIsConfirmBlockOpen(true)}
                                title={isBlocked ? "Разблокировать пользователя" : "Заблокировать пользователя"}
                                style={{ color: isBlocked ? '#ff4d4f' : 'inherit' }}
                                disabled={isBlocking}
                            >
                                {isBlocked ? (
                                    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                                        <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                                    </svg>
                                ) : (
                                    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <circle cx="12" cy="12" r="10" />
                                        <line x1="4.93" y1="4.93" x2="19.07" y2="19.07" />
                                    </svg>
                                )}
                            </button>
                        )}
                        <button className="header-icon-btn" onClick={onClose} title="Закрыть">
                            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2">
                                <line x1="18" y1="6" x2="6" y2="18" />
                                <line x1="6" y1="6" x2="18" y2="18" />
                            </svg>
                        </button>
                    </div>
                </div>

                <div className="modal-body user-profile-body">
                    {isProfileLoading ? (
                        <div className="skeleton-list">
                            {Array(3).fill().map((_, i) => <div key={i} className="skeleton skeleton-row" />)}
                        </div>
                    ) : (
                        <>
                            <div className="profile-header">
                                <AvatarImage avatarId={profileData?.avatarId} />
                                <div className="profile-details">
                                    <div className="profile-info-item"><strong>Статус:</strong> {profileData?.status || 'Не указан'}</div>
                                    <div className="profile-info-item"><strong>День рождения:</strong> {profileData?.birthday || 'Не указан'}</div>
                                </div>
                            </div>

                            <div className="photos-section">
                                <h3 className="profile-section-title">Фотографии</h3>
                                {profileData?.userImages?.length > 0 ? (
                                    <div className="photos-grid">
                                        {
                                            profileData.userImages.map(img => (
                                                <div key={img.id} className="profile-image-item" onClick={() => photoViewer.open(img.imageId)}>
                                                    <ProfilePhoto imageId={img.imageId} />
                                                </div>
                                            ))
                                        }
                                    </div>) : (<p className="placeholder">У пользователя нет фотографий.</p>)
                                }
                            </div>

                            {/* Вложения чата */}
                            <div className="attachments-section">
                                <div className="attachments-tabs">
                                    {['IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT'].map(type => (
                                        <button
                                            key={type}
                                            className={`tab-btn ${activeTab === type ? 'active' : ''}`}
                                            onClick={() => setActiveTab(type)}
                                        >
                                            {type === 'IMAGE' ? 'Изображения' : type === 'VIDEO' ? 'Видео' : type === 'AUDIO' ? 'Аудио' : 'Файлы'}
                                        </button>
                                    ))}
                                </div>

                                <div id="attachmentsContent">
                                    {isAttachmentsLoading ? (
                                        <div className="skeleton-list">
                                            <div className="skeleton skeleton-row" />
                                        </div>
                                    ) : (
                                        <div className={activeTab === 'IMAGE' || activeTab === 'VIDEO' ? "attachments-grid" : "attachments-list"}>
                                            {attachments.length > 0 ? attachments.map(att => (
                                                <AttachmentItem
                                                    key={att.fileId}
                                                    att={att}
                                                    type={activeTab}
                                                    imageObserver={imageObserver}
                                                    onMouseOverAI={handleMouseOverAI}
                                                    onMouseOutAI={handleMouseOutAI}
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

            {/* Локальный тултип компонента */}
            {tooltipData.visible && (
                <div
                    className="attachment-tooltip visible"
                    style={{
                        position: 'fixed',
                        left: `${tooltipData.x}px`,
                        top: `${tooltipData.y - 10}px`,
                        transform: 'translate(-50%, -100%)',
                        zIndex: 4000
                    }}
                >
                    {tooltipData.content}
                </div>
            )}

            {/* Модальное окно подтверждения блокировки/разблокировки */}
            <ConfirmationModal
                isOpen={isConfirmBlockOpen}
                title={isBlocked ? "Разблокировать пользователя?" : "Заблокировать пользователя?"}
                message={
                    isBlocked
                        ? `Вы действительно хотите убрать пользователя ${name} из черного списка?`
                        : `Вы действительно хотите добавить пользователя ${name} в черный список? Вы больше не будете получать от него сообщения.`
                }
                onConfirm={handleToggleBlock}
                onCancel={() => setIsConfirmBlockOpen(false)}
            />
        </div>
    );
};

// --- Вспомогательные компоненты ---

const AvatarImage = ({ avatarId }) => {
    const [src, setSrc] = useState(defaultProfileImage);
    useEffect(() => {
        if (avatarId) imageLoader.getImageSrc(avatarId).then(setSrc);
    }, [avatarId]);
    return <img src={src} className="profile-avatar" alt="Avatar" />;
};

const ProfilePhoto = ({ imageId }) => {
    const [src, setSrc] = useState("");
    useEffect(() => {
        if (imageId) imageLoader.getImageSrc(imageId).then(setSrc);
    }, [imageId]);
    return src ? <img src={src} alt="User photo" /> : <div className="skeleton" />;
};


export default UserProfileModal;