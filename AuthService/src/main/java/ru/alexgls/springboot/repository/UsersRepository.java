package ru.alexgls.springboot.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.alexgls.springboot.dto.GetUserDto;
import ru.alexgls.springboot.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsersRepository extends CrudRepository<User, Integer> {
    Optional<User> findByUsername(String username);

    Optional<User>findByEmail(String email);

    Boolean existsByUsernameOrEmail(String username, String email);

    @Query(nativeQuery = true, value = "select * from users where username ilike (concat(:username,'%'))")
    List<User> findAllByUsername(String username);
}
