package com.sankalp.expensetracker.group.repository;

import com.sankalp.expensetracker.group.entity.ExpenseGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<ExpenseGroup, UUID> {
    Optional<ExpenseGroup> findByInviteCode(String inviteCode);
    boolean existsByInviteCode(String inviteCode);

    @Query("""
            select distinct g from ExpenseGroup g
            join g.members m
            where m.userId = :userId
              and g.deleted = false
              and m.deleted = false
            order by g.createdAt desc
            """)
    List<ExpenseGroup> findGroupsForUser(@Param("userId") UUID userId);
}
