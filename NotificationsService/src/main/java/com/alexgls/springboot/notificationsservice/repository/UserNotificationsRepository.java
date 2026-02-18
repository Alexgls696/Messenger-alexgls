package com.alexgls.springboot.notificationsservice.repository;

import com.alexgls.springboot.notificationsservice.entity.UserNotification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNotificationsRepository extends CrudRepository<UserNotification, Long> {

    @Query("select count(*) from UserNotification where id.userId = :userId and read = false")
    Integer findAllByUserIdWhereUnread(@Param("userId") int userId);

    @Modifying
    @Query("update UserNotification set read = true where id.userId = :userId and read = false")
    void readAllByUserIdWhereUnread(@Param("userId") int userId);

    @Modifying
    @Query("update UserNotification set read = true where id.userId = :userId and id.notificationId = :notificationId")
    void readByNotificationId(@Param("notificationId") long notificationId, @Param("userId") int userId);

    @Modifying
    @Query("delete from UserNotification where id.userId =:userId")
    void deleteAllByIdUserId(@Param("userId") int userId);
}
