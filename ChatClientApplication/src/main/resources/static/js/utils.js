/**
 * Универсальная функция для безопасной загрузки аватаров и других изображений.
 * Загружает изображение в памяти и только после успеха отображает его в элементе.
 *
 * @param {HTMLImageElement} imgElement - HTML элемент <img>, в который нужно загрузить аватар.
 * @param {number | null} imageId - ID изображения для загрузки.
 * @param {string} apiBaseUrl - Базовый URL вашего API.
 * @param {string} fallbackSrc - Путь к стандартному изображению (заглушке) на случай ошибки или отсутствия ID.
 */
const loadAvatar = (imgElement, imageId, apiBaseUrl, fallbackSrc) => {
    // Проверка, что передан корректный элемент <img>
    if (!imgElement || imgElement.tagName !== 'IMG') {
        console.error("loadAvatar: в функцию должен быть передан HTML-элемент <img>.");
        return;
    }

    // Если нет ID изображения, сразу ставим заглушку
    if (!imageId) {
        imgElement.src = fallbackSrc;
        return;
    }

    const imageUrl = `${apiBaseUrl}/api/storage/proxy/download/by-id?id=${imageId}`;

    // Для улучшения UX можно временно показать скелетон, если он есть рядом с img
    const parent = imgElement.parentElement;
    if (parent && parent.classList.contains('avatar-container')) { // Пример
        parent.classList.add('loading'); // Добавляем класс для отображения скелетона
    }

    // Создаем временный объект Image для предзагрузки
    const tempImage = new Image();

    // Обработчик успешной загрузки
    tempImage.onload = () => {
        imgElement.src = imageUrl; // Устанавливаем src только после полной загрузки
        if (parent) parent.classList.remove('loading');
    };

    // Обработчик ошибки (сервер вернул 404, 500 и т.д.)
    tempImage.onerror = () => {
        console.warn(`Не удалось загрузить изображение с ID: ${imageId}. Установлено изображение по умолчанию.`);
        imgElement.src = fallbackSrc;
        if (parent) parent.classList.remove('loading');
    };

    // Запускаем загрузку
    tempImage.src = imageUrl;
};