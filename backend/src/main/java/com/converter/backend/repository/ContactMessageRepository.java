package com.converter.backend.repository;

import com.converter.backend.model.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    Page<ContactMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    long countByIsReadFalse();
    
    long countByIsRepliedFalse();
}
