import React, { useState } from "react";

const DeleteMessagesConfirmationModal = ({ isOpen, title, message, onConfirm, onCancel, forAll }) => {
    const [isDeleteForEveryone, setIsDeleteForEveryone] = useState(false);

    if (!isOpen) return null;

    const handleConfirm = () => {
        onConfirm(isDeleteForEveryone);
        setIsDeleteForEveryone(false);
    };

    return (
        <div className="modal" onClick={(e) => e.target.className === 'modal' && onCancel()}>
            <div className="modal-content confirmation-modal-content">
                <div className="modal-header">
                    <h2>{title || 'Подтверждение'}</h2>
                </div>
                <div className="modal-body">
                    <p className="confirmation-text">{message}</p>

                    {forAll && (
                        <label className="delete-forall-label">
                            <input
                                type="checkbox"
                                checked={isDeleteForEveryone}
                                onChange={(e) => setIsDeleteForEveryone(e.target.checked)}
                            />
                            <span>Удалить для всех?</span>
                        </label>
                    )}
                </div>
                <div className="modal-footer">
                    <button className="profile-cancel-btn" onClick={onCancel}>Нет</button>
                    <button className="profile-save-btn danger" onClick={handleConfirm}>Да</button>
                </div>
            </div>
        </div>
    );
};

export default DeleteMessagesConfirmationModal;