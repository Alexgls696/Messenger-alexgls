import React, { useState, useEffect, useRef } from 'react';
import { apiFetch } from '../utils/apiClient';
import { imageLoader } from '../utils/imageLoader';
import photoViewer from '../utils/photoViewer';
import AttachmentItem  from './AttachmentItem';

import defaultProfileImage from '../images/profile-default.png'

const UserProfileModal = ({ isOpen, onClose, id, chatId, name, imageObserver }) => {
    const [profileData, setProfileData] = useState(null);
    const [activeTab, setActiveTab] = useState('IMAGE');
    const [attachments, setAttachments] = useState([]);
    const [isProfileLoading, setIsProfileLoading] = useState(false);
    const [isAttachmentsLoading, setIsAttachmentsLoading] = useState(false);

    // Состояние для тултипа (AI анализ)
    const [tooltipData, setTooltipData] = useState({ visible: false, x: 0, y: 0, content: '' });
    const metadataCache = useRef(new Map());

    // 1. Загрузка данных профиля при открытии
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

    // 2. Загрузка вложений при смене вкладки или чата
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

    // --- Логика тултипа (AI) ---
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

    const handleMouseOutAI = () => {
        setTooltipData(prev => ({ ...prev, visible: false }));
    };

    if (!isOpen) return null;
    return (
        <div className="modal" onClick={(e) => e.target.className === 'modal' && onClose()}>
            <div className="modal-content my-profile-modal-content">
                <div className="modal-header">
                    <h2>Профиль: {name}</h2>
                    <button className="header-icon-btn" onClick={onClose}>
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2">
                            <line x1="18" y1="6" x2="6" y2="18" />
                            <line x1="6" y1="6" x2="18" y2="18" />
                        </svg>
                    </button>
                </div>

                <div className="modal-body user-profile-body">
                    {isProfileLoading ? (
                        <div className="skeleton-list">
                            {Array(3).fill().map((_, i) => <div key={i} className="skeleton skeleton-row" />)}
                        </div>
                    ) : (
                        <>
                            {/* Шапка профиля */}
                            <div className="profile-header">
                                <AvatarImage avatarId={profileData?.avatarId} />
                                <div className="profile-details">
                                    <div className="profile-info-item"><strong>Статус:</strong> {profileData?.status || 'Не указан'}</div>
                                    <div className="profile-info-item"><strong>День рождения:</strong> {profileData?.birthday || 'Не указан'}</div>
                                </div>
                            </div>

                            {/* Фотографии */}
                            <div className="photos-section">
                                <h3 className="profile-section-title">Фотографии</h3>
                                <div className="photos-grid">
                                    {profileData?.userImages?.length > 0 ? (
                                        profileData.userImages.map(img => (
                                            <div key={img.id} className="profile-image-item" onClick={() => photoViewer.open(img.imageId)}>
                                                <ProfilePhoto imageId={img.imageId} />
                                            </div>
                                        ))
                                    ) : <p className="placeholder">У пользователя нет фотографий.</p>}
                                </div>
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
                                            {attachments.length > 0 ?attachments.map(att => (
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