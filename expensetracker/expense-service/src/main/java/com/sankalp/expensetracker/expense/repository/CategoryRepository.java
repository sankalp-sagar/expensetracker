package com.sankalp.expensetracker.expense.repository;

import com.sankalp.expensetracker.expense.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByNameIgnoreCase(String name);

    @Query("select c from Category c where c.ownerId is null or c.ownerId = :userId order by c.name")
    List<Category> findVisible(@Param("userId") UUID userId);
}
