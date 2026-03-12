package com.alexgls.springboot.indatabasecontentstorageservice.repository;

import com.alexgls.springboot.indatabasecontentstorageservice.entity.FileMetadata;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilesRepository extends CrudRepository<FileMetadata,Integer> {
}
