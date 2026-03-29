import { useState, useEffect } from 'react';
import userProfileDefault from '../images/profile-default.png'
import { imageLoader } from '../utils/imageLoader';
import { apiFetch } from '../utils/apiClient';

// Вспомогательный компонент аватара
export const UserAvatar = ({ id }) => {
    const [src, setSrc] = useState(userProfileDefault);
    useEffect(() => {
        apiFetch(`/api/profiles/images/user-avatar/${id}`)
            .then(avatarId => {
                if (avatarId) imageLoader.getImageSrc(avatarId).then(setSrc);
            }).catch(() => { });
    }, [id]);
    return <img className="user-item-avatar" src={src} alt="" />;
};