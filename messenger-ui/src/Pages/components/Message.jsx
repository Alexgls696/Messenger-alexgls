import React, { useEffect, useRef, useState } from 'react';
import { formatDate } from '../utils/dateUtils';
import { apiFetch } from '../utils/apiClient';
import { imageLoader } from '../utils/imageLoader';
import { useLongPress } from '../../hooks/useLongPress';

const Message = ({
    msg,
    isSentByMe,
    participantCache,
    onContextMenu,
    messageReadObserver,
    imageObserver,
    photoViewer,
    onOpenProfile,
    isSelected,
    selectionMode,
    onSelect,
    setReplyingTo
}) => {
    const msgRef = useRef(null);
    const isService = msg.service || msg.isService;

    const [forwardFromName, setForwardFromName] = useState(null);

    const longPressHandlers = useLongPress(
        (coords, event) => {
            const fakeEvent = {
                preventDefault: () => { },
                stopPropagation: () => { },
                clientX: coords.clientX,
                clientY: coords.clientY
            };
            onContextMenu(fakeEvent, msg);
        },
        () => { },
        { delay: 600 }
    );

    const renderTextWithLinks = (text) => {
        if (!text) return null;

        // Регулярное выражение для поиска URL (http, https, ftp)
        const urlRegex = /(https?:\/\/[^\s]+)/g;

        const parts = text.split(urlRegex);

        return parts.map((part, index) => {
            if (part.match(urlRegex)) {
                return (
                    <a
                        key={index}
                        href={part}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="message-link"
                        onClick={(e) => e.stopPropagation()}
                    >
                        {part}
                    </a>
                );
            }
            // Если это обычный текст
            return part;
        });
    };

    // --- Логика жестов свайпа ---
    const [touchStart, setTouchStart] = useState(null);
    const [swipeOffset, setSwipeOffset] = useState(0);
    const [isSwiping, setIsSwiping] = useState(false);
    const SWIPE_LIMIT = -100;
    const TRIGGER_THRESHOLD = -65;

    // ОБЪЕДИНЕННЫЙ Start
    const handleTouchStart = (e) => {
        if (selectionMode) return;

        // Для свайпа
        setTouchStart(e.touches[0].clientX);
        setIsSwiping(true);

        // Для лонг-пресса (вызываем функцию из хука)
        longPressHandlers.onTouchStart(e);
    };

    // ОБЪЕДИНЕННЫЙ Move
    const handleTouchMove = (e) => {
        // Для лонг-пресса: если палец двинулся, отменяем таймер меню
        longPressHandlers.onTouchMove(e);

        if (!touchStart || !isSwiping) return;
        const diff = e.touches[0].clientX - touchStart;

        if (diff < 0) {
            const offset = Math.max(diff, SWIPE_LIMIT);
            setSwipeOffset(offset);

            // Если свайп пошел активно, можно прервать лонг-пресс совсем
            if (Math.abs(diff) > 10) {
                longPressHandlers.onTouchEnd(e);
            }

            if (diff <= TRIGGER_THRESHOLD && swipeOffset > TRIGGER_THRESHOLD) {
                if (window.navigator.vibrate) window.navigator.vibrate(10);
            }
        }
    };

    // ОБЪЕДИНЕННЫЙ End
    const handleTouchEnd = (e) => {
        // Для лонг-пресса
        longPressHandlers.onTouchEnd(e);

        // Для свайпа
        if (swipeOffset <= TRIGGER_THRESHOLD && setReplyingTo) {
            console.log(msg)
            setReplyingTo(msg);
        }
        setSwipeOffset(0);
        setIsSwiping(false);
        setTouchStart(null);
    };

    useEffect(() => {
        const element = msgRef.current;
        if (element && !isSentByMe && !msg.read && messageReadObserver) {
            console.log("Reading...")
            messageReadObserver.observe(element);
            return () => messageReadObserver.unobserve(element);
        }
    }, [msg.id, msg.read, isSentByMe, messageReadObserver]);

    useEffect(() => {
        if (msg.forwarded && msg.forwardFromUserId) {
            const cachedName = participantCache[msg.forwardFromUserId];
            if (cachedName) {
                setForwardFromName(cachedName);
            } else {
                apiFetch(`/api/users/${msg.forwardFromUserId}`)
                    .then(data => {
                        const fullName = `${data.name} ${data.surname || ''}`.trim();
                        setForwardFromName(fullName);
                        participantCache[msg.forwardFromUserId] = fullName;
                    })
                    .catch(() => setForwardFromName(`Пользователь #${msg.forwardFromUserId}`));
            }
        }
    }, [msg.forwarded, msg.forwardFromUserId, participantCache]);

    const handleNameClick = (e) => {
        if (selectionMode) {
            e.preventDefault();
            return;
        }
        if (!isSentByMe && onOpenProfile) {
            onOpenProfile(msg.senderId, msg.chatId, senderName);
        }
    };

    const handleForwardNameClick = (e) => {
        e.stopPropagation();
        if (selectionMode) {
            e.preventDefault();
            return;
        }
        if (onOpenProfile && msg.forwardFromUserId) {
            onOpenProfile(msg.forwardFromUserId, msg.chatId, forwardFromName);
        }
    };

    const handleMessageClick = () => {
        if (selectionMode && onSelect) {
            onSelect();
        }
    };

    if (isService) {
        return (
            <div
                ref={msgRef}
                className="message service"
                data-message-id={msg.id}
                data-sender-id={msg.senderId}
            >
                <div className="service-content">{msg.content}</div>
            </div>
        );
    }

    const renderAttachments = () => {
        if (!msg.attachments || msg.attachments.length === 0) return null;

        const imageAttachments = msg.attachments.filter(att => att.mimeType?.startsWith('image/'));
        const fileAttachments = msg.attachments.filter(att => !att.mimeType?.startsWith('image/'));

        return (
            <div className="attachments-container">
                {/* Изображения */}
                {imageAttachments.length > 0 && (
                    <div className={imageAttachments.length > 1 ? "image-gallery-grid" : ""}>
                        {imageAttachments.map(att => (
                            <AttachmentImage
                                key={att.fileId}
                                att={att}
                                imageObserver={imageObserver}
                                photoViewer={photoViewer}
                                isPending={msg.isPending}
                                localUrl={att.localUrl}
                            />
                        ))}
                    </div>
                )}

                {/* Файлы */}
                {fileAttachments.length > 0 && (
                    <div className="file-attachments-grid">
                        {fileAttachments.map(att => (
                            <FileAttachment key={att.fileId} att={att} />
                        ))}
                    </div>
                )}
            </div>
        );
    };

    const senderName = isSentByMe ? '' : (participantCache[msg.senderId] || `Пользователь #${msg.senderId}`);
    let statusText = isSentByMe ? (msg.read ? 'Прочитано' : 'Доставлено') : '';
    const statusClass = isSentByMe && msg.read ? 'read' : '';

    const isEdited = msg.updatedAt !== null && !msg.forwarded;

    return (
        <div
            className={`message-wrapper ${isSentByMe ? 'is-me' : 'is-other'} ${selectionMode ? 'selection-active' : ''}`}
            style={{
                display: 'flex',
                width: '100%',
                position: 'relative',
                justifyContent: isSentByMe ? 'flex-end' : 'flex-start',
                alignItems: 'center',
                marginBottom: '4px'
            }}
        >
            <div
                className="swipe-indicator"
                style={{
                    position: 'absolute',
                    right: '10px',
                    opacity: Math.min(Math.abs(swipeOffset) / 60, 1),
                    transform: `scale(${Math.min(Math.abs(swipeOffset) / 60, 1)})`,
                    color: 'var(--accent-primary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    pointerEvents: 'none'
                }}
            >
                <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2.5">
                    <path d="M9 17L4 12L9 7" strokeLinecap="round" strokeLinejoin="round" />
                    <path d="M20 18V15C20 13.3431 18.6569 12 17 12H4" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
            </div>

            <div
                ref={msgRef}
                className={`message ${isSentByMe ? 'sent' : 'received'} ${selectionMode ? 'message--selection-mode' : ''} ${isSelected ? 'message--selected' : ''}`}
                data-message-id={msg.id}
                data-sender-id={msg.senderId}
                onClick={handleMessageClick}

                // Используем объединенные обработчики вместо spread {...longPressHandlers}
                onTouchStart={handleTouchStart}
                onTouchMove={handleTouchMove}
                onTouchEnd={handleTouchEnd}

                // Для десктопа оставляем как есть в хуке
                onMouseDown={longPressHandlers.onMouseDown}
                onMouseUp={longPressHandlers.onMouseUp}
                onMouseLeave={longPressHandlers.onMouseLeave}

                style={{
                    transform: `translateX(${swipeOffset}px)`,
                    transition: isSwiping ? 'none' : 'transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275)',
                    willChange: 'transform',
                    zIndex: 1,
                    cursor: selectionMode ? 'default' : 'pointer',
                    userSelect: 'none',
                    WebkitUserSelect: 'none',
                    WebkitTouchCallout: 'none'
                }}
                onContextMenu={(e) => {
                    if (selectionMode) return;
                    onContextMenu(e, msg);
                }}
            >
                {selectionMode && (
                    <div className="message-checkbox">
                        <input type="checkbox" checked={isSelected} readOnly />
                    </div>
                )}

                <div className="message-inner">
                    {senderName && (
                        <div
                            className="message-sender"
                            style={{ cursor: selectionMode ? 'inherit' : 'pointer' }}
                            onClick={handleNameClick}
                        >
                            {senderName}
                        </div>
                    )}

                    {msg.forwarded && (
                        <div className="message-forwarded" onClick={handleForwardNameClick}>
                            <span className="forward-icon">↪</span> Переслано от: <span className="forward-name">{forwardFromName || 'Загрузка...'}</span>
                        </div>
                    )}

                    {msg.replyMessageContent && !msg.forwarded && (
                        <div
                            className="message-reply-preview"
                            onClick={(e) => {
                                if (selectionMode) {
                                    e.preventDefault();
                                } else {
                                    const originalMsg = document.querySelector(`[data-message-id="${msg.replyMessageContent.messageId}"]`);
                                    originalMsg?.scrollIntoView({ behavior: 'smooth', block: 'center' });
                                }
                            }}
                        >
                            <span className="reply-sender">
                                {participantCache[msg.replyMessageContent.senderId] || `Пользователь #${msg.replyMessageContent.senderId}`}
                            </span>
                            <p className="reply-content">
                                {msg.replyMessageContent.content}
                            </p>
                        </div>
                    )}

                    {renderAttachments()}

                    {msg.content && (
                        <div className="message-content"> {renderTextWithLinks(msg.content)}</div>
                    )}

                    <div className="message-meta">
                        {isEdited && <span className="message-edited-label">изменено</span>}
                        <span>{formatDate(msg.createdAt)}</span>
                        <span className={`message-status ${statusClass}`}>{!msg.optimistic ? statusText : "Отправка...⏳"}</span>
                    </div>
                </div>
            </div>
        </div>
    );
};


const AttachmentImage = ({ att, imageObserver, photoViewer, isPending, localUrl }) => {
    const imgRef = useRef(null);
    const [src, setSrc] = useState(isPending ? localUrl : null);
    const [isLoaded, setIsLoaded] = useState(false);

    // Логика загрузки через imageLoader
    useEffect(() => {
        if (isPending) return;

        let isMounted = true;
        const observer = new IntersectionObserver(([entry]) => {
            if (entry.isIntersecting && isMounted) {
                // Когда картинка появилась на экране, запрашиваем Blob URL
                imageLoader.getImageSrc(att.fileId).then(url => {
                    if (isMounted) setSrc(url);
                });
                observer.disconnect();
            }
        }, { threshold: 0.1 });

        if (imgRef.current) observer.observe(imgRef.current);

        return () => {
            isMounted = false;
            observer.disconnect();
        };
    }, [att.fileId, isPending]);

    const handleImageClick = (e) => {
        e.preventDefault();
        e.stopPropagation();
        photoViewer.open(att.fileId);
    };

    return (
        <div
            className="attachment-item image-attachment viewer-enabled"
            onClick={handleImageClick}
            ref={imgRef}
        >
            {/* Скелетон, пока нет src или пока картинка не прогрузилась в тег */}
            {(!src || !isLoaded) && <div className="skeleton skeleton-tile"></div>}

            {src && (
                <img
                    src={src}
                    className="attachment-image"
                    alt="Вложение"
                    onLoad={() => setIsLoaded(true)}
                    style={{
                        opacity: isLoaded ? 1 : 0,
                        transition: 'opacity 0.3s ease'
                    }}
                />
            )}
        </div>
    );
};

const FileAttachment = ({ att }) => {
    const handleDownload = async (e) => {
        e.preventDefault();
        try {
            const metadata = await apiFetch(`/api/files/${att.fileId}`);

            const responseData = await apiFetch(`/api/media-storage/generate-download-url`, {
                method: 'POST',
                body: JSON.stringify({ url: metadata.path })
            });

            const presignedUrl = responseData.url || responseData;

            const link = document.createElement('a');
            link.href = presignedUrl;
            link.setAttribute('download', att.fileName || 'file');
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        } catch (err) {
            console.error("Ошибка при скачивании файла:", err);
            alert("Не удалось скачать файл");
        }
    };

    return (
        <div key={att.fileId} className="attachment-item file-attachment" title={att.fileName}>
            <div className="file-icon">📁</div>
            <div className="file-info">
                <span className="file-name">{att.fileName || 'Файл'}</span>
                <button className="download-link-btn" onClick={handleDownload}>
                    Скачать
                </button>
            </div>
        </div>
    );
};

export default React.memo(Message);