import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import {useEffect} from 'react'
import LoginPage from './Pages/Login';
import ChatPage from './Pages/Chat';
import ProtectedRoute from './Pages/components/ProtectedRoute'
import Register from './Pages/Register'
import SimpleLogin from './Pages/SimpleLogin';
import SetupProfile from './Pages/SetupProfile';

import './Pages/Styles/Global.css';

function App() {
    useEffect(() => {
        const savedTheme = localStorage.getItem('theme') || 'light';
        document.documentElement.setAttribute('data-theme', savedTheme);
        document.body.setAttribute('data-theme', savedTheme);
    }, []);

    return (
        <Router>
            <Routes>
                {/* Публичный маршрут: Логин */}
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<Register />} />
                <Route path="/simple-login" element={<SimpleLogin />} />

                {/* Защищенный маршрут: Чат (Главная) */}
                <Route
                    path="/"
                    element={
                        <ProtectedRoute>
                            <ChatPage />
                        </ProtectedRoute>
                    }
                />
                <Route path="*" element={<Navigate to="/" replace />} />
                <Route path='setup-profile'
                    element={
                        <ProtectedRoute>
                            <SetupProfile />
                        </ProtectedRoute>
                    } />
            </Routes>
        </Router>
    );
}

export default App;