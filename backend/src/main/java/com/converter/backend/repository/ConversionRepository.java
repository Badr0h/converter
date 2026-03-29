package com.converter.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.converter.backend.model.Conversion;
import com.converter.backend.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ConversionRepository extends JpaRepository<Conversion, Long> {

    @Query("SELECT COUNT(c) FROM Conversion c WHERE c.user.id = :userId AND c.createdAt >= :startOfMonth")
    long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("startOfMonth") LocalDateTime startOfMonth);

    /**
     * Count conversions by user and creation time (for usage limiting)
     */
    @Query("SELECT COUNT(c) FROM Conversion c WHERE c.user = :user AND c.createdAt >= :since")
    long countByUserAndCreatedAtAfter(@Param("user") User user, @Param("since") LocalDateTime since);

    @EntityGraph(attributePaths = {"user"})
    List<Conversion> findByUserId(Long userId);
    
    @EntityGraph(attributePaths = {"user"})
    Page<Conversion> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Conversion c WHERE c.createdAt >= :since")
    long countByCreatedAtAfter(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT c.user.id) FROM Conversion c WHERE c.user IS NOT NULL AND c.createdAt >= :since")
    long countDistinctUsersSince(@Param("since") LocalDateTime since);

    @Query(value = "SELECT CAST(created_at AS DATE) as day, COUNT(*) as count FROM conversions WHERE created_at >= CURRENT_DATE - INTERVAL '7 days' GROUP BY CAST(created_at AS DATE) ORDER BY day ASC", nativeQuery = true)
    List<Map<String, Object>> countByDayLast7Days();

    /**
     * Find conversion by user, input format, output format and prompt (for caching/deduplication)
     */
    Optional<Conversion> findByUserAndInputFormatAndOutputFormatAndPrompt(
        User user,
        Conversion.Format inputFormat,
        Conversion.Format outputFormat,
        String prompt
    );

    /**
     * Find most recent conversion by input format, output format and prompt (ignores user for global cache)
     */
    @Query("SELECT c FROM Conversion c WHERE c.inputFormat = :inputFormat AND c.outputFormat = :outputFormat AND c.prompt = :prompt ORDER BY c.createdAt DESC LIMIT 1")
    Optional<Conversion> findFirstByInputFormatAndOutputFormatAndPromptOrderByCreatedAtDesc(
        @Param("inputFormat") Conversion.Format inputFormat,
        @Param("outputFormat") Conversion.Format outputFormat,
        @Param("prompt") String prompt
    );
}
