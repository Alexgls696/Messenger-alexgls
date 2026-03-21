import React from 'react';

const ContextMenu = ({ x, y, options, onClose }) => {

    return (
        <>
            <div
                className="context-menu-backdrop"
                onClick={onClose}
                onContextMenu={(e) => {
                    e.preventDefault();
                    onClose();
                }}
            />

            <div
                className="context-menu"
                style={{
                    top: y,
                    left: x
                }}
                onClick={(e) => e.stopPropagation()}
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