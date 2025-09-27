package ru.alexgls.springboot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UsersService {

    private final PasswordEncoder passwordEncoder;
    private final UsersRepository usersRepository;
    private final UserRolesRepository userRolesRepository;

    public Mono<Boolean> checkCredentials(String username, String password) {
        return usersRepository.findByUsername(username)
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .switchIfEmpty(Mono.just(false));
    }

    public Mono<User> getUserByUsername(String username) {
        return usersRepository.findByUsername(username)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new UsernameNotFoundException("User with username %s not found".formatted(username)))));
    }

    public Mono<User> findUserById(int id) {
        return usersRepository.findById(id)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new NoSuchUserException("User with id %d not found".formatted(id)))));
    }

    public Mono<GetUserDto> findUserDtoById(int id) {
        return usersRepository.findById(id)
                .map(user -> new GetUserDto(user.getId(), user.getName(), user.getSurname(), user.getUsername()))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new NoSuchUserException("User with id %d not found".formatted(id)))));
    }

    public Mono<String> findUserInitialsById(int id) {
        return usersRepository.findById(id)
                .flatMap(user -> Mono.just(user.getName() + " " + user.getSurname()))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new NoSuchUserException("User with id %d not found".formatted(id)))));
    }

    public Flux<String> getUserRoles(int id) {
        return userRolesRepository.findByUserId(id)
                .map(Role::getName)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new NoSuchUserRoleException("Roles for user with id %d not found".formatted(id)))));
    }

    @Transactional
    public Mono<GetUserDto> saveUser(UserRegisterDto userRegisterDto) {
        User user = UserMapper.fromUserRegisterDto(userRegisterDto, passwordEncoder);
        Mono<Role> monoUserRole = userRolesRepository.findRoleByName("ROLE_USER");
        Mono<User> monoSavedUser = usersRepository.save(user);
        return Mono.zip(monoSavedUser, monoUserRole)
                .flatMap(tuple -> {
                    User savedUser = tuple.getT1();
                    Role savedRole = tuple.getT2();
                    return userRolesRepository.insertIntoUserRoles(savedUser.getId(), savedRole.getId())
                            .thenReturn(UserMapper.toDto(savedUser));
                });
    }

    public Mono<Void> updateUserInfo(UpdateUserRequest updateUserRequest, int currentUserId) {
        return usersRepository.findById(updateUserRequest.id())
                .flatMap(foundUser -> {
                    if (foundUser.getId() != currentUserId) {
                        return Mono.error(new AccessDeniedException("У вас нет доступа для выполнения данного действия"));
                    }
                    foundUser.setSurname(Objects.isNull(updateUserRequest.surname()) ? "" : updateUserRequest.surname());
                    foundUser.setName(updateUserRequest.name());
                    return usersRepository.save(foundUser);
                }).then();
    }

    public Flux<GetUserDto> findAllUsers() {
        return usersRepository.findAll()
                .map(UserMapper::toDto);
    }

    public Mono<Boolean> existsByUsernameOrEmail(String username, String email) {
        return usersRepository.existsByUsernameOrEmail(username, email);
    }

}