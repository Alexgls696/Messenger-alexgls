import { useEffect, useRef, useState, useCallback } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { handleTokenRefresh } from '../Pages/utils/apiClient';

export const useChatWebSocket = (url, onMessageReceived, onReadStatus, onDeleteEvent, onNotificationReceived, onMessageUpdate) => {
    const stompClient = useRef(null);
    const socketRef = useRef(null);
    const [isConnected, setIsConnected] = useState(false);
    
    const connectionLock = useRef(false); 
    const reconnectTimeoutRef = useRef(null);

    const refs = useRef({ onMessageReceived, onReadStatus, onDeleteEvent, onNotificationReceived, onMessageUpdate });
    useEffect(() => {
        refs.current = { onMessageReceived, onReadStatus, onDeleteEvent, onNotificationReceived, onMessageUpdate };
    });

    const disconnect = useCallback(() => {
        if (reconnectTimeoutRef.current) {
            clearTimeout(reconnectTimeoutRef.current);
        }
        
        // Порядок важен: сначала закрываем Stomp, потом сокет
        if (stompClient.current) {
            try {
                // Старая версия stompjs требует callback или пустой объект
                stompClient.current.disconnect(() => {}, {});
            } catch (e) {}
            stompClient.current = null;
        }

        if (socketRef.current) {
            try {
                socketRef.current.close();
            } catch (e) {}
            socketRef.current = null;
        }

        setIsConnected(false);
    }, []);

    const connect = useCallback(async (reason = "initial") => {
        if (connectionLock.current) {
            console.log(`[WS] Connection ignored (lock active), reason: ${reason}`);
            return;
        }

        const accessToken = localStorage.getItem('accessToken');
        if (!accessToken) return;

        connectionLock.current = true;
        console.log(`[WS] Attempting to connect. Reason: ${reason}`);

        disconnect(); // Полная очистка перед новым стартом

         const socket = new SockJS(`${url}/ws-chat?token=${accessToken}`, null, {
            transports: ['websocket'],
            timeout: 10000
        });
        socketRef.current = socket;
        
        const client = Stomp.over(socket);
        client.heartbeat.outgoing = 10000;
        client.heartbeat.incoming = 10000;
        client.debug = null;

        const headers = { 'Authorization': `Bearer ${accessToken}` };

        client.connect(headers,
            () => {
                console.log('[WS] Connected successfully');
                setIsConnected(true);
                connectionLock.current = false; // Снимаем замок только при успехе

                // Подписки
                const subs = [
                    ['/user/queue/messages', refs.current.onMessageReceived],
                    ['/user/queue/updated-message', refs.current.onMessageUpdate],
                    ['/user/queue/read-status', refs.current.onReadStatus],
                    ['/user/queue/delete-event', refs.current.onDeleteEvent],
                    ['/user/queue/notifications', refs.current.onNotificationReceived]
                ];

                subs.forEach(([queue, action]) => {
                    client.subscribe(queue, (m) => action(JSON.parse(m.body)));
                });
            },
            async (error) => {
                setIsConnected(false);
                console.warn("[WS] Connection lost or Handshake failed.");

                try {
                    // Используем ваш handleTokenRefresh
                    await handleTokenRefresh();
                    console.log("[WS] Token refreshed. Reconnecting in 1s...");
                    
                    reconnectTimeoutRef.current = setTimeout(() => {
                        connectionLock.current = false; // Освобождаем перед вызовом
                        connect("reconnect-after-refresh");
                    }, 1000);

                } catch (err) {
                    console.error("[WS] Refresh failed:", err.message);
                    connectionLock.current = false;
                    
                    if (err.message !== "Session expired" && err.message !== "No refresh token") {
                        reconnectTimeoutRef.current = setTimeout(() => connect("retry-after-network-error"), 5000);
                    }
                }
            }
        );

        stompClient.current = client;
    }, [url, disconnect]);

    // 1. Эффект инициализации
    useEffect(() => {
        connect("mount");
        return () => disconnect();
    }, [connect, disconnect]);

    // 2. Эффект Visibility (сон/переключение вкладок)
    useEffect(() => {
        const handleVisibilityChange = () => {
            if (document.visibilityState === 'visible') {
                setTimeout(() => {
                    const isReallyConnected = stompClient.current && stompClient.current.connected;
                    
                    if (!isReallyConnected && !connectionLock.current) {
                        console.log("[WS] Tab visible and not connected. Reconnecting...");
                        connect("visibility-change");
                    }
                }, 500);
            }
        };

        window.addEventListener('visibilitychange', handleVisibilityChange);
        window.addEventListener('focus', handleVisibilityChange);

        return () => {
            window.removeEventListener('visibilitychange', handleVisibilityChange);
            window.removeEventListener('focus', handleVisibilityChange);
        };
    }, [connect]);

    useEffect(() => {
        const handleOnline = () => {
            if (!connectionLock.current) connect("online-event");
        };
        window.addEventListener('online', handleOnline);
        return () => window.removeEventListener('online', handleOnline);
    }, [connect]);

    const sendMessage = (payload) => {
        if (stompClient.current && stompClient.current.connected) {
            stompClient.current.send("/app/chat.send", {}, JSON.stringify(payload));
        } else {
            console.error("[WS] Send failed: not connected.");
        }
    };

    return { isConnected, sendMessage };
};