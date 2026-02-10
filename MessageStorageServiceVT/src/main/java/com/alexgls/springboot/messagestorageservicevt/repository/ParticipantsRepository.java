package com.alexgls.springboot.messagestorageservicevt.repository;

import com.alexgls.springboot.messagestorageservicevt.entity.Participants;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantsRepository extends CrudRepository<Participants, Long> {

    List<Participants> findAllByChatId(long chatId);

    @Query("select userId from Participants " +
            "where chat.id = :currentChatId and (isRemoved = false " +
            "and  isLeave = false)")
    List<Integer> findUserIdsByChatId(@Param("currentChatId") Integer chatId);

    Optional<Participants> findByChatIdAndUserId(long chatId, int userId);

    boolean existsByChatIdAndUserId(long chatId, int userId);

    @Modifying
    @Query("update  Participants  set isDeletedByUser = false " +
            "where chat.id = :currentChatId and userId = :currentUserId")
    void removeMarkIsDeletedForChatAndUserId(@Param("currentChatId") int chatId, @Param("currentUserId") int userId);


    @Modifying
    @Query("update Participants set isDeletedByUser = false " +
            "where userId in :userIds and chat.id = :chatId")
    void removeMarkIsDeletedForChatAndUserIdForAll(@Param("userIds") List<Integer> userIds,@Param("chatId") int chatId);

    @Query("select userId from Participants where chat.id = :currentChatId")
    List<Integer> findUserIdsWhoDeletedChat(@Param("currentChatId") int chatId);

    @Modifying
    @Query(value = "update participants set unread_count = unread_count + 1 where chat_id = :chatId and user_id != :senderId", nativeQuery = true)
    void incrementUpdateCountForUser(@Param("chatId") int chatId, @Param("senderId") int senderId);

    @Modifying
    @Query(value = "update participants set unread_count = 0 where chat_id = :chatId and user_id = :readerId", nativeQuery = true)
    void resetCountForCurrentUser(@Param("chatId") int chatId, @Param("readerId") int readerId);

    @Modifying
    @Query(value = """
                UPDATE participants
                SET unread_count = GREATEST(0, unread_count - :count),
                    last_read_message_id = GREATEST(last_read_message_id, :lastMessageId)
                WHERE chat_id = :chatId AND user_id = :userId
            """, nativeQuery = true)
    void updateUnreadCountAndLastMessageId(
            @Param("chatId") int chatId,
            @Param("userId") int userId,
            @Param("lastMessageId") long lastMessageId,
            @Param("count") int count
    );

    @Modifying
    @Query(value = "update participants set is_leave = true where chat_id = :chatId and user_id = :userId", nativeQuery = true)
    void leavingFromGroupByChatIdAndUserId(@Param("chatId") int chatId, @Param("userId") int userId);

    @Modifying
    @Query(value = "update participants set is_removed = true where chat_id = :chatId and user_id = :userId", nativeQuery = true)
    void removingUserFromGroupByChatIdAndUserId(@Param("chatId") int chatId, @Param("userId") int userId);
}
