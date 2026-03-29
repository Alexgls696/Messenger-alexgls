// src/utils/messageUtils.js
export const generateTempId = () => 'temp-' + Date.now() + '-' + Math.floor(Math.random() * 10000);

export const isDocumentType = (mimeType) => {
    const documentMimeTypes = [
        'application/pdf', 'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'text/plain', 'application/rtf'
    ];
    return documentMimeTypes.includes(mimeType);
};