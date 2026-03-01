import React, { useEffect, useRef, useState } from 'react';
import { formatDate } from '../utils/dateUtils';
import { apiFetch } from '../utils/apiClient';


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

    const[replyMsg, setReplyMsg] = useState(null);
    const [forwardFromName, setForwardFromName] = useState(null); 

    // --- Эффект для регистрации в Observer (прочтение сообщения) ---
    useEffect(() => {
        const element = msgRef.current;
        if (element && !isSentByMe && !msg.read && messageReadObserver) {
            messageReadObserver.observe(element);
            return () => messageReadObserver.unobserve(element);
        }
    },[msg.id, msg.read, isSentByMe, messageReadObserver]);

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
    },[msg.forwarded, msg.forwardFromUserId, participantCache]);


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

                {imageAttachments.length > 0 && (
                    <div className={imageAttachments.length > 1 ? "image-gallery-grid" : ""}>
                        {imageAttachments.map(att => (
                            <AttachmentImage
                                key={att.fileId}
                                att={att}
                                imageObserver={imageObserver}
                                photoViewer={photoViewer}
                                isPending={msg.isPending} // передаем флаг
                                localUrl={att.localUrl}
                            />
                        ))}
                    </div>
                )}

                {fileAttachments.map(att => {
                    const proxyUrl = `${API_BASE_URL}/api/storage/proxy/download/by-id?id=${att.fileId}`;
                    return (
                        <div key={att.fileId} className="attachment-item file-attachment">
                            <div className="file-icon">📁</div>
                            <div className="file-info">
                                <span className="file-name">{att.fileName || 'Файл'}</span>
                                <a href={proxyUrl} className="file-download-link" download={att.fileName}>
                                    Скачать
                                </a>
                            </div>
                        </div>
                    );
                })}
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
    const proxyUrl = `${API_BASE_URL}/api/storage/proxy/download/by-id?id=${att.fileId}`;

    if (isPending && localUrl) {
        return (
            <div className="attachment-item image-attachment">
                <img src={localUrl} className="attachment-image" style={{ opacity: 1 }} alt="" />
            </div>
        );
    }

    useEffect(() => {
        const imgElement = imgRef.current;
        if (imgElement && imageObserver) {
            imageObserver.observe(imgElement);
            return () => imageObserver.unobserve(imgElement);
        }
    }, [imageObserver, att.fileId]);

    const handleImageClick = (e) => {
        e.preventDefault();
        e.stopPropagation();
        photoViewer.open(att.fileId); 
    };

    return (
        <div
            className="attachment-item image-attachment viewer-enabled"
            data-file-id={att.fileId}
            onClick={handleImageClick}
        >
            <div className="skeleton skeleton-tile"></div>
            <img
                ref={imgRef}
                className="attachment-image lazy-load"
                data-src={proxyUrl}
                data-file-id={att.fileId}
                alt="Вложение"
                style={{ opacity: 0, transition: 'opacity 0.3s ease' }}
            />
        </div>
    );
};

export default Message;