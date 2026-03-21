import React from 'react';

const ConfirmationModal = ({ isOpen, title, message, onConfirm, onCancel }) => {
    if (!isOpen) return null;

    useEffect(() => {
        const handleKeydown = (event) => {
            console.log(event.key)
            if (event.key === 'Escape') {
                onClose()
            }
        }

        document.addEventListener('keydown', handleKeydown)

        return () => {
            document.removeEventListener('keydown', handleKeydown);
        }
    }, [onClose])

    return (
        <div className="modal" onClick={(e) => e.target.className === 'modal' && onCancel()}>
            <div className="modal-content confirmation-modal-content">
                <div className="modal-header">
                    <h2>{title || 'Подтверждение'}</h2>
                </div>
                <div className="modal-body">
                    <p className="confirmation-text">{message}</p>
                </div>
                <div className="modal-footer">
                    <button className="profile-cancel-btn" onClick={onCancel}>Нет</button>
                    <button className="profile-save-btn danger" onClick={onConfirm}>Да</button>
                </div>
            </div>
        </div>
    );
};

export default ConfirmationModal;