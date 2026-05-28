package com.sankalp.expensetracker.settlement.repository;

import com.sankalp.expensetracker.settlement.entity.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BalanceRepository extends JpaRepository<Balance, UUID> {
    Optional<Balance> findByGroupIdAndUserAAndUserBAndCurrency(UUID groupId, UUID userA, UUID userB, String currency);
    List<Balance> findByGroupId(UUID groupId);
    List<Balance> findByUserAOrUserB(UUID userA, UUID userB);
}
