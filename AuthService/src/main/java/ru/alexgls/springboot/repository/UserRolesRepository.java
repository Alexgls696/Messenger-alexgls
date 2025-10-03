package ru.alexgls.springboot.repository;


import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.alexgls.springboot.entity.Role;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRolesRepository extends CrudRepository<Role, Integer> {
    @Query(value = "select r.* from roles r join users_roles ur on r.id = ur.role_id where ur.user_id = :userId", nativeQuery = true)
    List<Role> findByUserId(@Param("userId") int userId);

    @Modifying
    @Query(value = "insert into users_roles (role_id, user_id) VALUES (:roleId,:userId)", nativeQuery = true)
    void insertIntoUserRoles(@Param("userId") int userId, @Param("roleId") int roleId);

    @Modifying
    @Query(value = "delete from users_roles ur where user_id = :userId", nativeQuery = true)
    void removeAllByUserId(@Param("userId") int userId);

     Optional<Role> findRoleByName(String name);
}
