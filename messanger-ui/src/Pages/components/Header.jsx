import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { imageLoader } from '../utils/imageLoader';
import '../Styles/Header.css';

import defaultProfile from '../images/profile-default.png'

function Header({ userData, avatarId, onLogout, onSearchClick, onCreateGroupClick, onProfileClick }) {
    const [isSettingsOpen, setIsSettingsOpen] = useState(false);
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
    const [isDark, setIsDark] = useState(localStorage.getItem('theme') === 'dark');
    const [avatarSrc, setAvatarSrc] = useState("/images/profile-default.png");

    const settingsRef = useRef(null);
    const mobileMenuRef = useRef(null);

    // Загрузка аватара
    useEffect(() => {
        if (avatarId) {
            imageLoader.getImageSrc(avatarId).then(setAvatarSrc);
        } else {
            setAvatarSrc(defaultProfile);
        }
    }, [avatarId]);

    // Тема
    const toggleTheme = () => {
        const newTheme = isDark ? 'light' : 'dark';
        setIsDark(!isDark);
        document.body.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
    };

    // Закрытие меню при клике вне
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (isSettingsOpen && settingsRef.current && !settingsRef.current.contains(event.target)) {
                setIsSettingsOpen(false);
            }
            if (isMobileMenuOpen && mobileMenuRef.current && !mobileMenuRef.current.contains(event.target)) {
                setIsMobileMenuOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [isSettingsOpen, isMobileMenuOpen]);

    return (
        <>
            {/* 1. ДЕСКТОПНАЯ ШАПКА */}
            <header className="main-header">
                <div className="header-buttons left">
                    <button className="header-icon-btn find-user-btn" onClick={onSearchClick} title="Поиск людей">
                        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                    </button>
                    <button className="header-icon-btn find-user-btn" onClick={onCreateGroupClick} title="Создать группу">
                        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                    </button>
                </div>

                <div className="user-info-header">
                    <h3>{userData?.name} {userData?.surname}</h3>
                    <button className="my-profile-btn" onClick={onProfileClick}>
                        <img src={avatarSrc} alt="Profile" />
                    </button>
                </div>

                <div className="header-buttons right" ref={settingsRef}>
                    <button className="header-icon-btn" onClick={() => setIsSettingsOpen(!isSettingsOpen)}>
                        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
                    </button>
                    {isSettingsOpen && (
                        <div className="dropdown-menu">
                            <div className="dropdown-item" onClick={toggleTheme}>
                                <span>{isDark ? '☀️' : '🌙'}</span> Сменить тему
                            </div>
                            <div className="dropdown-item danger" onClick={onLogout}>Выйти</div>
                        </div>
                    )}
                </div>
            </header>

            {/* 2. МОБИЛЬНАЯ ШАПКА */}
            <header className="mobile-header">
                <div className="user-info-header-mobile">
                    <button className="my-profile-btn" onClick={onProfileClick}>
                        <img src={avatarSrc} alt="Profile" />
                    </button>
                    <h3>{userData?.name}</h3>
                </div>
                <div ref={mobileMenuRef}>
                    <button className="header-icon-btn" onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}>
                        <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="3" y1="12" x2="21" y2="12"></line><line x1="3" y1="6" x2="21" y2="6"></line><line x1="3" y1="18" x2="21" y2="18"></line></svg>
                    </button>
                    {isMobileMenuOpen && (
                        <div className="dropdown-menu" id="mobileDropdownMenu">
                            <div className="dropdown-item" onClick={() => { onSearchClick(); setIsMobileMenuOpen(false); }}>
                                🔍 Поиск людей
                            </div>
                            <div className="dropdown-item" onClick={() => { onCreateGroupClick(); setIsMobileMenuOpen(false); }}>
                                ➕ Создать группу
                            </div>
                            <div className="dropdown-item" onClick={toggleTheme}>
                                <span>{isDark ? '☀️' : '🌙'}</span> Сменить тему
                            </div>
                            <div className="dropdown-item danger" onClick={onLogout}>Выйти</div>
                        </div>
                    )}
                </div>
            </header>
        </>
    );
}

export default Header;