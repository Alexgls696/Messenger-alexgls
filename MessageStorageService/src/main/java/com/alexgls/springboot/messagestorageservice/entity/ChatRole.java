package com.alexgls.springboot.messagestorageservice.entity;

public enum ChatRole {
    OWNER,
    ADMIN,
    MODERATOR,
    MEMBER;

    public static ChatRole getDefault() {
        return MEMBER;
    }

    public static String getTranslate(ChatRole role) {
        return switch (role) {
            case OWNER -> "Создатель";
            case ADMIN -> "Администратор";
            case MEMBER -> "Участник";
            case MODERATOR -> "Модератор";
        };
    }

    public static boolean CanEditGroupDescription(ChatRole role) {
        return role == OWNER || role == ADMIN;
    }
}