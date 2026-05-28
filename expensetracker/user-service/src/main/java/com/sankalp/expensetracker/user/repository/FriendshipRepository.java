package com.sankalp.expensetracker.user.repository;

import com.sankalp.expensetracker.user.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    Optional<Friendship> findByRequesterIdAndAddresseeId(UUID requesterId, UUID addresseeId);

    @Query("""
            select f from Friendship f
            where (f.requesterId = :uid or f.addresseeId = :uid)
              and f.status = com.sankalp.expensetracker.user.entity.Friendship$Status.ACCEPTED
            """)
    List<Friendship> findAcceptedFriendsOf(@Param("uid") UUID userId);

    @Query("""
            select f from Friendship f
            where f.addresseeId = :uid
              and f.status = com.sankalp.expensetracker.user.entity.Friendship$Status.PENDING
            """)
    List<Friendship> findPendingFor(@Param("uid") UUID userId);
}
