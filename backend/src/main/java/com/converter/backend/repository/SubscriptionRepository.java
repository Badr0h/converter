package com.converter.backend.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.converter.backend.model.Subscription;
import com.converter.backend.model.User;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;


@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    @EntityGraph(attributePaths = {"plan", "user"})
    @Override
    List<Subscription> findAll();

    @EntityGraph(attributePaths = {"plan"})
    List<Subscription> findByEndDateBeforeAndStatus(LocalDate date, Subscription.Status status);

    @EntityGraph(attributePaths = {"plan"})
    List<Subscription> findByUser(User user);

    @EntityGraph(attributePaths = {"plan"})
    @Query(value = "SELECT * FROM subscriptions WHERE user_id = :userId AND status = :status ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    Optional<Subscription> findByUserAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @EntityGraph(attributePaths = {"plan"})
    List<Subscription> findByUserAndStatus(User user, Subscription.Status status);

}