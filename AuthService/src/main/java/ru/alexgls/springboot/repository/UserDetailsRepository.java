package ru.alexgls.springboot.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.alexgls.springboot.entity.user_details.UserDetails;

import java.util.Optional;

@Repository
public interface UserDetailsRepository extends CrudRepository<UserDetails, Integer> {
    Optional<UserDetails> findByUserId(Integer userId);

    boolean existsByUserId(Integer userId);
}
