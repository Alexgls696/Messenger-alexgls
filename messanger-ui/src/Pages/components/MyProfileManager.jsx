import React, { useState, useEffect, useRef } from 'react';
import { apiFetch } from '../utils/apiClient';
import { imageLoader } from '../utils/imageLoader';
import photoViewer from '../utils/photoViewer';
import defaultProfileImage from '../images/profile-default.png'

const MyProfileManager = ({
    isOpen,
    onClose,
    userData,
    onUserDataUpdate
}) => {
    // --- Состояние профиля ---
    const [profileData, setProfileData] = useState(null);
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    // --- Состояние полей ввода (основное окно) ---
    const [status, setStatus] = useState('');
    const [birthday, setBirthday] = useState('');
    const [isSavingDetails, setIsSavingDetails] = useState(false);
    const [saveSuccess, setSaveSuccess] = useState(false);

    // --- Состояние полей ввода (окно редактирования имени) ---
    const [editForm, setEditForm] = useState({ name: '', surname: '', username: '' });
    const [isSavingUser, setIsSavingUser] = useState(false);
    const [editError, setEditError] = useState('');

    const fileInputRef = useRef(null);

    // Загрузка данных профиля при открытии
    useEffect(() => {
        if (isOpen && userData?.id) {
            fetchProfile();
            setEditForm({
                name: userData.name || '',
                surname: userData.surname || '',
                username: userData.username || ''
            });
        }
    }, [isOpen, userData]);

    const fetchProfile = async () => {
        setIsLoading(true);
        try {
            const data = await apiFetch(`/api/profiles/${userData.id}`);
            setProfileData(data);
            setStatus(data.status || '');
            setBirthday(data.birthday || '');
        } catch (error) {
            console.error("Ошибка загрузки профиля:", error);
        } finally {
            setIsLoading(false);
        }
    };

    // Сохранение статуса и даты рождения
    const handleSaveDetails = async () => {
        setIsSavingDetails(true);
        try {
            await apiFetch(`/api/profiles/update`, {
                method: 'POST',
                body: JSON.stringify({ status, birthday })
            });

            setSaveSuccess(true);
            if (onUserDataUpdate) await onUserDataUpdate();

            setTimeout(() => setSaveSuccess(false), 2000);
        } catch (error) {
            alert("Не удалось сохранить изменения");
        } finally {
            setIsSavingDetails(false);
        }
    };

    // Сохранение имени/фамилии/юзернейма
    const handleSaveUserInfo = async (e) => {
        e.preventDefault();
        if (!editForm.name || !editForm.username) {
            setEditError("Имя и юзернейм обязательны");
            return;
        }

        setIsSavingUser(true);
        setEditError('');
        try {
            await apiFetch(`/api/users/update`, {
                method: 'POST',
                body: JSON.stringify(editForm)
            });
            if (onUserDataUpdate) await onUserDataUpdate();
            setIsEditModalOpen(false);
        } catch (error) {
            setEditError(error.message || "Ошибка сохранения");
        } finally {
            setIsSavingUser(false);
        }
    };

    // Работа с фото (Загрузка)
    const handlePhotoUpload = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        try {
            const uploadConfig = await apiFetch('/api/media-storage/upload-url', {
                method: 'POST',
                body: JSON.stringify({
                    fileName: file.name,
                    contentType: file.type
                })
            });

            const s3Response = await fetch(uploadConfig.uploadUrl, {
                method: "PUT",
                headers: {
                    "Content-Type": file.type
                },
                body: file
            });

            if (!s3Response.ok) {
                throw new Error("S3 direct upload failed");
            }

            const savedFile = await apiFetch('/api/files', {
                method: 'POST',
                body: JSON.stringify({
                    path: uploadConfig.key,
                    filename: file.name
                })
            });

            await apiFetch(`/api/profiles/images`, {
                method: 'POST',
                body: JSON.stringify({ imageId: savedFile.id })
            });

            // 5. Обновляем состояние компонента и данные пользователя в хедере
            await fetchProfile();
            if (onUserDataUpdate) await onUserDataUpdate();

        } catch (error) {
            console.error("Ошибка при смене фото профиля:", error);
            alert("Ошибка при загрузке фото. Пожалуйста, попробуйте позже.");
        }
    };

    // Удаление фото
    const handleDeletePhoto = async (e, imageId) => {
        e.stopPropagation();
        if (!window.confirm("Удалить это фото?")) return;

        try {
            await apiFetch(`/api/profiles/images/${imageId}`, { method: 'DELETE' });
            await fetchProfile();
            if (onUserDataUpdate) await onUserDataUpdate();
        } catch (error) {
            alert("Ошибка при удалении");
        }
    };

    if (!isOpen) return null;

    return (
        <>
            {/* Основное модальное окно профиля */}
            <div className="modal" onClick={(e) => e.target.className === 'modal' && onClose()}>
                <div className="modal-content my-profile-modal-content">
                    <div className="modal-header">
                        <h2>Мой профиль</h2>
                        <button className="header-icon-btn" onClick={onClose}>
                            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2">
                                <line x1="18" y1="6" x2="6" y2="18"></line>
                                <line x1="6" y1="6" x2="18" y2="18"></line>
                            </svg>
                        </button>
                    </div>

                    <div className="modal-body my-profile-body">
                        {isLoading ? (
                            <div className="skeleton-list">
                                <div className="skeleton skeleton-row" style={{ height: '80px' }}></div>
                                <div className="skeleton skeleton-row"></div>
                            </div>
                        ) : (
                            <div className="profile-form-container">
                                <div className="profile-user-info">
                                    <div className="user-info-item"><strong>Имя:</strong> {userData?.name}</div>
                                    <div className="user-info-item"><strong>Фамилия:</strong> {userData?.surname || 'Не указана'}</div>
                                    <div className="user-info-item"><strong>Имя пользователя:</strong> @{userData?.username}</div>
                                    <button className="profile-edit-btn" onClick={() => setIsEditModalOpen(true)}>
                                        Редактировать основную информацию
                                    </button>
                                </div>

                                <div className="profile-form-header">
                                    <div id="myAvatarContainer">
                                        <AvatarImage avatarId={profileData?.avatarId} />
                                    </div>
                                    <span style={{ fontSize: '1.2em', fontWeight: 'bold' }}>Дополнительная информация</span>
                                </div>

                                <div className="profile-form-group">
                                    <label>Статус:</label>
                                    <input type='text'
                                        value={status}
                                        onChange={(e) => setStatus(e.target.value)}
                                        rows="3"
                                    />
                                </div>

                                <div className="profile-form-group">
                                    <label>Дата рождения:</label>
                                    <input
                                        type="date"
                                        value={birthday}
                                        onChange={(e) => setBirthday(e.target.value)}
                                    />
                                </div>

                                <div className="photos-section">
                                    <div className="photos-section-header">Мои фотографии</div>
                                    <div className="photos-grid">
                                        <div className="photo-tile add-photo-tile" onClick={() => fileInputRef.current.click()}>
                                            <span className="add-photo-tile-icon">+</span>
                                            <input
                                                type="file"
                                                ref={fileInputRef}
                                                hidden
                                                accept="image/*"
                                                onChange={handlePhotoUpload}
                                            />
                                        </div>
                                        {profileData?.userImages?.map(img => (
                                            <div key={img.id} className="photo-tile" onClick={() => photoViewer.open(img.imageId)}>
                                                <ProfileImage imageId={img.imageId} />
                                                <button
                                                    className="delete-photo-btn"
                                                    onClick={(e) => handleDeletePhoto(e, img.imageId)}
                                                >
                                                    &times;
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>

                    <div className="modal-footer">
                        <button
                            className={`profile-save-btn ${saveSuccess ? 'success' : ''}`}
                            disabled={isSavingDetails}
                            onClick={handleSaveDetails}
                        >
                            {isSavingDetails ? 'Сохранение...' : saveSuccess ? 'Сохранено!' : 'Сохранить'}
                        </button>
                        <button className="profile-cancel-btn" onClick={onClose}>Отмена</button>
                    </div>
                </div>
            </div>

            {/* Вложенное окно редактирования имени/юзера */}
            {isEditModalOpen && (
                <div className="modal" style={{ zIndex: 2100 }}>
                    <div className="modal-content edit-user-modal-content">
                        <div className="modal-header">
                            <h2>Изменить данные</h2>
                            <button className="header-icon-btn" onClick={() => setIsEditModalOpen(false)}>
                                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2">
                                    <line x1="18" y1="6" x2="6" y2="18"></line>
                                    <line x1="6" y1="6" x2="18" y2="18"></line>
                                </svg>
                            </button>
                        </div>
                        <form onSubmit={handleSaveUserInfo}>
                            <div className="modal-body">
                                <div className="profile-form-group">
                                    <label>Имя:</label>
                                    <input
                                        type="text"
                                        value={editForm.name}
                                        onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                                        required
                                    />
                                </div>
                                <div className="profile-form-group">
                                    <label>Фамилия:</label>
                                    <input
                                        type="text"
                                        value={editForm.surname}
                                        onChange={(e) => setEditForm({ ...editForm, surname: e.target.value })}
                                    />
                                </div>
                                <div className="profile-form-group">
                                    <label>Имя пользователя:</label>
                                    <input
                                        type="text"
                                        value={editForm.username}
                                        onChange={(e) => setEditForm({ ...editForm, username: e.target.value })}
                                        required
                                    />
                                </div>
                                {editError && <div className="error-message">{editError}</div>}
                            </div>
                            <div className="modal-footer">
                                <button type="submit" className="profile-save-btn" disabled={isSavingUser}>
                                    {isSavingUser ? 'Сохранение...' : 'Сохранить'}
                                </button>
                                <button type="button" className="profile-cancel-btn" onClick={() => setIsEditModalOpen(false)}>
                                    Отмена
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </>
    );
};

// --- Вспомогательные компоненты для работы с imageLoader ---

const AvatarImage = ({ avatarId }) => {
    const [src, setSrc] = useState(defaultProfileImage);
    useEffect(() => {
        if (avatarId) {
            imageLoader.getImageSrc(avatarId).then(setSrc);
        }
    }, [avatarId]);
    return <img src={src} className="profile-avatar" alt="Avatar" />;
};

const ProfileImage = ({ imageId }) => {
    const [src, setSrc] = useState("");
    useEffect(() => {
        if (imageId) {
            imageLoader.getImageSrc(imageId).then(setSrc);
        }
    }, [imageId]);
    return src ? <img src={src} alt="User" /> : <div className="skeleton"></div>;
};

export default MyProfileManager;