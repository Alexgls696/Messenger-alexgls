package ru.alexgls.springboot.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_blacklist")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBlacklist {
    @EmbeddedId
    private UserBlacklistId id;
}
