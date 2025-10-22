const imageLoader = (() => {
    const imageCache = new Map();
    const pendingRequests = new Map();

    /**
     * Получает безопасный SRC для изображения.
     * Если изображение есть в кэше, возвращает его мгновенно.
     * Если нет, загружает, кэширует и затем возвращает.
     * @param {number} imageId - ID изображения.
     * @param {string} apiBaseUrl - Базовый URL API.
     * @param {string} authToken - Токен авторизации.
     * @returns {Promise<string>} Промис, который разрешается в SRC для тега <img>.
     */
    const getImageSrc = (imageId, apiBaseUrl, authToken) => {
        // Проверяем, есть ли уже готовый URL в кэше
        if (imageCache.has(imageId)) {
            return Promise.resolve(imageCache.get(imageId));
        }

        // Проверяем, не загружается ли это изображение прямо сейчас
        if (pendingRequests.has(imageId)) {
            return pendingRequests.get(imageId);
        }

        const imageUrl = `${apiBaseUrl}/api/storage/proxy/download/by-id?id=${imageId}`;

        // Если нет, создаем новый промис для загрузки
        const promise = fetch(imageUrl, {
            headers: { 'Authorization': `Bearer ${authToken}` }
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Ошибка загрузки изображения: ${response.status}`);
                }
                return response.blob(); // Получаем данные как Blob
            })
            .then(blob => {
                const blobUrl = URL.createObjectURL(blob); // Создаем локальный URL
                imageCache.set(imageId, blobUrl); // Кэшируем результат
                pendingRequests.delete(imageId); // Удаляем из ожидающих запросов
                return blobUrl;
            })
            .catch(error => {
                console.error(`Не удалось загрузить изображение с ID ${imageId}:`, error);
                pendingRequests.delete(imageId);
                return '/images/image-error.png';
            });

        pendingRequests.set(imageId, promise);
        return promise;
    };

    return {
        getImageSrc
    };
})();