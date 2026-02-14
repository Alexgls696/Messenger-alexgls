import React, { useState, useEffect } from 'react';
import { apiFetch } from '../utils/apiClient';
import { imageLoader } from '../utils/imageLoader';
import { formatDate } from '../utils/dateUtils';

const ChatSearchModal = ({ isOpen, onClose, chatId, participantCache }) => {
    const [activeTab, setActiveTab] = useState('messages'); // 'messages' | 'attachments'
    const [messageQuery, setMessageQuery] = useState('');
    const [attachmentQuery, setAttachmentQuery] = useState('');
    const [messageResults, setMessageResults] = useState([]);
    const [attachmentResults, setAttachmentResults] = useState([]);
    const [isSearching, setIsSearching] = useState(false);

    // Очистка при закрытии/открытии
    useEffect(() => {
        if (isOpen) {
            setMessageQuery('');
            setAttachmentQuery('');
            setMessageResults([]);
            setAttachmentResults([]);
            setIsSearching(false);
            setActiveTab('messages');
        }
    }, [isOpen]);

    const handleMessageSearch = async (e) => {
        e.preventDefault();
        if (!messageQuery.trim()) return;

        setIsSearching(true);
        try {
            const data = await apiFetch('/api/search/messages/find-by-content-in-chat', {
                method: 'POST',
                body: JSON.stringify({ chatId, content: messageQuery })
            });
            setMessageResults(data || []);
        } catch (error) {
            console.error("Ошибка поиска сообщений:", error);
        } finally {
            setIsSearching(false);
        }
    };

    const handleAttachmentSearch = async (e) => {
        e.preventDefault();
        if (!attachmentQuery.trim()) return;

        setIsSearching(true);
        try {
            const data = await apiFetch('/api/metadata', {
                method: 'POST',
                body: JSON.stringify({ chatId, query: attachmentQuery })
            });
            setAttachmentResults(data || []);
        } catch (error) {
            console.error("Ошибка поиска вложений:", error);
        } finally {
            setIsSearching(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="modal" onClick={(e) => e.target.className === 'modal' && onClose()}>
            <div className="modal-content search-modal-content">
                <div className="modal-header">
                    <h2>Поиск в чате</h2>
                    <button className="header-icon-btn" onClick={onClose}>
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2.5">
                            <line x1="18" y1="6" x2="6" y2="18"></line>
                            <line x1="6" y1="6" x2="18" y2="18"></line>
                        </svg>
                    </button>
                </div>

                <div className="search-tabs">
                    <button 
                        className={`tab-btn ${activeTab === 'messages' ? 'active' : ''}`}
                        onClick={() => setActiveTab('messages')}
                    >
                        Поиск сообщений
                    </button>
                    <button 
                        className={`tab-btn ${activeTab === 'attachments' ? 'active' : ''}`}
                        onClick={() => setActiveTab('attachments')}
                    >
                        Поиск вложений
                    </button>
                </div>

                <div className="search-content">
                    {/* ВКЛАДКА СООБЩЕНИЙ */}
                    {activeTab === 'messages' && (
                        <div className="search-tab-content active">
                            <form className="search-form" onSubmit={handleMessageSearch}>
                                <input 
                                    type="text" 
                                    className="search-input" 
                                    placeholder="Введите текст сообщения..." 
                                    value={messageQuery}
                                    onChange={(e) => setMessageQuery(e.target.value)}
                                    autoFocus
                                />
                                <button type="submit" className="search-button" disabled={isSearching}>
                                    {isSearching ? '...' : 'Найти'}
                                </button>
                            </form>
                            <div className="user-list-container">
                                {messageResults.length > 0 ? (
                                    messageResults.map(msg => (
                                        <FoundMessageItem 
                                            key={msg.id} 
                                            msg={msg} 
                                            senderName={participantCache[msg.senderId]} 
                                        />
                                    ))
                                ) : (
                                    !isSearching && <p className="placeholder">Результатов нет</p>
                                )}
                            </div>
                        </div>
                    )}

                    {/* ВКЛАДКА ВЛОЖЕНИЙ */}
                    {activeTab === 'attachments' && (
                        <div className="search-tab-content active">
                            <form className="search-form" onSubmit={handleAttachmentSearch}>
                                <input 
                                    type="text" 
                                    className="search-input" 
                                    placeholder="Поиск по содержимому файлов..." 
                                    value={attachmentQuery}
                                    onChange={(e) => setAttachmentQuery(e.target.value)}
                                    autoFocus
                                />
                                <button type="submit" className="search-button" disabled={isSearching}>
                                    {isSearching ? '...' : 'Найти'}
                                </button>
                            </form>
                            <div className="user-list-container">
                                {attachmentResults.length > 0 ? (
                                    attachmentResults.map(file => (
                                        <FoundFileItem key={file.fileId} file={file} />
                                    ))
                                ) : (
                                    !isSearching && <p className="placeholder">Ничего не найдено</p>
                                )}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

// --- Вспомогательный компонент для сообщения ---
const FoundMessageItem = ({ msg, senderName }) => {
    const [avatar, setAvatar] = useState("/images/profile-default.png");

    useEffect(() => {
        apiFetch(`/api/profiles/images/user-avatar/${msg.senderId}`)
            .then(avatarId => {
                if (avatarId) imageLoader.getImageSrc(avatarId).then(setAvatar);
            }).catch(() => {});
    }, [msg.senderId]);

    return (
        <div className="user-item">
            <img className="user-item-avatar" src={avatar} alt="" />
            <div className="user-item-info">
                <div className="user-item-name">
                    {senderName || `Пользователь #${msg.senderId}`} 
                    <span style={{fontSize: '0.7em', color: 'var(--text-placeholder)', marginLeft: '8px'}}>
                        {formatDate(msg.createdAt)}
                    </span>
                </div>
                <div className="user-item-username">{msg.content || 'Вложение'}</div>
            </div>
        </div>
    );
};

// --- Вспомогательный компонент для файла ---
const FoundFileItem = ({ file }) => {
    const downloadUrl = `/api/storage/proxy/download/by-id?id=${file.fileId}`;
    
    return (
        <div className="found-file-item">
            <div className="found-file-preview">
                <svg className="file-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                    <polyline points="14 2 14 8 20 8"></polyline>
                    <line x1="16" y1="13" x2="8" y2="13"></line>
                    <line x1="16" y1="17" x2="8" y2="17"></line>
                </svg>
            </div>
            <div className="found-file-info">
                <div className="found-file-title">{file.title || 'Без названия'}</div>
                <div className="found-file-summary">{file.summary || 'Нет описания.'}</div>
                <a href={downloadUrl} download={file.title} className="found-file-download-btn">
                    Скачать
                </a>
            </div>
        </div>
    );
};

export default ChatSearchModal;