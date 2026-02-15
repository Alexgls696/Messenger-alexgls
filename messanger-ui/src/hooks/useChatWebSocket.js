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
        client.heartbeat.outgoing = 10000;
        client.heartbeat.incoming = 10000;
        client.debug = null;

        client.connect({}, () => {
            console.log('Connected')
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

    useEffect(() => {
        const handleVisibilityChange = () => {
            // Если пользователь открыл вкладку/разблокировал экран
            if (document.visibilityState === 'visible') {
                console.log("Вкладка активна, проверка соединения...");

                // Проверяем, жива ли сессия
                if (!stompClient.current || !stompClient.current.connected) {
                    console.log("Соединение потеряно после сна, переподключаюсь...");
                    connect();
                }
            }
        };

        document.addEventListener('visibilitychange', handleVisibilityChange);
        window.addEventListener('focus', handleVisibilityChange);

        return () => {
            document.removeEventListener('visibilitychange', handleVisibilityChange);
            window.removeEventListener('focus', handleVisibilityChange);
        };
    }, []);

    useEffect(() => {
        window.addEventListener('online', connect);
        return () => window.removeEventListener('online', connect);
    }, []);

    const sendMessage = (payload) => {
        if (stompClient.current && isConnected) {
            stompClient.current.send("/app/chat.send", {}, JSON.stringify(payload));
        } else {
            console.error("WebSocket is not connected");
        }
    };

    return { isConnected, sendMessage };
};