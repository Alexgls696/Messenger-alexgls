const imageLoader = (() => {
    // ИЗМЕНЕНИЕ: Теперь кеш хранит Promise<Blob>, а не Blob URL
    const blobCache = new Map();
    const pendingRequests = new Map();

    /**
     * Загружает Blob изображения и кэширует его. Это центральная функция.
     * @param {number} imageId
     * @param {string} apiBaseUrl
     * @param {string} authToken
     * @returns {Promise<Blob>} Промис, который разрешается в Blob.
     */
    const fetchAndCacheBlob = (imageId, apiBaseUrl, authToken) => {
        // Проверяем, не загружается ли это изображение прямо сейчас
        if (pendingRequests.has(imageId)) {
            return pendingRequests.get(imageId);
        }

        const imageUrl = `${apiBaseUrl}/api/storage/proxy/download/by-id?id=${imageId}`;

        const promise = fetch(imageUrl, {
            headers: { 'Authorization': `Bearer ${authToken}` }
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Ошибка загрузки изображения: ${response.status}`);
                }
                return response.blob();
            })
            .then(blob => {
                blobCache.set(imageId, blob); // Кэшируем сам Blob
                pendingRequests.delete(imageId); // Запрос завершен
                return blob;
            })
            .catch(error => {
                console.error(`Не удалось загрузить Blob с ID ${imageId}:`, error);
                pendingRequests.delete(imageId);
                // Пробрасываем ошибку дальше, чтобы .catch() в вызывающей функции мог ее обработать
                throw error;
            });

        pendingRequests.set(imageId, promise);
        return promise;
    };

    const getImageBlob = (imageId, apiBaseUrl, authToken) => {
        if (blobCache.has(imageId)) {
            return Promise.resolve(blobCache.get(imageId));
        }
        return fetchAndCacheBlob(imageId, apiBaseUrl, authToken);
    };

    const getImageSrc = async (imageId, apiBaseUrl, authToken) => {
        try {
            const blob = await getImageBlob(imageId, apiBaseUrl, authToken);
            return URL.createObjectURL(blob); // Создаем новый, свежий URL
        } catch (error) {
            // В случае ошибки загрузки Blob возвращаем картинку-заглушку
            return '/images/image-error.png';
        }
    };

    const revokeUrl = (url) => {
        if (url && url.startsWith('blob:')) {
            URL.revokeObjectURL(url);
        }
    };

    return {
        getImageSrc,
        getImageBlob,
        revokeUrl
    };
})();