// src/utils/photoViewer.js
import { imageLoader } from './imageLoader';

const photoViewer = (() => {
    let viewerModal = null;
    let viewerImage = null;
    let localApiBaseUrl = null;

    const close = () => {
        if (viewerModal) {
            viewerModal.classList.add('hidden');
            if (viewerImage && viewerImage.src) {
                imageLoader.revokeUrl(viewerImage.src);
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
                <img id="photoViewerImage" alt="Full size">
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
        if (!config || !config.apiBaseUrl) return;
        localApiBaseUrl = config.apiBaseUrl;
        createViewerDOM();
    };

    const open = (imageId) => {
        if (!viewerModal || !viewerImage || !localApiBaseUrl) return;

        if (viewerImage.src) imageLoader.revokeUrl(viewerImage.src);
        viewerImage.removeAttribute('src');
        viewerImage.style.opacity = '0';

        viewerModal.classList.add('loading');
        viewerModal.classList.remove('hidden');

        imageLoader.getImageBlob(imageId)
            .then(blob => {
                const url = URL.createObjectURL(blob);
                viewerImage.onload = () => {
                    viewerModal.classList.remove('loading');
                    viewerImage.style.opacity = '1';
                };
                viewerImage.src = url;
            })
            .catch(() => close());
    };

    return { init, open };
})();

export default photoViewer;