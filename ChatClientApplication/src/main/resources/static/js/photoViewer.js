const photoViewer = (() => {
    let viewerModal = null;
    let viewerImage = null;
    let localApiBaseUrl = null;

    const close = () => {
        if (viewerModal) {
            viewerModal.classList.add('hidden');
            // Важно очистить src после закрытия, чтобы освободить память
            if (viewerImage && viewerImage.src) {
                imageLoader.revokeUrl(viewerImage.src); // Используем новый метод
                viewerImage.removeAttribute('src');
            }
        }
    };

    const createViewerDOM = () => {
        if (document.getElementById('photoViewerModal')) return;

        const modal = document.createElement('div');
        modal.id = 'photoViewerModal';
        modal.className = 'photo-viewer-modal hidden';


        modal.innerHTML = `
            <button class="photo-viewer-close-btn" title="Закрыть">
                <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
            </button>
            <div class="photo-viewer-content">
                <div class="photo-viewer-spinner"></div>
                <img id="photoViewerImage" alt="Full size view">
            </div>
        `;

        document.body.appendChild(modal);

        viewerModal = modal;
        viewerImage = document.getElementById('photoViewerImage');

        viewerModal.addEventListener('click', (event) => {
            if (event.target === viewerModal || event.target.closest('.photo-viewer-content')) {
                close();
            }
        });

        viewerModal.querySelector('.photo-viewer-close-btn').addEventListener('click', close);
    };

    const init = (config) => {
        // ... (эта функция остается без изменений) ...
        if (!config || !config.apiBaseUrl) {
            console.error("photoViewer.init: Необходим объект конфигурации с полем apiBaseUrl.");
            return;
        }
        localApiBaseUrl = config.apiBaseUrl;
        createViewerDOM();
    };

    /**
     * Открывает модальное окно с указанным изображением.
     * @param {number} imageId - ID изображения для показа.
     */
    const open = (imageId) => {
        if (!viewerModal || !viewerImage || !localApiBaseUrl) {
            console.error("Photo Viewer не инициализирован.");
            return;
        }

        if (viewerImage.src) {
            imageLoader.revokeUrl(viewerImage.src);
        }
        viewerImage.removeAttribute('src');
        viewerImage.style.opacity = '0'; // Сбрасываем прозрачность
        viewerImage.onload = null;
        viewerImage.onerror = null;

        viewerModal.classList.add('loading');
        viewerModal.classList.remove('hidden');

        const authToken = localStorage.getItem('accessToken');

        imageLoader.getImageBlob(imageId, localApiBaseUrl, authToken)
            .then(blob => {
                if (!blob) throw new Error("Не удалось получить Blob изображения.");

                viewerImage.onload = () => {
                    viewerModal.classList.remove('loading');
                    viewerImage.style.opacity = '1'; // Плавно показываем изображение
                };
                viewerImage.onerror = (e) => {
                    console.error("Ошибка отображения Blob изображения.", e);
                    close();
                };

                viewerImage.src = URL.createObjectURL(blob);
            })
            .catch(error => {
                console.error(`Не удалось загрузить изображение ${imageId}:`, error);
                close();
            });
    };

    return {
        init,
        open
    };
})();