import React, { useRef, useEffect, useState } from 'react';
import photoViewer from '../utils/photoViewer';
import { imageLoader } from '../utils/imageLoader';

const API_BASE_URL = `http://${window.location.hostname}:8080`;

const AttachmentItem = ({ att, type, onMouseOverAI, onMouseOutAI }) => {
    const containerRef = useRef(null);
    const [src, setSrc] = useState(null); // Ссылка на загруженный Blob
    const [isLoaded, setIsLoaded] = useState(false); // Загрузилась ли картинка в тег img
    const [isInView, setIsInView] = useState(false); // Появился ли элемент на экране

    const proxyUrl = `${API_BASE_URL}/api/storage/proxy/download/by-id?id=${att.fileId}`;

    // 1. Следим за появлением элемента на экране
    useEffect(() => {
        const observer = new IntersectionObserver(([entry]) => {
            if (entry.isIntersecting) {
                setIsInView(true);
                observer.disconnect(); // Нам нужно загрузить только один раз
            }
        }, { threshold: 0.1 });

        if (containerRef.current) {
            observer.observe(containerRef.current);
        }

        return () => observer.disconnect();
    }, []);

    // 2. Загружаем данные через imageLoader, когда элемент в зоне видимости
    useEffect(() => {
        if (isInView && att.fileId) {
            imageLoader.getImageSrc(att.fileId)
                .then(url => {
                    setSrc(url);
                })
                .catch(err => {
                    console.error("Ошибка загрузки вложения:", err);
                    setSrc("error"); // Пометим ошибку
                });
        }
    }, [isInView, att.fileId]);

    const renderMedia = () => {
        if (type === 'IMAGE') {
            return (
                <div 
                    ref={containerRef}
                    className="attachment-item image-attachment viewer-enabled" 
                    onClick={() => photoViewer.open(att.fileId)}
                >
                    {/* Скелетон виден, пока картинка не загружена полностью */}
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
                    
                    {src === "error" && <div className="error-placeholder">⚠️</div>}
                    
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
                <div ref={containerRef} className="attachment-item">
                    {!isLoaded && <div className="skeleton skeleton-tile" />}
                    {src && (
                        <video 
                            src={src} 
                            className="lazy-load-attachment" 
                            onLoadedData={() => setIsLoaded(true)}
                            controls 
                            style={{ opacity: isLoaded ? 1 : 0 }} 
                        />
                    )}
                    {att.hasAnalysis && <div className="ai-icon" onMouseOver={(e) => onMouseOverAI(e, att.fileId)} onMouseOut={onMouseOutAI}>AI</div>}
                </div>
            );
        }

        return (
            <div className="attachment-list-item">
                <span>{att.fileName || 'Файл'}</span>
                <div className="file-actions">
                    {att.hasAnalysis && <div className="ai-icon" onMouseOver={(e) => onMouseOverAI(e, att.fileId)} onMouseOut={onMouseOutAI}>AI</div>}
                    <a href={proxyUrl} download={att.fileName}>Скачать</a>
                </div>
            </div>
        );
    };

    return renderMedia();
};

export default AttachmentItem;