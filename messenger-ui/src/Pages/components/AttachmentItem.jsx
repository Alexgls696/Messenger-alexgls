import React, { useRef, useEffect, useState, useCallback } from 'react';
import photoViewer from '../utils/photoViewer';
import { imageLoader } from '../utils/imageLoader';
import { apiFetch } from '../utils/apiClient';

const AttachmentItem = ({ att, type, onMouseOverAI, onMouseOutAI }) => {
    const containerRef = useRef(null);
    const [src, setSrc] = useState(null); 
    const [isLoaded, setIsLoaded] = useState(false); 
    const [isInView, setIsInView] = useState(false);

    useEffect(() => {
        const observer = new IntersectionObserver(([entry]) => {
            if (entry.isIntersecting) {
                setIsInView(true);
                observer.disconnect();
            }
        }, { threshold: 0.1 });

        if (containerRef.current) observer.observe(containerRef.current);
        return () => observer.disconnect();
    }, []);

    useEffect(() => {
        if (isInView && att.fileId && (type === 'IMAGE' || type === 'VIDEO')) {
            imageLoader.getImageSrc(att.fileId)
                .then(url => setSrc(url))
                .catch(err => {
                    console.error("Ошибка загрузки медиа:", err);
                    setSrc("error");
                });
        }
    }, [isInView, att.fileId, type]);

    const handleDownload = useCallback(async (e) => {
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
            console.error("Не удалось скачать файл:", err);
            alert("Ошибка при скачивании файла");
        }
    }, [att.fileId, att.fileName]);

    const renderMedia = () => {
        if (type === 'IMAGE') {
            return (
                <div 
                    ref={containerRef}
                    className="attachment-item image-attachment viewer-enabled" 
                    onClick={() => photoViewer.open(att.fileId)}
                >
                    {!isLoaded && <div className="skeleton skeleton-tile" />}
                    
                    {src && src !== "error" && (
                        <img 
                            src={src} 
                            className="attachment-image" 
                            onLoad={() => setIsLoaded(true)}
                            style={{ 
                                opacity: isLoaded ? 1 : 0, 
                                transition: 'opacity 0.3s ease' 
                            }} 
                            alt=""
                        />
                    )}
                    
                    {src === "error" && <div className="error-placeholder">⚠️ Ошибка загрузки</div>}
                    
                    {att.hasAnalysis && (
                        <div className="ai-icon" onMouseOver={(e) => onMouseOverAI(e, att.fileId)} onMouseOut={onMouseOutAI}>
                            AI
                        </div>
                    )}
                </div>
            );
        }

        if (type === 'VIDEO') {
            return (
                <div ref={containerRef} className="attachment-item video-attachment">
                    {!isLoaded && <div className="skeleton skeleton-tile" />}
                    {src && src !== "error" && (
                        <video 
                            src={src} 
                            className="lazy-load-attachment" 
                            onLoadedData={() => setIsLoaded(true)}
                            controls 
                            style={{ 
                                opacity: isLoaded ? 1 : 0,
                                width: '100%',
                                borderRadius: '8px'
                            }} 
                        />
                    )}
                    {att.hasAnalysis && (
                        <div className="ai-icon" onMouseOver={(e) => onMouseOverAI(e, att.fileId)} onMouseOut={onMouseOutAI}>
                            AI
                        </div>
                    )}
                </div>
            );
        }

        return (
            <div className="attachment-list-item">
                <div className="file-icon">📄</div>
                <div className="file-info">
                    <span className="file-name">{att.fileName || 'Файл'}</span>
                    <div className="file-actions">
                        {att.hasAnalysis && (
                            <div className="ai-icon" onMouseOver={(e) => onMouseOverAI(e, att.fileId)} onMouseOut={onMouseOutAI}>
                                AI
                            </div>
                        )}
                        <button className="download-link-btn" onClick={handleDownload}>
                            Скачать
                        </button>
                    </div>
                </div>
            </div>
        );
    };

    return renderMedia();
};

export default AttachmentItem;