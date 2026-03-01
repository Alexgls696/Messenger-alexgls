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

        const imageUrl = `${baseUrl}/api/storage/proxy/download/by-id?id=${imageId}`;
        const authToken = localStorage.getItem('accessToken');

        const promise = fetch(imageUrl, {
            headers: { 'Authorization': `Bearer ${authToken}` }
        })
        .then(res => {
            if (!res.ok) throw new Error();
            return res.blob();
        })
        .then(blob => {
            blobCache.set(imageId, blob);
            pendingRequests.delete(imageId);
            return blob;
        })
        .catch(err => {
            pendingRequests.delete(imageId);
            throw err;
        });

        pendingRequests.set(imageId, promise);
        return promise;
    },

    async getImageSrc(imageId) {
        if (!imageId) return "/images/profile-default.png";

        // ИЗМЕНЕНИЕ: Если мы уже создавали URL для этого ID, возвращаем его
        if (urlCache.has(imageId)) {
            return urlCache.get(imageId);
        }

        try {
            const blob = await this.getImageBlob(imageId);
            const url = URL.createObjectURL(blob);
            urlCache.set(imageId, url); // Сохраняем стабильную ссылку
            return url;
        } catch (error) {
            return "/images/profile-default.png";
        }
    },

    revokeUrl(url) {
        
    }
};