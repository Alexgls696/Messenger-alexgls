package com.alexgls.springboot.messagestorageservice.entity;

public enum ChatRole {
    OWNER,
    ADMIN,
    MEMBER;

    public static ChatRole getDefault() {
        return MEMBER;
    }

    public static String getTranslate(ChatRole role) {
        return switch (role) {
            case OWNER -> "Создатель";
            case ADMIN -> "Администратор";
            case MEMBER -> "Участник";
        };
    }
}