package ru.alexgls.springboot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.alexgls.springboot.dto.GetUserDto;
import ru.alexgls.springboot.dto.UpdateUserRequest;
import ru.alexgls.springboot.dto.UserRegisterDto;
import ru.alexgls.springboot.entity.Role;
import ru.alexgls.springboot.entity.User;
import ru.alexgls.springboot.exceptions.NoSuchUserException;
import ru.alexgls.springboot.exceptions.NoSuchUserRoleException;
import ru.alexgls.springboot.mapper.UserMapper;
import ru.alexgls.springboot.repository.UserRolesRepository;
import ru.alexgls.springboot.repository.UsersRepository;

import java.util.ArrayList;
import java.util.Objects;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UsersService {

    private final PasswordEncoder passwordEncoder;
    private final UsersRepository usersRepository;
    private final UserRolesRepository userRolesRepository;

    public Iterable<GetUserDto> findAllUsers() {
        List<GetUserDto> users = new ArrayList<>();
        Iterable<User> usersFromDatabase = usersRepository.findAll();
        usersFromDatabase.forEach(user -> users.add(UserMapper.toDto(user)));
        return users;
    }

    public boolean checkCredentials(String username, String password) {
        Optional<User> userOptional = usersRepository.findByUsername(username);
        return userOptional
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(false);
    }


    public User getUserByUsername(String username) {
        return usersRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchUserException("User with username %s not found".formatted(username)));
    }

    public GetUserDto findUserByEmail(String email) {
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchUserException("Пользователь с заданным адресом электронной почты не найден."));
        return UserMapper.toDto(user);
    }

    public User findUserById(int id) {
        return usersRepository.findById(id)
                .orElseThrow(() -> new NoSuchUserException("User with id %d not found".formatted(id)));
    }


    public GetUserDto findUserDtoById(int id) {
        User user = findUserById(id);
        return new GetUserDto(user.getId(), user.getName(), user.getSurname(), user.getUsername());
    }

    public String findUserInitialsById(int id) {
        User user = findUserById(id);
        return user.getName() + " " + user.getSurname();
    }


    public List<String> getUserRoles(int id) {
        List<Role> roles = userRolesRepository.findByUserId(id);
        if (roles.isEmpty()) {
            throw new NoSuchUserRoleException("Roles for user with id %d not found".formatted(id));
        }
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }


    @Transactional
    public GetUserDto saveUser(UserRegisterDto userRegisterDto) {
        User userToSave = UserMapper.fromUserRegisterDto(userRegisterDto, passwordEncoder);
        User savedUser = usersRepository.save(userToSave);

        Role userRole = userRolesRepository.findRoleByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role ROLE_USER not found"));

        userRolesRepository.insertIntoUserRoles(savedUser.getId(), userRole.getId());

        return UserMapper.toDto(savedUser);
    }

    public void updateUserInfo(UpdateUserRequest updateUserRequest, int currentUserId) {
        User userToUpdate = findUserById(currentUserId);
        userToUpdate.setSurname(Objects.isNull(updateUserRequest.surname()) ? "" : updateUserRequest.surname());
        userToUpdate.setName(updateUserRequest.name());
        usersRepository.save(userToUpdate);
    }


    public boolean existsByUsernameOrEmail(String username, String email) {
        return usersRepository.existsByUsernameOrEmail(username, email);
    }

    public void setPasswordForUserById(int userId, String password) {
        User user = findUserById(userId);
        user.setPassword(passwordEncoder.encode(password));
        usersRepository.save(user);
    }
}