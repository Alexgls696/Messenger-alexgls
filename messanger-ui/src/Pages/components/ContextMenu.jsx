import React from 'react';

const ContextMenu = ({ x, y, options, onClose }) => {
    return (
        <>
            {/* Невидимый слой на весь экран */}
            <div
                className="context-menu-backdrop"
                onClick={onClose}
                onContextMenu={(e) => {
                    e.preventDefault(); // Запрещаем системное меню
                    onClose();         // Закрываем наше меню
                }}
            />

            {/* Само меню */}
            <div
                className="context-menu"
                style={{
                    top: y,
                    left: x
                }}
                onClick={(e) => e.stopPropagation()} // Чтобы клик по пунктам не проваливался в подложку
            >
                {options.map((opt, idx) => (
                    <div
                        key={idx}
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