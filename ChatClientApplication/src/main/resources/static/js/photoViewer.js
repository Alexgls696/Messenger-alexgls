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
        // ... (эта функция остается без изменений) ...
        if (document.getElementById('photoViewerModal')) return;
        const modal = document.createElement('div');
        modal.id = 'photoViewerModal';
        modal.className = 'photo-viewer-modal hidden';
        modal.innerHTML = `
            <span class="photo-viewer-close-btn">&times;</span>
            <div class="photo-viewer-content">
                <div class="photo-viewer-spinner"></div>
                <img id="photoViewerImage" alt="Full size view">
            </div>
        `;
        document.body.appendChild(modal);
        viewerModal = modal;
        viewerImage = document.getElementById('photoViewerImage');
        viewerModal.addEventListener('click', (event) => {
            if (event.target === viewerModal) close();
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
            console.error("Photo Viewer не инициализирован. Вызовите photoViewer.init({ apiBaseUrl: '...' }).");
            return;
        }

        // 1. Очищаем предыдущее изображение и его обработчики
        if (viewerImage.src) {
            imageLoader.revokeUrl(viewerImage.src);
        }
        viewerImage.removeAttribute('src');
        viewerImage.onload = null;
        viewerImage.onerror = null;

        // 2. Показываем модальное окно и спиннер
        viewerModal.classList.add('loading');
        viewerModal.classList.remove('hidden');

        const authToken = localStorage.getItem('accessToken');

        // 3. Запрашиваем Blob у imageLoader
        imageLoader.getImageBlob(imageId, localApiBaseUrl, authToken)
            .then(blob => {
                if (!blob) throw new Error("Не удалось получить Blob изображения.");

                // 4. Устанавливаем обработчики ДО присвоения src
                viewerImage.onload = () => {
                    viewerModal.classList.remove('loading');
                };
                viewerImage.onerror = (e) => {
                    console.error("Ошибка отображения Blob изображения.", e);
                    viewerModal.classList.remove('loading');
                    close();
                };

                // 5. Создаем НОВЫЙ Blob URL и присваиваем его
                viewerImage.src = URL.createObjectURL(blob);
            })
            .catch(error => {
                console.error(`Не удалось загрузить изображение ${imageId}:`, error);
                viewerModal.classList.remove('loading');
                // Можно показать заглушку или просто закрыть
                close();
            });
    };

    return {
        init,
        open
    };
})();