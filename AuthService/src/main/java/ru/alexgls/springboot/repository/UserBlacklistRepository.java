package ru.alexgls.springboot.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.alexgls.springboot.entity.UserBlacklist;
import ru.alexgls.springboot.entity.UserBlacklistId;

import java.util.List;

public interface UserBlacklistRepository extends CrudRepository<UserBlacklist, UserBlacklistId> {
    @Query("select id.blockedUserId from UserBlacklist where id.userId = :userId")
    List<Integer> findAllByUserId(@Param("userId") int userId);
}