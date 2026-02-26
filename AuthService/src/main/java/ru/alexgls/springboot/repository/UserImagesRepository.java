package ru.alexgls.springboot.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.alexgls.springboot.entity.user_details.UserImage;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserImagesRepository extends CrudRepository<UserImage, Integer> {
    List<UserImage> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Integer userId);

    void deleteUserImageByImageIdAndUserId(int id, int userId);

    @Query("from UserImage where userId = :userId order by createdAt desc limit 2")
    List<UserImage> findTwoLastImagesByUserIdOrderByCreatedAtDesc(@Param("userId") int userId);

    Optional<UserImage> findByImageIdAndUserId(int id, int userId);
}
