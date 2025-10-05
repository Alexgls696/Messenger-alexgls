// Этот код нужно добавить на вашу страницу (например, в /js/login.js или /js/register.js)
// Убедитесь, что он находится внутри `document.addEventListener('DOMContentLoaded', () => { ... });`

document.addEventListener('DOMContentLoaded', () => {
    const verificationInput = document.getElementById('verification-code');
    const codeBoxesContainer = document.querySelector('.code-boxes');
    const codeBoxes = document.querySelectorAll('.code-box');

// При клике на контейнер с ячейками, фокусируемся на скрытом поле ввода
    codeBoxesContainer.addEventListener('click', () => {
        verificationInput.focus();
    });

// Основная логика, которая срабатывает при вводе, вставке или удалении
    verificationInput.addEventListener('input', () => {
        const value = verificationInput.value;

        // --- ПРЕОБРАЗОВАНИЕ В ЗАГЛАВНЫЕ БУКВЫ ---
        // Это сработает для букв, цифры останутся без изменений.
        const uppercaseValue = value.toUpperCase();

        // Обновляем значение в самом поле ввода, если оно изменилось
        if (verificationInput.value !== uppercaseValue) {
            verificationInput.value = uppercaseValue;
        }

        // Распределяем символы по видимым ячейкам
        for (let i = 0; i < codeBoxes.length; i++) {
            const box = codeBoxes[i];
            const char = uppercaseValue[i];

            if (char) {
                box.textContent = char;
                box.classList.add('filled');
            } else {
                box.textContent = '';
                box.classList.remove('filled');
            }
        }
    });

// Дополнительно: при фокусе на поле ввода можно подсветить первую пустую ячейку (опционально)
    verificationInput.addEventListener('focus', () => {
        // Можно добавить стиль для фокуса, например, на первую ячейку
        setTimeout(() => { // setTimeout, чтобы фокус успел примениться
            if (!verificationInput.value) {
                codeBoxes[0].classList.add('filled'); // Используем стиль "filled" для подсветки
            }
        }, 0);
    });

    verificationInput.addEventListener('blur', () => {
        // Убираем все подсветки при потере фокуса
        codeBoxes.forEach(box => {
            if (!box.textContent) {
                box.classList.remove('filled');
            }
        });
    });
});