package com.alexgls.springboot.messagestorageservicevt.repository;

import com.alexgls.springboot.messagestorageservicevt.entity.Participants;
import com.alexgls.springboot.messagestorageservicevt.repository.projection.RecipientProjection;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ParticipantsRepository extends CrudRepository<Participants, Long> {

    @Query("from Participants where chat.id = :chatId and removed = true or leave = true ")
    List<Participants> findAllRemovedParticipants(@Param("chatId") long chatId);


    @Query("from Participants " +
            "where chat.id = :currentChatId and removed is false " +
            "and leave is false")
    List<Participants> findAllByChatId(@Param("currentChatId") long chatId);

    @Query("select userId from Participants " +
            "where chat.id = :currentChatId")
    List<Integer> findUserIdsByChatId(@Param("currentChatId") long chatId);

    @Query("select userId from Participants " +
            "where chat.id = :currentChatId and leave = false and removed = false")
    List<Integer> findUserIdsByChatIdWhenUsersNotDeleted(@Param("currentChatId") long chatId);

    Optional<Participants> findByChatIdAndUserId(long chatId, int userId);

    boolean existsByChatIdAndUserId(long chatId, int userId);

    @Modifying
    @Query("update Participants set isDeletedByUser = false " +
            "where userId in :userIds and chat.id = :chatId")
    void removeMarkIsDeletedForChatAndUserIdForAll(@Param("userIds") List<Integer> userIds, @Param("chatId") long chatId);

    @Query("select userId from Participants where chat.id = :currentChatId")
    List<Integer> findUserIdsWhoDeletedChat(@Param("currentChatId") long chatId);

    @Modifying
    @Query(value = "update participants set unread_count = unread_count + 1 " +
            "where chat_id = :chatId and user_id != :senderId and (is_leave is false and is_removed is false)", nativeQuery = true)
    void incrementUpdateCountForUser(@Param("chatId") long chatId, @Param("senderId") int senderId);

    @Modifying
    @Query(value = "update participants set unread_count = GREATEST(0, unread_count - 1) " +
            "where chat_id = :chatId and user_id != :senderId", nativeQuery = true)
    void decrementUpdateCountForUser(@Param("chatId") int chatId, @Param("senderId") int senderId);

    @Modifying
    @Query(value = "update participants set unread_count = 0 where chat_id = :chatId and user_id = :readerId", nativeQuery = true)
    void resetCountForCurrentUser(@Param("chatId") long chatId, @Param("readerId") int readerId);

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
    @Query(value = "update participants set is_leave = true, remove_at = now() where chat_id = :chatId and user_id = :userId", nativeQuery = true)
    void leavingFromGroupByChatIdAndUserId(@Param("chatId") long chatId, @Param("userId") int userId);

    @Modifying
    @Query(value = "update participants set is_removed = true, remove_at = :removeAt where chat_id = :chatId " +
            "and user_id = :userId", nativeQuery = true)
    void removingUserFromGroupByChatIdAndUserId(@Param("chatId") int chatId, @Param("userId") int userId, @Param("removeAt") Timestamp removeAt);

    @Query(value = """
            select p2.user_id as second_user from participants p1
            join participants p2 on p1.chat_id = p2.chat_id
            join chats c on c.chat_id = p1.chat_id
            where p1.user_id = :userId and p2.user_id != :userId and c.is_group = false
            order by c.updated_at desc;
            """, nativeQuery = true)
    Iterable<Integer> findAllUsersWhoHadChatWith(@Param("userId") Integer currentUserId);

    @Query("select p1.userId from Participants p1 join Participants p2 " +
            "on p1.chat.id = p2.chat.id " +
            "where p1.chat.id = :chatId and p1.chat.isGroup = false " +
            "and p1.userId != p2.userId " +
            "and p1.userId != :userId")
    Optional<Integer> findRecipientByUserIdAndChatId(@Param("userId") int userId, @Param("chatId") long chatId);


    @Query(nativeQuery = true, value = """
            select p.chat_id as chatId, p.user_id as userId
            from participants p
            join chats c on p.chat_id = c.chat_id
            where p.user_id != :userId and c.is_group = false
            and c.chat_id in (:chatIds)
            """)
    List<RecipientProjection> findRecipientsProjections(int userId, List<Long> chatIds);
}
