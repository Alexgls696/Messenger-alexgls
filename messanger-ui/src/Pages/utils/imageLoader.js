import { apiFetch } from '../utils/apiClient';

const blobCache = new Map();
const urlCache = new Map(); 
const pendingRequests = new Map();
let baseUrl = '';

export const imageLoader = {
    init(apiBaseUrl) {
        baseUrl = apiBaseUrl;
    },

    async getImageBlob(imageId) {
        if (blobCache.has(imageId)) return blobCache.get(imageId);
        if (pendingRequests.has(imageId)) return pendingRequests.get(imageId);

        // Создаем цепочку промисов для загрузки
        const promise = (async () => {
            try {
                const metadata = await apiFetch(`/api/files/${imageId}`);
                if (!metadata || !metadata.path) {
                    throw new Error("Metadata path not found");
                }
                const presignedUrl = await apiFetch(`/api/media-storage/generate-download-url`, {
                    method: 'POST',
                    body: JSON.stringify({ url: metadata.path })
                });

                const response = await fetch(presignedUrl.url);
            
                if (!response.ok) {
                    throw new Error(`Failed to fetch image bytes from S3: ${response.status}`);
                }

                const blob = await response.blob();
                blobCache.set(imageId, blob);
                return blob;

            } catch (err) {
                console.error(`Error loading image ${imageId}:`, err);
                throw err;
            }
        })().finally(() => {
            pendingRequests.delete(imageId);
        });

        pendingRequests.set(imageId, promise);
        return promise;
    },

    async getImageSrc(imageId) {
        if (!imageId) return "/images/profile-default.png";

        if (urlCache.has(imageId)) {
            return urlCache.get(imageId);
        }

        try {
            const blob = await this.getImageBlob(imageId);
            const url = URL.createObjectURL(blob);
            urlCache.set(imageId, url);
            return url;
        } catch (error) {
            return "/images/profile-default.png";
        }
    },

    revokeUrl(imageId) {
        if (urlCache.has(imageId)) {
            URL.revokeObjectURL(urlCache.get(imageId));
            urlCache.delete(imageId);
        }
    },

    clearCache() {
        urlCache.forEach((url) => URL.revokeObjectURL(url));
        urlCache.clear();
        blobCache.clear();
    }
};