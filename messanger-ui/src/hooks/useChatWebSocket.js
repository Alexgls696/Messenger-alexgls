import { useEffect, useRef, useState } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { handleTokenRefresh } from '../Pages/utils/apiClient'

export const useChatWebSocket = (url, onMessageReceived, onReadStatus, onDeleteEvent) => {
    const stompClient = useRef(null);
    const [isConnected, setIsConnected] = useState(false);

    // Используем рефы для обработчиков, чтобы избежать замыканий
    const refs = useRef({ onMessageReceived, onReadStatus, onDeleteEvent });
    useEffect(() => {
        refs.current = { onMessageReceived, onReadStatus, onDeleteEvent };
    });

    const connect = () => {
        const accessToken = localStorage.getItem('accessToken');
        if (!accessToken) return;

        const socket = new SockJS(`${url}/ws-chat?token=${accessToken}`);
        const client = Stomp.over(socket);
        client.debug = null;

        client.connect({}, () => {
            setIsConnected(true);

            client.subscribe('/user/queue/messages', (m) =>
                refs.current.onMessageReceived(JSON.parse(m.body))
            );
            client.subscribe('/user/queue/read-status', (m) =>
                refs.current.onReadStatus(JSON.parse(m.body))
            );
            client.subscribe('/user/queue/delete-event', (m) =>
                refs.current.onDeleteEvent(JSON.parse(m.body))
            );
        }, async (error) => {
            setIsConnected(false);
            console.warn("WebSocket error, attempting to refresh token and reconnect... ");
            await handleTokenRefresh();
            setTimeout(connect, 3000);
        });

        stompClient.current = client;
    };

    useEffect(() => {
        connect();
        return () => {
            if (stompClient.current) stompClient.current.disconnect();
        };
    }, [url]);

    // ФУНКЦИЯ ОТПРАВКИ (проверьте имя!)
    const sendMessage = (payload) => {
        if (stompClient.current && isConnected) {
            stompClient.current.send("/app/chat.send", {}, JSON.stringify(payload));
        } else {
            console.error("WebSocket is not connected");
        }
    };

    return { isConnected, sendMessage };
};