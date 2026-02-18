package com.alexgls.springboot.notificationsservice.repository;

import com.alexgls.springboot.notificationsservice.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationsRepository extends CrudRepository<Notification, Long> {

    @Query("from Notification n join UserNotification un " +
            "on n.id = un.id.notificationId " +
            "where un.id.userId = :userId")
    Page<Notification> findAllByUserId(@Param("userId") int userId, Pageable pageable);

}
