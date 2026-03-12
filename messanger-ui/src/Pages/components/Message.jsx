import React, { useEffect, useRef, useState } from 'react';
import { formatDate } from '../utils/dateUtils';
import { apiFetch } from '../utils/apiClient';
import { imageLoader } from '../utils/imageLoader';


const API_BASE_URL = `http://${window.location.hostname}:8080`;

const Message = ({
    msg,
    isSentByMe,
    participantCache,
    onContextMenu,
    messageReadObserver,
    imageObserver,
    photoViewer,
    onOpenProfile,
    allMessages,
    replyCache,
    isSelected,
    selectionMode,
    onSelect
}) => {
    const msgRef = useRef(null);
    const isService = msg.service || msg.isService;

    const [replyMsg, setReplyMsg] = useState(null);
    const [forwardFromName, setForwardFromName] = useState(null);

    // --- Эффект для регистрации в Observer (прочтение сообщения) ---
    useEffect(() => {
        const element = msgRef.current;
        if (element && !isSentByMe && !msg.read && messageReadObserver) {
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

    //Ответы на сообщения
    useEffect(() => {
        if (!msg.replyToId) return;

        const found = allMessages.find(m => m.id === msg.replyToId);
        if (found) {
            setReplyMsg(found);
            return;
        }

        if (replyCache.has(msg.replyToId)) {
            setReplyMsg(replyCache.get(msg.replyToId));
            return;
        }

        apiFetch(`/api/messages/by-id?messageId=${msg.replyToId}&chatId=${msg.chatId}`)
            .then(data => {
                replyCache.set(msg.replyToId, data);
                setReplyMsg(data);
            })
            .catch(() => setReplyMsg({ content: "Сообщение удалено", senderId: null }));

    }, [msg.replyToId, allMessages, replyCache]);

    return (
        <div
            ref={msgRef}
            className={`message ${isSentByMe ? 'sent' : 'received'} ${selectionMode ? 'message--selection-mode' : ''} ${isSelected ? 'message--selected' : ''}`}
            data-message-id={msg.id}
            data-sender-id={msg.senderId}
            onClick={handleMessageClick}
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

                {/* Блок ответа (Reply) */}
                {msg.replyToId && !msg.forwarded && (
                    <div className="message-reply-preview" onClick={(e) => {
                        if (selectionMode) e.preventDefault();
                        else /*  прокрутка к оригиналу */ { }
                    }}>
                        <span className="reply-sender">
                            {replyMsg ? participantCache[replyMsg.senderId] : "..."}
                        </span>
                        <p className="reply-content">
                            {replyMsg ? replyMsg.content : "Загрузка..."}
                        </p>
                    </div>
                )}

                {renderAttachments()}

                {msg.content && (
                    <div className="message-content">{msg.content}</div>
                )}

                <div className="message-meta">
                    {isEdited && <span className="message-edited-label">изменено</span>}
                    <span>{formatDate(msg.createdAt)}</span>
                    <span className={`message-status ${statusClass}`}>{!msg.optimistic ? statusText : "Отправка...⏳"}</span>
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

export default Message;