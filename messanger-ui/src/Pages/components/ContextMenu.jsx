import React, { useLayoutEffect, useRef, useState } from 'react';

const ContextMenu = ({ x, y, options, onClose }) => {
    const menuRef = useRef(null);
    const [coords, setCoords] = useState({ top: y, left: x, opacity: 0 });

    useLayoutEffect(() => {
        if (menuRef.current) {
            const screenW = window.innerWidth;
            const screenH = window.innerHeight;
            const menuW = menuRef.current.offsetWidth;
            const menuH = menuRef.current.offsetHeight;

            let finalX = x;
            let finalY = y;

            // Если меню выходит за правый край — смещаем влево
            if (x + menuW > screenW) {
                finalX = x - menuW;
            }

            // Если меню выходит за нижний край — смещаем вверх
            if (y + menuH > screenH) {
                finalY = y - menuH;
            }

            // Предотвращаем выход за левый или верхний край (на случай маленьких экранов)
            finalX = Math.max(10, finalX);
            finalY = Math.max(10, finalY);

            setCoords({ top: finalY, left: finalX, opacity: 1 });
        }
    }, [x, y]);

    return (
        <>
            <div className="context-menu-backdrop" onClick={onClose} onContextMenu={(e) => { e.preventDefault(); onClose(); }} />
            <div
                ref={menuRef}
                className="context-menu"
                style={{
                    top: coords.top,
                    left: coords.left,
                    opacity: coords.opacity, // Прячем меню, пока не вычислим координаты
                    visibility: coords.opacity ? 'visible' : 'hidden'
                }}
            >
                {options.map((opt, i) => (
                    <div
                        key={i}
                        className={`context-menu-item ${opt.danger ? 'danger' : ''}`}
                        onClick={() => {
                            opt.action();
                            onClose();
                        }}
                    >
                        {opt.label}
                    </div>
                ))}
            </div>
        </>
    );
};

export default ContextMenu;