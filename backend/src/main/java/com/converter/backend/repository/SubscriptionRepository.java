package com.converter.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.converter.backend.model.Subscription;
import java.util.List;
import java.time.LocalDate;


@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    List<Subscription> findByEndDateBeforeAndStatus(LocalDate date, Subscription.Status status);


}
