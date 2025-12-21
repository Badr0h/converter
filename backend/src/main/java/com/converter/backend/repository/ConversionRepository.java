package com.converter.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.converter.backend.model.Conversion;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ConversionRepository extends JpaRepository<Conversion, Long> {

    @Query("SELECT COUNT(c) FROM Conversion c WHERE c.user.id = :userId AND c.createdAt >= :startOfMonth")
    long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("startOfMonth") LocalDateTime startOfMonth);

}
