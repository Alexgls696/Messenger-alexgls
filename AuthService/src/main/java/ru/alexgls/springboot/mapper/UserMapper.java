package ru.alexgls.springboot.mapper;

import org.springframework.security.crypto.password.PasswordEncoder;
import ru.alexgls.springboot.dto.GetUserDto;
import ru.alexgls.springboot.dto.UserRegisterDto;
import ru.alexgls.springboot.entity.User;

import java.util.Date;
import java.util.Objects;

public final class UserMapper {

    private UserMapper() {

    }

    public static GetUserDto toDto(User user) {
        Date lastSeen = Objects.isNull(user.getLastSeenAt()) ? null : user.getLastSeenAt();
        return new GetUserDto(user.getId(), user.getName(), user.getSurname(), user.getUsername(), lastSeen);
    }

    public static User fromUserRegisterDto(UserRegisterDto userRegisterDto, PasswordEncoder passwordEncoder) {
        User user = new User();
        user.setEmail(userRegisterDto.email());
        user.setPassword(passwordEncoder.encode(userRegisterDto.password()));
        user.setUsername(userRegisterDto.username());
        user.setName(userRegisterDto.name());
        user.setSurname(userRegisterDto.surname());
        return user;
    }

}
