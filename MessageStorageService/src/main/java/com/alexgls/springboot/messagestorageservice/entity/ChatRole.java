package com.alexgls.springboot.messagestorageservice.entity;

public enum ChatRole {
    OWNER,
    ADMIN,
    MEMBER;

    public static ChatRole getDefault() {
        return MEMBER;
    }
}