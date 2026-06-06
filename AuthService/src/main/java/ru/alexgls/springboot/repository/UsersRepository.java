package ru.alexgls.springboot.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.alexgls.springboot.dto.GetUserDto;
import ru.alexgls.springboot.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsersRepository extends CrudRepository<User, Integer> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    @Query(nativeQuery = true, value = "select * from users where username ilike (concat(:username,'%'))")
    List<User> findAllByUsername(String username);

    @Query("from User where id in :userIds " +
            "order by surname")
    List<User> findAllById(@Param("userIds") Iterable<Integer> userIds);


    /**
     * Вариант 1. Поиск по одному ключевому слову (username, name, surname)
     */
    @Query("from User u where " +
            "u.username = :part " +
            "or u.name = :part " +
            "or u.surname = :part " +
            "or (u.username ilike concat(:part, '%') and length(:part) >= 3) " +
            "or (u.name ilike concat(:part, '%') and length(:part) >= 3) " +
            "or (u.surname ilike concat(:part, '%') and length(:part) >= 3)")
    List<User> searchUsersSingle(@Param("part") String part);

    /**
     * Вариант 2. Поиск по двум ключевым словам (имя + фамилия в любом порядке)
     */
    @Query("from User u where " +
            "(u.name ilike concat(:part1, '%') and u.surname ilike concat(:part2, '%')) " +
            "or (u.name ilike concat(:part2, '%') and u.surname ilike concat(:part1, '%'))")
    List<User> searchUsersDouble(@Param("part1") String part1, @Param("part2") String part2);

    /**
     * Точка входа для поиска. В зависимости от количества слов вызывает нужный метод БД,
     * полностью исключая передачу null-параметров.
     */
    default List<User> findAllByKeyword(String key) {
        if (key == null) {
            return List.of();
        }

        // Очистка от пробелов
        String cleanKey = key.trim().replaceAll("\\s+", " ");
        if (cleanKey.isEmpty()) {
            return List.of();
        }

        String[] parts = cleanKey.split(" ");

        if (parts.length == 1) {
            // Если введено 1 слово - используем легкий точечный запрос
            return searchUsersSingle(parts[0]);
        } else {
            // Если введено 2 и более слов - используем поиск по паре имя/фамилия
            return searchUsersDouble(parts[0], parts[1]);
        }
    }
}
