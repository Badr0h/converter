package com.converter.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "monthly_conversion_usage", 
    indexes = {
        @Index(name = "idx_usage_user_month", columnList = "user_id, year_month DESC"),
        @Index(name = "idx_usage_year_month", columnList = "year_month")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_month", columnNames = {"user_id", "year_month"})
    })
public class MonthlyConversionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_usage_user"))
    private User user;

    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth; // Format: YYYY-MM (e.g., 2026-01)

    @Column(name = "conversion_count", nullable = false)
    private Integer conversionCount = 0;

    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
