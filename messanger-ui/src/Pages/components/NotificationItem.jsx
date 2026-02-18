import React, { useState, useEffect } from 'react';
import { imageLoader } from '../utils/imageLoader';
import { formatDate, formatDateTime } from '../utils/dateUtils';
import defaultProfile from '../images/profile-default.png';

function NotificationItem({ ntf }) {
    const [imgSrc, setImgSrc] = useState(defaultProfile);

    useEffect(() => {
        if (ntf.imageId) {
            imageLoader.getImageSrc(ntf.imageId).then(setImgSrc);
        }
    }, [ntf.imageId]);

    return (
        <div className="notification-dropdown-item" title={ntf.title}>
            <img src={imgSrc} className="notification-avatar" alt="" />
            <div className="notification-info">
                <div className="notification-header-row">
                    {/* Добавляем title для наведения */}
                    <span className="notification-title" title={ntf.title}>
                        {ntf.title}
                    </span>
                    <span className="notification-date">
                        {formatDateTime(ntf.createdAt)}
                    </span>
                </div>
                {/* Добавляем title для контента, чтобы видеть полный текст при наведении */}
                <div className="notification-text" title={ntf.content}>
                    {ntf.content}
                </div>
            </div>
        </div>
    );
}

export default NotificationItem;