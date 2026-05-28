package com.sankalp.expensetracker.settlement.repository;

import com.sankalp.expensetracker.settlement.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    @Query("""
            select s from Settlement s
            where s.payerId = :userId or s.payeeId = :userId
            order by s.settledAt desc
            """)
    Page<Settlement> findHistoryFor(@Param("userId") UUID userId, Pageable pageable);

    Page<Settlement> findByGroupIdOrderBySettledAtDesc(UUID groupId, Pageable pageable);
}
