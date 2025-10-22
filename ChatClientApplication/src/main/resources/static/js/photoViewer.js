const photoViewer = (() => {
    let viewerModal = null;
    let viewerImage = null;
    let localApiBaseUrl = null; // Хранилище для базового URL

    // --- Приватные функции ---
    const close = () => {
        if (viewerModal) {
            viewerModal.classList.add('hidden');
        }
    };

    const createViewerDOM = () => {
        // Создаем контейнер, только если его еще нет
        if (document.getElementById('photoViewerModal')) return;

        const modal = document.createElement('div');
        modal.id = 'photoViewerModal';
        modal.className = 'photo-viewer-modal hidden';

        modal.innerHTML = `
            <span class="photo-viewer-close-btn">&times;</span>
            <div class="photo-viewer-content">
                <div class="photo-viewer-spinner"></div>
                <img id="photoViewerImage" src="" alt="Full size view">
            </div>
        `;

        document.body.appendChild(modal);

        // Назначаем обработчики закрытия
        viewerModal = modal;
        viewerImage = document.getElementById('photoViewerImage');

        viewerModal.addEventListener('click', (event) => {
            // Закрываем по клику на фон, но не на само изображение
            if (event.target === viewerModal) {
                close();
            }
        });

        viewerModal.querySelector('.photo-viewer-close-btn').addEventListener('click', close);
    };


    // --- Публичные функции ---
    /**
     * Инициализирует модуль.
     * @param {object} config - Конфигурация.
     * @param {string} config.apiBaseUrl - Базовый URL API, необходимый для загрузчика.
     */
        // ИСПРАВЛЕНО: Добавлен аргумент 'config' в функцию init
    const init = (config) => {
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
        // ИСПРАВЛЕНО: Комментарий теперь соответствует параметру 'imageId'
    const open = (imageId) => {
            if (!viewerModal || !viewerImage || !localApiBaseUrl) {
                console.error("Photo Viewer не инициализирован или не передан apiBaseUrl. Вызовите photoViewer.init({ apiBaseUrl: '...' }).");
                return;
            }

            viewerImage.src = '';
            viewerModal.classList.add('loading');
            viewerModal.classList.remove('hidden');

            const authToken = localStorage.getItem('accessToken');

            // Используем новый загрузчик
            imageLoader.getImageSrc(imageId, localApiBaseUrl, authToken)
                .then(src => {
                    viewerImage.onload = () => viewerModal.classList.remove('loading');
                    viewerImage.onerror = () => {
                        console.error("Ошибка отображения Blob изображения.");
                        viewerModal.classList.remove('loading');
                        close();
                    };
                    viewerImage.src = src;
                });
        };

    return {
        init,
        open
    };
})();