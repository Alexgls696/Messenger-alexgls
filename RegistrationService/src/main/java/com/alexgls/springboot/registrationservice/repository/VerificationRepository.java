package com.alexgls.springboot.registrationservice.repository;

import com.alexgls.springboot.registrationservice.entity.InitializeUserData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationRepository extends CrudRepository<InitializeUserData, String> {
}
