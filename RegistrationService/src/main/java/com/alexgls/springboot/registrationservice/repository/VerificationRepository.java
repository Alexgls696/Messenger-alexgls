package com.alexgls.springboot.registrationservice.repository;

import com.alexgls.springboot.registrationservice.entity.UserData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationRepository extends CrudRepository<UserData, String> {
}
