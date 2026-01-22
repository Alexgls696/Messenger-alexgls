const confirmModal = (() => {
    const modal = document.getElementById('confirmationModal');
    const messageEl = document.getElementById('confirmationMessage');
    const yesBtn = document.getElementById('acceptConfirmBtn');
    const noBtn = document.getElementById('cancelConfirmBtn');

    // Функция закрытия
    const close = () => {
        modal.classList.add('hidden');
    };

    // Инициализация закрытия по клику на фон и кнопку "Нет"
    // Делаем это один раз, чтобы не дублировать обработчики
    if (modal) {
        noBtn.addEventListener('click', close);
        modal.addEventListener('click', (e) => {
            if (e.target === modal) close();
        });
    }

    /**
     * Открывает окно подтверждения.
     * @param {string} message - Текст вопроса.
     * @param {Function} onConfirm - Функция, которая выполнится при нажатии "Да".
     */
    const open = (message, onConfirm) => {
        if (!modal) return;

        messageEl.textContent = message;
        modal.classList.remove('hidden');

        // Переопределяем поведение кнопки "Да" для текущего вызова
        yesBtn.onclick = () => {
            onConfirm(); // Выполняем переданное действие
            close();     // Закрываем окно
        };
    };

    return { open };
})();