package ru.alexgls.springboot.usersmessagingservice.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.alexgls.springboot.usersmessagingservice.entity.UserOnline;

@Repository
public interface UserOnlineRepository extends CrudRepository<UserOnline, Integer> {
}
