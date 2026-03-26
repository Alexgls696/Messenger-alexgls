import { useCallback, useRef } from 'react';

export const useLongPress = (onLongPress, onClick, { delay = 500 } = {}) => {
    const timeout = useRef();
    const isLongPressActive = useRef(false);

    const start = useCallback((event) => {
        // Запоминаем координаты тача для контекстного меню
        const touch = event.touches ? event.touches[0] : event;
        const coords = { clientX: touch.clientX, clientY: touch.clientY };

        isLongPressActive.current = false;
        
        timeout.current = setTimeout(() => {
            onLongPress(coords, event);
            isLongPressActive.current = true;
            if (window.navigator.vibrate) {
                window.navigator.vibrate(50); // Легкая вибрация при срабатывании
            }
        }, delay);
    }, [onLongPress, delay]);

    const stop = useCallback((event) => {
        if (timeout.current) {
            clearTimeout(timeout.current);
        }
        // Если это был короткий клик — можно вызвать обычный onClick
        if (!isLongPressActive.current && onClick) {
            // onClick(event); 
        }
    }, [onClick]);

    return {
        onTouchStart: start,
        onTouchEnd: stop,
        onTouchMove: stop,
        onMouseDown: start,
        onMouseUp: stop,
        onMouseLeave: stop,
    };
};