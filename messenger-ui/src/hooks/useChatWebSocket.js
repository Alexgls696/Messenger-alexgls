import { useEffect, useRef, useState, useCallback } from 'react';
import SockJS from 'sockjs-client';
import Stomp, { client } from 'stompjs';
import { handleTokenRefresh } from '../Pages/utils/apiClient';

export const useChatWebSocket = (url, onMessageReceived, onMessageRead, onDeleteEvent, onNotificationReceived, onMessageUpdate, onUserOnlineChanged, isConnected, setIsConnected) => {
    const stompClient = useRef(null);
    const socketRef = useRef(null);
    const pingIntervalRef = useRef(null);

    const connectionLock = useRef(false);
    const reconnectTimeoutRef = useRef(null);

    const refs = useRef({ onMessageReceived, onMessageRead, onDeleteEvent, onNotificationReceived, onMessageUpdate, onUserOnlineChanged });
    useEffect(() => {
        refs.current = { onMessageReceived, onMessageRead, onDeleteEvent, onNotificationReceived, onMessageUpdate, onUserOnlineChanged };
    });

    const disconnect = useCallback(() => {
        if (reconnectTimeoutRef.current) {
            clearTimeout(reconnectTimeoutRef.current);
        }

        if (pingIntervalRef.current) {
            clearInterval(pingIntervalRef.current);
        }

        if (stompClient.current) {
            try {
                stompClient.current.disconnect(() => { }, {});
            } catch (e) { }
            stompClient.current = null;
        }

        if (socketRef.current) {
            try {
                socketRef.current.close();
            } catch (e) { }
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

        disconnect();

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
                connectionLock.current = false;

                client.subscribe('/user/queue/messages', (m) => {
                    refs.current.onMessageReceived?.(JSON.parse(m.body));
                });

                client.subscribe('/user/queue/updated-message', (m) => {
                    refs.current.onMessageUpdate?.(JSON.parse(m.body));
                });

                client.subscribe('/user/queue/read-status', (m) => {
                    console.log("[WS] Received read-status:", m.body); 
                    refs.current.onMessageRead?.(JSON.parse(m.body));
                });

                client.subscribe('/user/queue/delete-event', (m) => {
                    refs.current.onDeleteEvent?.(JSON.parse(m.body));
                });

                client.subscribe('/user/queue/notifications', (m) => {
                    refs.current.onNotificationReceived?.(JSON.parse(m.body));
                });

                client.subscribe('/user/queue/online-changed', (m) => {
                    refs.current.onUserOnlineChanged?.(JSON.parse(m.body));
                });

                // Пинг
                pingIntervalRef.current = setInterval(() => {
                    if (client.connected) {
                        client.send("/app/ping", {}, "Ping");
                    }
                }, 30000);
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


    useEffect(() => {
        connect("mount");
        return () => disconnect();
    }, [connect, disconnect]);

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